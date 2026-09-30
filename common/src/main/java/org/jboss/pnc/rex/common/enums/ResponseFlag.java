/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.enums;

import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * Flags that can be set by remote entity on callback which slightly change the behaviour of Task Controller.
 */
public enum ResponseFlag {
    /**
     * If set, the controller will not trigger rollback from a Milestone even if the Task could.
     *
     * APPLICABLE only to negative callbacks. The flag doesn't do anything for positive callback.
     */
    @ProtoEnumValue(0)
    SKIP_ROLLBACK
}
