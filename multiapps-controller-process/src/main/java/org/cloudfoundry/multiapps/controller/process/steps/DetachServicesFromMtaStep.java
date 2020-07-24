package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("detachServicesFromMtaStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetachServicesFromMtaStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.DETACHING_SERVICES_FROM_MTA);

        List<String> serviceNamesToDetachFromMta = context.getVariable(Variables.SERVICES_TO_DELETE);
        CloudControllerClient client = context.getControllerClient();
        List<CloudServiceInstance> servicesToDetachFromMta = getServices(serviceNamesToDetachFromMta, client);
        deleteMtaMetadataFromServices(servicesToDetachFromMta, client);

        getStepLogger().debug(Messages.SERVICES_DETACHED_FROM_MTA);
        return StepPhase.DONE;
    }

    private List<CloudServiceInstance> getServices(List<String> serviceNames, CloudControllerClient client) {
        return serviceNames.stream()
                           .map(service -> client.getServiceInstance(service, false))
                           .filter(Objects::nonNull)
                           .collect(Collectors.toList());
    }

    private void deleteMtaMetadataFromServices(List<CloudServiceInstance> servicesToDetachFromMta, CloudControllerClient client) {
        for (CloudServiceInstance serviceToDetachFromMta : servicesToDetachFromMta) {
            Metadata serviceMetadata = serviceToDetachFromMta.getV3Metadata();
            if (serviceMetadata == null) {
                continue;
            }
            getStepLogger().info(MessageFormat.format(Messages.DETACHING_SERVICE_0_FROM_MTA, serviceToDetachFromMta.getName()));
            UUID serviceGuid = serviceToDetachFromMta.getMetadata()
                                                     .getGuid();
            client.updateServiceInstanceMetadata(serviceGuid, getMetadataWithoutMtaFields(serviceMetadata));
        }
    }

    private Metadata getMetadataWithoutMtaFields(Metadata metadata) {
        return Metadata.builder()
                       .from(metadata)
                       .label(MtaMetadataLabels.MTA_ID, null)
                       .label(MtaMetadataLabels.MTA_NAMESPACE, null)
                       .annotation(MtaMetadataAnnotations.MTA_ID, null)
                       .annotation(MtaMetadataAnnotations.MTA_VERSION, null)
                       .annotation(MtaMetadataAnnotations.MTA_RESOURCE, null)
                       .annotation(MtaMetadataAnnotations.MTA_NAMESPACE, null)
                       .build();
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DETACHING_SERVICES_FROM_MTA;
    }

}
