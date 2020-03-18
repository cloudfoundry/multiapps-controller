package com.sap.cloud.lm.sl.cf.process.analytics.collectors;

import java.math.BigInteger;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.DeployProcessAttributes;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.process.variables.VariablesHandler;
import com.sap.cloud.lm.sl.common.SLException;

@Named("deployProcessAttributesCollector")
public class DeployProcessAttributesCollector extends AbstractCommonProcessAttributesCollector<DeployProcessAttributes> {

    private static final Integer DEFAULT_NON_EXISTING_MTA_SIZE = 0;

    private final FileService fileService;

    @Inject
    public DeployProcessAttributesCollector(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    protected DeployProcessAttributes getProcessAttributes() {
        return new DeployProcessAttributes();
    }

 // @formatter:off
    @Override
    public DeployProcessAttributes collectProcessVariables(DelegateExecution context) {
        VariablesHandler variablesHandler = new VariablesHandler(context);
        DeployProcessAttributes deployProcessAttributes = super.collectProcessVariables(context);
        deployProcessAttributes.setMtaSize(getMtaSize(context).intValue());
        deployProcessAttributes.setCustomDomains(
            getAttribute(context, Constants.VAR_CUSTOM_DOMAINS, () -> variablesHandler.get(Variables.CUSTOM_DOMAINS).size()));
        deployProcessAttributes.setServicesToCreate(
            getAttribute(context, Constants.VAR_SERVICES_TO_CREATE, () -> StepsUtil.getServicesToCreate(context).size()));
        deployProcessAttributes.setAppsToDeploy(
            getAttribute(context, Constants.VAR_APPS_TO_DEPLOY, () -> variablesHandler.get(Variables.APPS_TO_DEPLOY).size()));
        deployProcessAttributes.setPublishedEntries(
            getAttribute(context, Constants.VAR_PUBLISHED_ENTRIES, () -> variablesHandler.get(Variables.PUBLISHED_ENTRIES).size()));
        deployProcessAttributes.setSubscriptionsToCreate(
            getAttribute(context, Constants.VAR_SUBSCRIPTIONS_TO_CREATE, () -> variablesHandler.get(Variables.SUBSCRIPTIONS_TO_CREATE).size()));
        deployProcessAttributes.setServiceBrokersToCreate(
            getAttribute(context, Constants.VAR_ALL_MODULES_TO_DEPLOY, () -> StepsUtil.getCreatedOrUpdatedServiceBrokerNames(context).size()));
        deployProcessAttributes.setTriggeredServiceOperations(
            getAttribute(context, Constants.VAR_TRIGGERED_SERVICE_OPERATIONS, () -> getCreatedServicesCount(variablesHandler.get(Variables.TRIGGERED_SERVICE_OPERATIONS))));
        deployProcessAttributes.setServiceKeysToCreate(
            getAttribute(context, Constants.VAR_SERVICE_KEYS_TO_CREATE, () -> variablesHandler.get(Variables.SERVICE_KEYS_TO_CREATE).size()));
        return deployProcessAttributes;
    }
 // @formatter:on

    private Integer getCreatedServicesCount(Map<String, ServiceOperation.Type> triggeredServiceOperations) {
        return getOperationsCount(triggeredServiceOperations, ServiceOperation.Type.CREATE);
    }

    private Integer getOperationsCount(Map<String, ServiceOperation.Type> serviceOperations, ServiceOperation.Type targetType) {
        return (int) serviceOperations.values()
                                      .stream()
                                      .filter(operationType -> operationType == targetType)
                                      .count();
    }

    public BigInteger getMtaSize(DelegateExecution context) {
        String appArchiveId = (String) context.getVariable(Constants.PARAM_APP_ARCHIVE_ID);
        try {
            return computeMtaSize(appArchiveId, context);
        } catch (FileStorageException e) {
            throw new SLException(e);
        }
    }

    private BigInteger computeMtaSize(String appArchiveId, DelegateExecution context) throws FileStorageException {
        BigInteger mtaSize = BigInteger.valueOf(0);
        for (String appId : appArchiveId.split(",")) {
            FileEntry fileEntry = fileService.getFile(StepsUtil.getSpaceId(context), appId);
            mtaSize = mtaSize.add(entrySize(fileEntry));
        }
        return mtaSize;
    }

    private BigInteger entrySize(FileEntry fileEntry) {
        if (fileEntry == null) {
            return BigInteger.valueOf(DEFAULT_NON_EXISTING_MTA_SIZE);
        }
        return fileEntry.getSize();
    }

}
