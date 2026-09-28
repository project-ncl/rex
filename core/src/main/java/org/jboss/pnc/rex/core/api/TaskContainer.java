/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.api;

/**
 * The interface Task container. Container is a registry and target for installations.
 *
 * @author Jan Michalov {@literal <jmichalo@redhat.com>}
 */
public interface TaskContainer extends TaskRegistry, TaskTarget {
    /**
     * Gets the name of the container/node
     *
     * @return name of the instance
     */
    String getDeploymentName();
}
