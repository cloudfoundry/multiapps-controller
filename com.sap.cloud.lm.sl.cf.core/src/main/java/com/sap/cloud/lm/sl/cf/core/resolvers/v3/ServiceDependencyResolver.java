package com.sap.cloud.lm.sl.cf.core.resolvers.v3;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.mta.builders.v3.ResourceDependenciesCollector;
import com.sap.cloud.lm.sl.mta.handlers.v3.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ServiceDependencyResolver {

    private final DeploymentDescriptor descriptor;
    private final ResourceDependenciesCollector collector;

    public ServiceDependencyResolver(DeploymentDescriptor descriptor) {
        this.descriptor = descriptor;
        collector = new ResourceDependenciesCollector(descriptor, new DescriptorHandler());
    }

    public Map<Resource, Set<Resource>> getServicesToProcess(Map<Resource, Set<Resource>> serviceDependencies) {
        if (serviceDependencies == null) {
            return descriptor.getResources()
                             .stream()
                             .filter(CloudModelBuilderUtil::isService)
                             .collect(Collectors.toMap(service -> service, collector::collect));
        }

        serviceDependencies.values()
                           .removeIf(Collection::isEmpty);

        if (!serviceDependencies.isEmpty()) {
            Set<Resource> servicesToRemove = getServicesWithoutDependencies(serviceDependencies);
            for (Set<Resource> dependencies : serviceDependencies.values()) {
                dependencies.removeAll(servicesToRemove);
            }
        }
        return serviceDependencies;
    }

    public Set<Resource> getServicesWithoutDependencies(Map<Resource, Set<Resource>> serviceDependencies) {
        return serviceDependencies.entrySet()
                                  .stream()
                                  .filter(entry -> entry.getValue()
                                                        .isEmpty())
                                  .map(Map.Entry::getKey)
                                  .collect(Collectors.toSet());
    }

}
