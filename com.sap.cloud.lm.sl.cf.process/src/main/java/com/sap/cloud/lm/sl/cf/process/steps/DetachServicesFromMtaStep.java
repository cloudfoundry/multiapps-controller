package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.v3.Metadata;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataAnnotations;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataLabels;
import com.sap.cloud.lm.sl.cf.process.Messages;

@Named("detachServicesFromMtaStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetachServicesFromMtaStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.DETACHING_SERVICES_FROM_MTA);

        List<String> serviceNamesToDetachFromMta = StepsUtil.getServicesToDelete(execution.getContext());
        CloudControllerClient client = execution.getControllerClient();
        List<CloudServiceInstance> servicesToDetachFromMta = getServices(serviceNamesToDetachFromMta, client);
        deleteMtaMetadataFromServices(servicesToDetachFromMta, client);

        getStepLogger().debug(Messages.SERVICES_DETACHED_FROM_MTA);
        return StepPhase.DONE;
    }

    private List<CloudServiceInstance> getServices(List<String> serviceNames, CloudControllerClient client) {
        return serviceNames.stream()
                           .map(client::getServiceInstance)
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
            client.updateServiceMetadata(serviceGuid, getMetadataWithEmptyMtaFields(serviceMetadata));
        }
    }

    // FIXME Once the fix for deleting metadata is available in cf-java-client, the whole MTA metadata must be deleted instead of setting an
    // empty MTA_ID value as a way to exclude entities from being queried
    private Metadata getMetadataWithEmptyMtaFields(Metadata metadata) {
        return Metadata.builder()
                       .from(metadata)
                       .label(MtaMetadataLabels.MTA_ID, StringUtils.EMPTY)
                       .label(MtaMetadataLabels.MTA_VERSION, StringUtils.EMPTY)
                       .annotation(MtaMetadataAnnotations.MTA_RESOURCE, StringUtils.EMPTY)
                       .build();
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_DETACHING_SERVICES_FROM_MTA;
    }

}
