/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.api;

import java.util.Set;

import org.jboss.pnc.rex.common.enums.StateGroup;
import org.jboss.pnc.rex.core.model.TaskGraph;
import org.jboss.pnc.rex.model.Task;

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
