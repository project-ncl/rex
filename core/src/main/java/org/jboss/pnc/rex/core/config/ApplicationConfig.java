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
package org.jboss.pnc.rex.core.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import org.jboss.pnc.rex.core.config.api.HttpConfiguration;

import java.time.Duration;

@ConfigMapping(prefix = "scheduler")
public interface ApplicationConfig {

    /**
     * Unique name of the instance.
     *
     * @return instance identifier
     */
    String name();

    @WithName("baseUrl")
    String baseUrl();


    Options options();

    /**
     * Configuration options specific to Rex scheduler
     */
    interface Options {

        /**
         * Configuration related to handling Tasks. It is also independently injectable.
         * @return task configuration
         */
        TaskConfiguration taskConfiguration();

        /**
         * Internal retry policy is mainly used for @ApplyFaultTolerance FT annotations. Its configuration should
         * reflect how should Rex behave when a Transaction fails, and it needs to be retried.
         *
         * @return internal retry policy
         */
        InternalRetryPolicy internalRetryPolicy();

        /**
         * Configuration of internal HTTP client.
         *
         * @return http configuration
         */
        HttpConfiguration httpConfiguration();

        @ConfigMapping(prefix = "scheduler.options.task-configuration") //CDI
        interface TaskConfiguration {

            /**
             * This configuration decides if automatic removal of Tasks happens.
             *
             * If true, the Tasks are deleted automatically.
             * The circumstances of cleaning depend on a Task definition:
             *  - If Notification Request IS defined. The removal is done AFTER a Notification of FINAL transition
             *    succeeds.
             *  - If Notification Request IS NOT defined. The Task is removed immediately after it is finished.
             * Additionally, Tasks are deleted only if they have no dependants, meaning that the dependency tree must be
             * almost entirely finished before removal starts to happen. The deletion is recursive from dependants to
             * dependencies so the end result is that the entire/part of tree is deleted in one transaction.
             *
             * If false, Tasks are never deleted.
             *
             * @return if should clean
             */
            @WithName("clean")
            boolean shouldClean();

            /**
             * In case the Maximum concurrency (Queue size) has never been configured, this number serves as default.
             *
             * @return default Queue size
             */
            @WithName("default-concurrency")
            @WithDefault("5")
            int defaultConcurrency();

            /**
             * Configuration related to Task heartbeats.
             *
             * @return heartbeat configuration
             */
            HeartbeatConfig heartbeat();

            @ConfigMapping(prefix = "scheduler.options.task-configuration.heartbeat") //CDI
            interface HeartbeatConfig {

                /**
                 * Jitter tolerance a HeartbeatVerifierJob should account for when deciding that the last beat is
                 * early enough.
                 *
                 * There is a lot of DB operations in the verify loop, in the beat processing and there can be
                 * concurrency issues if the remote entity beat is very close to the verify loop. This is a setting that
                 * tries to account for these operations, where speed and time depends on the underlying hardware and
                 * could mistake the beat as being too slow.
                 *
                 * @return processing tolerance
                 */
                @WithDefault("200ms")
                Duration processingTolerance();
            }
        }
    }
}
