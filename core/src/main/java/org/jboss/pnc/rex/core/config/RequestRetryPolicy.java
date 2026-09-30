/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.config;

import io.smallrye.config.ConfigMapping;
import org.jboss.pnc.rex.core.config.api.MutinyRetryPolicy;

@ConfigMapping(prefix = "scheduler.options.http-configuration.request-retry-policy")
public interface RequestRetryPolicy extends MutinyRetryPolicy {
}
