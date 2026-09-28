/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.common;

public class Constants {

    /**
     * This is a prefix key for Max counter used in Queue implementation.
     */
    public static final String MAX_COUNTER_KEY = "MAX_CONCURRENT";

    /**
     * This is a prefix key for Running counter used in Queue implementation.
     */
    public static final String RUNNING_COUNTER_KEY = "RUNNING";

    /**
     * Separates prefix key and name in Counter.class implementations.
     */
    public static final String NAME_SEPARATOR = "-";
}
