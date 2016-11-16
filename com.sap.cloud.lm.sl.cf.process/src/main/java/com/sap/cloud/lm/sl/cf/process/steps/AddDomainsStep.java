package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("addDomainsStep")
public class AddDomainsStep extends AbstractXS2ProcessStep {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(AddDomainsStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("addDomainsTask", "Add Domains", "Add Domains");
    }

    protected Function<DelegateExecution, CloudFoundryOperations> clientSupplier = (context) -> getCloudFoundryClient(context, LOGGER);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.ADDING_DOMAINS, LOGGER);

            CloudFoundryOperations client = clientSupplier.apply(context);

            List<CloudDomain> existingDomains = client.getDomainsForOrg();
            List<String> existingDomainNames = getDomainNames(existingDomains);
            debug(context, "Existing domains: " + existingDomainNames, LOGGER);

            List<String> customDomains = StepsUtil.getCustomDomains(context);
            addDomains(context, client, customDomains, existingDomainNames);

            debug(context, Messages.DOMAINS_ADDED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_ADDING_DOMAINS, e, LOGGER);
            throw e;
        } catch (CloudFoundryException e) {
            SLException ex = StepsUtil.createException(e);
            error(context, Messages.ERROR_ADDING_DOMAINS, ex, LOGGER);
            throw ex;
        }
    }

    private List<String> getDomainNames(List<CloudDomain> domains) {
        List<String> domainNames = new ArrayList<>();
        for (CloudDomain domain : domains) {
            domainNames.add(domain.getName());
        }
        return domainNames;
    }

    private void addDomains(DelegateExecution context, CloudFoundryOperations client, List<String> domainNames,
        List<String> existingDomainNames) {
        for (String domainName : domainNames) {
            addDomain(context, client, domainName, existingDomainNames);
        }
    }

    private void addDomain(DelegateExecution context, CloudFoundryOperations client, String domainName, List<String> existingDomainNames) {
        if (existingDomainNames.contains(domainName)) {
            debug(context, format(Messages.DOMAIN_ALREADY_EXISTS, domainName), LOGGER);
        } else {
            info(context, format(Messages.ADDING_DOMAIN, domainName), LOGGER);
            client.addDomain(domainName);
        }
    }

}
