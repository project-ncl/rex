package org.jboss.pnc.scheduler.core;

import org.infinispan.client.hotrod.MetadataValue;
import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.enums.StopFlag;
import org.jboss.pnc.scheduler.common.enums.Transition;
import org.jboss.pnc.scheduler.core.api.Dependent;
import org.jboss.pnc.scheduler.core.api.ServiceContainer;
import org.jboss.pnc.scheduler.core.api.ServiceController;
import org.jboss.pnc.scheduler.core.exceptions.ConcurrentUpdateException;
import org.jboss.pnc.scheduler.core.exceptions.ServiceNotFoundException;
import org.jboss.pnc.scheduler.core.model.*;
import org.jboss.pnc.scheduler.core.tasks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceControllerImpl implements ServiceController, Dependent {

    private static final Logger logger = LoggerFactory.getLogger(ServiceControllerImpl.class);

    private ServiceName name;

    private ServiceContainerImpl container;

    private TransactionManager tm;

    public ServiceControllerImpl(ServiceName name, ServiceContainerImpl container) {
        this.name = name;
        this.container = container;
        tm = container.getTransactionManager();
    }

    @Override
    public void dependencyCreated(ServiceName dependencyName) {
        assertInTransaction();
        MetadataValue<Service> serviceMeta = container.getCache().getWithMetadata(name);
        assertNotNull(serviceMeta, new ServiceNotFoundException("Service " + name +" not found!"));
        Service service = serviceMeta.getValue();
        Service dependency = container.getService(dependencyName);

        newDependency(service, dependency);

        boolean pushed = container.getCache().replaceWithVersion(service.getName(),service,serviceMeta.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Service " + service.getName().getCanonicalName() + " was remotely updated during the transaction");
        }
        //Get get controller of the dependency and notify it that it has a new dependant
        ServiceControllerImpl dependencyController = (ServiceControllerImpl) container.getServiceController(dependencyName);
        dependencyController.dependantCreated(name);
    }

    public static void newDependency(Service dependant, Service dependency) {
        assertServiceNotNull(dependant, dependency);
        assertDependantRelationships(dependant, dependency);
        assertCanAcceptDependencies(dependant);
        //if the supposed new dependency didn't finish, increase unfinishedDependencies counter
        dependant.getDependencies().add(dependency.getName());
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
        assertNotNull(serviceMeta, new ServiceNotFoundException("Service " + name + "not found"));
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
        transition = getTransition(service);
        if (transition != null)
            System.out.println(String.format("transitioning; before: %s after: %s for service: %s", transition.getBefore().toString(),transition.getAfter().toString(), getName()));

        List<Runnable> tasks = new ArrayList<>();

        if (transition == null) {
            return tasks;
        }

        switch (transition) {
            case NEW_to_WAITING:
                //no tasks
                break;

            case NEW_to_STARTING:
            case WAITING_to_STARTING:
                tasks.add(new AsyncInvokeStartTask(tm, service, this));
                break;

            case UP_to_STOPPING:
            case STARTING_to_STOPPING:
                tasks.add(new AsyncInvokeStopTask(tm, service, this));
                break;

            case STOPPING_to_STOPPED:
                tasks.add(new DependencyCancelledTask(
                        service.getDependants().stream().map(dep -> container.getServiceControllerInternal(dep)).collect(Collectors.toSet()),
                        tm));
                break;

            case NEW_to_STOPPED:
            case WAITING_to_STOPPED:
                switch (service.getStopFlag()) {
                    case CANCELLED:
                        tasks.add(new DependencyCancelledTask(
                                service.getDependants().stream().map(dep -> container.getServiceControllerInternal(dep)).collect(Collectors.toSet()),
                                tm));
                        break;
                    case DEPENDENCY_FAILED:
                        tasks.add(new DependencyStoppedTask(
                                service.getDependants().stream().map(dep -> container.getServiceControllerInternal(dep)).collect(Collectors.toSet()),
                                tm));
                        break;

                };

            case UP_to_FAILED:
            case STARTING_to_START_FAILED:
            case STOPPING_to_STOP_FAILED:
                tasks.add(new DependencyStoppedTask(
                        service.getDependants().stream().map(dep -> container.getServiceControllerInternal(dep)).collect(Collectors.toSet()),
                        tm));
                break;

            case STARTING_to_UP:
                //no tasks
                break;

            case UP_to_SUCCESSFUL:
                tasks.add(new DependencySuccededTask(
                        service.getDependants().stream().map(dep -> container.getServiceControllerInternal(dep)).collect(Collectors.toSet()),
                        tm));
                break;
            default:
                throw new IllegalStateException("Controller returned unknown transition: " + transition);
        }
        service.setState(transition.getAfter());
        return tasks;
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
                List<ServerResponse> responses = service.getServerResponses().stream().filter(sr -> sr.getState() == State.STARTING).collect(Collectors.toList());
                if (responses.stream().anyMatch(ServerResponse::isPositive))
                    return Transition.STARTING_to_UP;
                if (responses.stream().anyMatch(ServerResponse::isNegative))
                    return Transition.STARTING_to_START_FAILED;
            }
            case UP: {
                if (service.getStopFlag() == StopFlag.CANCELLED)
                    return Transition.UP_to_STOPPING;
                List<ServerResponse> responses = service.getServerResponses().stream().filter(sr -> sr.getState() == State.UP).collect(Collectors.toList());
                if (responses.stream().anyMatch(ServerResponse::isPositive))
                    return Transition.UP_to_SUCCESSFUL;
                if (responses.stream().anyMatch(ServerResponse::isNegative))
                    return Transition.UP_to_FAILED;
            }
            case STOPPING: {
                List<ServerResponse> responses = service.getServerResponses().stream().filter(sr -> sr.getState() == State.STOPPING).collect(Collectors.toList());
                if (responses.stream().anyMatch(ServerResponse::isPositive))
                    return Transition.STOPPING_to_STOPPED;
                if (responses.stream().anyMatch(ServerResponse::isNegative))
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
        assertInTransaction();
        MetadataValue<Service> serviceMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(serviceMetadata, new ServiceNotFoundException("Service " + name + "not found"));
        Service service = serviceMetadata.getValue();

        Mode currentMode = service.getControllerMode();
        if (currentMode == mode || currentMode == Mode.CANCEL || (mode == Mode.IDLE && currentMode == Mode.ACTIVE)) {
            //no possible movement
            //TODO log
            return;
        }
        service.setControllerMode(mode);
        if (mode == Mode.CANCEL) {
            service.setStopFlag(StopFlag.CANCELLED);
        }

        List<Runnable> tasks = transition(service);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(service.getName(), service, serviceMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Service " + service.getName().getCanonicalName() + " was remotely updated during the transaction");
        }
    }

    @Override
    public State getState() {
        return null;
    }

    @Override
    public void accept() {
        assertInTransaction();
        MetadataValue<Service> serviceMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(serviceMetadata, new ServiceNotFoundException("Service " + name + "not found"));
        Service service = serviceMetadata.getValue();

        if (EnumSet.of(State.STARTING,State.UP,State.STOPPING).contains(service.getState())){
            ServerResponse positiveResponse = new ServerResponse(service.getState(), true);
            List<ServerResponse> responses = service.getServerResponses();
            responses.add(positiveResponse);
            service.setServerResponses(responses); //probably unnecessary
        } else {
            throw new IllegalStateException("Got response from the remote entity while not in a state to do so. Service: " + service.getName().getCanonicalName() + " State: " + service.getState());
        }

        List<Runnable> tasks = transition(service);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(service.getName(), service, serviceMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Service " + service.getName().getCanonicalName() + " was remotely updated during the transaction");
        }
    }

    private void doExecute(List<Runnable> tasks) {
        //run in single thread
        for (Runnable task : tasks) {
            task.run();
        }
    }

    @Override
    public void fail() {
        assertInTransaction();
        MetadataValue<Service> serviceMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(serviceMetadata, new ServiceNotFoundException("Service " + name + "not found"));
        Service service = serviceMetadata.getValue();

        if (EnumSet.of(State.STARTING,State.UP, State.STOPPING).contains(service.getState())){
            ServerResponse positiveResponse = new ServerResponse(service.getState(), false);
            List<ServerResponse> responses = service.getServerResponses();
            responses.add(positiveResponse);
            service.setServerResponses(responses); //probably unnecessary
            //maybe assert it was null before
            service.setStopFlag(StopFlag.UNSUCCESSFUL);
        } else {
            throw new IllegalStateException("Got response from the remote entity while not in a state to do so. Service: " + service.getName().getCanonicalName() + " State: " + service.getState());
        }

        List<Runnable> tasks = transition(service);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(service.getName(), service, serviceMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Service " + service.getName().getCanonicalName() + " was remotely updated during the transaction");
        }
    }

    @Override
    public void dependencySucceeded() {
        assertInTransaction();
        MetadataValue<Service> serviceMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(serviceMetadata, new ServiceNotFoundException("Service " + name + "not found"));
        Service service = serviceMetadata.getValue();

        //maybe assert it was null before
        service.decUnfinishedDependencies();

        List<Runnable> tasks = transition(service);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(service.getName(), service, serviceMetadata.getVersion());
        System.out.println("Called dep succeeded on " + name + " and pushed: " + pushed + "with prev version: " + serviceMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Service " + service.getName().getCanonicalName() + " was remotely updated during the transaction");
        }

    }

    @Override
    public void dependencyStopped() {
        assertInTransaction();
        MetadataValue<Service> serviceMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(serviceMetadata, new ServiceNotFoundException("Service " + name + "not found"));
        Service service = serviceMetadata.getValue();

        //maybe assert it was null before
        service.setStopFlag(StopFlag.DEPENDENCY_FAILED);

        List<Runnable> tasks = transition(service);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(service.getName(), service, serviceMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Service " + service.getName().getCanonicalName() + " was remotely updated during the transaction");
        }
    }


    @Override
    public void dependencyCancelled() {
        assertInTransaction();
        MetadataValue<Service> serviceMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(serviceMetadata, new ServiceNotFoundException("Service " + name + "not found"));
        Service service = serviceMetadata.getValue();

        //maybe assert it was null before
        service.setStopFlag(StopFlag.CANCELLED);

        List<Runnable> tasks = transition(service);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(service.getName(), service, serviceMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Service " + service.getName().getCanonicalName() + " was remotely updated during the transaction");
        }
    }

    private static void assertDependantRelationships(Service dependant, Service dependency){
        ServiceName dependantName = dependant.getName();
        ServiceName dependencyName = dependency.getName();

        if (dependantName.equals(dependencyName)) {
            throw new IllegalStateException("Service " + dependantName.getCanonicalName() + " cannot depend on itself");
        };

        if (dependant.getDependants().contains(dependencyName)) {
            throw new IllegalStateException("Service " + dependantName.getCanonicalName() + " cannot depend and be dependant on the "+ dependencyName.getCanonicalName());
        }
    };

    private static void assertDependencyRelationships(Service dependency, Service dependant){
        ServiceName dependencyName = dependency.getName();
        ServiceName dependantName = dependant.getName();

        if (dependantName.equals(dependencyName)) {
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
            if (tm.getTransaction() == null) {
                throw new IllegalStateException("Thread not in transaction");
            }
        } catch (SystemException e) {
            throw new IllegalStateException("Unexpected error thrown in TransactionManager", e);
        }
    }

}
