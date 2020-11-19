package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.ForkJoinPoolUtil;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("checkServicesToDeleteStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckServicesToDeleteStep extends CollectServicesInProgressStateStep {

    private ApplicationConfiguration applicationConfiguration;

    @Inject
    CheckServicesToDeleteStep(ServiceOperationGetter serviceOperationGetter,
                              ServiceProgressReporter serviceProgressReporter,
                              ApplicationConfiguration applicationConfiguration) {
        super(serviceOperationGetter, serviceProgressReporter);
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    protected List<CloudServiceInstanceExtended> getExistingServicesInProgress(ProcessContext context) {
        List<String> servicesToDelete = context.getVariable(Variables.SERVICES_TO_DELETE);
        if (servicesToDelete.isEmpty()) {
            return Collections.emptyList();
        }
        CloudControllerClient client = context.getControllerClient();
        int maxParallelThreads = getMaxParallelThreads(servicesToDelete);
        return ForkJoinPoolUtil.execute(maxParallelThreads, () ->  doGetExistingServicesInProgress(client, servicesToDelete));
    }

    private List<CloudServiceInstanceExtended> doGetExistingServicesInProgress(CloudControllerClient client, List<String> servicesToDelete) {
        return servicesToDelete.parallelStream()
                               .map(service -> getExistingService(client, buildCloudServiceExtended(service)))
                               .filter(Objects::nonNull)
                               .filter(this::isServiceOperationInProgress)
                               .collect(Collectors.toList());
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
