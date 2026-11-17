package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("addDomainsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AddDomainsStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        List<String> customDomains = context.getVariable(Variables.CUSTOM_DOMAINS);
        getStepLogger().debug("Custom domains: " + customDomains);
        if (customDomains.isEmpty()) {
            return StepPhase.DONE;
        }

        getStepLogger().debug(Messages.ADDING_DOMAINS);

        CloudControllerClient client = context.getControllerClient();
        for (String domainName : customDomains) {
            addDomain(client, domainName);
        }

        getStepLogger().debug(Messages.DOMAINS_ADDED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_ADDING_DOMAINS;
    }

    private void addDomain(CloudControllerClient client, String domainName) {
        getStepLogger().info(Messages.ADDING_DOMAIN, domainName);
        // addDomain looks the domain up by name and only creates it if it does not already exist,
        // so there is no need to list every domain visible to the (possibly admin) user up front.
        client.addDomain(domainName);
    }

}
