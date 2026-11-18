package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Map;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceKeyCredentialResolver;
import org.cloudfoundry.multiapps.controller.process.util.ServiceUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("resolveBindingCredentialReferencesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ResolveBindingCredentialReferencesStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        Map<String, Object> bindingParameters = context.getVariable(Variables.SERVICE_BINDING_PARAMETERS);
        if (bindingParameters == null || bindingParameters.isEmpty()) {
            getStepLogger().debug(Messages.NO_BINDING_CREDENTIAL_REFERENCES_TO_RESOLVE);
            return StepPhase.DONE;
        }
        CloudControllerClient client = context.getControllerClient();
        DeploymentDescriptor descriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        Map<String, String> serviceInstanceNamesByRelationship = ServiceUtil.buildServiceInstanceNamesByRelationship(descriptor);
        ServiceKeyCredentialResolver resolver = new ServiceKeyCredentialResolver(client, serviceInstanceNamesByRelationship);
        Map<String, Object> resolvedBindingParameters = resolver.resolveReferencesInMap(bindingParameters);

        context.setVariable(Variables.SERVICE_BINDING_PARAMETERS, resolvedBindingParameters);
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        getStepLogger().debug(Messages.RESOLVED_BINDING_CREDENTIAL_REFERENCES, app != null ? app.getName() : null, serviceInstanceName);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_RESOLVING_BINDING_CREDENTIAL_REFERENCES;
    }

}