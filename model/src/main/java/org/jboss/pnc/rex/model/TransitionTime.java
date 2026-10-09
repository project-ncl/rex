/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.jboss.pnc.rex.common.enums.Transition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@EqualsAndHashCode
@AllArgsConstructor(onConstructor_ = { @ProtoFactory })
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransitionTime implements Comparable<TransitionTime> {

    @Getter(onMethod_ = { @ProtoField(number = 1) })
    private final Transition transition;

    @Getter(onMethod_ = { @ProtoField(number = 2) })
    private final Instant time;

    @Override
    public int compareTo(TransitionTime other) {
        int diff = this.getTime().compareTo(other.getTime());
        if (diff == 0) {
            return Integer.compare(this.getTransition().ordinal(), other.getTransition().ordinal());
        }
        return diff;
    }

    @Override
    public String toString() {
        return "[ " + transition + " at " + formatTime(time) + " ]";
    }

    /**
     * f.e. 9/12/23, 5:56:49 PM CEST
     * 
     * @return formatted time
     */
    private String formatTime(Instant time) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.LONG)
                .format(ZonedDateTime.ofInstant(time, ZoneId.systemDefault()));
    }
}
