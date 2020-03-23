package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.analytics.model.ServiceAction;
import com.sap.cloud.lm.sl.cf.process.util.ExceptionMessageTailMapper;
import com.sap.cloud.lm.sl.cf.process.util.ExceptionMessageTailMapper.CloudComponents;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceRemover;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.MapUtil;

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
        StepsUtil.setServicesData(context.getExecution(), buildCloudServiceExtendedList(serviceInstance));

        List<CloudServiceKey> serviceKeys = client.getServiceKeys(serviceInstance.getService());

        if (isDeletePossible(context, serviceInstance.getBindings(), serviceKeys)) {
            serviceRemover.deleteService(context, serviceInstance, serviceKeys);
            context.setVariable(Variables.TRIGGERED_SERVICE_OPERATIONS, MapUtil.asMap(serviceToDelete, ServiceOperation.Type.DELETE));
            return StepPhase.POLL;
        }

        getStepLogger().warn(Messages.SERVICE_NOT_BE_DELETED_DUE_TO_SERVICE_BINDINGS_AND_SERVICE_KEYS, serviceToDelete);
        return StepPhase.DONE;

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

    private List<CloudServiceExtended> buildCloudServiceExtendedList(CloudServiceInstance serviceInstanceData) {
        return Collections.singletonList(buildCloudServiceExtended(serviceInstanceData));
    }

    private ImmutableCloudServiceExtended buildCloudServiceExtended(CloudServiceInstance service) {
        return ImmutableCloudServiceExtended.builder()
                                            .from(service.getService())
                                            .build();
    }

    private boolean isDeletePossible(ProcessContext context, List<CloudServiceBinding> serviceBindings, List<CloudServiceKey> serviceKeys) {
        return (serviceBindings.isEmpty() || StepsUtil.getServiceActionsToExecute(context.getExecution())
                                                      .contains(ServiceAction.RECREATE))
            && (serviceKeys.isEmpty() || context.getVariable(Variables.DELETE_SERVICE_KEYS));
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollServiceDeleteOperationsExecution(serviceOperationGetter, serviceProgressReporter));
    }

}
