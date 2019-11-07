package org.jboss.pnc.scheduler.core;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.ServiceContainer;
import org.jboss.pnc.scheduler.core.api.ServiceController;
import org.jboss.pnc.scheduler.core.exceptions.ConcurrentUpdateException;
import org.jboss.pnc.scheduler.core.exceptions.InvalidServiceDeclarationException;
import org.jboss.pnc.scheduler.core.exceptions.ServiceNotFoundException;
import org.jboss.pnc.scheduler.core.model.Service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.*;
import java.util.Collection;
import java.util.Set;

import static org.jboss.pnc.scheduler.core.ServiceControllerImpl.newDependant;
import static org.jboss.pnc.scheduler.core.ServiceControllerImpl.newDependency;

@ApplicationScoped
public class ServiceContainerImpl extends ServiceTargetImpl implements ServiceContainer {

    @ConfigProperty(name = "container.name", defaultValue = "undefined")
    String name;

    private RemoteCache<ServiceName, Service> services;

    public ServiceContainerImpl() {
    }

    @Inject
    public ServiceContainerImpl(RemoteCacheManager cacheManager, TransactionManager transactionManager) {
        services = cacheManager.getCache("near-services", TransactionMode.NON_DURABLE_XA, transactionManager);
    }

    //FIXME implement
    public void shutdown() {
        return;
    }

    public String getName() {
        return name;
    }

    public boolean isShutdown() {
        return false;
    }

    public ServiceController getServiceController(ServiceName service) {
        return getServiceControllerInternal(service);
    }

    public ServiceControllerImpl getServiceControllerInternal(ServiceName service) {
        return new ServiceControllerImpl(service, this);
    }

    public Service getService(ServiceName service) {
        return services.get(service);
    }

    public ServiceController getRequiredServiceController(ServiceName service) throws ServiceNotFoundException {
        Service s = getCache().get(service);
        if (s == null) {
            throw new ServiceNotFoundException("Service with name " + service.getCanonicalName() + " was not found");
        };
        return getServiceController(service);
    }

    public Service getRequiredService(ServiceName service) throws ServiceNotFoundException {
        Service s = getCache().get(service);
        if (s == null) {
            throw new ServiceNotFoundException("Service with name " + service.getCanonicalName() + " was not found");
        };
        return s;
    }

    public Collection<ServiceName> getServiceNames() {
        return services.keySet();
    }

    public TransactionManager getTransactionManager() {
        return services.getTransactionManager();
    }

    public RemoteCache<ServiceName, Service> getCache() {
        return services;
    }

    public MetadataValue<Service> getWithMetadata(ServiceName name) {
        return services.getWithMetadata(name);
    }

    @Override
    //TODO check for Circle
    protected void install(BatchServiceInstallerImpl serviceBuilder) {
        try {
            installInternal(serviceBuilder);
        } catch (RollbackException e) {
            throw new IllegalStateException("Installation rolled back.", e);
        } catch (HeuristicMixedException e) {
            throw new IllegalStateException("Part of transaction was committed and part rollback. Data corruption possible.", e);
        } catch (SystemException | NotSupportedException | HeuristicRollbackException e) {
            throw new IllegalStateException("Cannot start Transaction, unexpected error was thrown while committing transactions", e);
        }
    }

    private void installInternal(BatchServiceInstallerImpl serviceBuilder) throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        boolean joined = joinOrBeginTransation();
        try {
            Set<ServiceName> installed = serviceBuilder.getInstalledServices();

            for (ServiceBuilderImpl declaration : serviceBuilder.getServiceDeclarations()) {

                Service newService = declaration.toPartiallyFilledService();
                for (ServiceName dependant : declaration.getDependants()) {
                    if (installed.contains(dependant)) {
                        //get existing dependant
                        MetadataValue<Service> dependantServiceMetadata = getWithMetadata(dependant);
                        Service dependantService = dependantServiceMetadata.getValue();
                        if (dependantServiceMetadata == null) {
                            throw new ServiceNotFoundException("Service " + dependant.getCanonicalName() + " was not found while installing Batch");
                        }

                        //add new service as dependency to existing dependant
                        newDependency(dependantService, newService);

                        //update the dependency
                        if (!getCache().replaceWithVersion(dependant, dependantService, dependantServiceMetadata.getVersion())) {
                            throw new ConcurrentUpdateException("Service " + dependant.getCanonicalName() + " was remotely updated during the transaction");
                        }
                    }
                    //dependants are already initialized in SBImpl::toPartiallyFilledService
                }

                int unfinishedDependencies = 0;
                for (ServiceName dependency : declaration.getDependencies()) {
                    if (installed.contains(dependency)) {
                        //get existing dependency
                        MetadataValue<Service> dependencyServiceMetadata = getWithMetadata(dependency);
                        Service dependencyService = dependencyServiceMetadata.getValue();
                        if (dependencyServiceMetadata == null) {
                            throw new ServiceNotFoundException("Service " + dependency.getCanonicalName() + " was not found while installing Batch");
                        }

                        //add new service as dependant to existing dependency
                        newDependant(dependencyService, newService);

                        //update the dependency
                        if (!getCache().replaceWithVersion(dependency, dependencyService, dependencyServiceMetadata.getVersion())) {
                            throw new ConcurrentUpdateException("Service " + dependency.getCanonicalName() + " was remotely updated during the transaction");
                        }
                        if (dependencyService.getState().isFinal()) {
                            continue; //skip, unfinishedDep inc not needed
                        }
                    }
                    unfinishedDependencies++;
                }
                newService.setUnfinishedDependencies(unfinishedDependencies);

                Service previousValue = getCache().withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsent(newService.getName(), newService);
                if (previousValue != null) {
                    throw new InvalidServiceDeclarationException("Service " + newService.getName().getCanonicalName() + " already exists.");
                }
            }
            if (!joined) getTransactionManager().commit();
        } catch (RuntimeException e) {
            //rollback on failure
            if (!joined) getTransactionManager().rollback();
            throw e;
        }
    }

    /**
     *
     * @return returns true if joined
     */
    private boolean joinOrBeginTransation() throws SystemException, NotSupportedException {
        if (getTransactionManager().getTransaction() != null){
            getTransactionManager().begin();
            return false;
        }
        return true;
    }
}
