package org.jboss.pnc.scheduler.core;

import org.infinispan.client.hotrod.MetadataValue;
import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.Dependent;
import org.jboss.pnc.scheduler.core.api.ServiceContainer;
import org.jboss.pnc.scheduler.core.api.ServiceController;
import org.jboss.pnc.scheduler.core.exceptions.ConcurrentUpdateException;
import org.jboss.pnc.scheduler.core.exceptions.ServiceNotFoundException;
import org.jboss.pnc.scheduler.core.model.*;

import javax.transaction.SystemException;
import java.util.List;
import java.util.stream.Stream;

public class ServiceControllerImpl implements ServiceController, Dependent {

    private ServiceName name;

    private ServiceContainerImpl container;

    public ServiceControllerImpl(ServiceName name, ServiceContainerImpl container) {
        this.name = name;
        this.container = container;
    }

    @Override
    public void dependencyCreated(ServiceName dependencyName) {
        assertInTransaction();
        MetadataValue<Service> serviceMeta = container.getCache().getWithMetadata(name);
        Service service = serviceMeta.getValue();
        Service dependency = container.getService(dependencyName);
        newDependency(service, dependency);
        boolean pushed = container.getCache().replaceWithVersion(service.getName(),service,serviceMeta.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Service " + service.getName().getCanonicalName() + " was remotely updated during the transaction");
        }
        //Get get controller of the dependency and notify it that it has new dependant
        ServiceControllerImpl dependencyController = (ServiceControllerImpl) container.getServiceController(dependencyName);
        dependencyController.dependantCreated(name);
    }

    public static void newDependency(Service dependant, Service dependency) {
        assertServiceNotNull(dependant, dependency);
        assertDependantRelationships(dependant, dependency);
        assertCanAcceptDependencies(dependant);
        //if the supposed new dependency didn't finish, increase unfinishedDependencies counter
        dependant.getDependants().add(dependency.getName());
        if (!dependency.getState().isFinal()) {
            dependant.incUnfinishedDependencies();
        }
    }

    public static void newDependant(Service dependency, Service dependant) {
        assertServiceNotNull(dependant, dependency);
        assertDependencyRelationships(dependency, dependant);
        assertCanAcceptDependencies(dependant);
        dependency.getDependants().add(dependant.getName());
    }

    public void dependantCreated(ServiceName dependantName) {
        assertInTransaction();
        MetadataValue<Service> serviceMeta = container.getCache().getWithMetadata(name);
        Service service = serviceMeta.getValue();
        Service dependant = container.getService(dependantName);

        newDependant(service, dependant);

        boolean pushed = container.getCache().replaceWithVersion(service.getName(), service, serviceMeta.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Service " + service.getName().getCanonicalName() + " was remotely updated during the transaction");
        }
    }

    private List<Runnable> transition(Service service) {
        assertInTransaction();
        Transition transition;
        do {
            transition = getTransition(service);
        }
    }

    private Transition getTransition(Service service) {
        Mode mode = service.getControllerMode();
        switch (service.getState()) {
            case NEW: {
                if (shouldStop(service))
                    return Transition.NEW_to_STOPPED;
                if (shouldStart(service))
                    return Transition.NEW_to_STARTING;
                if (mode == Mode.ACTIVE && service.getUnfinishedDependencies() > 0)
                    return Transition.NEW_to_WAITING;
            }
            case WAITING: {
                if (shouldStop(service))
                    return Transition.WAITING_to_STOPPED;
                if (shouldStart(service))
                    return Transition.WAITING_to_STARTING;
            }
            case STARTING: {
                if (service.getStopFlag() == StopFlag.CANCELLED)
                    return Transition.STARTING_to_STOPPING;
                Stream<ServerResponse> responses = service.getServerResponses().stream().filter(sr -> sr.getState() == State.STARTING);
                if (responses.anyMatch(ServerResponse::isPositive))
                    return Transition.STARTING_to_UP;
                if (responses.anyMatch(ServerResponse::isNegative))
                    return Transition.STARTING_to_START_FAILED;
            }
            case UP: {
                if (service.getStopFlag() == StopFlag.CANCELLED)
                    return Transition.UP_to_STOPPING;
                Stream<ServerResponse> responses = service.getServerResponses().stream().filter(sr -> sr.getState() == State.UP);
                if (responses.anyMatch(ServerResponse::isPositive))
                    return Transition.UP_to_SUCCESSFUL;
                if (responses.anyMatch(ServerResponse::isNegative))
                    return Transition.UP_to_FAILED;
            }
            case STOPPING: {
                Stream<ServerResponse> responses = service.getServerResponses().stream().filter(sr -> sr.getState() == State.STOPPING);
                if (responses.anyMatch(ServerResponse::isPositive))
                    return Transition.STOPPING_to_STOPPED;
                if (responses.anyMatch(ServerResponse::isNegative))
                    return Transition.STOPPING_to_STOP_FAILED;
            }
            // final states have no possible transitions
            case START_FAILED:
            case STOP_FAILED:
            case FAILED:
            case SUCCESSFUL:
            case STOPPED:
                break;
            default:
                throw new IllegalStateException("Service is in unrecognized state.");
        }
        return null;
    }

    private boolean shouldStart(Service service) {
        return service.getControllerMode() == Mode.ACTIVE && service.getUnfinishedDependencies() <= 0;
    }

    private boolean shouldStop(Service service) {
        return service.getStopFlag() != StopFlag.NONE;
    }

    private static void assertCanAcceptDependencies(Service service) {
        if (!service.getState().isIdle()) {
            throw new IllegalStateException(String.format("Service %s cannot accept a dependency",
                    service.getName().getCanonicalName()));
        }
    }

    @Override
    public ServiceName getName() {
        return name;
    }

    @Override
    public ServiceContainer getContainer() {
        return container;
    }

    @Override
    public Mode getMode() {
        return getContainer().getRequiredService(name).getControllerMode();
    }

    @Override
    public void setMode(Mode mode) {

    }

    @Override
    public State getState() {
        return null;
    }

    @Override
    public void accept() {

    }

    @Override
    public void fail() {

    }

    @Override
    public void dependencySucceeded() {

    }

    @Override
    public void dependencyStopped() {

    }

    @Override
    public void dependencyFailed() {

    }

    @Override
    public void dependencyCancelled() {

    }

    private static void assertDependantRelationships(Service dependant, Service dependency){
        ServiceName dependantName = dependant.getName();
        ServiceName dependencyName = dependency.getName();

        if (!dependantName.equals(dependencyName)) {
            throw new IllegalStateException("Service " + dependantName.getCanonicalName() + " cannot depend on itself");
        };

        if (dependant.getDependants().contains(dependencyName)) {
            throw new IllegalStateException("Service " + dependantName.getCanonicalName() + " cannot depend and be dependant on the "+ dependencyName.getCanonicalName());
        }
    };

    private static void assertDependencyRelationships(Service dependency, Service dependant){
        ServiceName dependencyName = dependency.getName();
        ServiceName dependantName = dependant.getName();

        if (!dependantName.equals(dependencyName)) {
            throw new IllegalStateException("Service " + dependencyName.getCanonicalName() + " cannot depend on itself");
        };

        if (dependant.getDependants().contains(dependencyName)) {
            throw new IllegalStateException("Service " + dependencyName.getCanonicalName() + " cannot depend and be dependant on the "+ dependantName.getCanonicalName());
        }
    };

    private static void assertServiceNotNull(Service... services) {
        for (Service service : services) {
            assertNotNull(service, new ServiceNotFoundException("Service " + service.getName().getCanonicalName() + "was not found!"));
        }
    }

    private static <T> T assertNotNull(T object) {
        return assertNotNull(object, new IllegalArgumentException("Parameter of class: "+ object.getClass().getCanonicalName() + " cannot be null."));
    }

    private static <T> T assertNotNull(T object, RuntimeException e) {
        if (object == null) {
            throw e;
        }
        return object;
    }

    private void assertInTransaction() {
        try {
            if (container.getTransactionManager().getTransaction() == null) {
                throw new IllegalStateException("Thread not in transaction");
            }
        } catch (SystemException e) {
            throw new IllegalStateException("Unexpected error thrown in TransactionManager", e);
        }
    }

}
