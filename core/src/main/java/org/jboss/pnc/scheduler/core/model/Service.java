package org.jboss.pnc.scheduler.core.model;

import lombok.*;
import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.ServiceController;

import java.util.Set;

/**
 * Job(Service) is an entity that holds data of remotely executed task.
 *
 * ServiceController manipulates Service's data.
 *
 * Service has to be installed through BatchServiceInstaller that is provided by ServiceTarget. After installation, Service's
 * data is held by Infinispan cache inside ServiceRegistry/ServiceContainer.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
public class Service {
    /**
     * Uniquely identifies a Service and serves as a key in Infinispan cache.
     */
    private final ServiceName name;

    /**
     * Holds data for communication with remote entity.
     *
     * f.e. to start/stop remote execution
     */
    private RemoteAPI remoteEndpoints;

    /**
     * ServiceController mode.
     */
    private ServiceController.Mode controllerMode;

    /**
     * Current state of a service. Default is ServiceController.State.ACTIVE.
     */
    private ServiceController.State state;

    /**
     * Services that are dependent on this job(Service).
     *
     * Parents of this Service.
     */
    @Singular
    private Set<ServiceName> dependants;

    @Singular
    private Set<ServiceName> dependencies;
    /**
     * Number of unfinishedDependencies. Service can't remotely start if the number is positive.
     */
    private int unfinishedDependencies;

    /**
     * Payload sent to remote entity.
     */
    private String payload;

    private StopFlag stopFlag;

    private ServerResponse serverResponse;

    public void incUnfinishedDependencies() {
        unfinishedDependencies++;
    }
}
