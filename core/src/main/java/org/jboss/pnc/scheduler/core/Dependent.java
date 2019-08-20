package org.jboss.pnc.scheduler.core;

/**
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface Dependent {
    void dependencySucceded();
    void dependencyStopped();
    void dependencyFailed();
}
