package org.cloudfoundry.multiapps.controller.process.steps;

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
        List<CloudServiceInstanceExtended> servicesToBind = context.getVariable(Variables.SERVICES_TO_BIND);
        Map<String, String> serviceInstanceNamesByRelationship = ServiceKeyCredentialResolver.buildServiceInstanceNamesFromServicesToBind(servicesToBind);
        ServiceKeyCredentialResolver resolver = new ServiceKeyCredentialResolver(client, serviceInstanceNamesByRelationship);
        Map<String, Object> resolvedBindingParameters = resolver.resolveReferencesInMap(bindingParameters);

        context.setVariable(Variables.SERVICE_BINDING_PARAMETERS, resolvedBindingParameters);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_RESOLVING_BINDING_CREDENTIAL_REFERENCES;
    }

}