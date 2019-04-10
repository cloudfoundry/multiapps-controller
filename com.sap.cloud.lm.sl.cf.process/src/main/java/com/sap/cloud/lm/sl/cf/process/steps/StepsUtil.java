package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.DeploymentMode;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveElements;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.ServiceAction;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.common.util.YamlUtil;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.v2.ExtensionDescriptor;

public class StepsUtil {

    public static org.apache.log4j.Logger getLogger(DelegateExecution context, String name, ProcessLoggerProvider processLoggerProvider) {
        return processLoggerProvider.getLogger(context, name);
    }

    static CloudControllerClient getControllerClient(DelegateExecution context, CloudControllerClientProvider clientProvider,
        StepLogger stepLogger) {
        String userName = determineCurrentUser(context, stepLogger);
        String spaceId = getSpaceId(context);
        return clientProvider.getControllerClient(userName, spaceId);
    }

    static CloudControllerClient getControllerClient(DelegateExecution context, CloudControllerClientProvider clientProvider,
        StepLogger stepLogger, String org, String space) {
        // Determine the current user
        String userName = determineCurrentUser(context, stepLogger);
        return clientProvider.getControllerClient(userName, org, space, context.getProcessInstanceId());
    }

    static XsCloudControllerClient getXsControllerClient(DelegateExecution context, CloudControllerClientProvider clientProvider,
        StepLogger stepLogger) {
        CloudControllerClient client = StepsUtil.getControllerClient(context, clientProvider, stepLogger);
        if (client instanceof XsCloudControllerClient) {
            return (XsCloudControllerClient) client;
        }
        return null;
    }

    static XsCloudControllerClient getXsControllerClient(DelegateExecution context, CloudControllerClientProvider clientProvider,
        StepLogger stepLogger, String org, String space) {
        CloudControllerClient client = StepsUtil.getControllerClient(context, clientProvider, stepLogger, org, space);
        if (client instanceof XsCloudControllerClient) {
            return (XsCloudControllerClient) client;
        }
        return null;
    }

    public static String determineCurrentUser(VariableScope scope, StepLogger stepLogger) {
        String userId = Authentication.getAuthenticatedUserId();
        String previousUser = (String) scope.getVariable(Constants.VAR_USER);
        // Determine the current user
        if (userId != null && !userId.equals(previousUser)) {
            stepLogger.debug(Messages.AUTHENTICATED_USER_ID, userId);
            stepLogger.debug(Messages.PREVIOUS_USER, previousUser);
        }
        if (userId == null) {
            // If the authenticated user cannot be determined,
            // use the user saved by the previous service task
            userId = previousUser;
            if (userId == null) {
                // If there is no previous user, this must be the first service task
                // Use the process initiator in this case
                userId = (String) scope.getVariable(Constants.PARAM_INITIATOR);
                stepLogger.debug(Messages.PROCESS_INITIATOR, userId);
                if (userId == null) {
                    throw new SLException(Messages.CANT_DETERMINE_CURRENT_USER);
                }
            }
        }
        // Set the current user in the context for use by later service tasks
        scope.setVariable(Constants.VAR_USER, userId);

        return userId;
    }

    public static boolean isPortBasedRouting(ExecutionWrapper execution) {
        CloudControllerClient client = execution.getControllerClient();
        CloudInfo info = client.getCloudInfo();
        if (info instanceof CloudInfoExtended) {
            return ((CloudInfoExtended) info).isPortBasedRouting();
        }
        return false;
    }

    public static MtaArchiveElements getMtaArchiveElements(VariableScope scope) {
        return getFromJsonString(scope, Constants.VAR_MTA_ARCHIVE_ELEMENTS, MtaArchiveElements.class, new MtaArchiveElements());
    }

    public static void setMtaArchiveElements(VariableScope scope, MtaArchiveElements mtaArchiveElements) {
        setAsJsonString(scope, Constants.VAR_MTA_ARCHIVE_ELEMENTS, mtaArchiveElements);
    }

    static InputStream getModuleContentAsStream(VariableScope scope, String moduleName) {
        byte[] moduleContent = getModuleContent(scope, moduleName);
        if (moduleContent == null) {
            throw new SLException(Messages.MODULE_CONTENT_NA, moduleName);
        }
        return new ByteArrayInputStream(moduleContent);
    }

    static byte[] getModuleContent(VariableScope scope, String moduleName) {
        return getObject(scope, getModuleContentVariable(moduleName));
    }

    static void setModuleContent(VariableScope scope, String moduleName, byte[] moduleContent) {
        scope.setVariable(getModuleContentVariable(moduleName), moduleContent);
    }

    public static ApplicationColor getDeployedMtaColor(VariableScope scope) {
        return getEnum(scope, Constants.VAR_DEPLOYED_MTA_COLOR, ApplicationColor::valueOf);
    }

    public static void setDeployedMtaColor(VariableScope scope, ApplicationColor deployedMtaColor) {
        setEnum(scope, Constants.VAR_DEPLOYED_MTA_COLOR, deployedMtaColor);
    }

    public static ApplicationColor getMtaColor(VariableScope scope) {
        return getEnum(scope, Constants.VAR_MTA_COLOR, ApplicationColor::valueOf);
    }

    public static void setMtaColor(VariableScope scope, ApplicationColor mtaColor) {
        setEnum(scope, Constants.VAR_MTA_COLOR, mtaColor);
    }
    
    public static void setPhase(VariableScope scope, Phase phase) {
        setEnum(scope, Constants.VAR_PHASE, phase);
    }
    
    private static String getModuleContentVariable(String moduleName) {
        return Constants.VAR_MTA_MODULE_CONTENT_PREFIX + moduleName;
    }

    public static HandlerFactory getHandlerFactory(VariableScope scope) {
        int majorSchemaVersion = getInteger(scope, Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
        return new HandlerFactory(majorSchemaVersion);
    }

    public static String getOrg(VariableScope scope) {
        return getString(scope, Constants.VAR_ORG);
    }

    public static String getSpaceId(VariableScope scope) {
        return getString(scope, com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID);
    }

    public static void setSpaceId(VariableScope scope, String spaceId) {
        scope.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID, spaceId);
    }

    public static String getSpace(VariableScope scope) {
        return getString(scope, Constants.VAR_SPACE);
    }

    static String getNewMtaVersion(VariableScope scope) {
        return getString(scope, Constants.VAR_NEW_MTA_VERSION);
    }

    static void setNewMtaVersion(VariableScope scope, String version) {
        scope.setVariable(Constants.VAR_NEW_MTA_VERSION, version);
    }

    public static List<String> getCustomDomains(VariableScope scope) {
        Type type = new TypeToken<List<String>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_CUSTOM_DOMAINS, type);
    }

    static void setCustomDomains(VariableScope scope, List<String> customDomains) {
        setAsJsonBinary(scope, Constants.VAR_CUSTOM_DOMAINS, customDomains);
    }

    public static List<CloudServiceExtended> getServicesToCreate(VariableScope scope) {
        return getFromJsonStrings(scope, Constants.VAR_SERVICES_TO_CREATE, CloudServiceExtended.class);
    }

    static void setServicesToCreate(VariableScope scope, List<CloudServiceExtended> services) {
        setAsJsonStrings(scope, Constants.VAR_SERVICES_TO_CREATE, services);
    }

    public static List<CloudServiceExtended> getServicesToBind(VariableScope scope) {
        return getFromJsonStrings(scope, Constants.VAR_SERVICES_TO_BIND, CloudServiceExtended.class);
    }

    static void setServicesToBind(VariableScope scope, List<CloudServiceExtended> services) {
        setAsJsonStrings(scope, Constants.VAR_SERVICES_TO_BIND, services);
    }

    static void setServicesToPoll(VariableScope scope, List<CloudServiceExtended> servicesToPoll) {
        setAsJsonBinary(scope, Constants.VAR_SERVICES_TO_POLL, servicesToPoll);
    }

    static List<CloudServiceExtended> getServicesToPoll(VariableScope scope) {
        Type type = new TypeToken<List<CloudServiceExtended>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_SERVICES_TO_POLL, type);
    }

    static void setTriggeredServiceOperations(VariableScope scope, Map<String, ServiceOperationType> triggeredServiceOperations) {
        setAsJsonBinary(scope, Constants.VAR_TRIGGERED_SERVICE_OPERATIONS, triggeredServiceOperations);
    }

    public static Map<String, ServiceOperationType> getTriggeredServiceOperations(VariableScope scope) {
        Type type = new TypeToken<Map<String, ServiceOperationType>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_TRIGGERED_SERVICE_OPERATIONS, type);
    }

    public static Map<String, List<ServiceKey>> getServiceKeysToCreate(VariableScope scope) {
        Type type = new TypeToken<Map<String, List<ServiceKey>>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_SERVICE_KEYS_TO_CREATE, type);
    }

    static void setServiceKeysToCreate(VariableScope scope, Map<String, List<ServiceKey>> serviceKeys) {
        setAsJsonBinary(scope, Constants.VAR_SERVICE_KEYS_TO_CREATE, serviceKeys);
    }

    static List<CloudApplication> getDeployedApps(VariableScope scope) {
        Type type = new TypeToken<List<CloudApplication>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_DEPLOYED_APPS, type);
    }

    static void setDeployedApps(VariableScope scope, List<CloudApplication> apps) {
        setAsJsonBinary(scope, Constants.VAR_DEPLOYED_APPS, apps);
    }

    public static List<String> getAppsToDeploy(VariableScope scope) {
        Type type = new TypeToken<List<String>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_APPS_TO_DEPLOY, type, Collections.emptyList());
    }

    public static void setAppsToDeploy(VariableScope scope, List<String> apps) {
        setAsJsonBinary(scope, Constants.VAR_APPS_TO_DEPLOY, apps);
    }

    public static List<Module> getModulesToDeploy(VariableScope scope) {
        return getFromJsonBinaries(scope, Constants.VAR_MODULES_TO_DEPLOY, Module.class);
    }

    public static void setModulesToDeploy(VariableScope scope, List<? extends Module> modules) {
        setAsJsonBinaries(scope, Constants.VAR_MODULES_TO_DEPLOY, modules);
    }

    public static List<Module> getAllModulesToDeploy(VariableScope scope) {
        return getFromJsonBinaries(scope, Constants.VAR_ALL_MODULES_TO_DEPLOY, Module.class);
    }

    public static void setAllModulesToDeploy(VariableScope scope, List<? extends Module> modules) {
        setAsJsonBinaries(scope, Constants.VAR_ALL_MODULES_TO_DEPLOY, modules);
    }

    public static List<Module> getIteratedModulesInParallel(VariableScope scope) {
        return getFromJsonBinaries(scope, Constants.VAR_ITERATED_MODULES_IN_PARALLEL, Module.class);
    }

    public static void setIteratedModulesInParallel(VariableScope scope, List<? extends Module> modules) {
        setAsJsonBinaries(scope, Constants.VAR_ITERATED_MODULES_IN_PARALLEL, modules);
    }

    public static void setModulesToIterateInParallel(VariableScope scope, List<? extends Module> modules) {
        setAsJsonBinaries(scope, Constants.VAR_MODULES_TO_ITERATE_IN_PARALLEL, modules);
    }

    public static void setDeploymentMode(VariableScope scope, DeploymentMode deploymentMode) {
        scope.setVariable(Constants.VAR_DEPLOYMENT_MODE, deploymentMode);
    }

    static void setServiceKeysCredentialsToInject(VariableScope scope, Map<String, Map<String, String>> serviceKeysCredentialsToInject) {
        setAsJsonBinary(scope, Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT, serviceKeysCredentialsToInject);
    }

    static Map<String, Map<String, String>> getServiceKeysCredentialsToInject(VariableScope scope) {
        Type type = new TypeToken<Map<String, Map<String, String>>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT, type);
    }

    public static List<CloudApplication> getUpdatedSubscribers(VariableScope scope) {
        Type type = new TypeToken<List<CloudApplicationExtended>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_UPDATED_SUBSCRIBERS, type);
    }

    static void setUpdatedSubscribers(VariableScope scope, List<CloudApplication> apps) {
        setAsJsonBinary(scope, Constants.VAR_UPDATED_SUBSCRIBERS, apps);
    }

    public static List<CloudApplication> getServiceBrokerSubscribersToRestart(VariableScope scope) {
        Type type = new TypeToken<List<CloudApplication>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS, type);
    }

    static CloudApplication getServiceBrokerSubscriberToRestart(VariableScope scope) {
        List<CloudApplication> apps = getServiceBrokerSubscribersToRestart(scope);
        int index = (Integer) scope.getVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX);
        return apps.get(index);
    }

    static void setUpdatedServiceBrokerSubscribers(VariableScope scope, List<CloudApplication> apps) {
        setAsJsonBinary(scope, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS, apps);
    }

    static List<CloudTask> getTasksToExecute(VariableScope scope) {
        Type type = new TypeToken<List<CloudTask>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_TASKS_TO_EXECUTE, type);
    }

    public static void setTasksToExecute(VariableScope scope, List<CloudTask> tasks) {
        setAsJsonBinary(scope, Constants.VAR_TASKS_TO_EXECUTE, tasks);
    }

    static CloudTask getStartedTask(VariableScope scope) {
        return getFromJsonBinary(scope, Constants.VAR_STARTED_TASK, CloudTask.class);
    }

    static void setStartedTask(VariableScope scope, CloudTask task) {
        setAsJsonBinary(scope, Constants.VAR_STARTED_TASK, task);
    }

    public static List<CloudApplication> getAppsToUndeploy(VariableScope scope) {
        Type type = new TypeToken<List<CloudApplication>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_APPS_TO_UNDEPLOY, type);
    }

    public static CloudApplication getAppToUndeploy(VariableScope scope) {
        List<CloudApplication> appsToUndeploy = getAppsToUndeploy(scope);
        int index = (Integer) scope.getVariable(Constants.VAR_APPS_TO_UNDEPLOY_INDEX);
        return appsToUndeploy.get(index);
    }

    static void setAppsToUndeploy(VariableScope scope, List<CloudApplication> apps) {
        setAsJsonBinary(scope, Constants.VAR_APPS_TO_UNDEPLOY, apps);
    }

    public static List<String> getServicesToDelete(VariableScope scope) {
        Type type = new TypeToken<List<String>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_SERVICES_TO_DELETE, type);
    }

    public static void setServicesToDelete(VariableScope scope, List<String> services) {
        setAsJsonBinary(scope, Constants.VAR_SERVICES_TO_DELETE, services);
    }

    public static List<ConfigurationSubscription> getSubscriptionsToDelete(VariableScope scope) {
        Type type = new TypeToken<List<ConfigurationSubscription>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_SUBSCRIPTIONS_TO_DELETE, type);
    }

    static void setSubscriptionsToDelete(VariableScope scope, List<ConfigurationSubscription> subscriptions) {
        setAsJsonBinary(scope, Constants.VAR_SUBSCRIPTIONS_TO_DELETE, subscriptions);
    }

    public static List<ConfigurationSubscription> getSubscriptionsToCreate(VariableScope scope) {
        Type type = new TypeToken<List<ConfigurationSubscription>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_SUBSCRIPTIONS_TO_CREATE, type);
    }

    static void setSubscriptionsToCreate(VariableScope scope, List<ConfigurationSubscription> subscriptions) {
        setAsJsonBinary(scope, Constants.VAR_SUBSCRIPTIONS_TO_CREATE, subscriptions);
    }

    static void setConfigurationEntriesToPublish(VariableScope scope, List<ConfigurationEntry> configurationEntries) {
        setAsJsonBinary(scope, Constants.VAR_CONFIGURATION_ENTRIES_TO_PUBLISH, configurationEntries);
    }

    static List<ConfigurationEntry> getConfigurationEntriesToPublish(VariableScope scope) {
        Type type = new TypeToken<List<ConfigurationEntry>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_CONFIGURATION_ENTRIES_TO_PUBLISH, type);
    }

    static void setCreatedOrUpdatedServiceBroker(VariableScope scope, CloudServiceBroker serviceBroker) {
        setAsJsonBinary(scope, Constants.VAR_CREATED_OR_UPDATED_SERVICE_BROKER, serviceBroker);
    }

    public static CloudServiceBroker getCreatedOrUpdatedServiceBroker(VariableScope scope) {
        return getFromJsonBinary(scope, Constants.VAR_CREATED_OR_UPDATED_SERVICE_BROKER, CloudServiceBroker.class);
    }

    public static CloudServiceBroker getServiceBrokersToCreateForModule(VariableScope scope, String moduleName) {
        return getFromJsonBinary(scope, Constants.VAR_APP_SERVICE_BROKER_VAR_PREFIX + moduleName, CloudServiceBroker.class);
    }

    public static List<String> getCreatedOrUpdatedServiceBrokerNames(VariableScope scope) {
        List<Module> allModulesToDeploy = getAllModulesToDeploy(scope);
        return allModulesToDeploy.stream()
            .map(module -> getServiceBrokersToCreateForModule(scope, module.getName()))
            .filter(Objects::nonNull)
            .map(CloudServiceBroker::getName)
            .collect(Collectors.toList());
    }

    public static List<ConfigurationEntry> getDeletedEntries(VariableScope scope) {
        Type type = new TypeToken<List<ConfigurationEntry>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_DELETED_ENTRIES, type, Collections.emptyList());
    }

    static List<ConfigurationEntry> getDeletedEntriesFromProcess(FlowableFacade flowableFacade, String processInstanceId) {
        HistoricVariableInstance deletedEntries = flowableFacade.getHistoricVariableInstance(processInstanceId,
            Constants.VAR_DELETED_ENTRIES);
        if (deletedEntries == null) {
            return Collections.emptyList();
        }
        byte[] deletedEntriesByteArray = (byte[]) deletedEntries.getValue();
        return Arrays.asList(JsonUtil.fromJsonBinary(deletedEntriesByteArray, ConfigurationEntry[].class));
    }

    static List<ConfigurationEntry> getDeletedEntriesFromAllProcesses(VariableScope scope, FlowableFacade flowableFacade) {
        List<ConfigurationEntry> configurationEntries = new ArrayList<>(
            StepsUtil.getDeletedEntriesFromProcess(flowableFacade, StepsUtil.getCorrelationId(scope)));
        List<String> subProcessIds = flowableFacade.getHistoricSubProcessIds(StepsUtil.getCorrelationId(scope));
        for (String subProcessId : subProcessIds) {
            configurationEntries.addAll(getDeletedEntriesFromProcess(flowableFacade, subProcessId));
        }
        return configurationEntries;
    }

    static void setDeletedEntries(VariableScope scope, List<ConfigurationEntry> deletedEntries) {
        setAsJsonBinary(scope, Constants.VAR_DELETED_ENTRIES, deletedEntries);
    }

    public static List<ConfigurationEntry> getPublishedEntries(VariableScope scope) {
        Type type = new TypeToken<List<ConfigurationEntry>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_PUBLISHED_ENTRIES, type);
    }

    static List<ConfigurationEntry> getPublishedEntriesFromProcess(FlowableFacade flowableFacade, String processInstanceId) {
        HistoricVariableInstance publishedEntries = flowableFacade.getHistoricVariableInstance(processInstanceId,
            Constants.VAR_PUBLISHED_ENTRIES);
        if (publishedEntries == null) {
            return Collections.emptyList();
        }
        byte[] binaryJson = (byte[]) publishedEntries.getValue();
        return Arrays.asList(JsonUtil.fromJsonBinary(binaryJson, ConfigurationEntry[].class));
    }

    static List<ConfigurationEntry> getPublishedEntriesFromSubProcesses(VariableScope scope, FlowableFacade flowableFacade) {
        List<ConfigurationEntry> result = new ArrayList<>();
        List<String> subProcessIds = flowableFacade.getHistoricSubProcessIds(StepsUtil.getCorrelationId(scope));
        for (String subProcessId : subProcessIds) {
            result.addAll(getPublishedEntriesFromProcess(flowableFacade, subProcessId));
        }
        return result;
    }

    static void setPublishedEntries(VariableScope scope, List<ConfigurationEntry> publishedEntries) {
        setAsJsonBinary(scope, Constants.VAR_PUBLISHED_ENTRIES, publishedEntries);
    }

    static void setServiceUrlToRegister(VariableScope scope, ServiceUrl serviceUrl) {
        setAsJsonBinary(scope, Constants.VAR_SERVICE_URL_TO_REGISTER, serviceUrl);
    }

    public static ServiceUrl getServiceUrlToRegister(VariableScope scope) {
        return getFromJsonBinary(scope, Constants.VAR_SERVICE_URL_TO_REGISTER, ServiceUrl.class);
    }

    public static void setVariableInParentProcess(DelegateExecution context, String variablePrefix, Object variableValue) {
        String moduleName = StepsUtil.getApp(context)
            .getModuleName();
        if (moduleName == null) {
            throw new IllegalStateException("Not able to determine module name.");
        }
        String exportedVariableName = variablePrefix + moduleName;

        RuntimeService runtimeService = Context.getProcessEngineConfiguration()
            .getRuntimeService();

        String superExecutionId = context.getParentId();
        Execution superExecutionResult = runtimeService.createExecutionQuery()
            .executionId(superExecutionId)
            .singleResult();
        superExecutionId = superExecutionResult.getSuperExecutionId();

        byte[] binaryJson = variableValue == null ? null : JsonUtil.toJsonBinary(variableValue);
        runtimeService.setVariable(superExecutionId, exportedVariableName, binaryJson);
    }

    public static ServiceUrl getServiceUrlToRegisterForModule(VariableScope scope, String moduleName) {
        return getFromJsonBinary(scope, Constants.VAR_APP_SERVICE_URL_VAR_PREFIX + moduleName, ServiceUrl.class);
    }

    public static List<String> getRegisteredServiceUrlsNames(VariableScope scope) {
        List<Module> allModulesToDeploy = getAllModulesToDeploy(scope);
        return allModulesToDeploy.stream()
            .map(module -> getServiceUrlToRegisterForModule(scope, module.getName()))
            .filter(Objects::nonNull)
            .map(ServiceUrl::getServiceName)
            .collect(Collectors.toList());
    }

    static void setDeployedMta(VariableScope scope, DeployedMta deployedMta) {
        setAsJsonBinary(scope, Constants.VAR_DEPLOYED_MTA, deployedMta);
    }

    protected static DeployedMta getDeployedMta(VariableScope scope) {
        return getFromJsonBinary(scope, Constants.VAR_DEPLOYED_MTA, DeployedMta.class);
    }

    static Map<String, Set<Integer>> getAllocatedPorts(VariableScope scope) {
        Type type = new TypeToken<Map<String, Set<Integer>>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_ALLOCATED_PORTS, type);
    }

    static void setAllocatedPorts(VariableScope scope, Map<String, Set<Integer>> allocatedPorts) {
        setAsJsonBinary(scope, Constants.VAR_ALLOCATED_PORTS, allocatedPorts);
    }

    static void setXsPlaceholderReplacementValues(VariableScope scope, Map<String, Object> replacementValues) {
        setAsJsonBinary(scope, Constants.VAR_XS_PLACEHOLDER_REPLACEMENT_VALUES, replacementValues);
    }

    static Map<String, Object> getXsPlaceholderReplacementValues(VariableScope scope) {
        String json = new String(getObject(scope, Constants.VAR_XS_PLACEHOLDER_REPLACEMENT_VALUES), StandardCharsets.UTF_8);
        // JsonUtil.convertJsonToMap does some magic under the hood that converts doubles to integers whenever possible. We need it for
        // SupportedParameters.XSA_ROUTER_PORT_PLACEHOLDER. That's why we can't use getFromJsonBinary here.
        return JsonUtil.convertJsonToMap(json);
    }

    static XsPlaceholderResolver getXsPlaceholderResolver(VariableScope scope) {
        Map<String, Object> replacementValues = getXsPlaceholderReplacementValues(scope);
        XsPlaceholderResolver resolver = new XsPlaceholderResolver();
        resolver.setControllerEndpoint((String) replacementValues.get(SupportedParameters.XSA_CONTROLLER_ENDPOINT_PLACEHOLDER));
        resolver.setRouterPort((int) replacementValues.get(SupportedParameters.XSA_ROUTER_PORT_PLACEHOLDER));
        resolver.setAuthorizationEndpoint((String) replacementValues.get(SupportedParameters.XSA_AUTHORIZATION_ENDPOINT_PLACEHOLDER));
        resolver.setDeployServiceUrl((String) replacementValues.get(SupportedParameters.XSA_DEPLOY_SERVICE_URL_PLACEHOLDER));
        resolver.setProtocol((String) replacementValues.get(SupportedParameters.XSA_PROTOCOL_PLACEHOLDER));
        resolver.setDefaultDomain((String) replacementValues.get(SupportedParameters.XSA_DEFAULT_DOMAIN_PLACEHOLDER));
        return resolver;
    }

    public static DeploymentDescriptor getDeploymentDescriptor(VariableScope scope) {
        return getFromJsonString(scope, Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR, DeploymentDescriptor.class);
    }

    public static DeploymentDescriptor getDeploymentDescriptorWithSystemParameters(VariableScope scope) {
        return getFromJsonString(scope, Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS, DeploymentDescriptor.class);
    }

    public static DeploymentDescriptor getCompleteDeploymentDescriptor(VariableScope scope) {
        return getFromJsonString(scope, Constants.VAR_COMPLETE_MTA_DEPLOYMENT_DESCRIPTOR, DeploymentDescriptor.class);
    }

    public static Module findModuleInDeploymentDescriptor(VariableScope scope, String module) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(scope);
        DeploymentDescriptor deploymentDescriptor = getCompleteDeploymentDescriptor(scope);
        return handlerFactory.getDescriptorHandler()
            .findModule(deploymentDescriptor, module);
    }

    @SuppressWarnings("unchecked")
    public static List<ExtensionDescriptor> getExtensionDescriptorChain(VariableScope scope) {
        List<byte[]> binaryYamlList = (List<byte[]>) scope.getVariable(Constants.VAR_MTA_EXTENSION_DESCRIPTOR_CHAIN);
        List<String> yamlList = binaryYamlList.stream()
            .map(binaryYaml -> new String(binaryYaml, StandardCharsets.UTF_8))
            .collect(Collectors.toList());
        return parseExtensionDescriptors(yamlList);
    }

    private static List<ExtensionDescriptor> parseExtensionDescriptors(List<String> yamlList) {
        DescriptorParserFacade descriptorParserFacade = new DescriptorParserFacade();
        return yamlList.stream()
            .map(descriptorParserFacade::parseExtensionDescriptor)
            .collect(Collectors.toList());
    }

    public static void setDeploymentDescriptor(VariableScope scope, DeploymentDescriptor deploymentDescriptor) {
        setAsJsonString(scope, Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);
    }

    public static void setDeploymentDescriptorWithSystemParameters(VariableScope scope, DeploymentDescriptor deploymentDescriptor) {
        setAsJsonString(scope, Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS, deploymentDescriptor);
    }

    public static void setCompleteDeploymentDescriptor(VariableScope scope, DeploymentDescriptor deploymentDescriptor) {
        setAsJsonString(scope, Constants.VAR_COMPLETE_MTA_DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);
    }

    static void setExtensionDescriptorChain(VariableScope scope, List<ExtensionDescriptor> extensionDescriptors) {
        scope.setVariable(Constants.VAR_MTA_EXTENSION_DESCRIPTOR_CHAIN, toBinaryYamlList(extensionDescriptors));
    }

    private static List<byte[]> toBinaryYamlList(List<?> objects) {
        return objects.stream()
            .map(StepsUtil::toBinaryYaml)
            .collect(Collectors.toList());
    }

    private static byte[] toBinaryYaml(Object object) {
        String yaml = YamlUtil.convertToYaml(object);
        return yaml.getBytes(StandardCharsets.UTF_8);
    }

    static void setVcapAppPropertiesChanged(VariableScope scope, boolean state) {
        scope.setVariable(Constants.VAR_VCAP_APP_PROPERTIES_CHANGED, state);
    }

    static boolean getVcapAppPropertiesChanged(VariableScope scope) {
        return getBoolean(scope, Constants.VAR_VCAP_APP_PROPERTIES_CHANGED, false);
    }

    static void setVcapServicesPropertiesChanged(VariableScope scope, boolean state) {
        scope.setVariable(Constants.VAR_VCAP_SERVICES_PROPERTIES_CHANGED, state);
    }

    static boolean getVcapServicesPropertiesChanged(VariableScope scope) {
        return getBoolean(scope, Constants.VAR_VCAP_SERVICES_PROPERTIES_CHANGED, false);
    }

    static void setUserPropertiesChanged(VariableScope scope, boolean state) {
        scope.setVariable(Constants.VAR_USER_PROPERTIES_CHANGED, state);
    }

    static boolean getUserPropertiesChanged(VariableScope scope) {
        return getBoolean(scope, Constants.VAR_USER_PROPERTIES_CHANGED, false);
    }

    public static CloudApplicationExtended getApp(VariableScope scope) {
        return getFromJsonString(scope, Constants.VAR_APP_TO_DEPLOY, CloudApplicationExtended.class);
    }

    static void setApp(VariableScope scope, CloudApplicationExtended app) {
        setAsJsonString(scope, Constants.VAR_APP_TO_DEPLOY, app);
    }

    public static void setModuleToDeploy(VariableScope scope, Module module) {
        setAsJsonBinary(scope, Constants.VAR_MODULE_TO_DEPLOY, module);
    }

    public static Module getModuleToDeploy(VariableScope scope) {
        return getFromJsonBinary(scope, Constants.VAR_MODULE_TO_DEPLOY, Module.class);
    }

    static CloudTask getTask(VariableScope scope) {
        List<CloudTask> tasks = StepsUtil.getTasksToExecute(scope);
        int index = (Integer) scope.getVariable(Constants.VAR_TASKS_INDEX);
        return tasks.get(index);
    }

    static CloudApplication getExistingApp(VariableScope scope) {
        return getFromJsonBinary(scope, Constants.VAR_EXISTING_APP, CloudApplication.class);
    }

    static void setExistingApp(VariableScope scope, CloudApplication app) {
        setAsJsonBinary(scope, Constants.VAR_EXISTING_APP, app);
    }

    static Set<ApplicationStateAction> getAppStateActionsToExecute(VariableScope scope) {
        @SuppressWarnings("unchecked")
        Set<String> actionsAsStrings = (Set<String>) scope.getVariable(Constants.VAR_APP_STATE_ACTIONS_TO_EXECUTE);
        return actionsAsStrings.stream()
            .map(ApplicationStateAction::valueOf)
            .collect(Collectors.toSet());
    }

    static void setAppStateActionsToExecute(VariableScope scope, Set<ApplicationStateAction> actions) {
        Set<String> actionsAsStrings = actions.stream()
            .map(ApplicationStateAction::toString)
            .collect(Collectors.toSet());
        scope.setVariable(Constants.VAR_APP_STATE_ACTIONS_TO_EXECUTE, actionsAsStrings);
    }

    public static void setSubProcessId(VariableScope scope, String subProcessId) {
        scope.setVariable(Constants.VAR_SUBPROCESS_ID, subProcessId);
    }

    public static String getSubProcessId(VariableScope scope) {
        return getString(scope, Constants.VAR_SUBPROCESS_ID);
    }

    static String getParentProcessId(VariableScope scope) {
        return getString(scope, Constants.VAR_PARENTPROCESS_ID);
    }

    static void saveAppLogs(DelegateExecution context, CloudControllerClient client, RecentLogsRetriever recentLogsRetriever,
        CloudApplication app, Logger logger, ProcessLoggerProvider processLoggerProvider) {
        List<ApplicationLog> recentLogs = recentLogsRetriever.getRecentLogs(client, app.getName());
        if (recentLogs != null) {
            recentLogs.forEach(log -> appLog(context, app.getName(), log.toString(), logger, processLoggerProvider));
        }
    }

    static void appLog(DelegateExecution context, String appName, String message, Logger logger,
        ProcessLoggerProvider processLoggerProvider) {
        getLogger(context, appName, processLoggerProvider).debug(getLoggerPrefix(logger) + "[" + appName + "] " + message);
    }

    public static StartingInfo getStartingInfo(VariableScope scope) {
        String className = getString(scope, Constants.VAR_STARTING_INFO_CLASSNAME);
        return getFromJsonBinary(scope, Constants.VAR_STARTING_INFO, getStartingInfoClass(className));
    }

    public static void setStartingInfo(VariableScope scope, StartingInfo startingInfo) {
        setAsJsonBinary(scope, Constants.VAR_STARTING_INFO, startingInfo);
        String className = startingInfo != null ? startingInfo.getClass()
            .getName() : StartingInfo.class.getName();
        scope.setVariable(Constants.VAR_STARTING_INFO_CLASSNAME, className);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends StartingInfo> getStartingInfoClass(String className) {
        try {
            return (Class<? extends StartingInfo>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    static StreamingLogToken getStreamingLogsToken(VariableScope scope) {
        return getFromJsonBinary(scope, Constants.VAR_STREAMING_LOGS_TOKEN, StreamingLogToken.class);
    }

    static void setStreamingLogsToken(VariableScope scope, StreamingLogToken streamingLogToken) {
        setAsJsonBinary(scope, Constants.VAR_STREAMING_LOGS_TOKEN, streamingLogToken);
    }

    static void setMtaArchiveModules(VariableScope scope, Set<String> mtaArchiveModules) {
        setAsJsonBinary(scope, Constants.VAR_MTA_ARCHIVE_MODULES, mtaArchiveModules);
    }

    static Set<String> getMtaArchiveModules(VariableScope scope) {
        Type type = new TypeToken<Set<String>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_MTA_ARCHIVE_MODULES, type);
    }

    static void setMtaModules(VariableScope scope, Set<String> mtaModules) {
        setAsJsonBinary(scope, Constants.VAR_MTA_MODULES, mtaModules);
    }

    static Set<String> getMtaModules(VariableScope scope) {
        Type type = new TypeToken<Set<String>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_MTA_MODULES, type);
    }

    public static String getCorrelationId(VariableScope scope) {
        return getString(scope, Constants.VAR_CORRELATION_ID);
    }

    public static String getTaskId(VariableScope scope) {
        return getString(scope, Constants.TASK_ID);
    }

    public static ErrorType getErrorType(VariableScope scope) {
        return getEnum(scope, Constants.VAR_ERROR_TYPE, ErrorType::valueOf);
    }

    static void setErrorType(VariableScope scope, ErrorType errorType) {
        setEnum(scope, Constants.VAR_ERROR_TYPE, errorType);
    }

    static StepPhase getStepPhase(VariableScope scope) {
        StepPhase stepPhase = getEnum(scope, Constants.VAR_STEP_PHASE, StepPhase::valueOf);
        return stepPhase == null ? StepPhase.EXECUTE : stepPhase;
    }

    public static void setStepPhase(VariableScope scope, StepPhase stepPhase) {
        setEnum(scope, Constants.VAR_STEP_PHASE, stepPhase);
    }

    public static String getLoggerPrefix(Logger logger) {
        String name = logger.getName();
        return "[" + name.substring(name.lastIndexOf('.') + 1) + "] ";
    }

    public static void setArchiveFileId(VariableScope scope, String uploadedMtarId) {
        scope.setVariable(Constants.PARAM_APP_ARCHIVE_ID, uploadedMtarId);
    }

    public static String getServiceId(VariableScope scope) {
        return getString(scope, com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SERVICE_ID);
    }

    public static void incrementVariable(VariableScope scope, String name) {
        int value = getInteger(scope, name);
        scope.setVariable(name, value + 1);
    }

    public static final String DEPLOY_ID_PREFIX = "deploy-";

    static ApplicationCloudModelBuilder getApplicationCloudModelBuilder(VariableScope scope, UserMessageLogger stepLogger) {
        CloudModelConfiguration configuration = getCloudBuilderConfiguration(scope, true);
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(scope);

        String deployId = DEPLOY_ID_PREFIX + getCorrelationId(scope);

        XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(scope);

        DeploymentDescriptor deploymentDescriptor = StepsUtil.getCompleteDeploymentDescriptor(scope);
        DeployedMta deployedMta = StepsUtil.getDeployedMta(scope);

        return handlerFactory.getApplicationCloudModelBuilder(deploymentDescriptor, configuration, deployedMta, xsPlaceholderResolver,
            deployId, stepLogger);
    }

    static List<String> getDomainsFromApps(VariableScope scope, DeploymentDescriptor descriptor,
        ApplicationCloudModelBuilder applicationCloudModelBuilder, List<? extends Module> modules,
        ModuleToDeployHelper moduleToDeployHelper) {
        XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(scope);

        String defaultDomain = (String) descriptor.getParameters()
            .get(SupportedParameters.DEFAULT_DOMAIN);

        Set<String> domains = new TreeSet<>();
        for (Module module : modules) {
            if (!moduleToDeployHelper.isApplication(module)) {
                continue;
            }
            ParametersChainBuilder parametersChainBuilder = new ParametersChainBuilder(StepsUtil.getCompleteDeploymentDescriptor(scope));
            List<String> appDomains = applicationCloudModelBuilder
                .getApplicationDomains(parametersChainBuilder.buildModuleChain(module.getName()), module);
            if (appDomains != null) {
                domains.addAll(appDomains);
            }
        }

        if (xsPlaceholderResolver.getDefaultDomain() != null) {
            domains.remove(xsPlaceholderResolver.getDefaultDomain());
        }

        if (defaultDomain != null) {
            domains.remove(defaultDomain);
        }

        return new ArrayList<>(domains);
    }

    static ServicesCloudModelBuilder getServicesCloudModelBuilder(VariableScope scope) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(scope);
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getCompleteDeploymentDescriptor(scope);

        return handlerFactory.getServicesCloudModelBuilder(deploymentDescriptor);
    }

    static ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(VariableScope scope) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(scope);
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getCompleteDeploymentDescriptor(scope);
        return handlerFactory.getServiceKeysCloudModelBuilder(deploymentDescriptor);
    }

    protected static CloudModelConfiguration getCloudBuilderConfiguration(VariableScope scope, boolean prettyPrinting) {
        Boolean portBasedRouting = getBoolean(scope, Constants.VAR_PORT_BASED_ROUTING, false);
        CloudModelConfiguration configuration = new CloudModelConfiguration();
        configuration.setPortBasedRouting(portBasedRouting);
        configuration.setPrettyPrinting(prettyPrinting);
        return configuration;
    }

    static String getGitRepoRef(VariableScope scope) {
        Object gitRepoConfigObject = scope.getVariable(Constants.VAR_GIT_REPOSITORY_CONFIG_MAP);
        if (gitRepoConfigObject == null) {
            return (String) scope.getVariable(Constants.PARAM_GIT_REF);
        }
        @SuppressWarnings("unchecked")
        Map<String, String> gitRepoConfigMap = (Map<String, String>) gitRepoConfigObject;
        return gitRepoConfigMap.get(Constants.PARAM_GIT_REF);
    }

    static String getGitRepoUri(VariableScope scope) {
        Object gitRepoConfigObject = scope.getVariable(Constants.VAR_GIT_REPOSITORY_CONFIG_MAP);
        if (gitRepoConfigObject == null) {
            return (String) scope.getVariable(Constants.PARAM_GIT_URI);
        }
        @SuppressWarnings("unchecked")
        Map<String, String> gitRepoConfigMap = (Map<String, String>) gitRepoConfigObject;
        return gitRepoConfigMap.get(Constants.PARAM_GIT_URI);
    }

    static void setUseIdleUris(VariableScope scope, boolean state) {
        scope.setVariable(Constants.VAR_USE_IDLE_URIS, state);
    }

    static boolean getUseIdleUris(VariableScope scope) {
        return getBoolean(scope, Constants.VAR_USE_IDLE_URIS, false);
    }

    public static void setDeleteIdleUris(VariableScope scope, boolean state) {
        scope.setVariable(Constants.VAR_DELETE_IDLE_URIS, state);
    }

    static boolean getDeleteIdleUris(VariableScope scope) {
        return getBoolean(scope, Constants.VAR_DELETE_IDLE_URIS, false);
    }

    static boolean getUseNamespacesForService(VariableScope scope) {
        return getBoolean(scope, Constants.PARAM_USE_NAMESPACES_FOR_SERVICES, false);
    }

    static boolean getUseNamespaces(VariableScope scope) {
        return getBoolean(scope, Constants.PARAM_USE_NAMESPACES, false);
    }

    public static boolean getSkipUpdateConfigurationEntries(DelegateExecution context) {
        return getBoolean(context, Constants.VAR_SKIP_UPDATE_CONFIGURATION_ENTRIES, false);
    }

    public static void setSkipUpdateConfigurationEntries(VariableScope scope, boolean update) {
        scope.setVariable(Constants.VAR_SKIP_UPDATE_CONFIGURATION_ENTRIES, update);
    }

    public static boolean getSkipUpdateConfigurationEntries(VariableScope scope) {
        return getBoolean(scope, Constants.VAR_SKIP_UPDATE_CONFIGURATION_ENTRIES, false);
    }

    public static void setSkipManageServiceBroker(VariableScope scope, boolean manage) {
        scope.setVariable(Constants.VAR_SKIP_MANAGE_SERVICE_BROKER, manage);
    }

    public static boolean getSkipManageServiceBroker(VariableScope scope) {
        return getBoolean(scope, Constants.VAR_SKIP_MANAGE_SERVICE_BROKER);
    }

    public static void setServicesData(VariableScope scope, Map<String, CloudServiceExtended> servicesData) {
        scope.setVariable(Constants.VAR_SERVICES_DATA, JsonUtil.toJsonBinary(servicesData));
    }

    public static Map<String, CloudServiceExtended> getServicesData(VariableScope scope) {
        Type type = new TypeToken<Map<String, CloudServiceExtended>>() {
        }.getType();
        return getFromJsonBinary(scope, Constants.VAR_SERVICES_DATA, type, Collections.emptyMap());
    }

    public static CloudApplication getBoundApplication(List<CloudApplication> applications, UUID appGuid) {
        return applications.stream()
            .filter(app -> hasGuid(app, appGuid))
            .findFirst()
            .orElse(null);
    }

    private static boolean hasGuid(CloudApplication app, UUID appGuid) {
        return app.getMeta()
            .getGuid()
            .equals(appGuid);
    }

    public static boolean shouldDeleteServices(VariableScope scope) {
        return getBoolean(scope, Constants.PARAM_DELETE_SERVICES, false);
    }

    public static CloudServiceExtended getServiceToProcess(VariableScope scope) {
        return getFromJsonString(scope, Constants.VAR_SERVICE_TO_PROCESS, CloudServiceExtended.class);
    }

    public static void setServiceToProcess(CloudServiceExtended service, VariableScope scope) {
        setAsJsonString(scope, Constants.VAR_SERVICE_TO_PROCESS, service);
    }

    public static void setServiceActionsToExecute(List<ServiceAction> actions, VariableScope scope) {
        List<String> actionsStrings = actions.stream()
            .map(ServiceAction::toString)
            .collect(Collectors.toList());
        scope.setVariable(Constants.VAR_SERVICE_ACTIONS_TO_EXCECUTE, actionsStrings);
    }

    @SuppressWarnings("unchecked")
    public static List<ServiceAction> getServiceActionsToExecute(VariableScope execution) {
        List<String> actionStrings = (List<String>) execution.getVariable(Constants.VAR_SERVICE_ACTIONS_TO_EXCECUTE);
        return actionStrings.stream()
            .map(ServiceAction::valueOf)
            .collect(Collectors.toList());
    }

    public static void isServiceUpdated(boolean isUpdated, VariableScope scope) {
        scope.setVariable(Constants.VAR_IS_SERVICE_UPDATED, isUpdated);
    }

    public static boolean getIsServiceUpdated(VariableScope scope) {
        return getBoolean(scope, Constants.VAR_IS_SERVICE_UPDATED, false);
    }

    public static void setServiceToProcessName(String name, VariableScope scope) {
        scope.setVariable(Constants.VAR_SERVICE_TO_PROCESS_NAME, name);
    }

    public static String getServiceToProcessName(VariableScope scope) {
        return getString(scope, Constants.VAR_SERVICE_TO_PROCESS_NAME);
    }

    public static boolean getIsServiceUpdatedExportedVariable(VariableScope scope, String serviceName) {
        return getBoolean(scope, Constants.VAR_IS_SERVICE_UPDATED_VAR_PREFIX + serviceName, false);
    }

    public static List<String> getModulesForDeployment(VariableScope scope) {
        return getVariableWithCommaSepearator(scope, Constants.PARAM_MODULES_FOR_DEPLOYMENT);
    }

    public static List<String> getResourcesForDeployment(VariableScope scope) {
        return getVariableWithCommaSepearator(scope, Constants.PARAM_RESOURCES_FOR_DEPLOYMENT);
    }

    private static List<String> getVariableWithCommaSepearator(VariableScope scope, String variableName) {
        String variableWithCommaSeparator = (String) scope.getVariable(variableName);
        if (variableWithCommaSeparator == null) {
            return null;
        }
        return variableWithCommaSeparator.isEmpty() ? Collections.emptyList() : Arrays.asList(variableWithCommaSeparator.split(","));
    }

    public static void setUploadToken(UploadToken uploadToken, VariableScope scope) {
        setAsJsonString(scope, Constants.VAR_UPLOAD_TOKEN, uploadToken);
    }

    public static UploadToken getUploadToken(VariableScope scope) {
        return getFromJsonString(scope, Constants.VAR_UPLOAD_TOKEN, UploadToken.class);
    }

    static void setExecutedHooksForModule(VariableScope scope, String moduleName, Map<String, List<String>> moduleHooks) {
        setAsJsonBinary(scope, getExecutedHooksForModuleVariableName(moduleName), moduleHooks);
    }

    static Map<String, List<String>> getExecutedHooksForModule(VariableScope scope, String moduleName) {
        Type type = new TypeToken<Map<String, List<String>>>() {
        }.getType();
        return getFromJsonBinary(scope, getExecutedHooksForModuleVariableName(moduleName), type, Collections.emptyMap());
    }

    private static String getExecutedHooksForModuleVariableName(String moduleName) {
        return Constants.VAR_EXECUTED_HOOKS_FOR_PREFIX + moduleName;
    }

    static void setHooksForExecution(VariableScope scope, List<Hook> hooksForExecution) {
        setAsJsonStrings(scope, Constants.VAR_HOOKS_FOR_EXECUTION, hooksForExecution);
    }

    public static <E> E getEnum(VariableScope scope, String name, Function<String, E> factory) {
        String value = getObject(scope, name);
        return value == null ? null : factory.apply(value);
    }

    public static Boolean getBoolean(VariableScope scope, String name) {
        return getBoolean(scope, name, null);
    }

    public static Boolean getBoolean(VariableScope scope, String name, Boolean defaultValue) {
        return getObject(scope, name, defaultValue);
    }

    public static Integer getInteger(VariableScope scope, String name) {
        return getInteger(scope, name, null);
    }

    public static Integer getInteger(VariableScope scope, String name, Integer defaultValue) {
        return getObject(scope, name, defaultValue);
    }

    public static String getRequiredString(VariableScope scope, String name) {
        String value = getString(scope, name);
        if (value == null || value.isEmpty()) {
            throw new SLException(Messages.REQUIRED_PARAMETER_IS_MISSING, name);
        }
        return value;
    }

    public static String getString(VariableScope scope, String name) {
        return getString(scope, name, null);
    }

    public static String getString(VariableScope scope, String name, String defaultValue) {
        return getObject(scope, name, defaultValue);
    }

    public static <T> T getObject(VariableScope scope, String name) {
        return getObject(scope, name, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getObject(VariableScope scope, String name, T defaultValue) {
        T value = (T) scope.getVariable(name);
        return value != null ? value : defaultValue;
    }

    public static <T> T getFromJsonString(VariableScope scope, String name, Type type) {
        return getFromJsonString(scope, name, type, null);
    }

    public static <T> T getFromJsonString(VariableScope scope, String name, Type type, T defaultValue) {
        String stringJson = getString(scope, name);
        if (stringJson == null) {
            return defaultValue;
        }
        return JsonUtil.fromJson(stringJson, type);
    }

    public static <T> T getFromJsonBinary(VariableScope scope, String name, Type type) {
        return getFromJsonBinary(scope, name, type, null);
    }

    public static <T> T getFromJsonBinary(VariableScope scope, String name, Type type, T defaultValue) {
        byte[] jsonBinary = getObject(scope, name);
        if (jsonBinary == null) {
            return defaultValue;
        }
        String jsonString = new String(jsonBinary, StandardCharsets.UTF_8);
        return JsonUtil.fromJson(jsonString, type);
    }

    public static <T> List<T> getFromJsonStrings(VariableScope scope, String name, Type type) {
        return getFromJsonStrings(scope, name, type, Collections.emptyList());
    }

    public static <T> List<T> getFromJsonStrings(VariableScope scope, String name, Type type, List<T> defaultValue) {
        List<String> jsonStrings = getObject(scope, name);
        if (jsonStrings == null) {
            return defaultValue;
        }
        return jsonStrings.stream()
            .map(jsonString -> JsonUtil.<T> fromJson(jsonString, type))
            .collect(Collectors.toList());
    }

    public static <T> List<T> getFromJsonBinaries(VariableScope scope, String name, Type type) {
        return getFromJsonBinaries(scope, name, type, Collections.emptyList());
    }

    public static <T> List<T> getFromJsonBinaries(VariableScope scope, String name, Type type, List<T> defaultValue) {
        List<byte[]> jsonBinaries = getObject(scope, name);
        if (jsonBinaries == null) {
            return defaultValue;
        }
        return jsonBinaries.stream()
            .map(jsonBinary -> JsonUtil.<T> fromJsonBinary(jsonBinary, type))
            .collect(Collectors.toList());
    }

    public static void setEnum(VariableScope scope, String name, Object value) {
        if (value == null) {
            scope.setVariable(name, null);
            return;
        }
        scope.setVariable(name, value.toString());
    }

    public static void setAsJsonBinary(VariableScope scope, String name, Object value) {
        if (value == null) {
            scope.setVariable(name, null);
            return;
        }
        byte[] jsonBinary = JsonUtil.toJsonBinary(value);
        scope.setVariable(name, jsonBinary);
    }

    public static void setAsJsonString(VariableScope scope, String name, Object value) {
        if (value == null) {
            scope.setVariable(name, null);
            return;
        }
        String jsonString = JsonUtil.toJson(value);
        scope.setVariable(name, jsonString);
    }

    public static void setAsJsonStrings(VariableScope scope, String name, List<? extends Object> values) {
        if (values == null) {
            scope.setVariable(name, null);
            return;
        }
        List<String> jsonStrings = values.stream()
            .map(JsonUtil::toJson)
            .collect(Collectors.toList());
        scope.setVariable(name, jsonStrings);
    }

    public static void setAsJsonBinaries(VariableScope scope, String name, List<? extends Object> values) {
        if (values == null) {
            scope.setVariable(name, null);
            return;
        }
        List<byte[]> jsonBinaries = values.stream()
            .map(JsonUtil::toJsonBinary)
            .collect(Collectors.toList());
        scope.setVariable(name, jsonBinaries);
    }

}
