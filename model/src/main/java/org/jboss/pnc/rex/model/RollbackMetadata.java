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
package org.jboss.pnc.rex.model;

import lombok.*;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor(onConstructor_ = {@ProtoFactory})
public class RollbackMetadata {

    /**
     * It's used to limit rollback process.
     */
    @Getter(onMethod_ = @ProtoField(number = 1, defaultValue = "0"))
    private int triggerCounter;

    /**
     * The amount of times this Task has through rollback. It's used for tagging ServerResponses.
     */
    @Getter(onMethod_ = @ProtoField(number = 2, defaultValue = "0"))
    private int rollbackCounter;

    /**
     * Mark to start rollback process for this particular Task.
     */
    @Getter(onMethod_ = @ProtoField(number = 3, defaultValue = "false"))
    private boolean toRollback;

    /**
     * Marked to signify the Task which started the rollback process (it was a milestone for a dependant task which got
     * rolled back). This mark is used to reset Tasks themselves to initial state when all dependants have finished
     * rollback process.
     */
    @Getter(onMethod_ = @ProtoField(number = 4, defaultValue = "false"))
    private boolean rollbackSource;

    /**
     * Number of direct dependants that have not yet finished rollback process. Non-negative number signifies rollback
     * process.
     */
    @Getter(onMethod_ = @ProtoField(number = 5, defaultValue = "-1"))
    private int unrestoredDependants;

    public static RollbackMetadata init() {
        return new RollbackMetadata(0, 0, false, false, -1);
    }

    public void incUnrestoredDependants() {
        unrestoredDependants++;
    }

    public void decUnrestoredDependants() {
        unrestoredDependants--;
    }

    public void incRollbackCounter() {
        rollbackCounter++;
    }

    public void incTriggerCounter() {
        triggerCounter++;
    }


}
