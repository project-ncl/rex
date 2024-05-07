/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.rex.core.common;

import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.core.api.TaskContainer;
import org.jboss.pnc.rex.core.common.TransitionRecorder.Tuple;
import org.jboss.pnc.rex.model.Task;

import jakarta.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
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

    public static void waitTillTasksAreFinishedWith(State state, String... tasks) {
        TransitionRecorder recorder = CDI.current().select(TransitionRecorder.class).get();
        BlockingQueue<Tuple<String, State>> queue = recorder.subscribe(List.of(tasks));
        try {
            for (int i = 0; i < tasks.length; i++) {
                var tuple = queue.poll(10, TimeUnit.SECONDS);
                if (tuple == null) {
                    throw new AssertionError("Timeout " + 10 + " " + TimeUnit.SECONDS + " reached while waiting for some task to finish");
                }
                if (tuple.second() != state) {
                    throw new AssertionError("Task " + tuple.first() + " didn't have correct state. (" + state + " vs. " + tuple.second() + ")");
                }
            }
        } catch (InterruptedException e) {
            // shouldn't happen
        }

    }
}
