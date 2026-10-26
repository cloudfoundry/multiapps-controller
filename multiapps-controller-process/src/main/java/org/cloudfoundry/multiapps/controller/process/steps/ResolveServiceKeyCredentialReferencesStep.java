package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Map;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceKeyCredentialResolver;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("resolveServiceKeyCredentialReferencesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ResolveServiceKeyCredentialReferencesStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        CloudServiceKey serviceKeyToProcess = context.getVariable(Variables.SERVICE_KEY_TO_PROCESS);
        CloudControllerClient client = context.getControllerClient();

        DeploymentDescriptor descriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        Map<String, String> serviceInstanceNamesByRelationship = ServiceKeyCredentialResolver.buildServiceInstanceNamesFromDescriptor(descriptor);
        ServiceKeyCredentialResolver resolver = new ServiceKeyCredentialResolver(client, serviceInstanceNamesByRelationship);
        CloudServiceKey resolvedServiceKey = resolver.resolve(serviceKeyToProcess);

        context.setVariable(Variables.SERVICE_KEY_TO_PROCESS, resolvedServiceKey);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        CloudServiceKey serviceKeyToProcess = context.getVariable(Variables.SERVICE_KEY_TO_PROCESS);
        return MessageFormat.format(Messages.ERROR_WHILE_CREATING_SERVICE_KEY_0, serviceKeyToProcess.getName());
    }

}