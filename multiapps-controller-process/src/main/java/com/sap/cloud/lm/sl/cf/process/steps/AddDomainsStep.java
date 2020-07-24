package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

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

        List<CloudDomain> existingDomains = client.getDomains();
        List<String> existingDomainNames = getDomainNames(existingDomains);
        getStepLogger().debug("Existing domains: " + existingDomainNames);

        addDomains(client, customDomains, existingDomainNames);

        getStepLogger().debug(Messages.DOMAINS_ADDED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_ADDING_DOMAINS;
    }

    private List<String> getDomainNames(List<CloudDomain> domains) {
        return domains.stream()
                      .map(CloudDomain::getName)
                      .collect(Collectors.toList());
    }

    private void addDomains(CloudControllerClient client, List<String> domainNames, List<String> existingDomainNames) {
        for (String domainName : domainNames) {
            addDomain(client, domainName, existingDomainNames);
        }
    }

    private void addDomain(CloudControllerClient client, String domainName, List<String> existingDomainNames) {
        if (existingDomainNames.contains(domainName)) {
            getStepLogger().debug(Messages.DOMAIN_ALREADY_EXISTS, domainName);
        } else {
            getStepLogger().info(Messages.ADDING_DOMAIN, domainName);
            client.addDomain(domainName);
        }
    }

}
