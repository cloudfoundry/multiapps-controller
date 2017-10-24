package com.sap.cloud.lm.sl.cf.process.analytics;

import java.math.BigInteger;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AnalyticsData;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.persistence.services.AbstractFileService;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;

@Component("analyticsCollector")
public class AnalyticsCollector {
    private static final String MTA_SIZE = "mtaSize";
    private static final Integer DEFAULT_NON_EXISTING_MTA_SIZE = 0;
    private static final String RESTARTED_SERVICE_BROKER_SUBSCRIBERS_COUNT = "restartedServiceBrokerSubscribersCount";
    private static final String UPDATED_SUBSCRIBERS_COUNT = "updatedSubscribersCount";
    private static final String DELETED_SERVICES_COUNT = "deletedServicesCount";
    private static final String UNDEPLOYED_APPS_COUNT = "undeployedAppsCount";
    private static final String PUBLISHED_CONFIGURATION_ENTRIES_COUNT = "publishedConfigurationEntriesCount";
    private static final String DELETED_CONFIGURATION_ENTRIES_COUNT = "deletedConfigurationEntriesCount";
    private static final String DELETED_SUBSCRIPTIONS_COUNT = "deletedSubscriptionsCount";
    private static final String CREATED_SERVICE_KEYS_COUNT = "createdServiceKeysCount";
    private static final String CREATED_SERVICE_BROKERS_COUNT = "createdServiceBrokersCount";
    private static final String REGISTERED_URLS_COUNT = "registeredUrlsCount";
    private static final String CREATED_APPS_COUNT = "createdAppsCount";
    private static final String DECLARED_SERVICES_COUNT = "declaredServicesCount";
    private static final String CREATED_SERVICES_COUNT = "createdServicesCount";
    private static final String UPDATED_SERVICES_COUNT = "updatedServicesCount";
    private static final String CREATED_SUBSCRIPTIONS_COUNT = "createdSubscriptionsCount";
    private static final String CREATED_CUSTOM_DOMAINS_COUNT = "customDomainsCount";

    @Inject
    private AbstractFileService fileService;

    Supplier<Long> endTimeSupplier = () -> System.currentTimeMillis();
    Supplier<String> targetUrlSupplier = () -> ConfigurationUtil.getTargetURL().toString();
    Supplier<ZoneId> timeZoneSupplier = () -> ZoneId.systemDefault();

    public AnalyticsData collectAttributes(DelegateExecution context) throws SLException {
        String processId = context.getProcessInstanceId();
        ProcessType processType = StepsUtil.getProcessType(context);
        ZonedDateTime startTime = getStartTime(context, processId);
        ZonedDateTime endTime = getEndTime();
        long processDuration = getProcessDurationInSeconds(context, processId);
        String mtaId = (String) context.getVariable(Constants.PARAM_MTA_ID);
        String org = StepsUtil.getOrg(context);
        String space = StepsUtil.getSpace(context);
        String targetUrl = targetUrlSupplier.get();

        Map<String, Object> commonProcessVariables = collectCommonProcessVariables(context);

        if (processType.equals(ProcessType.BLUE_GREEN_DEPLOY) || processType.equals(ProcessType.DEPLOY)) {
            commonProcessVariables.putAll(collectDeployProcessVariables(context));
        }
        return new AnalyticsData(processId, processType, startTime, endTime, processDuration, null, mtaId, org, space, targetUrl,
            commonProcessVariables);

    }

    private Map<String, Object> collectDeployProcessVariables(DelegateExecution context) throws SLException {
        Map<String, Object> deployProcessVariables = new TreeMap<>();
        deployProcessVariables.put(MTA_SIZE, getMtaSize(context));
        putAttribute(context, Constants.VAR_CUSTOM_DOMAINS, CREATED_CUSTOM_DOMAINS_COUNT, () -> StepsUtil.getCustomDomains(context).size(),
            deployProcessVariables);
        putAttribute(context, Constants.VAR_SERVICES_TO_CREATE, DECLARED_SERVICES_COUNT,
            () -> StepsUtil.getServicesToCreate(context).size(), deployProcessVariables);
        putAttribute(context, Constants.VAR_APPS_TO_DEPLOY, CREATED_APPS_COUNT, () -> StepsUtil.getAppsToDeploy(context).size(),
            deployProcessVariables);
        putAttribute(context, Constants.VAR_PUBLISHED_ENTRIES, PUBLISHED_CONFIGURATION_ENTRIES_COUNT,
            () -> StepsUtil.getPublishedEntries(context).size(), deployProcessVariables);
        putAttribute(context, Constants.VAR_SUBSCRIPTIONS_TO_CREATE, CREATED_SUBSCRIPTIONS_COUNT,
            () -> StepsUtil.getSubscriptionsToCreate(context).size(), deployProcessVariables);
        putAttribute(context, Constants.VAR_SERVICE_URLS_TO_REGISTER, REGISTERED_URLS_COUNT,
            () -> StepsUtil.getServiceUrlsToRegister(context).size(), deployProcessVariables);
        putAttribute(context, Constants.VAR_SERVICE_BROKERS_TO_CREATE, CREATED_SERVICE_BROKERS_COUNT,
            () -> StepsUtil.getServiceBrokersToCreate(context).size(), deployProcessVariables);
        putAttribute(context, Constants.VAR_TRIGGERED_SERVICE_OPERATIONS, CREATED_SERVICES_COUNT,
            () -> getCreatedServicesCount(StepsUtil.getTriggeredServiceOperations(context)), deployProcessVariables);
        putAttribute(context, Constants.VAR_TRIGGERED_SERVICE_OPERATIONS, UPDATED_SERVICES_COUNT,
            () -> getUpdatedServicesCount(StepsUtil.getTriggeredServiceOperations(context)), deployProcessVariables);
        putAttribute(context, Constants.VAR_SERVICE_KEYS_TO_CREATE, CREATED_SERVICE_KEYS_COUNT,
            () -> StepsUtil.getServiceKeysToCreate(context).size(), deployProcessVariables);

        return deployProcessVariables;
    }

    private long getCreatedServicesCount(Map<String, ServiceOperationType> triggeredServiceOperations) {
        return getOperationsCount(triggeredServiceOperations, ServiceOperationType.CREATE);
    }

    private long getUpdatedServicesCount(Map<String, ServiceOperationType> triggeredServiceOperations) {
        return getOperationsCount(triggeredServiceOperations, ServiceOperationType.UPDATE);
    }

    private long getOperationsCount(Map<String, ServiceOperationType> serviceOperations, ServiceOperationType targetType) {
        return serviceOperations.values().stream().filter(operationType -> operationType == targetType).count();
    }

    private Map<String, Object> collectCommonProcessVariables(DelegateExecution context) {
        Map<String, Object> commonProcessVariables = new TreeMap<>();
        putAttribute(context, Constants.VAR_SUBSCRIPTIONS_TO_DELETE, DELETED_SUBSCRIPTIONS_COUNT,
            () -> StepsUtil.getSubscriptionsToDelete(context).size(), commonProcessVariables);
        putAttribute(context, Constants.VAR_DELETED_ENTRIES, DELETED_CONFIGURATION_ENTRIES_COUNT,
            () -> StepsUtil.getDeletedEntries(context).size(), commonProcessVariables);
        putAttribute(context, Constants.VAR_APPS_TO_UNDEPLOY, UNDEPLOYED_APPS_COUNT, () -> StepsUtil.getAppsToUndeploy(context).size(),
            commonProcessVariables);
        putAttribute(context, Constants.VAR_SERVICES_TO_DELETE, DELETED_SERVICES_COUNT, () -> StepsUtil.getServicesToDelete(context).size(),
            commonProcessVariables);
        putAttribute(context, Constants.VAR_UPDATED_SUBSCRIBERS, UPDATED_SUBSCRIBERS_COUNT,
            () -> StepsUtil.getUpdatedSubscribers(context).size(), commonProcessVariables);
        putAttribute(context, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS, RESTARTED_SERVICE_BROKER_SUBSCRIBERS_COUNT,
            () -> StepsUtil.getServiceBrokerSubscribersToRestart(context).size(), commonProcessVariables);

        return commonProcessVariables;
    }

    private void putAttribute(DelegateExecution context, String variableName, String attributeKey, Supplier<Object> attributeValueSupplier,
        Map<String, Object> processAttributes) {
        if (context.getVariable(variableName) != null) {
            processAttributes.put(attributeKey, attributeValueSupplier.get());
        }
    }

    public BigInteger getMtaSize(DelegateExecution context) throws SLException {
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

    private ZonedDateTime getStartTime(DelegateExecution context, String processId) {
        HistoryService historyService = context.getEngineServices().getHistoryService();
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(
            processId).singleResult();
        return processInstance.getStartTime().toInstant().atZone(timeZoneSupplier.get());
    }

    protected ZonedDateTime getEndTime() {
        Date date = new Date(endTimeSupplier.get());
        return date.toInstant().atZone(timeZoneSupplier.get());
    }

    public long getProcessDurationInSeconds(DelegateExecution context, String processId) {
        long startTime = getStartTime(context, processId).toInstant().toEpochMilli();
        long endTime = getEndTime().toInstant().toEpochMilli();
        return TimeUnit.MILLISECONDS.toSeconds(endTime - startTime);
    }

}
