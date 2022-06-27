package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper.CloudComponents;
import org.cloudfoundry.multiapps.controller.process.util.ServiceAction;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceRemover;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

@Named("deleteServiceStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteServiceStep extends AsyncFlowableStep {

    private ServiceOperationGetter serviceOperationGetter;
    private ServiceProgressReporter serviceProgressReporter;
    private ServiceRemover serviceRemover;

    @Inject
    public DeleteServiceStep(ServiceOperationGetter serviceOperationGetter, ServiceProgressReporter serviceProgressReporter,
                             ServiceRemover serviceRemover) {
        this.serviceOperationGetter = serviceOperationGetter;
        this.serviceProgressReporter = serviceProgressReporter;
        this.serviceRemover = serviceRemover;
    }

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        String serviceToDelete = context.getVariable(Variables.SERVICE_TO_DELETE);
        if (serviceToDelete == null) {
            getStepLogger().debug(Messages.MISSING_SERVICE_TO_DELETE);
            return StepPhase.DONE;
        }

        getStepLogger().debug(Messages.DELETING_DISCONTINUED_SERVICE_0, serviceToDelete);

        CloudControllerClient client = context.getControllerClient();

        CloudServiceInstance serviceInstance = client.getServiceInstance(serviceToDelete, false);
        if (serviceInstance == null) {
            getStepLogger().info(Messages.SERVICE_IS_ALREADY_DELETED, serviceToDelete);
            return StepPhase.DONE;
        }

        var deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        if (serviceInstance.getV3Metadata() != null && CloudModelBuilderUtil.isExistingService(deploymentDescriptor.getResources(),
                                                                                               serviceToDelete)) {
            clearMtaMetadata(client, serviceInstance);
            return StepPhase.DONE;
        }

        context.setVariable(Variables.SERVICES_DATA, buildCloudServiceExtendedList(serviceInstance));

        List<CloudServiceKey> serviceKeys = client.getServiceKeys(serviceInstance);

        List<CloudServiceBinding> serviceBindings = client.getServiceBindings(serviceInstance.getMetadata()
                                                                                             .getGuid());
        if (isDeletePossible(context, serviceBindings, serviceKeys)) {
            serviceRemover.deleteService(context, serviceInstance, serviceBindings, serviceKeys);
            context.setVariable(Variables.TRIGGERED_SERVICE_OPERATIONS, Map.of(serviceToDelete, ServiceOperation.Type.DELETE));
            return StepPhase.POLL;
        }

        getStepLogger().warn(Messages.SERVICE_NOT_BE_DELETED_DUE_TO_SERVICE_BINDINGS_AND_SERVICE_KEYS, serviceToDelete);
        return StepPhase.DONE;

    }

    private void clearMtaMetadata(CloudControllerClient client, CloudServiceInstance serviceInstance) {
        getStepLogger().debug(MessageFormat.format(Messages.CLEARING_EXISTING_SERVICE_0_METADATA, serviceInstance.getName()));
        var serviceGuid = serviceInstance.getGuid();
        client.updateServiceInstanceMetadata(serviceGuid, MtaMetadataUtil.getMetadataWithoutMtaFields(serviceInstance.getV3Metadata()));
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DELETING_SERVICES;
    }

    @Override
    protected String getStepErrorMessageAdditionalDescription(ProcessContext context) {
        String offering = context.getVariable(Variables.SERVICE_OFFERING);
        return ExceptionMessageTailMapper.map(configuration, CloudComponents.SERVICE_BROKERS, offering);
    }

    private List<CloudServiceInstanceExtended> buildCloudServiceExtendedList(CloudServiceInstance serviceInstanceData) {
        return Collections.singletonList(buildCloudServiceExtended(serviceInstanceData));
    }

    private CloudServiceInstanceExtended buildCloudServiceExtended(CloudServiceInstance service) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .from(service)
                                                    .build();
    }

    private boolean isDeletePossible(ProcessContext context, List<CloudServiceBinding> serviceBindings, List<CloudServiceKey> serviceKeys) {
        return (serviceBindings.isEmpty() || context.getVariable(Variables.SERVICE_ACTIONS_TO_EXCECUTE)
                                                    .contains(ServiceAction.RECREATE))
            && (serviceKeys.isEmpty() || context.getVariable(Variables.DELETE_SERVICE_KEYS));
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollServiceDeleteOperationsExecution(serviceOperationGetter, serviceProgressReporter));
    }

}
