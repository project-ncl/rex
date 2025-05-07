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
package org.jboss.pnc.rex.core;

import io.quarkus.narayana.jta.TransactionSemantics;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.VersionedValue;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StopFlag;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.core.api.DependencyMessenger;
import org.jboss.pnc.rex.core.api.DependentMessenger;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.common.exceptions.ConcurrentUpdateException;
import org.jboss.pnc.rex.core.config.ApplicationConfig.Options.TaskConfiguration;
import org.jboss.pnc.rex.core.delegates.FaultToleranceDecorator;
import org.jboss.pnc.rex.core.jobs.ClearConstraintJob;
import org.jboss.pnc.rex.core.jobs.ControllerJob;
import org.jboss.pnc.rex.core.jobs.DecreaseCounterJob;
import org.jboss.pnc.rex.core.jobs.DelegateJob;
import org.jboss.pnc.rex.core.jobs.DeleteTaskJob;
import org.jboss.pnc.rex.core.jobs.DependantDeletedJob;
import org.jboss.pnc.rex.core.jobs.DependencyCancelledJob;
import org.jboss.pnc.rex.core.jobs.DependencyNotificationFailedJob;
import org.jboss.pnc.rex.core.jobs.DependencyStoppedJob;
import org.jboss.pnc.rex.core.jobs.DependencySucceededJob;
import org.jboss.pnc.rex.core.jobs.InvokeStartJob;
import org.jboss.pnc.rex.core.jobs.InvokeStopJob;
import org.jboss.pnc.rex.core.jobs.MarkForCleaningJob;
import org.jboss.pnc.rex.core.jobs.NotifyCallerJob;
import org.jboss.pnc.rex.core.jobs.PokeCleanJob;
import org.jboss.pnc.rex.core.jobs.PokeQueueJob;
import org.jboss.pnc.rex.core.jobs.TimeoutCancelClusterJob;
import org.jboss.pnc.rex.core.jobs.TreeJob;
import org.jboss.pnc.rex.core.jobs.rollback.DependantRolledBackJob;
import org.jboss.pnc.rex.core.jobs.rollback.DependencyIsToRollbackJob;
import org.jboss.pnc.rex.core.jobs.rollback.DependencyResetJob;
import org.jboss.pnc.rex.core.jobs.rollback.InvokeRollbackJob;
import org.jboss.pnc.rex.core.jobs.rollback.ResetFromMilestoneJob;
import org.jboss.pnc.rex.core.jobs.rollback.RollbackFromMilestoneJob;
import org.jboss.pnc.rex.core.jobs.rollback.RollbackTriggeredJob;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.TransitionTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static jakarta.enterprise.event.TransactionPhase.BEFORE_COMPLETION;
import static jakarta.enterprise.event.TransactionPhase.IN_PROGRESS;
import static jakarta.transaction.Transactional.TxType.MANDATORY;

@Slf4j
@ApplicationScoped
public class TaskControllerImpl implements TaskController, DependentMessenger, DependencyMessenger {

    private final TaskContainerImpl container;

    private final Event<ControllerJob> scheduleJob;

    private final TaskConfiguration config;

    private final FaultToleranceDecorator ft;


    public TaskControllerImpl(TaskContainerImpl container,
                              Event<ControllerJob> scheduleJob,
                              TaskConfiguration config,
                              FaultToleranceDecorator ftDecorator) {
        this.container = container;
        this.scheduleJob = scheduleJob;
        this.config = config;
        this.ft = ftDecorator;
    }

    private List<ControllerJob> transition(Task task) {
        Transition transition = getTransition(task);
        if (transition != null) {
            log.info("TRANSITION {}: {}", task.getName(), transition);
            task.getTimestamps().add(new TransitionTime(transition, Instant.now()));
        }

        List<ControllerJob> tasks = new ArrayList<>();

        if (transition == null) {
            return tasks;
        }

        tasks.addAll(switch (transition) {
            //no tasks
            case NEW_to_WAITING, NEW_to_ENQUEUED, WAITING_to_ENQUEUED -> List.of();

            case ENQUEUED_to_STARTING -> List.of(new InvokeStartJob(task));

            case UP_to_STOP_REQUESTED, STARTING_to_STOP_REQUESTED -> List.of(new InvokeStopJob(task));

            case STOPPING_TO_STOPPED -> List.of(new DependencyCancelledJob(task, task.getName()));

            case NEW_to_STOPPED, WAITING_to_STOPPED, ENQUEUED_to_STOPPED, ROLLBACK_TRIGGERED_to_STOPPED,
                 ROLLEDBACK_to_STOPPED, ROLLBACK_FAILED_to_STOPPED -> {
                var jobs = new ArrayList<ControllerJob>();
                switch (task.getStopFlag()) {
                    case CANCELLED -> jobs.add(new DependencyCancelledJob(task, task.getStoppedCause()));
                    case DEPENDENCY_FAILED -> jobs.add(new DependencyStoppedJob(task, task.getStoppedCause()));
                    case DEPENDENCY_NOTIFY_FAILED -> jobs.add(new DependencyNotificationFailedJob(task, task.getStoppedCause()));
                    // UNSUCCESSFUL is in FAILED transitions
                    case UNSUCCESSFUL, NONE -> {}
                }
                yield jobs;
            }

            case UP_to_FAILED, STARTING_to_START_FAILED, STOPPING_TO_STOP_FAILED, STOP_REQUESTED_to_STOP_FAILED ->
                    List.of(new DependencyStoppedJob(task, task.getName()));

            case STOP_REQUESTED_to_STOPPING -> List.of(new TimeoutCancelClusterJob(task));

            // no jobs
            case STARTING_to_UP -> List.of();

            case UP_to_SUCCESSFUL -> {
                var jobs = new ArrayList<ControllerJob>();
                // either signal DEPENDANTS immediately or after notification (NotifyCallerJob)
                if (task.getConfiguration() == null || !shouldWaitWithSuccessJob(task, transition)) {
                    jobs.add(new DependencySucceededJob(task));
                }
                yield jobs;
            }

            //region Rollback Handling
            case UP_to_ROLLBACK_TRIGGERED, STARTING_to_ROLLBACK_TRIGGERED -> List.of();

            case SUCCESSFUL_to_TO_ROLLBACK -> List.of();

            case TO_ROLLBACK_to_ROLLBACK_REQUESTED, ROLLBACK_TRIGGERED_to_ROLLBACK_REQUESTED, UP_to_ROLLBACK_REQUESTED,
                 STARTING_to_ROLLBACK_REQUESTED, START_FAILED_to_ROLLBACK_REQUESTED, FAILED_to_ROLLBACK_REQUESTED,
                 SUCCESSFUL_to_ROLLBACK_REQUESTED
                  -> List.of(new InvokeRollbackJob(task));

            // no rollback request invoked
            case NEW_to_ROLLEDBACK, WAITING_to_ROLLEDBACK, ENQUEUED_to_ROLLEDBACK, STOPPED_to_ROLLEDBACK -> List.of();

            case TO_ROLLBACK_to_ROLLEDBACK, ROLLINGBACK_to_ROLLEDBACK,
                 ROLLINGBACK_to_ROLLBACK_FAILED, ROLLBACK_REQUESTED_to_ROLLBACK_FAILED  -> {
                var jobs = new ArrayList<ControllerJob>();
                if (task.getRollbackMeta().isRollbackSource()) {
                    jobs.add(new ResetFromMilestoneJob(task));
                } else {
                    jobs.add(new DependantRolledBackJob(task));
                }
                yield jobs;
            }

            case ROLLBACK_TRIGGERED_to_ROLLEDBACK, SUCCESSFUL_to_ROLLEDBACK -> {
                var jobs = new ArrayList<ControllerJob>();
                if (task.getRollbackMeta().isRollbackSource()) {
                    jobs.add(new ResetFromMilestoneJob(task));
                }
                yield jobs;
            }

            // no jobs
            case UP_to_ROLLEDBACK, STARTING_to_ROLLEDBACK, FAILED_to_ROLLEDBACK, START_FAILED_to_ROLLEDBACK,
                 ROLLBACK_REQUESTED_to_ROLLINGBACK-> List.of();

            case TO_ROLLBACK_to_STOPPED, ROLLBACK_REQUESTED_to_STOPPED, ROLLINGBACK_to_STOPPED -> {
                var jobs = new ArrayList<ControllerJob>();

                jobs.add(new DependantRolledBackJob(task));

                switch (task.getStopFlag()) {
                    case CANCELLED -> jobs.add(new DependencyCancelledJob(task, task.getStoppedCause()));
                    case DEPENDENCY_FAILED -> jobs.add(new DependencyStoppedJob(task, task.getStoppedCause()));
                    case DEPENDENCY_NOTIFY_FAILED -> jobs.add(new DependencyNotificationFailedJob(task, task.getStoppedCause()));
                    // UNSUCCESSFUL is in FAILED transitions
                    case UNSUCCESSFUL, NONE -> {}
                }
                yield jobs;
            }

            case ROLLEDBACK_to_NEW, ROLLBACK_FAILED_to_NEW -> List.of(new DependencyResetJob(task));
            //endregion
        });

        // "StageGroup transition"
        if (transition.getBefore().getGroup() != transition.getAfter().getGroup()) {

            // add common tasks on transitioning from a specific StateGroup
            tasks.addAll(switch (transition.getBefore().getGroup()) {
                case IDLE, FINAL, ROLLBACK_TODO, ROLLBACK -> List.of();
                case QUEUED -> List.of(new PokeQueueJob());
                case RUNNING -> List.of(new DecreaseCounterJob(task), new PokeQueueJob());
            });

            // add common tasks on transitioning into a specific StateGroup
            tasks.addAll(switch (transition.getAfter().getGroup()) {
                case IDLE, QUEUED, RUNNING -> List.of();
                case ROLLBACK_TODO -> List.of(new RollbackTriggeredJob(task), new RollbackFromMilestoneJob(task));
                case ROLLBACK -> List.of(new DependencyIsToRollbackJob(task));
                case FINAL -> {
                    var jobs = new ArrayList<ControllerJob>();
                    if (shouldMarkImmediately(task, transition))
                        jobs.add(new MarkForCleaningJob(task));
                    if (shouldDeleteImmediately(task, transition))
                        jobs.add(moveToTheEndOfTransaction(new DeleteTaskJob(task)));

                    yield jobs;
                }
            });
        }

        task.setState(transition.getAfter());

        // notify the caller about a transition
        tasks.add(addNotificationRequestToTransition(task, transition));

        log.info("SCHEDULE {}: {}", task.getName(), tasks);
        return tasks;
    }

    private ControllerJob addNotificationRequestToTransition(Task task, Transition transition) {
        var notifyJob = new NotifyCallerJob(transition, task);

        // only final transitions have edge cases
        if (!transition.getAfter().isFinal()) {
            return notifyJob;
        }

        if (!shouldMarkAfterNotification(task, transition)
                && !shouldWaitWithSuccessJob(task, transition)) {
            return notifyJob;
        }

        var treeJobBuilder = TreeJob.of(notifyJob);

        // send notification but notify dependants after successful response
        if (shouldWaitWithSuccessJob(task, transition)) {
            addDependantPostNotifyJobs(task, treeJobBuilder, notifyJob);
        }

        // send notification and delete jobs after a success
        if (shouldMarkAfterNotification(task, transition)) {

            // remove constraint after notification completes
            if (task.getConstraint() != null) {
                var removeConstraint = new ClearConstraintJob(task);
                treeJobBuilder.triggerAfter(notifyJob, removeConstraint);
            }

            var cleaningJob = withTransactionAndTolerance(new MarkForCleaningJob(task, true));
            treeJobBuilder.triggerAfterSuccess(notifyJob, cleaningJob);
        }

        return treeJobBuilder.build();
    }

    private void addDependantPostNotifyJobs(Task task, TreeJob.TreeJobBuilder treeJobBuilder, NotifyCallerJob notifyJob) {
        var successJob = withTransactionAndTolerance(new DependencySucceededJob(task));
        treeJobBuilder.triggerAfterSuccess(notifyJob, successJob);
        treeJobBuilder.triggerAfter(successJob, new PokeQueueJob());

        treeJobBuilder.triggerAfterFailure(notifyJob, withTransactionAndTolerance(new DependencyNotificationFailedJob(task, task.getName())));
    }

    private DelegateJob moveToTheEndOfTransaction(ControllerJob delegate) {
        return DelegateJob.builder()
                .async(false)
                .invocationPhase(BEFORE_COMPLETION)
                .tolerant(false, null)
                .transactional(true)
                .transactionSemantics(TransactionSemantics.JOIN_EXISTING)
                .context(delegate.getContext().orElse(null))
                .delegate(delegate)
                .build();
    }
    private DelegateJob withTransactionAndTolerance(ControllerJob delegate) {
        return DelegateJob.builder()
                .async(false)
                .invocationPhase(IN_PROGRESS)
                .tolerant(true, ft)
                .transactional(true)
                .transactionSemantics(TransactionSemantics.REQUIRE_NEW)
                .context(delegate.getContext().orElse(null))
                .delegate(delegate)
                .build();
    }

    private Transition getTransition(Task task) {
        return switch (task.getState()) {
            case NEW -> {
                if (shouldRollback(task))
                    yield Transition.NEW_to_ROLLEDBACK;
                if (shouldStop(task))
                    yield Transition.NEW_to_STOPPED;
                if (shouldQueue(task))
                    yield Transition.NEW_to_ENQUEUED;
                if (shouldWait(task))
                    yield Transition.NEW_to_WAITING;
                yield null;
            }
            case WAITING -> {
                if (shouldRollback(task))
                    yield Transition.WAITING_to_ROLLEDBACK;
                if (shouldStop(task))
                    yield Transition.WAITING_to_STOPPED;
                if (shouldQueue(task))
                    yield Transition.WAITING_to_ENQUEUED;
                yield null;
            }
            case ENQUEUED -> {
                if (shouldRollback(task))
                    yield Transition.ENQUEUED_to_ROLLEDBACK;
                if (shouldStop(task))
                    yield Transition.ENQUEUED_to_STOPPED;
                if (shouldStart(task))
                    yield Transition.ENQUEUED_to_STARTING;
                yield null;
            }
            case STARTING -> {
                if (shouldRequestRollback(task))
                    yield Transition.STARTING_to_ROLLBACK_REQUESTED;
                if (shouldRollbackWithNoActions(task))
                    yield Transition.STARTING_to_ROLLEDBACK;
                if (task.getStopFlag() == StopFlag.CANCELLED)
                    yield Transition.STARTING_to_STOP_REQUESTED;
                List<ServerResponse> responses = task.getServerResponses().stream().filter(sr -> sr.getState() == State.STARTING).toList();
                if (responses.stream().anyMatch(ServerResponse::isPositive))
                    yield Transition.STARTING_to_UP;
                if (responses.stream().anyMatch(ServerResponse::isNegative))
                    if (shouldTriggerRollback(task))
                        yield Transition.STARTING_to_ROLLBACK_TRIGGERED;
                    else
                        yield Transition.STARTING_to_START_FAILED;
                yield null;
            }
            case UP -> {
                if (shouldRequestRollback(task))
                    yield Transition.UP_to_ROLLBACK_REQUESTED;
                if (shouldRollbackWithNoActions(task))
                    yield Transition.UP_to_ROLLEDBACK;
                if (task.getStopFlag() == StopFlag.CANCELLED)
                    yield Transition.UP_to_STOP_REQUESTED;
                List<ServerResponse> responses = task.getServerResponses().stream().filter(sr -> sr.getState() == State.UP).toList();
                if (responses.stream().anyMatch(ServerResponse::isPositive))
                    yield Transition.UP_to_SUCCESSFUL;
                if (responses.stream().anyMatch(ServerResponse::isNegative))
                    if (shouldTriggerRollback(task))
                        yield Transition.UP_to_ROLLBACK_TRIGGERED;
                    else
                        yield Transition.UP_to_FAILED;
                yield null;
            }
            case STOP_REQUESTED -> {
                List<ServerResponse> responses = task.getServerResponses().stream().filter(sr -> sr.getState() == State.STOP_REQUESTED).toList();
                if (responses.stream().anyMatch(ServerResponse::isPositive))
                    yield Transition.STOP_REQUESTED_to_STOPPING;
                if (responses.stream().anyMatch(ServerResponse::isNegative))
                    yield Transition.STOP_REQUESTED_to_STOP_FAILED;
                yield null;
            }
            case STOPPING -> {
                List<ServerResponse> responses = task.getServerResponses().stream().filter(sr -> sr.getState() == State.STOPPING).toList();
                if (responses.stream().anyMatch(ServerResponse::isPositive))
                    yield Transition.STOPPING_TO_STOPPED;
                if (responses.stream().anyMatch(ServerResponse::isNegative))
                    yield Transition.STOPPING_TO_STOP_FAILED;
                yield null;
            }
            case START_FAILED -> {
                if (shouldRollbackWithNoActions(task))
                    yield Transition.START_FAILED_to_ROLLEDBACK;
                if (shouldRequestRollback(task))
                    yield Transition.START_FAILED_to_ROLLBACK_REQUESTED;
                yield null;
            }
            case FAILED -> {
                if (shouldRollbackWithNoActions(task) || shouldHandleNotStartedFails(task))
                    yield Transition.FAILED_to_ROLLEDBACK;
                if (shouldRequestRollback(task))
                    yield Transition.FAILED_to_ROLLBACK_REQUESTED;
                yield null;
            }
            case STOPPED -> {
                if (shouldRollback(task) && task.getStopFlag() != StopFlag.CANCELLED)
                    yield Transition.STOPPED_to_ROLLEDBACK;
                yield null;
            }
            case SUCCESSFUL -> {
                if (shouldRollbackWithNoActions(task))
                    yield Transition.SUCCESSFUL_to_ROLLEDBACK;
                if (shouldRequestRollback(task))
                    yield Transition.SUCCESSFUL_to_ROLLBACK_REQUESTED;
                if (shouldWaitWithRollback(task))
                    yield Transition.SUCCESSFUL_to_TO_ROLLBACK;
                yield null;
            }
            case ROLLBACK_TRIGGERED -> {
                if (shouldRollbackWithNoActions(task))
                    yield Transition.ROLLBACK_TRIGGERED_to_ROLLEDBACK;
                if (shouldRequestRollback(task))
                    yield Transition.ROLLBACK_TRIGGERED_to_ROLLBACK_REQUESTED;
                if (shouldStop(task))
                    yield Transition.ROLLBACK_TRIGGERED_to_STOPPED;
                yield null;
            }
            case TO_ROLLBACK -> {
                if (shouldRollbackWithNoActions(task))
                    yield Transition.TO_ROLLBACK_to_ROLLEDBACK;
                if (shouldRequestRollback(task))
                    yield Transition.TO_ROLLBACK_to_ROLLBACK_REQUESTED;
                if (shouldStop(task))
                    yield Transition.TO_ROLLBACK_to_STOPPED;
                yield null;
            }
            case ROLLBACK_REQUESTED -> {
                int counter = task.getRollbackMeta().getRollbackCounter();
                List<ServerResponse> responses = task.getServerResponses().stream().filter(sr -> counter == sr.getRollbackCounter() && sr.getState() == State.ROLLBACK_REQUESTED).toList();
                if (responses.stream().anyMatch(ServerResponse::isPositive))
                    yield Transition.ROLLBACK_REQUESTED_to_ROLLINGBACK;
                if (responses.stream().anyMatch(ServerResponse::isNegative))
                    yield Transition.ROLLBACK_REQUESTED_to_ROLLBACK_FAILED;
                if (shouldStop(task))
                    yield Transition.ROLLBACK_REQUESTED_to_STOPPED;
                yield null;
            }
            case ROLLINGBACK -> {
                int counter = task.getRollbackMeta().getRollbackCounter();
                List<ServerResponse> responses = task.getServerResponses().stream().filter(sr -> counter == sr.getRollbackCounter() && sr.getState() == State.ROLLINGBACK).toList();
                if (responses.stream().anyMatch(ServerResponse::isPositive))
                    yield Transition.ROLLINGBACK_to_ROLLEDBACK;
                if (responses.stream().anyMatch(ServerResponse::isNegative))
                    yield Transition.ROLLINGBACK_to_ROLLBACK_FAILED;
                if (shouldStop(task))
                    yield Transition.ROLLINGBACK_to_STOPPED;
                yield null;
            }

            case ROLLEDBACK -> {
                if (shouldResetTask(task))
                    yield Transition.ROLLEDBACK_to_NEW;
                if (shouldStop(task))
                    yield Transition.ROLLEDBACK_to_STOPPED;
                yield null;
            }
            case ROLLBACK_FAILED -> {
                if (shouldResetTask(task))
                    yield Transition.ROLLBACK_FAILED_to_NEW;
                if (shouldStop(task))
                    yield Transition.ROLLBACK_FAILED_to_STOPPED;
                yield null;
            }
            // cancel states have no possible transitions
            case STOP_FAILED -> null;
        };
    }

    private boolean shouldTriggerRollback(Task task) {
        return task.getMilestoneTask() != null
                && task.getRollbackMeta().getTriggerCounter() < task.getConfiguration().getRollbackLimit()
                && !task.getRollbackMeta().isToRollback();
    }

    private boolean shouldRollback(Task task) {
        return task.getRollbackMeta().isToRollback() && task.getRollbackMeta().getUnrestoredDependants() == 0;
    }

    private boolean shouldWaitWithRollback(Task task) {
        return task.getRollbackMeta().isToRollback() && task.getRollbackMeta().getUnrestoredDependants() > 0;
    }

    private boolean shouldRequestRollback(Task task) {
        return shouldRollback(task) && task.getRemoteRollback() != null;
    }

    private boolean shouldHandleNotStartedFails(Task task) {
        return shouldRollback(task) && task.getRemoteRollback() != null
                && (task.getStopFlag() == StopFlag.DEPENDENCY_FAILED || task.getStopFlag() == StopFlag.DEPENDENCY_NOTIFY_FAILED);
    }

    private boolean shouldRollbackWithNoActions(Task task) {
        return shouldRollback(task) && task.getRemoteRollback() == null;
    }

    private boolean shouldResetTask(Task task) {
        return !task.getRollbackMeta().isToRollback();
    }

    private static boolean shouldWaitWithSuccessJob(Task task, Transition transition) {
        return task.getConfiguration() != null
                && task.getConfiguration().isDelayDependantsForFinalNotification()
                && transition.getAfter() == State.SUCCESSFUL;
    }

    private static boolean shouldWait(Task task) {
        return task.getControllerMode() == Mode.ACTIVE && task.getUnfinishedDependencies() > 0;
    }

    private boolean shouldQueue(Task task) {
        return task.getControllerMode() == Mode.ACTIVE && task.getUnfinishedDependencies() <= 0;
    }

    private boolean shouldStop(Task task) {
        return task.getStopFlag() != StopFlag.NONE;
    }

    private boolean shouldStart(Task task) {
        return task.getStarting();
    }

    private boolean shouldDeleteImmediately(Task task, Transition transition) {
        return shouldDelete(task, transition) && task.getCallerNotifications() == null;
    }

    private boolean shouldDelete(Task task, Transition transition) {
        return config.shouldClean() && task.getDependants().isEmpty() && transition.getAfter().isFinal();
    }

    private boolean shouldMarkAfterNotification(Task task, Transition transition) {
        return shouldMark(task, transition) && task.getCallerNotifications() != null;
    }

    private boolean shouldMark(Task task, Transition transition) {
        return config.shouldClean() && transition.getAfter().isFinal();
    }

    /**
     * A Task gets marked for deletion either immediately upon transitioning into a final state or after a FINAL
     * NOTIFICATION completes.
     */
    private boolean shouldMarkImmediately(Task task, Transition transition) {
        return config.shouldClean() && transition.getAfter().isFinal() && task.getCallerNotifications() == null;
    }

    private void handle(VersionedValue<Task> taskMetadata, Task task) {
        handle(taskMetadata, task, null);
    }

    private void handle(VersionedValue<Task> taskMetadata, Task task, ControllerJob[] forcedJobs) {
        List<ControllerJob> jobs = transition(task);
        if (forcedJobs != null && forcedJobs.length != 0) {
            jobs.addAll(Arrays.asList(forcedJobs));
        }

        saveChanges(taskMetadata, task);

        doExecute(jobs);
    }

    private void saveChanges(VersionedValue<Task> taskMetadata, Task task) {
        log.debug("SAVE {}: Saving task into ISPN. (ISPN-VERSION: {}) BODY: {}",
                task.getName(),
                taskMetadata.getVersion(),
                task);

        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        if (!pushed) {
            log.error("SAVE {}: Concurrent update detected. Transaction will fail.", task.getName());
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
    }

    private void doExecute(List<ControllerJob> jobs) {
        for (ControllerJob job : jobs) {
            scheduleJob.fire(job);
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void setMode(String name, Mode mode) {
        setMode(name, mode, false);
    }

    @Override
    @Transactional(MANDATORY)
    public void setMode(String name, Mode mode, boolean pokeQueue) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        Mode currentMode = task.getControllerMode();
        if (currentMode == Mode.CANCEL || (mode == Mode.IDLE && currentMode == Mode.ACTIVE)) {
            //no possible movement
            log.error("SET-MODE {}: Incorrect request. (current-mode: {}, proposed-mode: {}) ",
                    name,
                    currentMode,
                    mode);
            return;
        }
        task.setControllerMode(mode);
        if (mode == Mode.CANCEL) {
            task.setStopFlag(StopFlag.CANCELLED);
            task.setStoppedCause(task.getName());
        }

        // #3 HANDLE
        if (pokeQueue) {
            handle(taskMetadata, task, new ControllerJob[]{new PokeQueueJob()});
        } else {
            handle(taskMetadata, task);
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void accept(String name, Object response, Origin origin, boolean isRollback) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        if (assertStateForResponses(task, isRollback, true)) return;

        ServerResponse positiveResponse = new ServerResponse(task.getState(), true, response, origin, task.getRollbackMeta().getRollbackCounter());
        List<ServerResponse> responses = task.getServerResponses();
        responses.add(positiveResponse);

        // #3 HANDLE
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void fail(String name, Object response, Origin origin, boolean isRollback) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        if (assertStateForResponses(task, isRollback, false)) return;

        ServerResponse negativeResponse = new ServerResponse(task.getState(), false, response, origin, task.getRollbackMeta().getRollbackCounter());
        List<ServerResponse> responses = task.getServerResponses();
        responses.add(negativeResponse);

        task.setStopFlag(StopFlag.UNSUCCESSFUL);
        task.setStoppedCause(task.getName());

        // #3 HANDLE
        handle(taskMetadata, task);
    }

    private static boolean assertStateForResponses(Task task, boolean isRollback, boolean isPositive) {
        var acceptedRollbackStates = EnumSet.of(State.TO_ROLLBACK, State.ROLLBACK_REQUESTED, State.ROLLINGBACK);
        var acceptedStandardStates = EnumSet.of(State.STARTING, State.UP, State.STOP_REQUESTED, State.STOPPING);
        var failFast = false;

        if (isRollback) {
            if (acceptedStandardStates.contains(task.getState())) {
                if (task.getState() != State.STOPPED) {
                    throw new IllegalStateException("Got rollback response from remote entity while not in a state to do so." +
                            " Task: " + task.getName() + " State: " + task.getState());
                }

                // callback from rollback came too late (task may have been cancelled in the middle of rollback process)
                log.warn("Cannot accept rollback callback from {} because task was cancelled. State {}",
                        task.getName(),
                        task.getState());
                failFast = true;
            } else if (!acceptedRollbackStates.contains(task.getState())) {
                throw new IllegalStateException("Got response from the remote entity while not in a state to do so." +
                        " Task: " + task.getName() + " State: " + task.getState());
            }
        } else {
            if (acceptedRollbackStates.contains(task.getState())) {
                // callback from finished task came too late
                log.warn("Cannot accept {} callback from {} because task is in process of rollback. State {}",
                        isPositive ? "positive" : "negative",
                        task.getName(),
                        task.getState());
                failFast = true;
            } else if (!acceptedStandardStates.contains(task.getState())) {
                throw new IllegalStateException("Got response from the remote entity while not in a state to do so." +
                        " Task: " + task.getName() + " State: " + task.getState());
            }
        }
        return failFast;
    }

    @Override
    @Transactional(MANDATORY)
    public void dequeue(String name) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        if (task.getState() == State.ENQUEUED) {
            task.setStarting(true);
        } else {
            throw new IllegalStateException("Attempting to dequeue while not in a state to do. Task: " + task.getName() + " State: " + task.getState());
        }

        // #3 HANDLE
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void dependencySucceeded(String name) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getWithMetadata(name);
        if (taskMetadata == null) {
            throw new ConcurrentUpdateException("Task missing in critical moment. This could happen with concurrent deletion of this Task. Task: " + name);
        }
        Task task = taskMetadata.getValue();

        // #2 ALTER
        if (task.getState().isRollback()) {
            // rollback has to have fixed unfinishedDependency
            return;
        }
        task.decUnfinishedDependencies();

        // #3 HANDLE
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void dependencyStopped(String name, String cause) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getWithMetadata(name);
        if (taskMetadata == null) {
            throw new ConcurrentUpdateException("Task missing in critical moment. This could happen with concurrent deletion of this Task. Task: " + name);
        }
        Task task = taskMetadata.getValue();

        // #2 ALTER
        //maybe assert it was NONE before
        task.setStopFlag(StopFlag.DEPENDENCY_FAILED);

        if (cause == null) {
            throw new IllegalStateException("Cause must not be null. Task: " + name);
        }
        task.setStoppedCause(cause);

        // #3 HANDLE
        handle(taskMetadata, task);
    }


    @Override
    @Transactional(MANDATORY)
    public void dependencyCancelled(String name, String cause) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getWithMetadata(name);
        if (taskMetadata == null) {
            throw new ConcurrentUpdateException("Task missing in critical moment. This could happen with concurrent deletion of this Task. Task: " + name);
        }

        Task task = taskMetadata.getValue();

        // #2 ALTER
        //maybe assert it was NONE before
        task.setStopFlag(StopFlag.CANCELLED);
        if (cause == null) {
            throw new IllegalStateException("Cause must not be null. Task: " + name);
        }
        task.setStoppedCause(cause);

        // #3 HANDLE
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void dependencyNotificationFailed(String name, String cause) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getWithMetadata(name);
        if (taskMetadata == null) {
            throw new ConcurrentUpdateException("Task missing in critical moment. This could happen with concurrent deletion of this Task. Task: " + name);
        }
        Task task = taskMetadata.getValue();

        // #2 ALTER
        if (task.getState().isFinal()) {
            return;
        }
        task.setStopFlag(StopFlag.DEPENDENCY_NOTIFY_FAILED);

        if (cause == null) {
            throw new IllegalStateException("Cause must not be null. Task: " + name);
        }
        task.setStoppedCause(cause);

        // #3 HANDLE
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void dependencyReset(String name) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getWithMetadata(name);
        if (taskMetadata == null) {
            throw new ConcurrentUpdateException("Task missing in critical moment. This could happen with concurrent deletion of this Task. Task: " + name);
        }
        Task task = taskMetadata.getValue();

        // #2 ALTER
        if (!Set.of(State.ROLLEDBACK, State.ROLLBACK_FAILED).contains(task.getState())) {
            return;
        }
        resetTask(task);

        // #3 HANDLE (-> NEW)
        handle(taskMetadata, task);

        // STEP 2

        // #4 PULL
        taskMetadata = container.getWithMetadata(name);
        if (taskMetadata == null) {
            throw new ConcurrentUpdateException("Task missing in critical moment. This could happen with concurrent deletion of this Task. Task: " + name);
        }
        task = taskMetadata.getValue();

        // #5 HANDLE AGAIN (NEW -> <<NEW/WAITING/ENQUEUED>>)
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void dependencyIsToRollback(String name) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        Set<State> noRollback = Set.of(State.STOP_REQUESTED, State.STOPPING, State.STOP_FAILED);

        if (task.getState().isRollback()
                || noRollback.contains(task.getState())
                || task.getStopFlag() == StopFlag.CANCELLED) {
            // ignore if a Task is not part of Rollback process
            return;
        }

        markToRollback(task);

        // #3 HANDLE
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void dependantDeleted(String name, String deletedDependant) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getWithMetadata(name);
        if (taskMetadata == null) {
            // Task could have been already deleted DependantDeletedJob in the same transaction
            return;
        }
        Task task = taskMetadata.getValue();

        if (!task.getState().isFinal() || !task.isDisposable()) {
            log.info("TASK {}: Not in final state, removing deleted dependant {} from the task.", name, deletedDependant);
            //REMOVE dependant so it doesn't get referenced later
            task.getDependants().remove(deletedDependant);

            saveChanges(taskMetadata, task);
        } else {
            // DELETE
            doExecute(List.of(new DeleteTaskJob(task)));
        }

    }

    @Override
    @Transactional(MANDATORY)
    public void dependantRolledBack(String name) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();
        
        // #2 ALTER
        if (!task.getState().isRollback()) {
            // ignore if a Task is not part of Rollback process
            return;
        }
        task.getRollbackMeta().decUnrestoredDependants();

        // #3 HANDLE
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void markForDisposal(String name, boolean pokeCleaner) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        if (!task.getState().isFinal()) {
            throw new IllegalStateException("Attempting to mark a task for disposal while not in a final state. Task: " + task.getName() + " State: " + task.getState());
        }

        log.info("TASK {}: MARKING FOR DELETION.", name);
        task.setDisposable(true);

        // once marked for removal, we can allow scheduling tasks with same constraints
        handleOptionalConstraint(task);

        saveChanges(taskMetadata, task);

        // #3 HANDLE (there is no transition)
        if (pokeCleaner) {
            doExecute(List.of(new PokeCleanJob(task)));
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void delete(String name) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 DELETE
        if (!task.getState().isFinal()) {
            throw new IllegalStateException("Attempting to delete a task while not in a final state. Task: " + task.getName() + " State: " + task.getState());
        }

        if (!task.isDisposable()) {
            throw new IllegalStateException("Attempting to delete a task while not marked as disposable. Task: " + task.getName() + " State: " + task.getState());
        }

        log.debug("DELETE {}: Deleting task from ISPN. (ISPN-VERSION: {}) BODY: {}",
                task.getName(),
                taskMetadata.getVersion(),
                task);

        boolean deleted = container.getCache().removeWithVersion(name, taskMetadata.getVersion());
        if (!deleted) {
            log.error("DELETE {}: Concurrent update detected. Transaction will fail.", task.getName());
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }

        handleOptionalConstraint(task);

        // #3 HANDLE (there is no transition) and CASCADE
        doExecute(List.of(new DependantDeletedJob(task)));
    }

    @Override
    @Transactional(MANDATORY)
    public void clearConstraint(String name) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 CLEAR
        handleOptionalConstraint(task);
    }

    @Override
    @Transactional(MANDATORY)
    public void reset(String name) {
        // delegate
        dependencyReset(name);

        // some dependencies will Transition to ENQUEUED, therefore, there should be a Poke
        doExecute(List.of(new PokeQueueJob()));
    }

    @Override
    @Transactional(MANDATORY)
    public void primeForRollback(String name, int rollbackDependants, int prepareDependencies) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getWithMetadata(name);
        if (taskMetadata == null) {
            throw new ConcurrentUpdateException("Task missing in critical moment. This could happen with concurrent deletion of this Task. Task: " + name);
        }
        Task task = taskMetadata.getValue();

        // #2 ALTER
        if (task.getState().isRollback()) {
            log.debug("ROLLBACK {}: Updating counters of this Task. Other rollback process shares this Task.", name);
        }
        task.getRollbackMeta().setUnrestoredDependants(rollbackDependants);
        task.setUnfinishedDependencies(prepareDependencies);
        task.setDisposable(false);
        task.setStopFlag(StopFlag.NONE);
        task.setStoppedCause(null);

        // #3 SAVE (can't be handled because it causes race conditions)
        saveChanges(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void rollbackTriggered(String name) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        if (task.getState() != State.ROLLBACK_TRIGGERED) {
            throw new IllegalStateException("Task '" + task.getName() + "' not in ROLLBACK_TRIGGERED state. Can't increase counter.");
        }
        task.setStopFlag(StopFlag.NONE);
        task.getRollbackMeta().incTriggerCounter();

        // #3 HANDLE (shouldn't do Transitions though)
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void startRollbackProcess(String name) {
        VersionedValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        markToRollback(task);
        task.getRollbackMeta().setRollbackSource(true);

        // #3 HANDLE (doesn't do Transitions though)
        handle(taskMetadata, task);
    }

    @Override
    public void involveInTransaction(String name) {
        // #1 PULL
        VersionedValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 HANDLE (doesn't do Transitions though)
        handle(taskMetadata, task);
    }

    private void markToRollback(Task task) {
        task.getRollbackMeta().setToRollback(true);
    }

    private void resetTask(Task task) {
        reintroduceConstraint(task);
        // unfinishedDependencies must be preset by RollbackManager to Transition from NEW
        task.setStopFlag(StopFlag.NONE);
        task.setStoppedCause(null);
        task.setStarting(false);
        task.setDisposable(false);
        
        
        task.getRollbackMeta().setUnrestoredDependants(-1);
        task.getRollbackMeta().setRollbackSource(false);
        // state will get reset to NEW by this
        task.getRollbackMeta().setToRollback(false);
        task.getRollbackMeta().incRollbackCounter();
    }

    private void reintroduceConstraint(Task task) {
        String constraint = task.getConstraint();
        if (constraint != null) {
            String previousHolder = container.getConstraintCache().putIfAbsent(constraint, task.getName());
            if (previousHolder != null && !previousHolder.equals(task.getName())) {
                log.warn("RACE CONDITION {}: Constraint {} is already taken by '{}'. This will cause unique constraint issues.",
                        task.getName(),
                        task.getConstraint(),
                        previousHolder);
            }
        }
    }

    private void handleOptionalConstraint(Task task) {
        String constraint = task.getConstraint();
        if (constraint != null) {
            VersionedValue<String> constraintMeta = container.getConstraintCache().getWithMetadata(constraint);
            if (constraintMeta != null) {
                log.debug("TASK {}: Removing constraint '{}' from cache.", task.getName(), constraint);
                container.getConstraintCache().removeWithVersion(constraint, constraintMeta.getVersion());
            }
        }
    }
}