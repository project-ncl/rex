package org.jboss.pnc.scheduler.core;

/**
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface ServiceContainer extends ServiceRegistry, ServiceTarget {
    void shutdown();
    String getName();
    boolean isShutdown();
}
