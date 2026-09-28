/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jboss.pnc.rex.common.enums.Transition;

import java.time.Instant;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TransitionTimeDTO implements Comparable<TransitionTimeDTO> {

    public Transition transition;

    public Instant time;

    @Override
    public int compareTo(TransitionTimeDTO other) {
        int diff = this.getTime().compareTo(other.getTime());
        if (diff == 0) {
            return Integer.compare(this.getTransition().ordinal(), other.getTransition().ordinal());
        }
        return diff;
    }
}
