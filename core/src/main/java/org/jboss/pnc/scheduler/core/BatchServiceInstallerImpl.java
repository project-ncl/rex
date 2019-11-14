package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.BatchServiceInstaller;
import org.jboss.pnc.scheduler.core.api.ServiceBuilder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class BatchServiceInstallerImpl implements BatchServiceInstaller {
    private Set<ServiceBuilderImpl> serviceDeclarations = new HashSet<>();
    private Set<ServiceName> installed = new HashSet<>();
    private boolean committed = false;
    private ServiceTargetImpl target;

    BatchServiceInstallerImpl(ServiceTargetImpl target) {
        this.target = target;
    }

    @Override
    public ServiceBuilder addService(ServiceName name) {
        assertNotCommitted();
        assertNotAlreadyDeclared(name);
        return new ServiceBuilderImpl(name, this);
    }

    @Override
    public void commit() {
        assertNotCommitted();
        assertRelationsAndFillInstalled();
        committed = true;
        target.install(this);
    }

    public void commitBuilder(ServiceBuilderImpl builder) {
        assertNotNull(builder);
        assertFromThisBatch(builder);
        assertNotCommitted();
        serviceDeclarations.add(builder);
    }

    public Set<ServiceBuilderImpl> getServiceDeclarations() {
        return Collections.unmodifiableSet(serviceDeclarations);
    }

    public Set<ServiceName> getInstalledServices() {
        return Collections.unmodifiableSet(installed);
    }

    private void assertRelationsAndFillInstalled() {
        // Brand new services and their builders
        Map<ServiceName, ServiceBuilderImpl> declaredNewServiceNames = serviceDeclarations.stream()
                .collect(Collectors.toMap(ServiceBuilderImpl::getName,x -> x));

        for (ServiceBuilderImpl builder : serviceDeclarations) {
            ServiceName builderName = builder.getName();
            //All dependants have to be declared as dependencies on the other side
            for (ServiceName dependant : builder.getDependants()) {
                //Skip already installed services
                if (!declaredNewServiceNames.containsKey(dependant)) {
                    installed.add(dependant);
                    continue;
                }
                ServiceBuilderImpl dependantsBuilder = declaredNewServiceNames.get(dependant);
                if (!dependantsBuilder.getDependencies().contains(builderName)) {
                    throw new IllegalArgumentException("Builder of '" + dependant.getCanonicalName() + "' does not have declared dependency for '" + builderName + "'");
                }
            }

            //All dependencies have to be declared as dependants on the other side
            for (ServiceName dependency : builder.getDependencies()) {
                //Skip already installed services
                if (!declaredNewServiceNames.containsKey(dependency)) {
                    installed.add(dependency);
                    continue;
                }
                ServiceBuilderImpl dependantsBuilder = declaredNewServiceNames.get(dependency);
                if (!dependantsBuilder.getDependants().contains(builderName)) {
                    throw new IllegalArgumentException("Builder of '" + dependency.getCanonicalName() + "' does not have declared dependant of '" + builderName + "'");
                }
            }
        }
    }

    private void assertFromThisBatch(ServiceBuilderImpl builder) {
        if (builder.getInstaller() != this) {
            throw new IllegalArgumentException("Builder is not from this batch");
        }
    }

    private void assertNotAlreadyDeclared(ServiceName name) {
        if(serviceDeclarations.stream().anyMatch(sb -> sb.getName().equals(name))) {
            throw new IllegalArgumentException("Cannot create SB with the same name");
        }
    }

    private <T> T assertNotNull(T object) {
        if (object == null) {
            throw new IllegalArgumentException("Parameter of class: "+ object.getClass().getCanonicalName() + " cannot be null.");
        }
        return object;
    }

    private void assertNotCommitted() {
        if (committed) {
            throw new IllegalArgumentException("Batch is already committed. Please refrain from using it again");
        }
    }
}
