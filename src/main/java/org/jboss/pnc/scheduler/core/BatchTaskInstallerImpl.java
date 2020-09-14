package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.BatchTaskInstaller;
import org.jboss.pnc.scheduler.core.api.TaskBuilder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BatchTaskInstallerImpl implements BatchTaskInstaller {
    private Set<TaskBuilderImpl> taskDeclarations = new HashSet<>();
    private Set<ServiceName> installed = new HashSet<>();
    private boolean committed = false;
    private TaskTargetImpl target;

    BatchTaskInstallerImpl(TaskTargetImpl target) {
        this.target = target;
    }

    @Override
    public TaskBuilder addTask(ServiceName name) {
        assertNotCommitted();
        assertNotAlreadyDeclared(name);
        return new TaskBuilderImpl(name, this);
    }

    @Override
    public void commit() {
        assertNotCommitted();
        assertRelationsAndFillInstalled();
        committed = true;
        target.install(this);
    }

    public void commitBuilder(TaskBuilderImpl builder) {
        assertNotNull(builder);
        assertFromThisBatch(builder);
        assertNotCommitted();
        taskDeclarations.add(builder);
    }

    public Set<TaskBuilderImpl> getTaskDeclarations() {
        return Collections.unmodifiableSet(taskDeclarations);
    }

    public Set<ServiceName> getInstalledTasks() {
        return Collections.unmodifiableSet(installed);
    }

    private void assertRelationsAndFillInstalled() {
        // Brand new services and their builders
        Map<ServiceName, TaskBuilderImpl> declaredNewServiceNames = taskDeclarations.stream()
                .collect(Collectors.toMap(TaskBuilderImpl::getName, x -> x));

        for (TaskBuilderImpl builder : taskDeclarations) {
            ServiceName builderName = builder.getName();
            //All dependants have to be declared as dependencies on the other side
            for (ServiceName dependant : builder.getDependants()) {
                //Skip already installed services
                if (!declaredNewServiceNames.containsKey(dependant)) {
                    installed.add(dependant);
                    continue;
                }
                TaskBuilderImpl dependantsBuilder = declaredNewServiceNames.get(dependant);
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
                TaskBuilderImpl dependantsBuilder = declaredNewServiceNames.get(dependency);
                if (!dependantsBuilder.getDependants().contains(builderName)) {
                    throw new IllegalArgumentException("Builder of '" + dependency.getCanonicalName() + "' does not have declared dependant of '" + builderName + "'");
                }
            }
        }
    }

    private void assertFromThisBatch(TaskBuilderImpl builder) {
        if (builder.getInstaller() != this) {
            throw new IllegalArgumentException("Builder is not from this batch");
        }
    }

    private void assertNotAlreadyDeclared(ServiceName name) {
        if(taskDeclarations.stream().anyMatch(sb -> sb.getName().equals(name))) {
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
