package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.ForkJoinPoolUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("checkServicesToDeleteStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckServicesToDeleteStep extends CheckForOperationsInProgressStep {

    @Inject
    private ApplicationConfiguration applicationConfiguration;

    @Override
    protected Set<CloudServiceInstanceExtended> getExistingServicesToProcess(ProcessContext context) {
        List<String> servicesToDelete = context.getVariable(Variables.SERVICES_TO_DELETE);
        CloudControllerClient client = context.getControllerClient();
        int maxParallelThreads = getMaxParallelThreads(servicesToDelete);
        return ForkJoinPoolUtil.execute(maxParallelThreads, () ->  getExistingServices(client, servicesToDelete));
    }

    private Set<CloudServiceInstanceExtended> getExistingServices(CloudControllerClient client, List<String> servicesToDelete) {
        return servicesToDelete.parallelStream()
                               .map(service -> getExistingService(client, buildCloudServiceExtended(service)))
                               .filter(Objects::nonNull)
                               .collect(Collectors.toSet());
    }

    @Override
    protected Map<CloudServiceInstanceExtended, ServiceOperation>
            getServicesInProgressState(ProcessContext context, Set<CloudServiceInstanceExtended> existingServices) {
        CloudControllerClient client = context.getControllerClient();
        int maxParallelThreads = getMaxParallelThreads(existingServices);
        return ForkJoinPoolUtil.execute(maxParallelThreads, () -> getServicesInProgressStateInternal(client, existingServices));
    }

    private Map<CloudServiceInstanceExtended, ServiceOperation>
            getServicesInProgressStateInternal(CloudControllerClient client, Set<CloudServiceInstanceExtended> existingServices) {
        return existingServices.parallelStream()
                               .map(existingService -> Pair.of(existingService,
                                                               serviceOperationGetter.getLastServiceOperation(client, existingService)))
                               .filter(pair -> isServiceOperationInProgress(pair.getRight()))
                               .collect(Collectors.toConcurrentMap(Pair::getLeft, Pair::getRight));
    }

    private <E> int getMaxParallelThreads(Collection<E> collectionToProcess) {
        return Math.min(collectionToProcess.size(), applicationConfiguration.getServiceHandlingMaxParallelThreads());
    }

    private CloudServiceInstanceExtended buildCloudServiceExtended(String serviceName) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(serviceName)
                                                    .build();
    }

}
