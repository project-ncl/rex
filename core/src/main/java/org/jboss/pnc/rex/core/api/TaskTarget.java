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
package org.jboss.pnc.rex.core.api;

import org.jboss.pnc.rex.common.enums.StateGroup;
import org.jboss.pnc.rex.core.model.TaskGraph;
import org.jboss.pnc.rex.model.Task;

import java.util.Set;

/**
 * Target where Tasks are installed into and removed from.
 *
 * @author Jan Michalov {@literal <jmichalo@redhat.com>}
 */
public interface TaskTarget {

    /**
     * Starts scheduling a graph of Tasks. Vertices have to be NEW tasks. Edges can be between EXISTING or NEW tasks.
     * If an edge would introduce dependency relationship where the dependant is an EXISTING Task in
     * {@link StateGroup#FINAL} or {@link StateGroup#RUNNING} state, it will get rejected.
     *
     * @param taskGraph graph of task consisting of edges and vertices
     * @return new scheduled tasks
     */
    Set<Task> install(TaskGraph taskGraph);
}
