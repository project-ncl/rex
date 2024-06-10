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
package org.jboss.pnc.rex.facade.api;

/**
 * General provider for be-all end-all maintenance purposes of Rex service.
 */
public interface MaintenanceProvider {

    /**
     * The method brings back Rex to an initial state without Tasks. The Queues, internal Counters and all internal
     * caches are brought back to their factory state.
     *
     * In general 'settings' keep their value (even though they can change and have factory value). For example The
     * value of setting for Maximum Queue size is kept and not reset to initial value from config.
     *
     * Use this method with caution. The results are irreversible.
     */
    void clearEverything();
}
