package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper.CloudComponents;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceRemover;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

@Named("deleteServiceStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteServiceStep extends AsyncFlowableStep {

    private final ServiceOperationGetter serviceOperationGetter;
    private final ServiceProgressReporter serviceProgressReporter;
    private final ServiceRemover serviceRemover;

    @Inject
    public DeleteServiceStep(ServiceOperationGetter serviceOperationGetter, ServiceProgressReporter serviceProgressReporter,
                             ServiceRemover serviceRemover) {
        this.serviceOperationGetter = serviceOperationGetter;
        this.serviceProgressReporter = serviceProgressReporter;
        this.serviceRemover = serviceRemover;
    }

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        String serviceInstanceToDelete = context.getVariable(Variables.SERVICE_TO_DELETE);
        getStepLogger().debug(Messages.DELETING_DISCONTINUED_SERVICE_0, serviceInstanceToDelete);
        CloudControllerClient controllerClient = context.getControllerClient();
        CloudServiceInstance serviceInstance = controllerClient.getServiceInstanceWithoutAuxiliaryContent(serviceInstanceToDelete);
        context.setVariable(Variables.SERVICES_DATA, buildCloudServiceExtendedList(serviceInstance));
        serviceRemover.deleteService(context, serviceInstance);
        context.setVariable(Variables.TRIGGERED_SERVICE_OPERATIONS, Map.of(serviceInstanceToDelete, ServiceOperation.Type.DELETE));
        return StepPhase.POLL;
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

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollServiceDeleteOperationsExecution(serviceOperationGetter, serviceProgressReporter));
    }

}
