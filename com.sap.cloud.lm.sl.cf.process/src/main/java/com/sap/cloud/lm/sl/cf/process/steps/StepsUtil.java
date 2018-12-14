package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceBrokerExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.DeploymentMode;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.core.model.ModuleToDeploy;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
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

    private static org.apache.log4j.Logger getAppLogger(DelegateExecution context, String appName,
        ProcessLoggerProvider processLoggerProvider) {
        return processLoggerProvider.getLogger(context, appName);
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

    static String getModuleFileName(DelegateExecution context, String moduleName) {
        return (String) context.getVariable(getModuleFileNameVariable(moduleName));
    }

    static String getResourceFileName(DelegateExecution context, String resourceName) {
        return (String) context.getVariable(getResourceFileNameVariable(resourceName));
    }

    static String getRequiresFileName(DelegateExecution context, String requiresName) {
        return (String) context.getVariable(getRequiresFileNameVariable(requiresName));
    }

    static void setModuleFileName(DelegateExecution context, String moduleName, String fileName) {
        context.setVariable(getModuleFileNameVariable(moduleName), fileName);
    }

    static void setRequiresFileName(DelegateExecution context, String requiresName, String fileName) {
        context.setVariable(getRequiresFileNameVariable(requiresName), fileName);
    }

    static void setResourceFileName(DelegateExecution context, String resourceName, String fileName) {
        context.setVariable(getResourceFileNameVariable(resourceName), fileName);
    }

    static InputStream getModuleContentAsStream(DelegateExecution context, String moduleName) {
        byte[] moduleContent = getModuleContent(context, moduleName);
        if (moduleContent == null) {
            throw new SLException(Messages.MODULE_CONTENT_NA, moduleName);
        }
        return new ByteArrayInputStream(moduleContent);
    }

    static byte[] getModuleContent(DelegateExecution context, String moduleName) {
        return (byte[]) context.getVariable(getModuleContentVariable(moduleName));
    }

    static void setModuleContent(DelegateExecution context, String moduleName, byte[] moduleContent) {
        context.setVariable(getModuleContentVariable(moduleName), moduleContent);
    }

    public static ApplicationColor getDeployedMtaColor(DelegateExecution context) {
        String deployedMtaColor = (String) context.getVariable(Constants.VAR_DEPLOYED_MTA_COLOR);
        return (deployedMtaColor == null) ? null : ApplicationColor.valueOf(deployedMtaColor);
    }

    public static void setDeployedMtaColor(DelegateExecution context, ApplicationColor deployedMtaColor) {
        context.setVariable(Constants.VAR_DEPLOYED_MTA_COLOR, (deployedMtaColor == null) ? null : deployedMtaColor.toString());
    }

    public static ApplicationColor getMtaColor(DelegateExecution context) {
        String mtaColor = (String) context.getVariable(Constants.VAR_MTA_COLOR);
        return (mtaColor == null) ? null : ApplicationColor.valueOf(mtaColor);
    }

    public static void setMtaColor(DelegateExecution context, ApplicationColor mtaColor) {
        context.setVariable(Constants.VAR_MTA_COLOR, (mtaColor == null) ? null : mtaColor.toString());
    }

    private static String getModuleContentVariable(String moduleName) {
        return Constants.VAR_MTA_MODULE_CONTENT_PREFIX + moduleName;
    }

    private static String getModuleFileNameVariable(String moduleName) {
        return Constants.VAR_MTA_MODULE_FILE_NAME_PREFIX + moduleName;
    }

    private static String getRequiresFileNameVariable(String requiresName) {
        return Constants.VAR_MTA_REQUIRES_FILE_NAME_PREFIX + requiresName;
    }

    private static String getResourceFileNameVariable(String resourceName) {
        return Constants.VAR_MTA_RESOURCE_FILE_NAME_PREFIX + resourceName;
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
        int majorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
        return new HandlerFactory(majorSchemaVersion);
    }

    public static String getRequiredStringParameter(DelegateExecution context, String variableName) {
        String value = (String) context.getVariable(variableName);
        if (value == null || value.isEmpty()) {
            throw new SLException(Messages.REQUIRED_PARAMETER_IS_MISSING, variableName);
        }
        return value;
    }

    public static String getOrg(DelegateExecution context) {
        return (String) context.getVariable(Constants.VAR_ORG);
    }

    public static String getSpaceId(DelegateExecution context) {
        return (String) context.getVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID);
    }

    public static void setSpaceId(DelegateExecution context, String spaceId) {
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID, spaceId);
    }

    public static String getSpace(DelegateExecution context) {
        return (String) context.getVariable(Constants.VAR_SPACE);
    }

    static String getNewMtaVersion(DelegateExecution context) {
        return (String) context.getVariable(Constants.VAR_NEW_MTA_VERSION);
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

    @SuppressWarnings("unchecked")
    public static List<CloudServiceExtended> getServicesToCreate(DelegateExecution context) {
        List<String> services = (List<String>) context.getVariable(Constants.VAR_SERVICES_TO_CREATE);
        return services == null ? Collections.emptyList()
            : services.stream()
                .map(service -> (CloudServiceExtended) JsonUtil.fromJson(service, CloudServiceExtended.class))
                .collect(Collectors.toList());
    }

    static void setServicesToCreate(DelegateExecution context, List<CloudServiceExtended> services) {
        List<String> servicesAsStrings = services.stream()
            .map(JsonUtil::toJson)
            .collect(Collectors.toList());
        context.setVariable(Constants.VAR_SERVICES_TO_CREATE, servicesAsStrings);
    }

    @SuppressWarnings("unchecked")
    public static List<CloudServiceExtended> getServicesToBind(DelegateExecution context) {
        List<String> services = (List<String>) context.getVariable(Constants.VAR_SERVICES_TO_BIND);
        return services.stream()
            .map(service -> (CloudServiceExtended) JsonUtil.fromJson(service, CloudServiceExtended.class))
            .collect(Collectors.toList());
    }

    static void setServicesToBind(DelegateExecution context, List<CloudServiceExtended> services) {
        List<String> servicesAsStrings = services.stream()
            .map(JsonUtil::toJson)
            .collect(Collectors.toList());
        context.setVariable(Constants.VAR_SERVICES_TO_BIND, servicesAsStrings);
    }

    static void setServicesToPoll(DelegateExecution context, List<CloudServiceExtended> servicesToPoll) {
        context.setVariable(Constants.VAR_SERVICES_TO_POLL, JsonUtil.toBinaryJson(servicesToPoll));
    }

    static List<CloudServiceExtended> getServicesToPoll(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_SERVICES_TO_POLL);
        if (binaryJson == null) {
            return null;
        }
        String jsonString = new String(binaryJson, StandardCharsets.UTF_8);
        return JsonUtil.fromJson(jsonString, new TypeToken<List<CloudServiceExtended>>() {
        }.getType());
    }

    static void setTriggeredServiceOperations(DelegateExecution context, Map<String, ServiceOperationType> triggeredServiceOperations) {
        context.setVariable(Constants.VAR_TRIGGERED_SERVICE_OPERATIONS, JsonUtil.toBinaryJson(triggeredServiceOperations));
    }

    public static Map<String, ServiceOperationType> getTriggeredServiceOperations(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_TRIGGERED_SERVICE_OPERATIONS);
        String jsonString = new String(binaryJson, StandardCharsets.UTF_8);
        return JsonUtil.fromJson(jsonString, new TypeToken<Map<String, ServiceOperationType>>() {
        }.getType());
    }

    public static Map<String, List<ServiceKey>> getServiceKeysToCreate(DelegateExecution context) {
        String json = new String((byte[]) context.getVariable(Constants.VAR_SERVICE_KEYS_TO_CREATE), StandardCharsets.UTF_8);
        return JsonUtil.fromJson(json, new TypeToken<Map<String, List<ServiceKey>>>() {
        }.getType());
    }

    static void setServiceKeysToCreate(DelegateExecution context, Map<String, List<ServiceKey>> serviceKeys) {
        context.setVariable(Constants.VAR_SERVICE_KEYS_TO_CREATE, JsonUtil.toBinaryJson(serviceKeys));
    }

    static List<CloudApplication> getDeployedApps(DelegateExecution context) {
        CloudApplication[] apps = JsonUtil.fromBinaryJson((byte[]) context.getVariable(Constants.VAR_DEPLOYED_APPS),
            CloudApplication[].class);
        return Arrays.asList(apps);
    }

    static void setDeployedApps(DelegateExecution context, List<CloudApplication> apps) {
        context.setVariable(Constants.VAR_DEPLOYED_APPS, JsonUtil.toBinaryJson(apps.toArray(new CloudApplication[] {})));
    }

    @SuppressWarnings("unchecked")
    public static List<CloudApplicationExtended> getAppsToDeploy(DelegateExecution context) {
        List<String> cldoudApplicationsAsStrings = (List<String>) context.getVariable(Constants.VAR_APPS_TO_DEPLOY);
        return cldoudApplicationsAsStrings.stream()
            .map(app -> (CloudApplicationExtended) JsonUtil.fromJson(app, CloudApplicationExtended.class))
            .collect(Collectors.toList());
    }

    public static void setAppsToDeploy(DelegateExecution context, List<CloudApplicationExtended> apps) {
        List<String> cloudApplicationsAsStrings = apps.stream()
            .map(JsonUtil::toJson)
            .collect(Collectors.toList());
        context.setVariable(Constants.VAR_APPS_TO_DEPLOY, cloudApplicationsAsStrings);
    }
    
    @SuppressWarnings("unchecked")
    public static List<ModuleToDeploy> getModulesToDeploy(DelegateExecution context) {
        List<String> cldoudApplicationsAsStrings = (List<String>) context.getVariable(Constants.VAR_MODULES_TO_DEPLOY);
        return cldoudApplicationsAsStrings.stream()
            .map(app -> (ModuleToDeploy) JsonUtil.fromJson(app, ModuleToDeploy.class))
            .collect(Collectors.toList());
    }

    public static void setModulesToDeploy(DelegateExecution context, List<ModuleToDeploy> apps) {
        List<String> cloudApplicationsAsStrings = apps.stream()
            .map(JsonUtil::toJson)
            .collect(Collectors.toList());
        context.setVariable(Constants.VAR_MODULES_TO_DEPLOY, cloudApplicationsAsStrings);
    }
    
    @SuppressWarnings("unchecked")
    public static List<ModuleToDeploy> getAllModulesToDeploy(DelegateExecution context) {
        List<String> cldoudApplicationsAsStrings = (List<String>) context.getVariable(Constants.VAR_ALL_MODULES_TO_DEPLOY);
        return cldoudApplicationsAsStrings.stream()
            .map(app -> (ModuleToDeploy) JsonUtil.fromJson(app, ModuleToDeploy.class))
            .collect(Collectors.toList());
    }

    public static void setAllModulesToDeploy(DelegateExecution context, List<ModuleToDeploy> modulesCalculatedForDeployment) {
        List<String> cloudApplicationsAsStrings = modulesCalculatedForDeployment.stream()
            .map(JsonUtil::toJson)
            .collect(Collectors.toList());
        context.setVariable(Constants.VAR_ALL_MODULES_TO_DEPLOY, cloudApplicationsAsStrings);
    }

    @SuppressWarnings("unchecked")
    public static List<ModuleToDeploy> getIteratedModulesInParallel(DelegateExecution context) {
        List<String> appsIteratedInParallel = (List<String>) context.getVariable(Constants.VAR_ITERATED_MODULES_IN_PARALLEL);
        if (appsIteratedInParallel == null) {
            return Collections.emptyList();
        }
        return appsIteratedInParallel.stream()
            .map(app -> (ModuleToDeploy) JsonUtil.fromJson(app, CloudApplicationExtended.class))
            .collect(Collectors.toList());
    }

    public static void setIteratedModulesInParallel(DelegateExecution context, List<ModuleToDeploy> apps) {
        List<String> cloudApplicationsAsStrings = apps.stream()
            .map(JsonUtil::toJson)
            .collect(Collectors.toList());
        context.setVariable(Constants.VAR_ITERATED_MODULES_IN_PARALLEL, cloudApplicationsAsStrings);
    }

    public static void setModulesToIterateInParallel(DelegateExecution context, List<ModuleToDeploy> apps) {
        List<String> cloudApplicationsAsStrings = apps.stream()
            .map(JsonUtil::toJson)
            .collect(Collectors.toList());
        context.setVariable(Constants.VAR_MODULES_TO_ITERATE_IN_PARALLEL, cloudApplicationsAsStrings);
    }

    public static void setDeploymentMode(DelegateExecution context, DeploymentMode deploymentMode) {
        context.setVariable(Constants.VAR_DEPLOYMENT_MODE, deploymentMode);
    }

    static void setServiceKeysCredentialsToInject(DelegateExecution context,
        Map<String, Map<String, String>> serviceKeysCredentialsToInject) {
        byte[] serviceKeysToInjectByteArray = JsonUtil.toBinaryJson(serviceKeysCredentialsToInject);
        context.setVariable(Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT, serviceKeysToInjectByteArray);
    }

    static Map<String, Map<String, String>> getServiceKeysCredentialsToInject(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT);
        return JsonUtil.fromJson(new String(binaryJson, StandardCharsets.UTF_8), new TypeToken<Map<String, Map<String, String>>>() {
        }.getType());
    }

    public static List<CloudApplication> getUpdatedSubscribers(DelegateExecution context) {
        CloudApplication[] apps = JsonUtil.fromBinaryJson((byte[]) context.getVariable(Constants.VAR_UPDATED_SUBSCRIBERS),
            CloudApplicationExtended[].class);
        return Arrays.asList(apps);
    }

    static void setUpdatedSubscribers(DelegateExecution context, List<CloudApplication> apps) {
        context.setVariable(Constants.VAR_UPDATED_SUBSCRIBERS, JsonUtil.toBinaryJson(apps.toArray(new CloudApplication[] {})));
    }

    public static List<CloudApplication> getServiceBrokerSubscribersToRestart(DelegateExecution context) {
        CloudApplication[] apps = JsonUtil.fromBinaryJson((byte[]) context.getVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS),
            CloudApplication[].class);
        return Arrays.asList(apps);
    }

    static CloudApplication getServiceBrokerSubscriberToRestart(DelegateExecution context) {
        List<CloudApplication> apps = getServiceBrokerSubscribersToRestart(context);
        int index = (Integer) context.getVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX);
        return apps.get(index);
    }

    static void setUpdatedServiceBrokerSubscribers(DelegateExecution context, List<CloudApplication> apps) {
        context.setVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS,
            JsonUtil.toBinaryJson(apps.toArray(new CloudApplication[] {})));
    }

    static List<CloudTask> getTasksToExecute(DelegateExecution context) {
        CloudTask[] tasks = JsonUtil.fromBinaryJson((byte[]) context.getVariable(Constants.VAR_TASKS_TO_EXECUTE), CloudTask[].class);
        return Arrays.asList(tasks);
    }

    static void setTasksToExecute(DelegateExecution context, List<CloudTask> tasks) {
        context.setVariable(Constants.VAR_TASKS_TO_EXECUTE, JsonUtil.toBinaryJson(tasks.toArray(new CloudTask[] {})));
    }

    static CloudTask getStartedTask(DelegateExecution context) {
        return JsonUtil.fromBinaryJson((byte[]) context.getVariable(Constants.VAR_STARTED_TASK), CloudTask.class);
    }

    static void setStartedTask(DelegateExecution context, CloudTask task) {
        context.setVariable(Constants.VAR_STARTED_TASK, JsonUtil.toBinaryJson(task));
    }

    public static List<CloudApplication> getAppsToUndeploy(DelegateExecution context) {
        CloudApplication[] apps = JsonUtil.fromBinaryJson((byte[]) context.getVariable(Constants.VAR_APPS_TO_UNDEPLOY),
            CloudApplication[].class);
        return Arrays.asList(apps);
    }

    public static CloudApplication getAppToUndeploy(DelegateExecution context) {
        List<CloudApplication> appsToUndeploy = getAppsToUndeploy(context);
        int index = (Integer) context.getVariable(Constants.VAR_APPS_TO_UNDEPLOY_INDEX);
        return appsToUndeploy.get(index);
    }

    static void setAppsToUndeploy(DelegateExecution context, List<CloudApplication> apps) {
        context.setVariable(Constants.VAR_APPS_TO_UNDEPLOY, JsonUtil.toBinaryJson(apps.toArray(new CloudApplication[] {})));
    }

    public static List<String> getServicesToDelete(DelegateExecution context) {
        List<String> arrayVariableAsList = getArrayVariableAsList(context, Constants.VAR_SERVICES_TO_DELETE);
        return arrayVariableAsList != null ? arrayVariableAsList : Collections.emptyList();
    }

    public static void setServicesToDelete(DelegateExecution context, List<String> services) {
        setArrayVariableFromCollection(context, Constants.VAR_SERVICES_TO_DELETE, services);
    }

    public static List<ConfigurationSubscription> getSubscriptionsToDelete(DelegateExecution context) {
        ConfigurationSubscription[] subscriptionsArray = JsonUtil
            .fromBinaryJson((byte[]) context.getVariable(Constants.VAR_SUBSCRIPTIONS_TO_DELETE), ConfigurationSubscription[].class);
        return Arrays.asList(subscriptionsArray);
    }

    static void setSubscriptionsToDelete(DelegateExecution context, List<ConfigurationSubscription> subscriptions) {
        byte[] subscriptionsByteArray = JsonUtil.toBinaryJson(subscriptions.toArray(new ConfigurationSubscription[] {}));
        context.setVariable(Constants.VAR_SUBSCRIPTIONS_TO_DELETE, subscriptionsByteArray);
    }

    public static List<ConfigurationSubscription> getSubscriptionsToCreate(DelegateExecution context) {
        ConfigurationSubscription[] subscriptionsArray = JsonUtil
            .fromBinaryJson((byte[]) context.getVariable(Constants.VAR_SUBSCRIPTIONS_TO_CREATE), ConfigurationSubscription[].class);
        return Arrays.asList(subscriptionsArray);
    }

    static void setSubscriptionsToCreate(DelegateExecution context, List<ConfigurationSubscription> subscriptions) {
        byte[] subscriptionsByteArray = JsonUtil.toBinaryJson(subscriptions.toArray(new ConfigurationSubscription[] {}));
        context.setVariable(Constants.VAR_SUBSCRIPTIONS_TO_CREATE, subscriptionsByteArray);
    }

    static void setConfigurationEntriesToPublish(DelegateExecution context, List<ConfigurationEntry> configurationEntries) {
        byte[] configurationEntriesByteArray = JsonUtil.toBinaryJson(configurationEntries.toArray(new ConfigurationEntry[] {}));
        context.setVariable(Constants.VAR_CONFIGURATION_ENTRIES_TO_PUBLISH, configurationEntriesByteArray);
    }

    static List<ConfigurationEntry> getConfigurationEntriesToPublish(DelegateExecution context) {
        ConfigurationEntry[] configurationEntriesArray = JsonUtil
            .fromBinaryJson((byte[]) context.getVariable(Constants.VAR_CONFIGURATION_ENTRIES_TO_PUBLISH), ConfigurationEntry[].class);
        return Arrays.asList(configurationEntriesArray);
    }

    static void setServiceBrokersToCreate(DelegateExecution context, List<CloudServiceBrokerExtended> serviceBrokers) {
        context.setVariable(Constants.VAR_SERVICE_BROKERS_TO_CREATE,
            JsonUtil.toBinaryJson(serviceBrokers.toArray(new CloudServiceBrokerExtended[] {})));
    }

    public static List<CloudServiceBrokerExtended> getServiceBrokersToCreate(DelegateExecution context) {
        CloudServiceBrokerExtended[] serviceBrokers = JsonUtil
            .fromBinaryJson((byte[]) context.getVariable(Constants.VAR_SERVICE_BROKERS_TO_CREATE), CloudServiceBrokerExtended[].class);
        return Arrays.asList(serviceBrokers);
    }

    public static List<ConfigurationEntry> getDeletedEntries(DelegateExecution context) {
        byte[] deletedEntriesByteArray = (byte[]) context.getVariable(Constants.VAR_DELETED_ENTRIES);
        if (deletedEntriesByteArray == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(JsonUtil.fromBinaryJson(deletedEntriesByteArray, ConfigurationEntry[].class));
    }

    static List<ConfigurationEntry> getDeletedEntriesFromProcess(FlowableFacade flowableFacade, String processInstanceId) {
        HistoricVariableInstance deletedEntries = flowableFacade.getHistoricVariableInstance(processInstanceId,
            Constants.VAR_DELETED_ENTRIES);
        if (deletedEntries == null) {
            return Collections.emptyList();
        }
        byte[] deletedEntriesByteArray = (byte[]) deletedEntries.getValue();
        return Arrays.asList(JsonUtil.fromBinaryJson(deletedEntriesByteArray, ConfigurationEntry[].class));
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
        if (deletedEntries == null) {
            return;
        }
        byte[] deletedEntriesByteArray = JsonUtil.toBinaryJson(deletedEntries.toArray(new ConfigurationEntry[] {}));
        context.setVariable(Constants.VAR_DELETED_ENTRIES, deletedEntriesByteArray);
    }

    public static List<ConfigurationEntry> getPublishedEntries(DelegateExecution context) {
        ConfigurationEntry[] publishedEntriesArray = JsonUtil.fromBinaryJson((byte[]) context.getVariable(Constants.VAR_PUBLISHED_ENTRIES),
            ConfigurationEntry[].class);
        return Arrays.asList(publishedEntriesArray);
    }

    static List<ConfigurationEntry> getPublishedEntriesFromProcess(FlowableFacade flowableFacade, String processInstanceId) {
        HistoricVariableInstance publishedEntries = flowableFacade.getHistoricVariableInstance(processInstanceId,
            Constants.VAR_PUBLISHED_ENTRIES);
        if (publishedEntries == null) {
            return Collections.emptyList();
        }
        byte[] binaryJson = (byte[]) publishedEntries.getValue();
        return Arrays.asList(JsonUtil.fromBinaryJson(binaryJson, ConfigurationEntry[].class));
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
        byte[] publishedEntriesByteArray = JsonUtil.toBinaryJson(publishedEntries.toArray(new ConfigurationEntry[] {}));
        context.setVariable(Constants.VAR_PUBLISHED_ENTRIES, publishedEntriesByteArray);
    }

    static void setServiceUrlsToRegister(DelegateExecution context, List<ServiceUrl> serviceUrls) {
        context.setVariable(Constants.VAR_SERVICE_URLS_TO_REGISTER, JsonUtil.toBinaryJson(serviceUrls.toArray(new ServiceUrl[] {})));
    }

    public static List<ServiceUrl> getServiceUrlsToRegister(DelegateExecution context) {
        ServiceUrl[] serviceUrls = JsonUtil.fromBinaryJson((byte[]) context.getVariable(Constants.VAR_SERVICE_URLS_TO_REGISTER),
            ServiceUrl[].class);
        return Arrays.asList(serviceUrls);
    }

    static void setDeployedMta(DelegateExecution context, DeployedMta deployedMta) {
        byte[] binaryJson = deployedMta == null ? null : JsonUtil.toBinaryJson(deployedMta);
        context.setVariable(Constants.VAR_DEPLOYED_MTA, binaryJson);
    }

    static DeployedMta getDeployedMta(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_DEPLOYED_MTA);
        return binaryJson == null ? null : JsonUtil.fromBinaryJson(binaryJson, DeployedMta.class);
    }

    static Set<Integer> getAllocatedPorts(DelegateExecution context) {
        byte[] allocatedPortsBytes = (byte[]) context.getVariable(Constants.VAR_ALLOCATED_PORTS);
        Integer[] allocatedPorts = allocatedPortsBytes != null ? JsonUtil.fromBinaryJson(allocatedPortsBytes, Integer[].class) : null;
        return allocatedPorts != null ? Arrays.stream(allocatedPorts)
            .collect(Collectors.toSet()) : Collections.emptySet();
    }

    static void setAllocatedPorts(DelegateExecution context, Set<Integer> allocatedPorts) {
        context.setVariable(Constants.VAR_ALLOCATED_PORTS, JsonUtil.toBinaryJson(allocatedPorts.toArray(new Integer[0])));
    }

    static void setXsPlaceholderReplacementValues(DelegateExecution context, Map<String, Object> replacementValues) {
        byte[] replacementValuesJson = JsonUtil.toBinaryJson(replacementValues);
        context.setVariable(Constants.VAR_XS_PLACEHOLDER_REPLACEMENT_VALUES, replacementValuesJson);
    }

    static Map<String, Object> getXsPlaceholderReplacementValues(DelegateExecution context) {
        byte[] replacementValuesJson = (byte[]) context.getVariable(Constants.VAR_XS_PLACEHOLDER_REPLACEMENT_VALUES);
        return JsonUtil.convertJsonToMap(new String(replacementValuesJson, StandardCharsets.UTF_8));
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
        return JsonUtil.fromBinaryJson((byte[]) context.getVariable(Constants.VAR_SYSTEM_PARAMETERS), SystemParameters.class);
    }

    static void setSystemParameters(DelegateExecution context, SystemParameters systemParameters) {
        setAsBinaryJson(context, Constants.VAR_SYSTEM_PARAMETERS, systemParameters);
    }

    static void setVcapAppPropertiesChanged(DelegateExecution context, boolean state) {
        context.setVariable(Constants.VAR_VCAP_APP_PROPERTIES_CHANGED, state);
    }

    static boolean getVcapAppPropertiesChanged(DelegateExecution context) {
        return (boolean) context.getVariable(Constants.VAR_VCAP_APP_PROPERTIES_CHANGED);
    }

    static void setVcapServicesPropertiesChanged(DelegateExecution context, boolean state) {
        context.setVariable(Constants.VAR_VCAP_SERVICES_PROPERTIES_CHANGED, state);
    }

    static boolean getVcapServicesPropertiesChanged(DelegateExecution context) {
        return (boolean) context.getVariable(Constants.VAR_VCAP_SERVICES_PROPERTIES_CHANGED);
    }

    static void setUserPropertiesChanged(DelegateExecution context, boolean state) {
        context.setVariable(Constants.VAR_USER_PROPERTIES_CHANGED, state);
    }

    static boolean getUserPropertiesChanged(DelegateExecution context) {
        return (boolean) context.getVariable(Constants.VAR_USER_PROPERTIES_CHANGED);
    }

    static CloudApplicationExtended getApp(DelegateExecution context) {
        return JsonUtil.fromJson((String) context.getVariable(Constants.VAR_APP_TO_DEPLOY), CloudApplicationExtended.class);
    }
    
    static ModuleToDeploy getModuleToDeploy(DelegateExecution context) {
        return JsonUtil.fromJson((String) context.getVariable(Constants.VAR_MODULE_TO_DEPLOY), ModuleToDeploy.class);
    }

    static void setApp(DelegateExecution context, CloudApplicationExtended app) {
        context.setVariable(Constants.VAR_APP_TO_DEPLOY, JsonUtil.toJson(app));
    }

    static CloudTask getTask(DelegateExecution context) {
        List<CloudTask> tasks = StepsUtil.getTasksToExecute(context);
        int index = (Integer) context.getVariable(Constants.VAR_TASKS_INDEX);
        return tasks.get(index);
    }

    static CloudApplication getExistingApp(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_EXISTING_APP);
        return (binaryJson != null) ? JsonUtil.fromBinaryJson(binaryJson, CloudApplication.class) : null;
    }

    static void setExistingApp(DelegateExecution context, CloudApplication app) {
        byte[] binaryJson = (app != null) ? JsonUtil.toBinaryJson(app) : null;
        context.setVariable(Constants.VAR_EXISTING_APP, binaryJson);
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
        return (String) context.getVariable(Constants.VAR_SUBPROCESS_ID);
    }

    static String getParentProcessId(DelegateExecution context) {
        return (String) context.getVariable(Constants.VAR_PARENTPROCESS_ID);
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
        getAppLogger(context, appName, processLoggerProvider).debug(getPrefix(logger) + "[" + appName + "] " + message);
    }

    static StartingInfo getStartingInfo(DelegateExecution context) {
        String className = (String) context.getVariable(Constants.VAR_STARTING_INFO_CLASSNAME);
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_STARTING_INFO);
        return (binaryJson != null) ? JsonUtil.fromBinaryJson(binaryJson, getStartingInfoClass(className)) : null;
    }

    static void setStartingInfo(DelegateExecution context, StartingInfo startingInfo) {
        byte[] binaryJson = (startingInfo != null) ? JsonUtil.toBinaryJson(startingInfo) : null;
        context.setVariable(Constants.VAR_STARTING_INFO, binaryJson);
        String className = (startingInfo != null) ? startingInfo.getClass()
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
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_STREAMING_LOGS_TOKEN);
        return (binaryJson != null) ? JsonUtil.fromBinaryJson(binaryJson, StreamingLogToken.class) : null;
    }

    static void setStreamingLogsToken(DelegateExecution context, StreamingLogToken streamingLogToken) {
        byte[] binaryJson = (streamingLogToken != null) ? JsonUtil.toBinaryJson(streamingLogToken) : null;
        context.setVariable(Constants.VAR_STREAMING_LOGS_TOKEN, binaryJson);
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
        return (String) context.getVariable(Constants.VAR_CORRELATION_ID);
    }

    public static String getTaskId(DelegateExecution context) {
        return (String) context.getVariable(Constants.TASK_ID);
    }

    public static String getTaskIndex(DelegateExecution context) {
        return (String) context.getVariable(Constants.TASK_INDEX);
    }

    public static ErrorType getErrorType(DelegateExecution context) {
        String errorType = (String) context.getVariable(Constants.VAR_ERROR_TYPE);
        return errorType == null ? null : ErrorType.valueOf(errorType);
    }

    static void setErrorType(DelegateExecution context, ErrorType errorType) {
        if (errorType == null) {
            return;
        }
        context.setVariable(Constants.VAR_ERROR_TYPE, errorType.toString());
    }

    static StepPhase getStepPhase(DelegateExecution context) {
        String stepPhase = (String) context.getVariable(Constants.VAR_STEP_PHASE);
        return stepPhase == null ? StepPhase.EXECUTE : StepPhase.valueOf(stepPhase);
    }

    public static void setStepPhase(DelegateExecution context, StepPhase stepPhase) {
        if (stepPhase == null) {
            return;
        }
        context.setVariable(Constants.VAR_STEP_PHASE, stepPhase.toString());
    }

    @SuppressWarnings("unchecked")
    public static <T> T getVariableOrDefault(DelegateExecution context, String name, T defaultValue) {
        if (!context.hasVariable(name)) {
            return defaultValue;
        }
        return (T) context.getVariable(name);
    }

    private static String getPrefix(Logger logger) {
        String name = logger.getName();
        return "[" + name.substring(name.lastIndexOf('.') + 1) + "] ";
    }

    public static void setArchiveFileId(DelegateExecution context, String uploadedMtarId) {
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, uploadedMtarId);
    }

    public static String getServiceId(DelegateExecution context) {
        return (String) context.getVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SERVICE_ID);
    }

    public static void incrementVariable(DelegateExecution context, String name) {
        int value = (Integer) context.getVariable(name);
        context.setVariable(name, value + 1);
    }

    public static void setArrayVariable(DelegateExecution context, String name, String[] array) {
        context.setVariable(name, JsonUtil.toBinaryJson(array));
    }

    public static void setArrayVariableFromCollection(DelegateExecution context, String name, Collection<String> collection) {
        setArrayVariable(context, name, collection.toArray(new String[collection.size()]));
    }

    public static String[] getArrayVariable(DelegateExecution context, String name) {
        return JsonUtil.fromBinaryJson((byte[]) context.getVariable(name), String[].class);
    }

    public static List<String> getArrayVariableAsList(DelegateExecution context, String name) {
        return Arrays.asList(getArrayVariable(context, name));
    }

    public static Set<String> getArrayVariableAsSet(DelegateExecution context, String name) {
        return new HashSet<>(Arrays.asList(getArrayVariable(context, name)));
    }

    public static void setAsBinaryJson(DelegateExecution context, String name, Object object) {
        context.setVariable(name, JsonUtil.toBinaryJson(object));
    }

    public static final String DEPLOY_ID_PREFIX = "deploy-";

    static ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DelegateExecution context) {

        CloudModelConfiguration configuration = getCloudBuilderConfiguration(context, true);
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);

        SystemParameters systemParameters = StepsUtil.getSystemParameters(context);

        String deployId = DEPLOY_ID_PREFIX + getCorrelationId(context);

        XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(context);

        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);
        DeployedMta deployedMta = StepsUtil.getDeployedMta(context);

        return handlerFactory.getApplicationsCloudModelBuilder(deploymentDescriptor, configuration, deployedMta, systemParameters,
            xsPlaceholderResolver, deployId);
    }

    static List<String> getDomainsFromApps(DelegateExecution context, List<CloudApplicationExtended> apps) {
        Set<String> domains = new TreeSet<>();
        SystemParameters systemParameters = StepsUtil.getSystemParameters(context);
        XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(context);

        String defaultDomain = (String) systemParameters.getGeneralParameters()
            .getOrDefault(SupportedParameters.DEFAULT_DOMAIN, null);

        for (CloudApplicationExtended app : apps) {
            if (app.getDomains() != null) {
                domains.addAll(app.getDomains());
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

        return handlerFactory.getServicesCloudModelBuilder(deploymentDescriptor, handlerFactory.getPropertiesAccessor(), configuration);
    }

    static ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DelegateExecution context) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);
        return handlerFactory.getServiceKeysCloudModelBuilder(deploymentDescriptor, handlerFactory.getPropertiesAccessor());
    }

    static CloudModelConfiguration getCloudBuilderConfiguration(DelegateExecution context, boolean prettyPrinting) {
        Boolean useNamespaces = getVariableOrDefault(context, Constants.PARAM_USE_NAMESPACES, Boolean.FALSE);
        Boolean useNamespacesForServices = getVariableOrDefault(context, Constants.PARAM_USE_NAMESPACES_FOR_SERVICES, Boolean.FALSE);
        Boolean portBasedRouting = getVariableOrDefault(context, Constants.VAR_PORT_BASED_ROUTING, Boolean.FALSE);
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
        return (boolean) context.getVariable(Constants.VAR_USE_IDLE_URIS);
    }

    public static void setSkipUpdateConfigurationEntries(DelegateExecution context, boolean update) {
        context.setVariable(Constants.VAR_SKIP_UPDATE_CONFIGURATION_ENTRIES, update);
    }

    public static boolean getSkipUpdateConfigurationEntries(DelegateExecution context) {
        return (boolean) context.getVariable(Constants.VAR_SKIP_UPDATE_CONFIGURATION_ENTRIES);
    }

    public static void setServicesGuids(DelegateExecution context, Map<String, String> serviceGuids) {
        context.setVariable(Constants.VAR_SERVICES_GUIDS, JsonUtil.toBinaryJson(serviceGuids));
    }

    public static Map<String, String> getServicesGuids(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_SERVICES_GUIDS);
        if (binaryJson == null) {
            return Collections.emptyMap();
        }
        String jsonString = new String(binaryJson, StandardCharsets.UTF_8);
        return JsonUtil.fromJson(jsonString, new TypeToken<Map<String, String>>() {
        }.getType());
    }

    public static CloudApplication getBoundApplication(List<CloudApplication> applications, UUID appGuid) {
        return applications.stream()
            .filter(app -> hasGuid(app, appGuid))
            .findFirst()
            .get();
    }

    private static boolean hasGuid(CloudApplication app, UUID appGuid) {
        return app.getMeta()
            .getGuid()
            .equals(appGuid);
    }

    public static boolean shouldDeleteServices(DelegateExecution context) {
        boolean deleteServicesFlag = (boolean) context.getVariable(Constants.PARAM_DELETE_SERVICES);

        return deleteServicesFlag;
    }

    public static List<String> getModulesForDeployment(DelegateExecution context) {
        return getVariableWithCommaSepearator(context, Constants.PARAM_MODULES_FOR_DEPLOYMENT);
    }

    public static List<String> getResourcesForDeployment(DelegateExecution context) {
        return getVariableWithCommaSepearator(context, Constants.PARAM_RESOURCES_FOR_DEPLOYMENT);
    }

    private static List<String> getVariableWithCommaSepearator(DelegateExecution context, String variableName) {
        String variableWithCommaSeparator = (String) context.getVariable(variableName);
        return variableWithCommaSeparator == null ? Collections.emptyList() : Arrays.asList(variableWithCommaSeparator.split(","));
    }
}
