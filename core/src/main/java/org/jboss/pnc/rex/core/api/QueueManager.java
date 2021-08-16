package org.jboss.pnc.rex.core.api;

/**
 * Interface for interacting with internal queue. Each queue has 2 counters. Maximum counter which limits maximum amount
 * of concurrently running Tasks, and Running counter which signifies current number of concurrently running Tasks.
 *
 * If the amount of running Tasks is higher than the maximum amount, Tasks that are able to start are left in the queue
 * (in the ENQUEUED state).
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface QueueManager {
    /**
     * The method checks whether there is a room to schedule new Tasks. If the answer is no, the method does nothing,
     * otherwise, maximum possible amount of Tasks in {@link ENQUEUED} state is transitioned to {@link STARTING}.
     */
    void poke();

    /**
     * Decrease amount of running counter by one. The method is invoked when a Task transitions from {@link RUNNING}
     * state into {@link FINAL}.
     */
    void decreaseRunningCounter();

    /**
     * The method changes the maximum amount of concurrently running Tasks. If the amount is lower than the number of
     * currently running Tasks, the mentioned Tasks are unaffected but no new Tasks are scheduled. The queue is poked
     * after (and potentially starting {@link ENQUEUED} Tasks).
     *
     * @param amount new amount of maximum concurrent running Tasks
     */
    void setMaximumConcurrency(Long amount);

    /**
     * Returns current number in the maximum counter.
     * @return maximum counter value
     */
    Long getMaximumConcurrency();
}
