package org.jboss.pnc.rex.core.config;

import io.smallrye.config.ConfigMapping;
import org.jboss.pnc.rex.core.config.api.MPRetryPolicy;

@ConfigMapping(prefix = "scheduler.options.internal-retry-policy")
public interface InternalRetryPolicy extends MPRetryPolicy {
}
