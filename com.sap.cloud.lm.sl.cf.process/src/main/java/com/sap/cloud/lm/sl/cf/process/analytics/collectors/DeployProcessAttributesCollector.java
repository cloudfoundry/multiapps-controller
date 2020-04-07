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
import com.sap.cloud.lm.sl.cf.process.analytics.model.DeployProcessAttributes;
import com.sap.cloud.lm.sl.cf.process.variables.VariableHandling;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
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

    @Override
    public DeployProcessAttributes collectProcessVariables(DelegateExecution execution) {
        DeployProcessAttributes deployProcessAttributes = super.collectProcessVariables(execution);
        deployProcessAttributes.setMtaSize(getMtaSize(execution).intValue());
        deployProcessAttributes.setCustomDomains(getAttribute(execution, Variables.CUSTOM_DOMAINS.getName(),
                                                              () -> VariableHandling.get(execution, Variables.CUSTOM_DOMAINS)
                                                                                    .size()));
        deployProcessAttributes.setServicesToCreate(getAttribute(execution, Variables.SERVICES_TO_CREATE.getName(),
                                                                 () -> VariableHandling.get(execution, Variables.SERVICES_TO_CREATE)
                                                                                       .size()));
        deployProcessAttributes.setAppsToDeploy(getAttribute(execution, Variables.APPS_TO_DEPLOY.getName(),
                                                             () -> VariableHandling.get(execution, Variables.APPS_TO_DEPLOY)
                                                                                   .size()));
        deployProcessAttributes.setPublishedEntries(getAttribute(execution, Variables.PUBLISHED_ENTRIES.getName(),
                                                                 () -> VariableHandling.get(execution, Variables.PUBLISHED_ENTRIES)
                                                                                       .size()));
        deployProcessAttributes.setSubscriptionsToCreate(getAttribute(execution, Variables.SUBSCRIPTIONS_TO_CREATE.getName(),
                                                                      () -> VariableHandling.get(execution,
                                                                                                 Variables.SUBSCRIPTIONS_TO_CREATE)
                                                                                            .size()));
        deployProcessAttributes.setTriggeredServiceOperations(getAttribute(execution, Variables.TRIGGERED_SERVICE_OPERATIONS.getName(),
                                                                           () -> getCreatedServicesCount(VariableHandling.get(execution,
                                                                                                                              Variables.TRIGGERED_SERVICE_OPERATIONS))));
        deployProcessAttributes.setServiceKeysToCreate(getAttribute(execution, Variables.SERVICE_KEYS_TO_CREATE.getName(),
                                                                    () -> VariableHandling.get(execution, Variables.SERVICE_KEYS_TO_CREATE)
                                                                                          .size()));
        return deployProcessAttributes;
    }

    private Integer getCreatedServicesCount(Map<String, ServiceOperation.Type> triggeredServiceOperations) {
        return getOperationsCount(triggeredServiceOperations, ServiceOperation.Type.CREATE);
    }

    private Integer getOperationsCount(Map<String, ServiceOperation.Type> serviceOperations, ServiceOperation.Type targetType) {
        return (int) serviceOperations.values()
                                      .stream()
                                      .filter(operationType -> operationType == targetType)
                                      .count();
    }

    public BigInteger getMtaSize(DelegateExecution execution) {
        String appArchiveId = VariableHandling.get(execution, Variables.APP_ARCHIVE_ID);
        try {
            return computeMtaSize(appArchiveId, execution);
        } catch (FileStorageException e) {
            throw new SLException(e);
        }
    }

    private BigInteger computeMtaSize(String appArchiveId, DelegateExecution execution) throws FileStorageException {
        BigInteger mtaSize = BigInteger.valueOf(0);
        for (String appId : appArchiveId.split(",")) {
            FileEntry fileEntry = fileService.getFile(VariableHandling.get(execution, Variables.SPACE_ID), appId);
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
