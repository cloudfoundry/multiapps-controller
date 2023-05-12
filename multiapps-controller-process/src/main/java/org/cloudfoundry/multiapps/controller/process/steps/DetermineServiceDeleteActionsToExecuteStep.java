package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.util.ServiceAction;
import org.cloudfoundry.multiapps.controller.process.util.ServiceDeletionActions;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;

@Named("determineServiceDeleteActionsToExecuteStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineServiceDeleteActionsToExecuteStep extends SyncFlowableStep {

    private final ProcessTypeParser processTypeParser;

    @Inject
    public DetermineServiceDeleteActionsToExecuteStep(ProcessTypeParser processTypeParser) {
        this.processTypeParser = processTypeParser;
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        String serviceInstanceToDelete = context.getVariable(Variables.SERVICE_TO_DELETE);
        if (serviceInstanceToDelete == null) {
            getStepLogger().debug(Messages.MISSING_SERVICE_TO_DELETE);
            context.setVariable(Variables.SERVICE_DELETION_ACTIONS, Collections.emptyList());
            return StepPhase.DONE;
        }
        context.getStepLogger()
               .debug(Messages.DETERMINING_DELETE_ACTIONS_FOR_SERVICE_INSTANCE_0, serviceInstanceToDelete);
        return calculateDeleteActions(context, serviceInstanceToDelete);
    }

    private StepPhase calculateDeleteActions(ProcessContext context, String serviceInstanceToDelete) {
        CloudControllerClient controllerClient = context.getControllerClient();
        CloudServiceInstance serviceInstance = controllerClient.getServiceInstance(serviceInstanceToDelete, false);
        if (serviceInstance == null) {
            getStepLogger().info(Messages.SERVICE_IS_ALREADY_DELETED, serviceInstanceToDelete);
            context.setVariable(Variables.SERVICE_DELETION_ACTIONS, Collections.emptyList());
            return StepPhase.DONE;
        }
        if (serviceInstance.getV3Metadata() != null && isExistingService(context, serviceInstanceToDelete)) {
            context.getStepLogger()
                   .debug(Messages.WILL_ONLY_REMOVE_SERVICE_INSTANCE_METADATA_BECAUSE_THE_SERVICE_TYPE_IS_EXISTING);
            context.setVariable(Variables.SERVICE_DELETION_ACTIONS, List.of(ServiceDeletionActions.DELETE_METADATA));
            return StepPhase.DONE;
        }
        List<CloudServiceBinding> serviceBindings = controllerClient.getServiceAppBindings(serviceInstance.getGuid());
        List<CloudServiceKey> serviceKeys = controllerClient.getServiceKeys(serviceInstance);
        if (isDeletePossible(context, serviceBindings, serviceKeys)) {
            context.getStepLogger()
                   .debug(Messages.WILL_DELETE_SERVICE_BINDINGS_SERVICE_KEYS_AND_SERVICE_INSTANCE_0, serviceInstanceToDelete);
            logServiceBindingsAndKeys(context, serviceBindings, serviceKeys);
            context.setVariable(Variables.CLOUD_SERVICE_BINDINGS_TO_DELETE, serviceBindings);
            context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE, serviceKeys);
            context.setVariable(Variables.SERVICE_DELETION_ACTIONS,
                                List.of(ServiceDeletionActions.DELETE_SERVICE_BINDINGS, ServiceDeletionActions.DELETE_SERVICE_KEYS,
                                        ServiceDeletionActions.DELETE_SERVICE_INSTANCE));
            return StepPhase.DONE;
        }
        getStepLogger().warn(Messages.SERVICE_NOT_BE_DELETED_DUE_TO_SERVICE_BINDINGS_AND_SERVICE_KEYS, serviceInstanceToDelete);
        context.setVariable(Variables.SERVICE_DELETION_ACTIONS, Collections.emptyList());
        return StepPhase.DONE;
    }

    private boolean isExistingService(ProcessContext context, String serviceName) {
        var deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        // the process type is checked because the deployment descriptor is null during undeployment
        return !ProcessType.UNDEPLOY.equals(processTypeParser.getProcessType(context.getExecution()))
            && CloudModelBuilderUtil.isExistingService(deploymentDescriptor.getResources(), serviceName);
    }

    private boolean isDeletePossible(ProcessContext context, List<CloudServiceBinding> serviceBindings, List<CloudServiceKey> serviceKeys) {
        return shouldDeleteServiceBindings(context, serviceBindings) && shouldDeleteServiceKeys(context, serviceKeys);
    }

    private boolean shouldDeleteServiceBindings(ProcessContext context, List<CloudServiceBinding> serviceBindings) {
        return serviceBindings.isEmpty() || context.getVariable(Variables.SERVICE_ACTIONS_TO_EXCECUTE)
                                                   .contains(ServiceAction.RECREATE);
    }

    private boolean shouldDeleteServiceKeys(ProcessContext context, List<CloudServiceKey> serviceKeys) {
        return serviceKeys.isEmpty() || StepsUtil.canDeleteServiceKeys(context);
    }

    private void logServiceBindingsAndKeys(ProcessContext context, List<CloudServiceBinding> serviceBindings,
                                           List<CloudServiceKey> serviceKeys) {
        context.getStepLogger()
               .debug(Messages.EXISTING_SERVICE_BINDINGS, SecureSerialization.toJson(serviceBindings));
        context.getStepLogger()
               .debug(Messages.EXISTING_SERVICE_KEYS, SecureSerialization.toJson(serviceKeys));
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        String serviceInstanceToDelete = context.getVariable(Variables.SERVICE_TO_DELETE);
        return MessageFormat.format(Messages.ERROR_WHILE_CALCULATING_SERVICE_BINDINGS_TO_DELETE_0, serviceInstanceToDelete);
    }
}
