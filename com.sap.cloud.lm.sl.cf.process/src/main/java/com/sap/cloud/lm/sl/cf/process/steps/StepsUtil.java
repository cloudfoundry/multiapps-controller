package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.ServiceAction;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.BinaryJson;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.model.json.MapWithNumbersAdapterFactory;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;
import com.sap.cloud.lm.sl.mta.util.YamlUtil;

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

    public static String determineCurrentUser(DelegateExecution context, StepLogger stepLogger) {
        String userId = Authentication.getAuthenticatedUserId();
        String previousUser = (String) context.getVariable(Constants.VAR_USER);
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
                userId = (String) context.getVariable(Constants.PARAM_INITIATOR);
                stepLogger.debug(Messages.PROCESS_INITIATOR, userId);
                if (userId == null) {
                    throw new SLException(Messages.CANT_DETERMINE_CURRENT_USER);
                }
            }
        }
        // Set the current user in the context for use by later service tasks
        context.setVariable(Constants.VAR_USER, userId);

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

    public static MtaArchiveElements getMtaArchiveElements(DelegateExecution context) {
        return getFromJsonString(context, Constants.VAR_MTA_ARCHIVE_ELEMENTS, MtaArchiveElements.class, new MtaArchiveElements());
    }

    public static void setMtaArchiveElements(DelegateExecution context, MtaArchiveElements mtaArchiveElements) {
        setAsJsonString(context, Constants.VAR_MTA_ARCHIVE_ELEMENTS, mtaArchiveElements);
    }

    static InputStream getModuleContentAsStream(DelegateExecution context, String moduleName) {
        byte[] moduleContent = getModuleContent(context, moduleName);
        if (moduleContent == null) {
            throw new SLException(Messages.MODULE_CONTENT_NA, moduleName);
        }
        return new ByteArrayInputStream(moduleContent);
    }

    static byte[] getModuleContent(DelegateExecution context, String moduleName) {
        return getObject(context, getModuleContentVariable(moduleName));
    }

    static void setModuleContent(DelegateExecution context, String moduleName, byte[] moduleContent) {
        context.setVariable(getModuleContentVariable(moduleName), moduleContent);
    }

    public static ApplicationColor getDeployedMtaColor(DelegateExecution context) {
        return getEnum(context, Constants.VAR_DEPLOYED_MTA_COLOR, ApplicationColor::valueOf);
    }

    public static void setDeployedMtaColor(DelegateExecution context, ApplicationColor deployedMtaColor) {
        setEnum(context, Constants.VAR_DEPLOYED_MTA_COLOR, deployedMtaColor);
    }

    public static ApplicationColor getMtaColor(DelegateExecution context) {
        return getEnum(context, Constants.VAR_MTA_COLOR, ApplicationColor::valueOf);
    }

    public static void setMtaColor(DelegateExecution context, ApplicationColor mtaColor) {
        setEnum(context, Constants.VAR_MTA_COLOR, mtaColor);
    }

    private static String getModuleContentVariable(String moduleName) {
        return Constants.VAR_MTA_MODULE_CONTENT_PREFIX + moduleName;
    }

    private static BinaryJson getBinaryJsonForMtaModel() {
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new MapWithNumbersAdapterFactory())
            .create();
        return new BinaryJson(gson);
    }

    static Platform getPlatform(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_PLATFORM);

        int majorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
        switch (majorSchemaVersion) {
            case 2:
                return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v2.Platform.class);
            case 3:
                return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v3.Platform.class);
            default:
                return null;
        }
    }

    static void setPlatform(DelegateExecution context, Platform platform) {
        context.setVariable(Constants.VAR_PLATFORM, getBinaryJsonForMtaModel().marshal(platform));
    }

    public static HandlerFactory getHandlerFactory(DelegateExecution context) {
        int majorSchemaVersion = getInteger(context, Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
        return new HandlerFactory(majorSchemaVersion);
    }

    public static String getOrg(DelegateExecution context) {
        return getString(context, Constants.VAR_ORG);
    }

    public static String getSpaceId(DelegateExecution context) {
        return getString(context, com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID);
    }

    public static void setSpaceId(DelegateExecution context, String spaceId) {
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID, spaceId);
    }

    public static String getSpace(DelegateExecution context) {
        return getString(context, Constants.VAR_SPACE);
    }

    static String getNewMtaVersion(DelegateExecution context) {
        return getString(context, Constants.VAR_NEW_MTA_VERSION);
    }

    static void setNewMtaVersion(DelegateExecution context, String version) {
        context.setVariable(Constants.VAR_NEW_MTA_VERSION, version);
    }

    public static List<String> getCustomDomains(DelegateExecution context) {
        return getArrayVariableAsList(context, Constants.VAR_CUSTOM_DOMAINS);
    }

    static void setCustomDomains(DelegateExecution context, List<String> customDomains) {
        setArrayVariableFromCollection(context, Constants.VAR_CUSTOM_DOMAINS, customDomains);
    }

    public static List<CloudServiceExtended> getServicesToCreate(DelegateExecution context) {
        return getFromJsonStrings(context, Constants.VAR_SERVICES_TO_CREATE, CloudServiceExtended.class);
    }

    static void setServicesToCreate(DelegateExecution context, List<CloudServiceExtended> services) {
        setAsJsonStrings(context, Constants.VAR_SERVICES_TO_CREATE, services);
    }

    public static List<CloudServiceExtended> getServicesToBind(DelegateExecution context) {
        return getFromJsonStrings(context, Constants.VAR_SERVICES_TO_BIND, CloudServiceExtended.class);
    }

    static void setServicesToBind(DelegateExecution context, List<CloudServiceExtended> services) {
        setAsJsonStrings(context, Constants.VAR_SERVICES_TO_BIND, services);
    }

    static void setServicesToPoll(DelegateExecution context, List<CloudServiceExtended> servicesToPoll) {
        setAsJsonBinary(context, Constants.VAR_SERVICES_TO_POLL, servicesToPoll);
    }

    static List<CloudServiceExtended> getServicesToPoll(DelegateExecution context) {
        Type type = new TypeToken<List<CloudServiceExtended>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_SERVICES_TO_POLL, type);
    }

    static void setTriggeredServiceOperations(DelegateExecution context, Map<String, ServiceOperationType> triggeredServiceOperations) {
        setAsJsonBinary(context, Constants.VAR_TRIGGERED_SERVICE_OPERATIONS, triggeredServiceOperations);
    }

    public static Map<String, ServiceOperationType> getTriggeredServiceOperations(DelegateExecution context) {
        Type type = new TypeToken<Map<String, ServiceOperationType>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_TRIGGERED_SERVICE_OPERATIONS, type);
    }

    public static Map<String, List<ServiceKey>> getServiceKeysToCreate(DelegateExecution context) {
        Type type = new TypeToken<Map<String, List<ServiceKey>>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_SERVICE_KEYS_TO_CREATE, type);
    }

    static void setServiceKeysToCreate(DelegateExecution context, Map<String, List<ServiceKey>> serviceKeys) {
        setAsJsonBinary(context, Constants.VAR_SERVICE_KEYS_TO_CREATE, serviceKeys);
    }

    static List<CloudApplication> getDeployedApps(DelegateExecution context) {
        Type type = new TypeToken<List<CloudApplication>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_DEPLOYED_APPS, type);
    }

    static void setDeployedApps(DelegateExecution context, List<CloudApplication> apps) {
        setAsJsonBinary(context, Constants.VAR_DEPLOYED_APPS, apps);
    }

    public static List<String> getAppsToDeploy(DelegateExecution context) {
        List<String> arrayVariableAsList = getArrayVariableAsList(context, Constants.VAR_APPS_TO_DEPLOY);
        return arrayVariableAsList != null ? arrayVariableAsList : Collections.emptyList();
    }

    public static void setAppsToDeploy(DelegateExecution context, List<String> apps) {
        setArrayVariableFromCollection(context, Constants.VAR_APPS_TO_DEPLOY, apps);
    }

    public static List<Module> getModulesToDeploy(DelegateExecution context) {
        return getFromJsonBinaries(context, Constants.VAR_MODULES_TO_DEPLOY, getModuleToDeployClass(context));
    }

    public static void setModulesToDeploy(DelegateExecution context, List<? extends Module> modules) {
        setAsJsonBinaries(context, Constants.VAR_MODULES_TO_DEPLOY, modules);
    }

    private static void setModuleToDeployClass(DelegateExecution context, List<? extends Module> modules) {
        String className = (!CollectionUtils.isEmpty(modules)) ? modules.get(0)
            .getClass()
            .getName() : Module.class.getName();
        context.setVariable(Constants.VAR_MODULES_TO_DEPLOY_CLASSNAME, className);
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends Module> getModuleToDeployClass(DelegateExecution context) {
        String className = (String) context.getVariable(Constants.VAR_MODULES_TO_DEPLOY_CLASSNAME);
        try {
            return (Class<? extends Module>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<Module> getAllModulesToDeploy(DelegateExecution context) {
        return getFromJsonBinaries(context, Constants.VAR_ALL_MODULES_TO_DEPLOY, getModuleToDeployClass(context));
    }

    public static void setAllModulesToDeploy(DelegateExecution context, List<? extends Module> modules) {
        setAsJsonBinaries(context, Constants.VAR_ALL_MODULES_TO_DEPLOY, modules);
        setModuleToDeployClass(context, modules);
    }

    public static List<Module> getIteratedModulesInParallel(DelegateExecution context) {
        return getFromJsonBinaries(context, Constants.VAR_ITERATED_MODULES_IN_PARALLEL, getModuleToDeployClass(context));
    }

    public static void setIteratedModulesInParallel(DelegateExecution context, List<? extends Module> modules) {
        setAsJsonBinaries(context, Constants.VAR_ITERATED_MODULES_IN_PARALLEL, modules);
    }

    public static void setModulesToIterateInParallel(DelegateExecution context, List<? extends Module> modules) {
        setAsJsonBinaries(context, Constants.VAR_MODULES_TO_ITERATE_IN_PARALLEL, modules);
    }

    public static void setDeploymentMode(DelegateExecution context, DeploymentMode deploymentMode) {
        context.setVariable(Constants.VAR_DEPLOYMENT_MODE, deploymentMode);
    }

    static void setServiceKeysCredentialsToInject(DelegateExecution context,
        Map<String, Map<String, String>> serviceKeysCredentialsToInject) {
        setAsJsonBinary(context, Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT, serviceKeysCredentialsToInject);
    }

    static Map<String, Map<String, String>> getServiceKeysCredentialsToInject(DelegateExecution context) {
        Type type = new TypeToken<Map<String, Map<String, String>>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT, type);
    }

    public static List<CloudApplication> getUpdatedSubscribers(DelegateExecution context) {
        Type type = new TypeToken<List<CloudApplicationExtended>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_UPDATED_SUBSCRIBERS, type);
    }

    static void setUpdatedSubscribers(DelegateExecution context, List<CloudApplication> apps) {
        setAsJsonBinary(context, Constants.VAR_UPDATED_SUBSCRIBERS, apps);
    }

    public static List<CloudApplication> getServiceBrokerSubscribersToRestart(DelegateExecution context) {
        Type type = new TypeToken<List<CloudApplication>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS, type);
    }

    static CloudApplication getServiceBrokerSubscriberToRestart(DelegateExecution context) {
        List<CloudApplication> apps = getServiceBrokerSubscribersToRestart(context);
        int index = (Integer) context.getVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX);
        return apps.get(index);
    }

    static void setUpdatedServiceBrokerSubscribers(DelegateExecution context, List<CloudApplication> apps) {
        setAsJsonBinary(context, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS, apps);
    }

    static List<CloudTask> getTasksToExecute(DelegateExecution context) {
        Type type = new TypeToken<List<CloudTask>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_TASKS_TO_EXECUTE, type);
    }

    static void setTasksToExecute(DelegateExecution context, List<CloudTask> tasks) {
        setAsJsonBinary(context, Constants.VAR_TASKS_TO_EXECUTE, tasks);
    }

    static CloudTask getStartedTask(DelegateExecution context) {
        return getFromJsonBinary(context, Constants.VAR_STARTED_TASK, CloudTask.class);
    }

    static void setStartedTask(DelegateExecution context, CloudTask task) {
        setAsJsonBinary(context, Constants.VAR_STARTED_TASK, task);
    }

    public static List<CloudApplication> getAppsToUndeploy(DelegateExecution context) {
        Type type = new TypeToken<List<CloudApplication>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_APPS_TO_UNDEPLOY, type);
    }

    public static CloudApplication getAppToUndeploy(DelegateExecution context) {
        List<CloudApplication> appsToUndeploy = getAppsToUndeploy(context);
        int index = (Integer) context.getVariable(Constants.VAR_APPS_TO_UNDEPLOY_INDEX);
        return appsToUndeploy.get(index);
    }

    static void setAppsToUndeploy(DelegateExecution context, List<CloudApplication> apps) {
        setAsJsonBinary(context, Constants.VAR_APPS_TO_UNDEPLOY, apps);
    }

    public static List<String> getServicesToDelete(DelegateExecution context) {
        List<String> arrayVariableAsList = getArrayVariableAsList(context, Constants.VAR_SERVICES_TO_DELETE);
        return arrayVariableAsList != null ? arrayVariableAsList : Collections.emptyList();
    }

    public static void setServicesToDelete(DelegateExecution context, List<String> services) {
        setArrayVariableFromCollection(context, Constants.VAR_SERVICES_TO_DELETE, services);
    }

    public static List<ConfigurationSubscription> getSubscriptionsToDelete(DelegateExecution context) {
        Type type = new TypeToken<List<ConfigurationSubscription>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_SUBSCRIPTIONS_TO_DELETE, type);
    }

    static void setSubscriptionsToDelete(DelegateExecution context, List<ConfigurationSubscription> subscriptions) {
        setAsJsonBinary(context, Constants.VAR_SUBSCRIPTIONS_TO_DELETE, subscriptions);
    }

    public static List<ConfigurationSubscription> getSubscriptionsToCreate(DelegateExecution context) {
        Type type = new TypeToken<List<ConfigurationSubscription>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_SUBSCRIPTIONS_TO_CREATE, type);
    }

    static void setSubscriptionsToCreate(DelegateExecution context, List<ConfigurationSubscription> subscriptions) {
        setAsJsonBinary(context, Constants.VAR_SUBSCRIPTIONS_TO_CREATE, subscriptions);
    }

    static void setConfigurationEntriesToPublish(DelegateExecution context, List<ConfigurationEntry> configurationEntries) {
        setAsJsonBinary(context, Constants.VAR_CONFIGURATION_ENTRIES_TO_PUBLISH, configurationEntries);
    }

    static List<ConfigurationEntry> getConfigurationEntriesToPublish(DelegateExecution context) {
        Type type = new TypeToken<List<ConfigurationEntry>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_CONFIGURATION_ENTRIES_TO_PUBLISH, type);
    }

    static void setCreatedOrUpdatedServiceBroker(DelegateExecution context, CloudServiceBroker serviceBroker) {
        setAsJsonBinary(context, Constants.VAR_CREATED_OR_UPDATED_SERVICE_BROKER, serviceBroker);
    }

    public static CloudServiceBroker getCreatedOrUpdatedServiceBroker(DelegateExecution context) {
        return getFromJsonBinary(context, Constants.VAR_CREATED_OR_UPDATED_SERVICE_BROKER, CloudServiceBroker.class);
    }

    public static CloudServiceBroker getServiceBrokersToCreateForModule(DelegateExecution context, String moduleName) {
        return getFromJsonBinary(context, Constants.VAR_APP_SERVICE_BROKER_VAR_PREFIX + moduleName, CloudServiceBroker.class);
    }

    public static List<String> getCreatedOrUpdatedServiceBrokerNames(DelegateExecution context) {
        List<Module> allModulesToDeploy = getAllModulesToDeploy(context);
        return allModulesToDeploy.stream()
            .map(module -> getServiceBrokersToCreateForModule(context, module.getName()))
            .filter(Objects::nonNull)
            .map(CloudServiceBroker::getName)
            .collect(Collectors.toList());
    }

    public static List<ConfigurationEntry> getDeletedEntries(DelegateExecution context) {
        Type type = new TypeToken<List<ConfigurationEntry>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_DELETED_ENTRIES, type, Collections.emptyList());
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

    static List<ConfigurationEntry> getDeletedEntriesFromAllProcesses(DelegateExecution context, FlowableFacade flowableFacade) {
        List<ConfigurationEntry> configurationEntries = new ArrayList<>(
            StepsUtil.getDeletedEntriesFromProcess(flowableFacade, StepsUtil.getCorrelationId(context)));
        List<String> subProcessIds = flowableFacade.getHistoricSubProcessIds(StepsUtil.getCorrelationId(context));
        for (String subProcessId : subProcessIds) {
            configurationEntries.addAll(getDeletedEntriesFromProcess(flowableFacade, subProcessId));
        }
        return configurationEntries;
    }

    static void setDeletedEntries(DelegateExecution context, List<ConfigurationEntry> deletedEntries) {
        setAsJsonBinary(context, Constants.VAR_DELETED_ENTRIES, deletedEntries);
    }

    public static List<ConfigurationEntry> getPublishedEntries(DelegateExecution context) {
        Type type = new TypeToken<List<ConfigurationEntry>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_PUBLISHED_ENTRIES, type);
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

    static List<ConfigurationEntry> getPublishedEntriesFromSubProcesses(DelegateExecution context, FlowableFacade flowableFacade) {
        List<ConfigurationEntry> result = new ArrayList<>();
        List<String> subProcessIds = flowableFacade.getHistoricSubProcessIds(StepsUtil.getCorrelationId(context));
        for (String subProcessId : subProcessIds) {
            result.addAll(getPublishedEntriesFromProcess(flowableFacade, subProcessId));
        }
        return result;
    }

    static void setPublishedEntries(DelegateExecution context, List<ConfigurationEntry> publishedEntries) {
        setAsJsonBinary(context, Constants.VAR_PUBLISHED_ENTRIES, publishedEntries);
    }

    static void setServiceUrlToRegister(DelegateExecution context, ServiceUrl serviceUrl) {
        setAsJsonBinary(context, Constants.VAR_SERVICE_URL_TO_REGISTER, serviceUrl);
    }

    public static ServiceUrl getServiceUrlToRegister(DelegateExecution context) {
        return getFromJsonBinary(context, Constants.VAR_SERVICE_URL_TO_REGISTER, ServiceUrl.class);
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

    public static ServiceUrl getServiceUrlToRegisterForModule(DelegateExecution context, String moduleName) {
        return getFromJsonBinary(context, Constants.VAR_APP_SERVICE_URL_VAR_PREFIX + moduleName, ServiceUrl.class);
    }

    public static List<String> getRegisteredServiceUrlsNames(DelegateExecution context) {
        List<Module> allModulesToDeploy = getAllModulesToDeploy(context);
        return allModulesToDeploy.stream()
            .map(module -> getServiceUrlToRegisterForModule(context, module.getName()))
            .filter(Objects::nonNull)
            .map(ServiceUrl::getServiceName)
            .collect(Collectors.toList());
    }

    static void setDeployedMta(DelegateExecution context, DeployedMta deployedMta) {
        setAsJsonBinary(context, Constants.VAR_DEPLOYED_MTA, deployedMta);
    }

    protected static DeployedMta getDeployedMta(DelegateExecution context) {
        return getFromJsonBinary(context, Constants.VAR_DEPLOYED_MTA, DeployedMta.class);
    }

    static Map<String, Set<Integer>> getAllocatedPorts(DelegateExecution context) {
        Type type = new TypeToken<Map<String, Set<Integer>>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_ALLOCATED_PORTS, type);
    }

    static void setAllocatedPorts(DelegateExecution context, Map<String, Set<Integer>> allocatedPorts) {
        setAsJsonBinary(context, Constants.VAR_ALLOCATED_PORTS, allocatedPorts);
    }

    static void setXsPlaceholderReplacementValues(DelegateExecution context, Map<String, Object> replacementValues) {
        setAsJsonBinary(context, Constants.VAR_XS_PLACEHOLDER_REPLACEMENT_VALUES, replacementValues);
    }

    static Map<String, Object> getXsPlaceholderReplacementValues(DelegateExecution context) {
        String json = new String(getObject(context, Constants.VAR_XS_PLACEHOLDER_REPLACEMENT_VALUES), StandardCharsets.UTF_8);
        // JsonUtil.convertJsonToMap does some magic under the hood that converts doubles to integers whenever possible. We need it for
        // SupportedParameters.XSA_ROUTER_PORT_PLACEHOLDER. That's why we can't use getFromJsonBinary here.
        return JsonUtil.convertJsonToMap(json);
    }

    static XsPlaceholderResolver getXsPlaceholderResolver(DelegateExecution context) {
        Map<String, Object> replacementValues = getXsPlaceholderReplacementValues(context);
        XsPlaceholderResolver resolver = new XsPlaceholderResolver();
        resolver.setControllerEndpoint((String) replacementValues.get(SupportedParameters.XSA_CONTROLLER_ENDPOINT_PLACEHOLDER));
        resolver.setRouterPort((int) replacementValues.get(SupportedParameters.XSA_ROUTER_PORT_PLACEHOLDER));
        resolver.setAuthorizationEndpoint((String) replacementValues.get(SupportedParameters.XSA_AUTHORIZATION_ENDPOINT_PLACEHOLDER));
        resolver.setDeployServiceUrl((String) replacementValues.get(SupportedParameters.XSA_DEPLOY_SERVICE_URL_PLACEHOLDER));
        resolver.setProtocol((String) replacementValues.get(SupportedParameters.XSA_PROTOCOL_PLACEHOLDER));
        resolver.setDefaultDomain((String) replacementValues.get(SupportedParameters.XSA_DEFAULT_DOMAIN_PLACEHOLDER));
        return resolver;
    }

    public static DeploymentDescriptor getUnresolvedDeploymentDescriptor(DelegateExecution context) {
        byte[] binaryYaml = (byte[]) context.getVariable(Constants.VAR_MTA_UNRESOLVED_DEPLOYMENT_DESCRIPTOR);
        String yaml = new String(binaryYaml, StandardCharsets.UTF_8);
        return parseDeploymentDescriptor(yaml);
    }

    public static DeploymentDescriptor getDeploymentDescriptor(DelegateExecution context) {
        byte[] binaryYaml = (byte[]) context.getVariable(Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR);
        if (binaryYaml == null) {
            return null;
        }
        String yaml = new String(binaryYaml, StandardCharsets.UTF_8);
        return parseDeploymentDescriptor(yaml);
    }

    public static Module findModuleInDeploymentDescriptor(DelegateExecution context, String module) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
        DeploymentDescriptor deploymentDescriptor = getDeploymentDescriptor(context);
        return handlerFactory.getDescriptorHandler()
            .findModule(deploymentDescriptor, module);
    }

    private static DeploymentDescriptor parseDeploymentDescriptor(String yaml) {
        DescriptorParserFacade descriptorParserFacade = new DescriptorParserFacade();
        return descriptorParserFacade.parseDeploymentDescriptor(yaml);
    }

    @SuppressWarnings("unchecked")
    public static List<ExtensionDescriptor> getExtensionDescriptorChain(DelegateExecution context) {
        List<byte[]> binaryYamlList = (List<byte[]>) context.getVariable(Constants.VAR_MTA_EXTENSION_DESCRIPTOR_CHAIN);
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

    public static void setUnresolvedDeploymentDescriptor(DelegateExecution context, DeploymentDescriptor unresolvedDeploymentDescriptor) {
        context.setVariable(Constants.VAR_MTA_UNRESOLVED_DEPLOYMENT_DESCRIPTOR, toBinaryYaml(unresolvedDeploymentDescriptor));
    }

    public static void setDeploymentDescriptor(DelegateExecution context, DeploymentDescriptor deploymentDescriptor) {
        context.setVariable(Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR, toBinaryYaml(deploymentDescriptor));
    }

    static void setExtensionDescriptorChain(DelegateExecution context, List<ExtensionDescriptor> extensionDescriptors) {
        context.setVariable(Constants.VAR_MTA_EXTENSION_DESCRIPTOR_CHAIN, toBinaryYamlList(extensionDescriptors));
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

    static SystemParameters getSystemParameters(DelegateExecution context) {
        return getFromJsonBinary(context, Constants.VAR_SYSTEM_PARAMETERS, SystemParameters.class);
    }

    static void setSystemParameters(DelegateExecution context, SystemParameters systemParameters) {
        setAsJsonBinary(context, Constants.VAR_SYSTEM_PARAMETERS, systemParameters);
    }

    static void setVcapAppPropertiesChanged(DelegateExecution context, boolean state) {
        context.setVariable(Constants.VAR_VCAP_APP_PROPERTIES_CHANGED, state);
    }

    static boolean getVcapAppPropertiesChanged(DelegateExecution context) {
        return getBoolean(context, Constants.VAR_VCAP_APP_PROPERTIES_CHANGED);
    }

    static void setVcapServicesPropertiesChanged(DelegateExecution context, boolean state) {
        context.setVariable(Constants.VAR_VCAP_SERVICES_PROPERTIES_CHANGED, state);
    }

    static boolean getVcapServicesPropertiesChanged(DelegateExecution context) {
        return getBoolean(context, Constants.VAR_VCAP_SERVICES_PROPERTIES_CHANGED);
    }

    static void setUserPropertiesChanged(DelegateExecution context, boolean state) {
        context.setVariable(Constants.VAR_USER_PROPERTIES_CHANGED, state);
    }

    static boolean getUserPropertiesChanged(DelegateExecution context) {
        return getBoolean(context, Constants.VAR_USER_PROPERTIES_CHANGED);
    }

    public static CloudApplicationExtended getApp(DelegateExecution context) {
        return getFromJsonString(context, Constants.VAR_APP_TO_DEPLOY, CloudApplicationExtended.class);
    }

    static void setApp(DelegateExecution context, CloudApplicationExtended app) {
        setAsJsonString(context, Constants.VAR_APP_TO_DEPLOY, app);
    }

    public static void setModuleToDeploy(DelegateExecution context, Module module) {
        setAsJsonBinary(context, Constants.VAR_MODULE_TO_DEPLOY, module);
    }

    public static Module getModuleToDeploy(DelegateExecution context) {
        return getFromJsonBinary(context, Constants.VAR_MODULE_TO_DEPLOY, getModuleToDeployClass(context));
    }

    static CloudTask getTask(DelegateExecution context) {
        List<CloudTask> tasks = StepsUtil.getTasksToExecute(context);
        int index = (Integer) context.getVariable(Constants.VAR_TASKS_INDEX);
        return tasks.get(index);
    }

    static CloudApplication getExistingApp(DelegateExecution context) {
        return getFromJsonBinary(context, Constants.VAR_EXISTING_APP, CloudApplication.class);
    }

    static void setExistingApp(DelegateExecution context, CloudApplication app) {
        setAsJsonBinary(context, Constants.VAR_EXISTING_APP, app);
    }

    static Set<ApplicationStateAction> getAppStateActionsToExecute(DelegateExecution context) {
        @SuppressWarnings("unchecked")
        Set<String> actionsAsStrings = (Set<String>) context.getVariable(Constants.VAR_APP_STATE_ACTIONS_TO_EXECUTE);
        return actionsAsStrings.stream()
            .map(ApplicationStateAction::valueOf)
            .collect(Collectors.toSet());
    }

    static void setAppStateActionsToExecute(DelegateExecution context, Set<ApplicationStateAction> actions) {
        Set<String> actionsAsStrings = actions.stream()
            .map(ApplicationStateAction::toString)
            .collect(Collectors.toSet());
        context.setVariable(Constants.VAR_APP_STATE_ACTIONS_TO_EXECUTE, actionsAsStrings);
    }

    public static void setSubProcessId(DelegateExecution context, String subProcessId) {
        context.setVariable(Constants.VAR_SUBPROCESS_ID, subProcessId);
    }

    public static String getSubProcessId(DelegateExecution context) {
        return getString(context, Constants.VAR_SUBPROCESS_ID);
    }

    static String getParentProcessId(DelegateExecution context) {
        return getString(context, Constants.VAR_PARENTPROCESS_ID);
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

    public static StartingInfo getStartingInfo(DelegateExecution context) {
        String className = getString(context, Constants.VAR_STARTING_INFO_CLASSNAME);
        return getFromJsonBinary(context, Constants.VAR_STARTING_INFO, getStartingInfoClass(className));
    }

    public static void setStartingInfo(DelegateExecution context, StartingInfo startingInfo) {
        setAsJsonBinary(context, Constants.VAR_STARTING_INFO, startingInfo);
        String className = startingInfo != null ? startingInfo.getClass()
            .getName() : StartingInfo.class.getName();
        context.setVariable(Constants.VAR_STARTING_INFO_CLASSNAME, className);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends StartingInfo> getStartingInfoClass(String className) {
        try {
            return (Class<? extends StartingInfo>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    static StreamingLogToken getStreamingLogsToken(DelegateExecution context) {
        return getFromJsonBinary(context, Constants.VAR_STREAMING_LOGS_TOKEN, StreamingLogToken.class);
    }

    static void setStreamingLogsToken(DelegateExecution context, StreamingLogToken streamingLogToken) {
        setAsJsonBinary(context, Constants.VAR_STREAMING_LOGS_TOKEN, streamingLogToken);
    }

    static void setMtaArchiveModules(DelegateExecution context, Set<String> mtaArchiveModules) {
        setArrayVariableFromCollection(context, Constants.VAR_MTA_ARCHIVE_MODULES, mtaArchiveModules);
    }

    static Set<String> getMtaArchiveModules(DelegateExecution context) {
        return getArrayVariableAsSet(context, Constants.VAR_MTA_ARCHIVE_MODULES);
    }

    static void setMtaModules(DelegateExecution context, Set<String> mtaModules) {
        setArrayVariableFromCollection(context, Constants.VAR_MTA_MODULES, mtaModules);
    }

    static Set<String> getMtaModules(DelegateExecution context) {
        return getArrayVariableAsSet(context, Constants.VAR_MTA_MODULES);
    }

    public static String getCorrelationId(DelegateExecution context) {
        return getString(context, Constants.VAR_CORRELATION_ID);
    }

    public static String getTaskId(DelegateExecution context) {
        return getString(context, Constants.TASK_ID);
    }

    public static ErrorType getErrorType(DelegateExecution context) {
        return getEnum(context, Constants.VAR_ERROR_TYPE, ErrorType::valueOf);
    }

    static void setErrorType(DelegateExecution context, ErrorType errorType) {
        setEnum(context, Constants.VAR_ERROR_TYPE, errorType);
    }

    static StepPhase getStepPhase(DelegateExecution context) {
        StepPhase stepPhase = getEnum(context, Constants.VAR_STEP_PHASE, StepPhase::valueOf);
        return stepPhase == null ? StepPhase.EXECUTE : stepPhase;
    }

    public static void setStepPhase(DelegateExecution context, StepPhase stepPhase) {
        setEnum(context, Constants.VAR_STEP_PHASE, stepPhase);
    }

    public static String getLoggerPrefix(Logger logger) {
        String name = logger.getName();
        return "[" + name.substring(name.lastIndexOf('.') + 1) + "] ";
    }

    public static void setArchiveFileId(DelegateExecution context, String uploadedMtarId) {
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, uploadedMtarId);
    }

    public static String getServiceId(DelegateExecution context) {
        return getString(context, com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SERVICE_ID);
    }

    public static void incrementVariable(DelegateExecution context, String name) {
        int value = getInteger(context, name);
        context.setVariable(name, value + 1);
    }

    public static void setArrayVariable(DelegateExecution context, String name, String[] array) {
        context.setVariable(name, JsonUtil.toJsonBinary(array));
    }

    public static void setArrayVariableFromCollection(DelegateExecution context, String name, Collection<String> collection) {
        setArrayVariable(context, name, collection.toArray(new String[collection.size()]));
    }

    public static String[] getArrayVariable(DelegateExecution context, String name) {
        return getFromJsonBinary(context, name, String[].class);
    }

    public static List<String> getArrayVariableAsList(DelegateExecution context, String name) {
        return Arrays.asList(getArrayVariable(context, name));
    }

    public static Set<String> getArrayVariableAsSet(DelegateExecution context, String name) {
        return new HashSet<>(Arrays.asList(getArrayVariable(context, name)));
    }

    public static final String DEPLOY_ID_PREFIX = "deploy-";

    static ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DelegateExecution context, UserMessageLogger stepLogger) {
        CloudModelConfiguration configuration = getCloudBuilderConfiguration(context, true);
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);

        SystemParameters systemParameters = StepsUtil.getSystemParameters(context);

        String deployId = DEPLOY_ID_PREFIX + getCorrelationId(context);

        XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(context);

        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);
        DeployedMta deployedMta = StepsUtil.getDeployedMta(context);

        return handlerFactory.getApplicationCloudModelBuilder(deploymentDescriptor, configuration, deployedMta, systemParameters,
            xsPlaceholderResolver, deployId, stepLogger);
    }

    static List<String> getDomainsFromApps(DelegateExecution context, ApplicationCloudModelBuilder applicationCloudModelBuilder,
        List<? extends Module> modules, ModuleToDeployHelper moduleToDeployHelper) {
        SystemParameters systemParameters = StepsUtil.getSystemParameters(context);
        XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(context);
        String defaultDomain = (String) systemParameters.getGeneralParameters()
            .getOrDefault(SupportedParameters.DEFAULT_DOMAIN, null);

        Set<String> domains = new TreeSet<>();
        for (Module module : modules) {
            if (!moduleToDeployHelper.isApplication(module)) {
                continue;
            }
            List<String> appDomains = applicationCloudModelBuilder.getApplicationDomains(module);
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

    static ServicesCloudModelBuilder getServicesCloudModelBuilder(DelegateExecution context) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
        CloudModelConfiguration configuration = getCloudBuilderConfiguration(context, true);
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);

        return handlerFactory.getServicesCloudModelBuilder(deploymentDescriptor, configuration);
    }

    static ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DelegateExecution context) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);
        return handlerFactory.getServiceKeysCloudModelBuilder(deploymentDescriptor);
    }

    protected static CloudModelConfiguration getCloudBuilderConfiguration(DelegateExecution context, boolean prettyPrinting) {
        Boolean useNamespaces = getBoolean(context, Constants.PARAM_USE_NAMESPACES, false);
        Boolean useNamespacesForServices = getBoolean(context, Constants.PARAM_USE_NAMESPACES_FOR_SERVICES, false);
        Boolean portBasedRouting = getBoolean(context, Constants.VAR_PORT_BASED_ROUTING, false);
        CloudModelConfiguration configuration = new CloudModelConfiguration();
        configuration.setPortBasedRouting(portBasedRouting);
        configuration.setPrettyPrinting(prettyPrinting);
        configuration.setUseNamespaces(useNamespaces);
        configuration.setUseNamespacesForServices(useNamespacesForServices);
        return configuration;
    }

    static String getGitRepoRef(DelegateExecution context) {
        Object gitRepoConfigObject = context.getVariable(Constants.VAR_GIT_REPOSITORY_CONFIG_MAP);
        if (gitRepoConfigObject == null) {
            return (String) context.getVariable(Constants.PARAM_GIT_REF);
        }
        @SuppressWarnings("unchecked")
        Map<String, String> gitRepoConfigMap = (Map<String, String>) gitRepoConfigObject;
        return gitRepoConfigMap.get(Constants.PARAM_GIT_REF);
    }

    static String getGitRepoUri(DelegateExecution context) {
        Object gitRepoConfigObject = context.getVariable(Constants.VAR_GIT_REPOSITORY_CONFIG_MAP);
        if (gitRepoConfigObject == null) {
            return (String) context.getVariable(Constants.PARAM_GIT_URI);
        }
        @SuppressWarnings("unchecked")
        Map<String, String> gitRepoConfigMap = (Map<String, String>) gitRepoConfigObject;
        return gitRepoConfigMap.get(Constants.PARAM_GIT_URI);
    }

    static void setUseIdleUris(DelegateExecution context, boolean state) {
        context.setVariable(Constants.VAR_USE_IDLE_URIS, state);
    }

    static boolean getUseIdleUris(DelegateExecution context) {
        return getBoolean(context, Constants.VAR_USE_IDLE_URIS);
    }

    public static void setDeleteIdleUris(DelegateExecution context, boolean state) {
        context.setVariable(Constants.VAR_DELETE_IDLE_URIS, state);
    }

    static boolean getDeleteIdleUris(DelegateExecution context) {
        return (boolean) context.getVariable(Constants.VAR_DELETE_IDLE_URIS);
    }

    public static void setSkipUpdateConfigurationEntries(DelegateExecution context, boolean update) {
        context.setVariable(Constants.VAR_SKIP_UPDATE_CONFIGURATION_ENTRIES, update);
    }

    public static boolean getSkipUpdateConfigurationEntries(DelegateExecution context) {
        return getBoolean(context, Constants.VAR_SKIP_UPDATE_CONFIGURATION_ENTRIES);
    }

    public static void setServicesData(DelegateExecution context, Map<String, CloudServiceExtended> servicesData) {
        context.setVariable(Constants.VAR_SERVICES_DATA, JsonUtil.toJsonBinary(servicesData));
    }

    public static Map<String, CloudServiceExtended> getServicesData(DelegateExecution context) {
        Type type = new TypeToken<Map<String, CloudServiceExtended>>() {
        }.getType();
        return getFromJsonBinary(context, Constants.VAR_SERVICES_DATA, type, Collections.emptyMap());
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

    public static boolean shouldDeleteServices(DelegateExecution context) {
        return getBoolean(context, Constants.PARAM_DELETE_SERVICES, false);
    }

    public static CloudServiceExtended getServiceToProcess(DelegateExecution context) {
        return getFromJsonString(context, Constants.VAR_SERVICE_TO_PROCESS, CloudServiceExtended.class);
    }

    public static void setServiceToProcess(CloudServiceExtended service, DelegateExecution context) {
        setAsJsonString(context, Constants.VAR_SERVICE_TO_PROCESS, service);
    }

    public static void setServiceActionsToExecute(List<ServiceAction> actions, DelegateExecution context) {
        List<String> actionsStrings = actions.stream()
            .map(ServiceAction::toString)
            .collect(Collectors.toList());
        context.setVariable(Constants.VAR_SERVICE_ACTIONS_TO_EXCECUTE, actionsStrings);
    }

    @SuppressWarnings("unchecked")
    public static List<ServiceAction> getServiceActionsToExecute(DelegateExecution execution) {
        List<String> actionStrings = (List<String>) execution.getVariable(Constants.VAR_SERVICE_ACTIONS_TO_EXCECUTE);
        return actionStrings.stream()
            .map(ServiceAction::valueOf)
            .collect(Collectors.toList());
    }

    public static void isServiceUpdated(boolean isUpdated, DelegateExecution context) {
        context.setVariable(Constants.VAR_IS_SERVICE_UPDATED, isUpdated);
    }

    public static boolean getIsServiceUpdated(DelegateExecution context) {
        return getBoolean(context, Constants.VAR_IS_SERVICE_UPDATED, false);
    }

    public static void setServiceToProcessName(String name, DelegateExecution context) {
        context.setVariable(Constants.VAR_SERVICE_TO_PROCESS_NAME, name);
    }

    public static String getServiceToProcessName(DelegateExecution context) {
        return getString(context, Constants.VAR_SERVICE_TO_PROCESS_NAME);
    }

    public static boolean getIsServiceUpdatedExportedVariable(DelegateExecution context, String serviceName) {
        return getBoolean(context, Constants.VAR_IS_SERVICE_UPDATED_VAR_PREFIX + serviceName, false);
    }

    public static List<String> getModulesForDeployment(DelegateExecution context) {
        return getVariableWithCommaSepearator(context, Constants.PARAM_MODULES_FOR_DEPLOYMENT);
    }

    public static List<String> getResourcesForDeployment(DelegateExecution context) {
        return getVariableWithCommaSepearator(context, Constants.PARAM_RESOURCES_FOR_DEPLOYMENT);
    }

    private static List<String> getVariableWithCommaSepearator(DelegateExecution context, String variableName) {
        String variableWithCommaSeparator = (String) context.getVariable(variableName);
        if (variableWithCommaSeparator == null) {
            return null;
        }
        return variableWithCommaSeparator.isEmpty() ? Collections.emptyList() : Arrays.asList(variableWithCommaSeparator.split(","));
    }

    public static void setUploadToken(UploadToken uploadToken, DelegateExecution context) {
        setAsJsonString(context, Constants.VAR_UPLOAD_TOKEN, uploadToken);
    }

    public static UploadToken getUploadToken(DelegateExecution context) {
        return getFromJsonString(context, Constants.VAR_UPLOAD_TOKEN, UploadToken.class);
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
