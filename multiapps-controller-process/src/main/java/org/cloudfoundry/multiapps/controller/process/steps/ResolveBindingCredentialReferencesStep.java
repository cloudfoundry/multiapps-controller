package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceKeyCredentialResolver;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("resolveBindingCredentialReferencesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ResolveBindingCredentialReferencesStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        Map<String, Object> bindingParameters = context.getVariable(Variables.SERVICE_BINDING_PARAMETERS);
        if (bindingParameters == null || bindingParameters.isEmpty()) {
            return StepPhase.DONE;
        }
        CloudControllerClient client = context.getControllerClient();
        Map<String, String> serviceInstanceNamesByRelationship = buildServiceInstanceNamesByRelationship(context);
        ServiceKeyCredentialResolver resolver = new ServiceKeyCredentialResolver(client, serviceInstanceNamesByRelationship);

        Map<String, Object> resolvedBindingParameters = resolver.resolveReferencesInMap(bindingParameters);

        context.setVariable(Variables.SERVICE_BINDING_PARAMETERS, resolvedBindingParameters);
        return StepPhase.DONE;
    }

    private Map<String, String> buildServiceInstanceNamesByRelationship(ProcessContext context) {
        DeploymentDescriptor descriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        Map<String, String> serviceInstanceNamesByRelationship = new HashMap<>();
        if (descriptor == null) {
            return serviceInstanceNamesByRelationship;
        }
        for (Resource resource : descriptor.getResources()) {
            Object serviceName = resource.getParameters()
                                         .get(SupportedParameters.SERVICE_NAME);
            serviceInstanceNamesByRelationship.put(resource.getName(),
                                                   serviceName instanceof String ? (String) serviceName : resource.getName());
        }
        return serviceInstanceNamesByRelationship;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_RESOLVING_BINDING_CREDENTIAL_REFERENCES;
    }

}