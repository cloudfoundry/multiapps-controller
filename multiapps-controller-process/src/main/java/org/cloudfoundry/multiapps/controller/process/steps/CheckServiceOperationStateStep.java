package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

@Named("checkServiceOperationStateStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckServiceOperationStateStep extends CollectServicesInProgressStateStep {

    @Inject
    CheckServiceOperationStateStep(ServiceOperationGetter serviceOperationGetter, ServiceProgressReporter serviceProgressReporter) {
        super(serviceOperationGetter, serviceProgressReporter);
    }

    @Override
    protected List<CloudServiceInstanceExtended> getExistingServicesInProgress(ProcessContext context) {
        CloudControllerClient client = context.getControllerClient();
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        CloudServiceInstanceExtended existingServiceInstance = getExistingService(client, serviceToProcess);
        if (existingServiceInstance == null || !isServiceOperationInProgress(existingServiceInstance)) {
            return Collections.emptyList();
        }
        return List.of(existingServiceInstance);
    }
}
