/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.core.jobs;

import io.opentelemetry.context.Context;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.pnc.rex.common.enums.CJobOperation;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.api.TaskRegistry;
import org.jboss.pnc.rex.core.delegates.FaultToleranceDecorator;
import org.jboss.pnc.rex.core.jobs.cluster.ClusteredJob;
import org.jboss.pnc.rex.core.utils.OTELUtils;
import org.jboss.pnc.rex.model.ClusteredJobReference;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.TransitionTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import static java.time.Duration.between;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

public class TimeoutCancelClusterJob extends ClusteredJob {

    private static final CJobOperation CLUSTER_JOB_OPERATION_TYPE = CJobOperation.CANCEL_TIMEOUT;
    private static final Logger log = LoggerFactory.getLogger(TimeoutCancelClusterJob.class);

    private final Vertx vertx;
    private final TaskRegistry taskRegistry;
    private final TaskController taskController;
    private final FaultToleranceDecorator decorator;

    public TimeoutCancelClusterJob(Task context) {
        super(context, CLUSTER_JOB_OPERATION_TYPE);
        this.vertx = CDI.current().select(Vertx.class).get();
        this.taskRegistry = CDI.current().select(TaskRegistry.class).get();
        this.taskController = CDI.current().select(TaskController.class).get();
        this.decorator = CDI.current().select(FaultToleranceDecorator.class).get();
    }

    public TimeoutCancelClusterJob(ClusteredJobReference reference) {
        super(reference, CLUSTER_JOB_OPERATION_TYPE);
        this.vertx = CDI.current().select(Vertx.class).get();
        this.taskRegistry = CDI.current().select(TaskRegistry.class).get();
        this.taskController = CDI.current().select(TaskController.class).get();
        this.decorator = CDI.current().select(FaultToleranceDecorator.class).get();
    }

    @Override
    public boolean execute() {
        // If the Job is recreated after failover, the Task it was associated with could've been already deleted.
        if (context == null) {
            return true;
        }

        // Do not rely on the Task from Constructor because of possibly outdated data
        Task refreshedTask = taskRegistry.getTask(context.getName());

        // Verify that the task is in correct state because the callback may have arrived, task was forcefully cancelled
        // by other means or some mistake happened.
        if (refreshedTask != null && refreshedTask.getState() != State.STOPPING) {
            return true;
        }
        Duration timeoutDelta = refreshedTask.getConfiguration().getCancelTimeout();
        if (timeoutDelta == null) {
            return true;
        }

        Instant instant = refreshedTask.getTimestamps()
            .stream()
            .filter(transitionTime -> transitionTime.getTransition().getAfter() == State.STOPPING)
            .map(TransitionTime::getTime)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Task in state STOPPING without timed Transition."));
        Instant supposedStart = instant.plus(timeoutDelta);
        if (supposedStart.isBefore(Instant.now())) {
            log.info("Running cancel timeout for task {} immediately.", context.getName());
        } else {
            log.info("Setting cancel timer to {}",
                ZonedDateTime.ofInstant(supposedStart, ZoneId.systemDefault()).format(ISO_OFFSET_DATE_TIME));

            Uni.createFrom().nullItem().onItem().delayIt().by(between(Instant.now(), supposedStart)).await().indefinitely();
        }

        // verify owner didn't change
        if (!isOwned()) {
            return true;
        }

        Context otelContext = OTELUtils.setOTELContext(reference.getTelemetry());

        // wrap with OTEL + Fault tolerance + transaction
        return otelContext.wrapSupplier( // OTEL
                () -> decorator.withTolerance( // FT
                    () -> QuarkusTransaction.requiringNew() // Transaction
                        .call(this::executeTimeout))).get(); // Run
    }

    private boolean executeTimeout() {
        Task refreshedTask = taskRegistry.getTask(context.getName());

        // Verify that the task is in correct state because the callback may have arrived, task was forcefully cancelled
        // by other means or some mistake happened. It also could've been processed and is deleted.
        if (refreshedTask == null || refreshedTask.getState() != State.STOPPING || !manager.isOwned(reference.getId())) {
            return true;
        }
        log.info("TIMEOUT: Timing out task {}.", refreshedTask.getName());
        taskController.accept(refreshedTask.getName(), null, Origin.REX_TIMEOUT, false, Set.of());
        return true;
    }
}
