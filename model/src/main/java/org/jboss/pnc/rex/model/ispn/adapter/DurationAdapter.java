/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.model.ispn.adapter;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.time.Duration;

@ProtoAdapter(Duration.class)
public class DurationAdapter {

    @ProtoFactory
    public Duration create(String duration) {
        if (duration == null || duration.isEmpty()) {
            return null;
        }

        return Duration.parse(duration);
    }

    @ProtoField(value = 1, javaType = String.class)
    public String getDuration(Duration duration) {
        if (duration == null) {
            return null;
        }

        return duration.toString();
    }
}
