package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceKeyCredentialResolver;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
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
        Map<String, String> serviceInstanceNamesByRelationship = new HashMap<>();
        List<CloudServiceInstanceExtended> servicesToBind = context.getVariable(Variables.SERVICES_TO_BIND);
        if (servicesToBind == null) {
            return serviceInstanceNamesByRelationship;
        }
        for (CloudServiceInstanceExtended service : servicesToBind) {
            serviceInstanceNamesByRelationship.put(service.getResourceName(), service.getName());
        }
        return serviceInstanceNamesByRelationship;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_RESOLVING_BINDING_CREDENTIAL_REFERENCES;
    }

}