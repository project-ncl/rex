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

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.pnc.rex.common.enums.CJobOperation;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StateGroup;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.api.TaskRegistry;
import org.jboss.pnc.rex.core.config.ApplicationConfig.Options.TaskConfiguration;
import org.jboss.pnc.rex.core.config.ApplicationConfig.Options.TaskConfiguration.HeartbeatConfig;
import org.jboss.pnc.rex.core.delegates.FaultToleranceDecorator;
import org.jboss.pnc.rex.core.jobs.cluster.ClusteredJob;
import org.jboss.pnc.rex.core.utils.OTELUtils;
import org.jboss.pnc.rex.model.ClusteredJobReference;
import org.jboss.pnc.rex.model.HeartbeatMetadata;
import org.jboss.pnc.rex.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class HeartbeatVerifierClusterJob extends ClusteredJob {

    private static final CJobOperation CLUSTER_JOB_OPERATION_TYPE = CJobOperation.HEARTBEAT_VERIFY;
    private static final Logger log = LoggerFactory.getLogger(HeartbeatVerifierClusterJob.class);

    private final TaskRegistry taskRegistry;
    private final HeartbeatConfig config;
    private final TaskController taskController;
    private final FaultToleranceDecorator decorator;

    public HeartbeatVerifierClusterJob(Task context) {
        super(context, CLUSTER_JOB_OPERATION_TYPE);
        this.taskRegistry = CDI.current().select(TaskRegistry.class).get();
        this.config = CDI.current().select(TaskConfiguration.class).get().heartbeat();
        this.taskController = CDI.current().select(TaskController.class).get();
        this.decorator = CDI.current().select(FaultToleranceDecorator.class).get();
    }

    public HeartbeatVerifierClusterJob(ClusteredJobReference reference) {
        super(reference, CLUSTER_JOB_OPERATION_TYPE);
        this.taskRegistry = CDI.current().select(TaskRegistry.class).get();
        this.config = CDI.current().select(TaskConfiguration.class).get().heartbeat();
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

        if (refreshedTask == null) {
            return true;
        }

        // Verify that the task is in correct state because the callback may have arrived, task was forcefully cancelled
        if (refreshedTask.getState() != State.UP) {
            return true;
        }

        if (refreshedTask.getConfiguration() == null
                || !refreshedTask.getConfiguration().isHeartbeatEnable()
                || refreshedTask.getConfiguration().getHeartbeatInterval() == null) {
            return true;
        }

        Duration heartbeatInterval = refreshedTask.getConfiguration().getHeartbeatInterval();
        Duration initialDelay = refreshedTask.getConfiguration().getHeartbeatInitialDelay();
        int failureThreshold = refreshedTask.getConfiguration().getHeartbeatToleranceThreshold();

        io.opentelemetry.context.Context otel = OTELUtils.setOTELContext(reference.getTelemetry());

        BiConsumer<Context, CompletableFuture<Void>> periodicVerifier = otel.wrapConsumer(
                (Context ctx, CompletableFuture<Void> complete) -> decorator.withTolerance(
                        () -> QuarkusTransaction.requiringNew().run(
                                () -> verify(ctx, complete, heartbeatInterval, failureThreshold))));

        theTicker(heartbeatInterval, initialDelay, periodicVerifier);

        return true;
    }

    private void verify(Context context, CompletableFuture<Void> complete, Duration interval, int failureThreshold) {
        Instant timeCheck = context.get("startTime");

        // preconditions
        Task refreshedTask = taskRegistry.getTask(this.context.getName());

        if (refreshedTask == null) {
            complete.complete(null);
            return;
        }

        if (EnumSet.of(StateGroup.FINAL, StateGroup.ROLLBACK, StateGroup.ROLLBACK_TODO).contains(refreshedTask.getState().getGroup())) {
            log.info("HEARTBEAT {}: Task is in {} state, cancelling verifier.", refreshedTask.getName(), refreshedTask.getState());
            complete.complete(null);
            return;
        }

        if (!isOwned()) {
            complete.complete(null);
            return;
        }

        int failureCount = context.getOrElse("failureCount", () -> 0);
        if (refreshedTask.getHeartbeatMeta() == null || refreshedTask.getHeartbeatMeta().getLastBeat() == null) {
            failureCount++;
        } else {
            HeartbeatMetadata meta = refreshedTask.getHeartbeatMeta();
            Instant lastBeat = meta.getLastBeat();

            Duration diff = Duration.between(lastBeat, timeCheck);
            if (diff.compareTo(interval.plus(config.processingTolerance())) > 0) {
                failureCount++;
            } else {
                failureCount = 0;
            }
        }

        log.info("failure count is {} ", failureCount);
        if (failureCount > failureThreshold) {
            log.info("HEARTBEAT {}: Threshold reached, failing Task.", refreshedTask.getName());
            taskController.fail(refreshedTask.getName(), null, Origin.REX_HEARTBEAT_TIMEOUT, false);
            return;
        }

        context.put("failureCount", failureCount);
    }

    private void theTicker(Duration interval,
                               Duration initialDelay,
                               BiConsumer<Context, CompletableFuture<Void>> workAction) {
        var future = new CompletableFuture<Void>();
        Context sharedContext = Context.of();
        Cancellable cancellable = Multi.createBy().repeating().uni(() -> Uni.createFrom().<Object>nullItem().attachContext()
                        .call(ictx -> {
                            var delay = Uni.createFrom().item(ictx).onItem().delayIt();

                            return delay.by(calculateNextTick(initialDelay, interval, ictx.context().getOrElse("startTime", () -> null)));
                        })
                        .invoke(ictx -> ictx.context().put("startTime", Instant.now())) // capture startTime of the action
                        .invoke(ictx -> workAction.accept(ictx.context(), future))
                        .onFailure().invoke(() -> future.complete(null)))
                .until(item -> future.isDone())
                .subscribe()
                    .with(sharedContext, (ign) -> {});
        future.join();
        cancellable.cancel();
    }

    private Duration calculateNextTick(Duration initialDelay, Duration interval, Instant lastRunStart) {
        if (lastRunStart == null) {
            Duration firstDelay = firstDelay(initialDelay, interval);
            log.debug("HEARTBEAT {}: verification period starts in {}", this.context.getName(), firstDelay);
            return firstDelay;
        }

        Duration lastRunDuration = Duration.between(lastRunStart, Instant.now());

        // if the last run duration is higher than one interval, truncate it
        Duration truncatedDuration = truncateByInterval(lastRunDuration, interval);

        // align to next time interval
        Duration timeToNextInterval = interval.minus(truncatedDuration);

        log.debug("HEARTBEAT {}: last verification took {} next tick in {}", this.context.getName(), lastRunDuration, timeToNextInterval);
        return timeToNextInterval;
    }

    private static Duration firstDelay(Duration initialDelay, Duration interval) {
        if (initialDelay != null) {
            return initialDelay.plus(interval);
        }

        return interval;
    }

    private static Duration truncateByInterval(Duration lastRunDuration, Duration interval) {
        while (lastRunDuration.compareTo(interval) > 0) {
            lastRunDuration = lastRunDuration.minus(interval);
        }
        return lastRunDuration;
    }

}
