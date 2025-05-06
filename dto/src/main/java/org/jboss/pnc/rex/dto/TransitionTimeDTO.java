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
