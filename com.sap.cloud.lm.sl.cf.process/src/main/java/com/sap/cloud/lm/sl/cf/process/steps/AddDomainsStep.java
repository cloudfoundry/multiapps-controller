package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("addDomainsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AddDomainsStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        try {
            CloudControllerClient client = execution.getControllerClient();

            List<String> customDomains = StepsUtil.getCustomDomains(execution.getContext());
            getStepLogger().debug("Custom domains: " + customDomains);
            if (customDomains.isEmpty()) {
                return StepPhase.DONE;
            }

            getStepLogger().info(Messages.ADDING_DOMAINS);

            List<CloudDomain> existingDomains = client.getDomains();
            List<String> existingDomainNames = getDomainNames(existingDomains);
            getStepLogger().debug("Existing domains: " + existingDomainNames);

            addDomains(client, customDomains, existingDomainNames);

            getStepLogger().debug(Messages.DOMAINS_ADDED);
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_ADDING_DOMAINS);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_ADDING_DOMAINS);
            throw e;
        }
    }

    private List<String> getDomainNames(List<CloudDomain> domains) {
        List<String> domainNames = new ArrayList<>();
        for (CloudDomain domain : domains) {
            domainNames.add(domain.getName());
        }
        return domainNames;
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
