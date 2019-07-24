package com.sap.cloud.lm.sl.cf.process.analytics.collectors;

import java.math.BigInteger;
import java.util.Map;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.DeployProcessAttributes;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.common.SLException;

@Component("deployProcessAttributesCollector")
public class DeployProcessAttributesCollector extends AbstractCommonProcessAttributesCollector<DeployProcessAttributes> {

    private static final Integer DEFAULT_NON_EXISTING_MTA_SIZE = 0;

    private FileService fileService;

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
        DeployProcessAttributes deployProcessAttributes = super.collectProcessVariables(context);
        deployProcessAttributes.setMtaSize(getMtaSize(context).intValue());
        deployProcessAttributes.setCustomDomains(
            getAttribute(context, Constants.VAR_CUSTOM_DOMAINS, () -> StepsUtil.getCustomDomains(context).size()));
        deployProcessAttributes.setServicesToCreate(
            getAttribute(context, Constants.VAR_SERVICES_TO_CREATE, () -> StepsUtil.getServicesToCreate(context).size()));
        deployProcessAttributes.setAppsToDeploy(
            getAttribute(context, Constants.VAR_APPS_TO_DEPLOY, () -> StepsUtil.getAppsToDeploy(context).size()));
        deployProcessAttributes.setPublishedEntries(
            getAttribute(context, Constants.VAR_PUBLISHED_ENTRIES, () -> StepsUtil.getPublishedEntries(context).size()));
        deployProcessAttributes.setSubscriptionsToCreate(
            getAttribute(context, Constants.VAR_SUBSCRIPTIONS_TO_CREATE, () -> StepsUtil.getSubscriptionsToCreate(context).size()));
        deployProcessAttributes.setServiceBrokersToCreate(
            getAttribute(context, Constants.VAR_ALL_MODULES_TO_DEPLOY, () -> StepsUtil.getCreatedOrUpdatedServiceBrokerNames(context).size()));
        deployProcessAttributes.setTriggeredServiceOperations(
            getAttribute(context, Constants.VAR_TRIGGERED_SERVICE_OPERATIONS, () -> getCreatedServicesCount(StepsUtil.getTriggeredServiceOperations(context))));
        deployProcessAttributes.setServiceKeysToCreate(
            getAttribute(context, Constants.VAR_SERVICE_KEYS_TO_CREATE, () -> StepsUtil.getServiceKeysToCreate(context).size()));
        return deployProcessAttributes;
    }
 // @formatter:on

    private Integer getCreatedServicesCount(Map<String, ServiceOperationType> triggeredServiceOperations) {
        return getOperationsCount(triggeredServiceOperations, ServiceOperationType.CREATE);
    }

    private Integer getOperationsCount(Map<String, ServiceOperationType> serviceOperations, ServiceOperationType targetType) {
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
