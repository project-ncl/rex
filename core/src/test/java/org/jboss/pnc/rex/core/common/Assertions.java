package org.jboss.pnc.rex.core.common;

import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.core.api.TaskContainer;
import org.jboss.pnc.rex.model.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class Assertions {
    public static void assertCorrectTaskRelations(Task testing,
                                                  int unfinishedDeps,
                                                  String[] dependants,
                                                  String[] dependencies) {
        assertThat(testing)
                .isNotNull();
        assertThat(testing.getUnfinishedDependencies())
                .isEqualTo(unfinishedDeps);
        if (dependants != null) {
            assertThat(testing.getDependants())
                    .containsExactlyInAnyOrder(dependants);
        }
        if (dependencies != null) {
            assertThat(testing.getDependencies())
                    .containsExactlyInAnyOrder(dependencies);
        }
    }

    public static void waitTillTasksAre(State state, TaskContainer container, Task... tasks) {
        waitTillTasksAre(state, container, 5, Arrays.stream(tasks).map(Task::getName).toArray(String[]::new));
    }
    public static void waitTillTasksAre(State state, TaskContainer container, int timeout, Task... tasks) {
        waitTillTasksAre(state, container, timeout, Arrays.stream(tasks).map(Task::getName).toArray(String[]::new));
    }
    public static void waitTillTasksAre(State state, TaskContainer container, String... tasks) {
        waitTillTasksAre(state, container, 10, tasks);
    }

    public static void waitTillTasksAre(State state, TaskContainer container, int timeout, String... strings) {
        List<String> fine = new ArrayList<>(Arrays.asList(strings));
        waitSynchronouslyFor(() -> {
            Iterator<String> iterator = fine.iterator();
            while (iterator.hasNext()) {
                Task s = container.getTask(iterator.next());
                if (s.getState().equals(state))
                    iterator.remove();
            }
            return fine.isEmpty();
        }, timeout, TimeUnit.SECONDS);
    }

    public static void waitSynchronouslyFor(Supplier<Boolean> condition, long timeout, TimeUnit timeUnit) {
        long stopTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        do {
            try {
                TimeUnit.MILLISECONDS.sleep(25);
            } catch (InterruptedException e) {
                throw new AssertionError("Unexpected interruption", e);
            }
            if (System.currentTimeMillis() > stopTime) {
                throw new AssertionError("Timeout " + timeout + " " + timeUnit + " reached while waiting for condition");
            }
        } while (!condition.get());
    }
}
