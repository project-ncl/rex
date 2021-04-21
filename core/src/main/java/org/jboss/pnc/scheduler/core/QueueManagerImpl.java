package org.jboss.pnc.scheduler.core;

import org.infinispan.client.hotrod.MetadataValue;
import org.jboss.pnc.scheduler.core.api.QueueManager;
import org.jboss.pnc.scheduler.core.api.TaskContainer;
import org.jboss.pnc.scheduler.core.api.TaskController;
import org.jboss.pnc.scheduler.core.counter.Counter;
import org.jboss.pnc.scheduler.core.counter.MaxConcurrent;
import org.jboss.pnc.scheduler.core.counter.Running;
import org.jboss.pnc.scheduler.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import java.util.ConcurrentModificationException;
import java.util.List;

import static javax.transaction.Transactional.TxType.MANDATORY;

@ApplicationScoped
public class QueueManagerImpl implements QueueManager {

    private static final Logger log = LoggerFactory.getLogger(QueueManagerImpl.class);

    private final Counter max;
    private final Counter running;
    private final TaskContainer container;
    private final TaskController controller;

    public QueueManagerImpl(@MaxConcurrent Counter max, @Running Counter running, TaskContainer container, TaskController controller) {
        this.max = max;
        this.running = running;
        this.container = container;
        this.controller = controller;
    }

    @Override
    @Transactional
    public void poke() {
        MetadataValue<Long> maxMetadata = max.getMetadataValue();
        Long maxValue = maxMetadata.getValue();

        MetadataValue<Long> runningMetadata = running.getMetadataValue();
        Long runningValue = runningMetadata.getValue();

        if (runningValue >= maxValue) {
            // maximum amount of Tasks are running
            return;
        }

        long freeSpace = maxValue - runningValue;

        List<Task> randomEnqueuedTasks = container.getEnqueuedTasks(freeSpace);

        if (randomEnqueuedTasks.size() == 0) {
            return;
        }
        randomEnqueuedTasks.forEach(task -> controller.dequeue(task.getName()));
        log.info("Increasing running with version " + runningMetadata.getVersion() + " counter from "
                + runningValue + " to " + (runningValue + randomEnqueuedTasks.size()));
        if (!running.replaceValue(runningMetadata, runningValue + randomEnqueuedTasks.size())) {
            throw new ConcurrentModificationException("Running cache was modified concurrently.");
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void decreaseRunningCounter() {
        MetadataValue<Long> runningMetadata = running.getMetadataValue();
        long runningValue = runningMetadata.getValue() - 1;
        log.info("Decreasing running with version "+ runningMetadata.getVersion() + " from " + runningMetadata.getValue() + " to " + runningValue);
        if (!running.replaceValue(runningMetadata, runningValue)) {
            throw new ConcurrentModificationException("Running cache was modified concurrently.");
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void setMaximumConcurrency(Long amount) {
        MetadataValue<Long> maxMetadata = max.getMetadataValue();
        if (maxMetadata == null) {
            max.initialize(amount);
        } else {
            max.replaceValue(maxMetadata, amount);
        }
        poke();
    }

    @Override
    public Long getMaximumConcurrency() {
        MetadataValue<Long> meta = max.getMetadataValue();
        return meta == null ? null : meta.getValue();
    }
}
