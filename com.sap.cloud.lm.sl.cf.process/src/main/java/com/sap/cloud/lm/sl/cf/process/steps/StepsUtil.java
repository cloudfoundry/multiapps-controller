package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.activiti.common.util.GsonHelper;
import com.sap.cloud.lm.sl.cf.api.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceBrokerExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.DomainsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ContextExtensionDao;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ContextExtension;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.ProcessLoggerProviderFactory;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.BinaryJson;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.model.json.PropertiesAdapterFactory;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public class StepsUtil {

    private static final String PARENT_LOGGER = "com.sap.cloud.lm.sl.xs2";

    private static org.apache.log4j.Logger getAppLogger(DelegateExecution context, String appName,
        ProcessLoggerProviderFactory processLoggerProviderFactory) {
        return processLoggerProviderFactory.getLoggerProvider(appName).getLogger(getCorrelationId(context), PARENT_LOGGER, appName);
    }

    static CloudFoundryOperations getCloudFoundryClient(DelegateExecution context, CloudFoundryClientProvider clientProvider,
        StepLogger stepLogger) throws SLException {
        return getCloudFoundryClient(context, clientProvider, stepLogger, getOrg(context), getSpace(context));
    }

    static CloudFoundryOperations getCloudFoundryClient(DelegateExecution context, CloudFoundryClientProvider clientProvider,
        StepLogger stepLogger, String org, String space) throws SLException {
        // Determine the current user
        String userName = determineCurrentUser(context, stepLogger);
        return clientProvider.getCloudFoundryClient(userName, org, space, context.getProcessInstanceId());
    }

    static ClientExtensions getClientExtensions(DelegateExecution context, CloudFoundryClientProvider clientProvider, StepLogger stepLogger)
        throws SLException {
        CloudFoundryOperations cloudFoundryClient = StepsUtil.getCloudFoundryClient(context, clientProvider, stepLogger);
        if (cloudFoundryClient instanceof ClientExtensions) {
            return (ClientExtensions) cloudFoundryClient;
        }
        return null;
    }

    static ClientExtensions getClientExtensions(DelegateExecution context, CloudFoundryClientProvider clientProvider, StepLogger stepLogger,
        String org, String space) throws SLException {
        CloudFoundryOperations cloudFoundryClient = StepsUtil.getCloudFoundryClient(context, clientProvider, stepLogger, org, space);
        if (cloudFoundryClient instanceof ClientExtensions) {
            return (ClientExtensions) cloudFoundryClient;
        }
        return null;
    }

    public static String determineCurrentUser(DelegateExecution context, StepLogger stepLogger) throws SLException {
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

    static InputStream getModuleContentAsStream(DelegateExecution context, String moduleName) throws SLException {
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
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new PropertiesAdapterFactory()).create();
        return new BinaryJson(gson);
    }

    static Target getTarget(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_TARGET);

        int majorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
        int minorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION);
        switch (majorSchemaVersion) {
            case 1:
                return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v1_0.Target.class);
            case 2:
                return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v2_0.Target.class);
            case 3:
                switch (minorSchemaVersion) {
                    case 0:
                        return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v3_0.Target.class);
                    case 1:
                        return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v3_1.Target.class);
                }
            default:
                return null;
        }
    }

    static void setTarget(DelegateExecution context, Target target) {
        context.setVariable(Constants.VAR_TARGET, getBinaryJsonForMtaModel().marshal(target));
    }

    static Platform getPlatform(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_PLATFORM);

        int majorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
        int minorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION);
        switch (majorSchemaVersion) {
            case 1:
                return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v1_0.Platform.class);
            case 2:
                return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v2_0.Platform.class);
            case 3:
                switch (minorSchemaVersion) {
                    case 0:
                        return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v3_0.Platform.class);
                    case 1:
                        return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v3_1.Platform.class);
                }
            default:
                return null;
        }
    }

    static void setPlatform(DelegateExecution context, Platform platform) {
        context.setVariable(Constants.VAR_PLATFORM, getBinaryJsonForMtaModel().marshal(platform));
    }

    static HandlerFactory getHandlerFactory(DelegateExecution context) {
        int majorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
        int minorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION);
        return new HandlerFactory(majorSchemaVersion, minorSchemaVersion);
    }

    static String getRequiredStringParameter(DelegateExecution context, String variableName) throws SLException {
        String value = (String) context.getVariable(variableName);
        if (value == null || value.isEmpty()) {
            throw new SLException(Messages.REQUIRED_PARAMETER_IS_MISSING, variableName);
        }
        return value;
    }

    static void validateOrg(String org, DelegateExecution context) throws SLException {
        String urlOrg = getOrg(context);
        if (!urlOrg.equals(org)) {
            throw new SLException(Messages.TARGETED_ORG_DOES_NOT_MATCH_URL_ORG, org, urlOrg);
        }
    }

    public static String getOrg(DelegateExecution context) {
        return (String) context.getVariable(Constants.VAR_ORG);
    }

    public static String getSpaceId(DelegateExecution context) {
        return (String) context.getVariable(com.sap.cloud.lm.sl.cf.api.activiti.Constants.VARIABLE_NAME_SPACE_ID);
    }

    public static void setSpaceId(DelegateExecution context, String spaceId) {
        context.setVariable(com.sap.cloud.lm.sl.cf.api.activiti.Constants.VARIABLE_NAME_SPACE_ID, spaceId);
    }

    static void validateSpace(String space, DelegateExecution context) throws SLException {
        String urlSpace = getSpace(context);
        if (!urlSpace.equals(space)) {
            throw new SLException(Messages.TARGETED_SPACE_DOES_NOT_MATCH_URL_SPACE, space, urlSpace);
        }
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
        return ContextUtil.getArrayVariableAsList(context, Constants.VAR_CUSTOM_DOMAINS);
    }

    static void setCustomDomains(DelegateExecution context, List<String> customDomains) {
        ContextUtil.setArrayVariableFromCollection(context, Constants.VAR_CUSTOM_DOMAINS, customDomains);
    }

    @SuppressWarnings("unchecked")
    public static List<CloudServiceExtended> getServicesToCreate(DelegateExecution context) {
        List<String> services = (List<String>) context.getVariable(Constants.VAR_SERVICES_TO_CREATE);
        return services.stream().map(service -> (CloudServiceExtended) JsonUtil.fromJson(service, CloudServiceExtended.class)).collect(
            Collectors.toList());
    }

    static void setServicesToCreate(DelegateExecution context, List<CloudServiceExtended> services) {
        List<String> servicesAsStrings = services.stream().map(service -> JsonUtil.toJson(service)).collect(Collectors.toList());
        context.setVariable(Constants.VAR_SERVICES_TO_CREATE, servicesAsStrings);
    }

    @SuppressWarnings("unchecked")
    public static List<CloudServiceExtended> getServicesToBind(DelegateExecution context) {
        List<String> services = (List<String>) context.getVariable(Constants.VAR_SERVICES_TO_BIND);
        return services.stream().map(service -> (CloudServiceExtended) JsonUtil.fromJson(service, CloudServiceExtended.class)).collect(
            Collectors.toList());
    }

    static void setServicesToBind(DelegateExecution context, List<CloudServiceExtended> services) {
        List<String> servicesAsStrings = services.stream().map(service -> JsonUtil.toJson(service)).collect(Collectors.toList());
        context.setVariable(Constants.VAR_SERVICES_TO_BIND, servicesAsStrings);
    }

    static void setServicesToPoll(DelegateExecution context, List<CloudServiceExtended> servicesToPoll) {
        context.setVariable(Constants.VAR_SERVICES_TO_POLL, GsonHelper.getAsBinaryJson(servicesToPoll));
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
        context.setVariable(Constants.VAR_TRIGGERED_SERVICE_OPERATIONS, GsonHelper.getAsBinaryJson(triggeredServiceOperations));
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
        context.setVariable(Constants.VAR_SERVICE_KEYS_TO_CREATE, GsonHelper.getAsBinaryJson(serviceKeys));
    }

    static List<CloudApplication> getDeployedApps(DelegateExecution context) {
        CloudApplication[] apps = GsonHelper.getFromBinaryJson((byte[]) context.getVariable(Constants.VAR_DEPLOYED_APPS),
            CloudApplication[].class);
        return Arrays.asList(apps);
    }

    static void setDeployedApps(DelegateExecution context, List<CloudApplication> apps) {
        context.setVariable(Constants.VAR_DEPLOYED_APPS, GsonHelper.getAsBinaryJson(apps.toArray(new CloudApplication[] {})));
    }

    @SuppressWarnings("unchecked")
    public static List<CloudApplicationExtended> getAppsToDeploy(DelegateExecution context) {
        List<String> cldoudApplicationsAsStrings = (List<String>) context.getVariable(Constants.VAR_APPS_TO_DEPLOY);
        return cldoudApplicationsAsStrings.stream().map(
            app -> (CloudApplicationExtended) JsonUtil.fromJson(app, CloudApplicationExtended.class)).collect(Collectors.toList());
    }

    static void setAppsToDeploy(DelegateExecution context, List<CloudApplicationExtended> apps) {
        List<String> cloudApplicationsAsStrings = apps.stream().map(app -> JsonUtil.toJson(app)).collect(Collectors.toList());
        context.setVariable(Constants.VAR_APPS_TO_DEPLOY, cloudApplicationsAsStrings);
    }

    static void setServiceKeysCredentialsToInject(DelegateExecution context,
        Map<String, Map<String, String>> serviceKeysCredentialsToInject) {
        byte[] serviceKeysToInjectByteArray = GsonHelper.getAsBinaryJson(serviceKeysCredentialsToInject);
        context.setVariable(Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT, serviceKeysToInjectByteArray);
    }

    static Map<String, Map<String, String>> getServiceKeysCredentialsToInject(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT);
        return JsonUtil.fromJson(new String(binaryJson, StandardCharsets.UTF_8), new TypeToken<Map<String, Map<String, String>>>() {
        }.getType());
    }

    public static List<CloudApplication> getUpdatedSubscribers(DelegateExecution context) {
        CloudApplication[] apps = GsonHelper.getFromBinaryJson((byte[]) context.getVariable(Constants.VAR_UPDATED_SUBSCRIBERS),
            CloudApplicationExtended[].class);
        return Arrays.asList(apps);
    }

    static void setUpdatedSubscribers(DelegateExecution context, List<CloudApplication> apps) {
        context.setVariable(Constants.VAR_UPDATED_SUBSCRIBERS, GsonHelper.getAsBinaryJson(apps.toArray(new CloudApplication[] {})));
    }

    public static List<CloudApplicationExtended> getServiceBrokerSubscribersToRestart(DelegateExecution context) {
        CloudApplicationExtended[] apps = GsonHelper.getFromBinaryJson(
            (byte[]) context.getVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS), CloudApplicationExtended[].class);
        return Arrays.asList(apps);
    }

    static CloudApplicationExtended getServiceBrokerSubscriberToRestart(DelegateExecution context) {
        List<CloudApplicationExtended> apps = getServiceBrokerSubscribersToRestart(context);
        int index = (Integer) context.getVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX);
        return apps.get(index);
    }

    static void setUpdatedServiceBrokerSubscribers(DelegateExecution context, List<CloudApplication> apps) {
        context.setVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS,
            GsonHelper.getAsBinaryJson(apps.toArray(new CloudApplication[] {})));
    }

    static List<CloudTask> getTasksToExecute(DelegateExecution context) {
        CloudTask[] tasks = GsonHelper.getFromBinaryJson((byte[]) context.getVariable(Constants.VAR_TASKS_TO_EXECUTE), CloudTask[].class);
        return Arrays.asList(tasks);
    }

    static void setTasksToExecute(DelegateExecution context, List<CloudTask> tasks) {
        context.setVariable(Constants.VAR_TASKS_TO_EXECUTE, GsonHelper.getAsBinaryJson(tasks.toArray(new CloudTask[] {})));
    }

    static CloudTask getStartedTask(DelegateExecution context) {
        return GsonHelper.getFromBinaryJson((byte[]) context.getVariable(Constants.VAR_STARTED_TASK), CloudTask.class);
    }

    static void setStartedTask(DelegateExecution context, CloudTask task) {
        context.setVariable(Constants.VAR_STARTED_TASK, GsonHelper.getAsBinaryJson(task));
    }

    static void setAppsToRestart(DelegateExecution context, List<String> apps) {
        context.setVariable(Constants.VAR_APPS_TO_RESTART, GsonHelper.getAsBinaryJson(apps.toArray(new String[] {})));
    }

    static List<String> getAppsToRestart(DelegateExecution context) {
        String[] apps = GsonHelper.getFromBinaryJson((byte[]) context.getVariable(Constants.VAR_APPS_TO_RESTART), String[].class);
        return Arrays.asList(apps);
    }

    public static List<CloudApplication> getAppsToUndeploy(DelegateExecution context) {
        CloudApplication[] apps = GsonHelper.getFromBinaryJson((byte[]) context.getVariable(Constants.VAR_APPS_TO_UNDEPLOY),
            CloudApplication[].class);
        return Arrays.asList(apps);
    }

    public static CloudApplication getAppToUndeploy(DelegateExecution context) {
        List<CloudApplication> appsToUndeploy = getAppsToUndeploy(context);
        int index = (Integer) context.getVariable(Constants.VAR_APPS_TO_UNDEPLOY_INDEX);
        return appsToUndeploy.get(index);
    }

    static void setAppsToUndeploy(DelegateExecution context, List<CloudApplication> apps) {
        context.setVariable(Constants.VAR_APPS_TO_UNDEPLOY, GsonHelper.getAsBinaryJson(apps.toArray(new CloudApplication[] {})));
    }

    public static List<String> getServicesToDelete(DelegateExecution context) {
        return ContextUtil.getArrayVariableAsList(context, Constants.VAR_SERVICES_TO_DELETE);
    }

    static void setServicesToDelete(DelegateExecution context, List<String> services) {
        ContextUtil.setArrayVariableFromCollection(context, Constants.VAR_SERVICES_TO_DELETE, services);
    }

    public static List<ConfigurationSubscription> getSubscriptionsToDelete(DelegateExecution context) {
        ConfigurationSubscription[] subscriptionsArray = GsonHelper.getFromBinaryJson(
            (byte[]) context.getVariable(Constants.VAR_SUBSCRIPTIONS_TO_DELETE), ConfigurationSubscription[].class);
        return Arrays.asList(subscriptionsArray);
    }

    static void setSubscriptionsToDelete(DelegateExecution context, List<ConfigurationSubscription> subscriptions) {
        byte[] subscriptionsByteArray = GsonHelper.getAsBinaryJson(subscriptions.toArray(new ConfigurationSubscription[] {}));
        context.setVariable(Constants.VAR_SUBSCRIPTIONS_TO_DELETE, subscriptionsByteArray);
    }

    public static List<ConfigurationSubscription> getSubscriptionsToCreate(DelegateExecution context) {
        ConfigurationSubscription[] subscriptionsArray = GsonHelper.getFromBinaryJson(
            (byte[]) context.getVariable(Constants.VAR_SUBSCRIPTIONS_TO_CREATE), ConfigurationSubscription[].class);
        return Arrays.asList(subscriptionsArray);
    }

    static void setSubscriptionsToCreate(DelegateExecution context, List<ConfigurationSubscription> subscriptions) {
        byte[] subscriptionsByteArray = GsonHelper.getAsBinaryJson(subscriptions.toArray(new ConfigurationSubscription[] {}));
        context.setVariable(Constants.VAR_SUBSCRIPTIONS_TO_CREATE, subscriptionsByteArray);
    }

    static void setConfigurationEntriesToPublish(DelegateExecution context, Map<String, List<ConfigurationEntry>> configurationEntries) {
        byte[] configurationEntriesByteArray = GsonHelper.getAsBinaryJson(configurationEntries);
        context.setVariable(Constants.VAR_CONFIGURATION_ENTRIES_TO_PUBLISH, configurationEntriesByteArray);
    }

    static Map<String, List<ConfigurationEntry>> getConfigurationEntriesToPublish(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_CONFIGURATION_ENTRIES_TO_PUBLISH);
        return JsonUtil.fromJson(new String(binaryJson, StandardCharsets.UTF_8), new TypeToken<Map<String, List<ConfigurationEntry>>>() {
        }.getType());
    }

    static void setServiceBrokersToCreate(DelegateExecution context, List<CloudServiceBrokerExtended> serviceBrokers) {
        context.setVariable(Constants.VAR_SERVICE_BROKERS_TO_CREATE,
            GsonHelper.getAsBinaryJson(serviceBrokers.toArray(new CloudServiceBrokerExtended[] {})));
    }

    public static List<CloudServiceBrokerExtended> getServiceBrokersToCreate(DelegateExecution context) {
        CloudServiceBrokerExtended[] serviceBrokers = GsonHelper.getFromBinaryJson(
            (byte[]) context.getVariable(Constants.VAR_SERVICE_BROKERS_TO_CREATE), CloudServiceBrokerExtended[].class);
        return Arrays.asList(serviceBrokers);
    }

    public static List<ConfigurationEntry> getDeletedEntries(DelegateExecution context) {
        byte[] deletedEntriesByteArray = (byte[]) context.getVariable(Constants.VAR_DELETED_ENTRIES);
        if (deletedEntriesByteArray == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(GsonHelper.getFromBinaryJson(deletedEntriesByteArray, ConfigurationEntry[].class));
    }

    static List<ConfigurationEntry> getDeletedEntriesFromProcess(ActivitiFacade activitiFacade, String processInstanceId) {
        HistoricVariableInstance deletedEntries = activitiFacade.getHistoricVariableInstance(processInstanceId,
            Constants.VAR_DELETED_ENTRIES);
        if (deletedEntries == null) {
            return Collections.emptyList();
        }
        byte[] deletedEntriesByteArray = (byte[]) deletedEntries.getValue();
        return Arrays.asList(GsonHelper.getFromBinaryJson(deletedEntriesByteArray, ConfigurationEntry[].class));
    }

    static List<ConfigurationEntry> getDeletedEntriesFromAllProcesses(DelegateExecution context, ActivitiFacade activitiFacade) {
        List<ConfigurationEntry> configurationEntries = new ArrayList<>(
            StepsUtil.getDeletedEntriesFromProcess(activitiFacade, StepsUtil.getCorrelationId(context)));
        List<String> subProcessIds = activitiFacade.getHistoricSubProcessIds(StepsUtil.getCorrelationId(context));
        for (String subProcessId : subProcessIds) {
            configurationEntries.addAll(getDeletedEntriesFromProcess(activitiFacade, subProcessId));
        }
        return configurationEntries;
    }

    static void setDeletedEntries(DelegateExecution context, List<ConfigurationEntry> deletedEntries) {
        if (deletedEntries == null) {
            return;
        }
        byte[] deletedEntriesByteArray = GsonHelper.getAsBinaryJson(deletedEntries.toArray(new ConfigurationEntry[] {}));
        context.setVariable(Constants.VAR_DELETED_ENTRIES, deletedEntriesByteArray);
    }

    public static List<ConfigurationEntry> getPublishedEntries(DelegateExecution context) {
        ConfigurationEntry[] publishedEntriesArray = GsonHelper.getFromBinaryJson(
            (byte[]) context.getVariable(Constants.VAR_PUBLISHED_ENTRIES), ConfigurationEntry[].class);
        return Arrays.asList(publishedEntriesArray);
    }

    static List<ConfigurationEntry> getPublishedEntriesFromProcess(ActivitiFacade activitiFacade, String processInstanceId) {
        HistoricVariableInstance publishedEntries = activitiFacade.getHistoricVariableInstance(processInstanceId,
            Constants.VAR_PUBLISHED_ENTRIES);
        if (publishedEntries == null) {
            return Collections.emptyList();
        }
        byte[] binaryJson = (byte[]) publishedEntries.getValue();
        return Arrays.asList(GsonHelper.getFromBinaryJson(binaryJson, ConfigurationEntry[].class));
    }

    static List<ConfigurationEntry> getPublishedEntriesFromSubProcesses(DelegateExecution context, ActivitiFacade activitiFacade) {
        List<ConfigurationEntry> result = new ArrayList<>();
        List<String> subProcessIds = activitiFacade.getHistoricSubProcessIds(StepsUtil.getCorrelationId(context));
        for (String subProcessId : subProcessIds) {
            result.addAll(getPublishedEntriesFromProcess(activitiFacade, subProcessId));
        }
        return result;
    }

    static void setPublishedEntries(DelegateExecution context, List<ConfigurationEntry> publishedEntries) {
        byte[] publishedEntriesByteArray = GsonHelper.getAsBinaryJson(publishedEntries.toArray(new ConfigurationEntry[] {}));
        context.setVariable(Constants.VAR_PUBLISHED_ENTRIES, publishedEntriesByteArray);
    }

    static void setServiceUrlsToRegister(DelegateExecution context, List<ServiceUrl> serviceUrls) {
        context.setVariable(Constants.VAR_SERVICE_URLS_TO_REGISTER, GsonHelper.getAsBinaryJson(serviceUrls.toArray(new ServiceUrl[] {})));
    }

    public static List<ServiceUrl> getServiceUrlsToRegister(DelegateExecution context) {
        ServiceUrl[] serviceUrls = GsonHelper.getFromBinaryJson((byte[]) context.getVariable(Constants.VAR_SERVICE_URLS_TO_REGISTER),
            ServiceUrl[].class);
        return Arrays.asList(serviceUrls);
    }

    static void setDeployedMta(DelegateExecution context, DeployedMta deployedMta) {
        byte[] binaryJson = deployedMta == null ? null : GsonHelper.getAsBinaryJson(deployedMta);
        context.setVariable(Constants.VAR_DEPLOYED_MTA, binaryJson);
    }

    static DeployedMta getDeployedMta(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_DEPLOYED_MTA);
        return binaryJson == null ? null : GsonHelper.getFromBinaryJson(binaryJson, DeployedMta.class);
    }

    static Set<Integer> getAllocatedPorts(DelegateExecution context) {
        Integer[] allocatedPorts = GsonHelper.getFromBinaryJson((byte[]) context.getVariable(Constants.VAR_ALLOCATED_PORTS),
            Integer[].class);
        return allocatedPorts != null ? Arrays.stream(allocatedPorts).collect(Collectors.toSet()) : Collections.emptySet();
    }

    static void setAllocatedPorts(DelegateExecution context, Set<Integer> allocatedPorts) {
        context.setVariable(Constants.VAR_ALLOCATED_PORTS, GsonHelper.getAsBinaryJson(allocatedPorts.toArray(new Integer[0])));
    }

    static void setXsPlaceholderReplacementValues(DelegateExecution context, Map<String, Object> replacementValues) {
        byte[] replacementValuesJson = GsonHelper.getAsBinaryJson(replacementValues);
        context.setVariable(Constants.VAR_XS_PLACEHOLDER_REPLACEMENT_VALUES, replacementValuesJson);
    }

    static Map<String, Object> getXsPlaceholderReplacementValues(DelegateExecution context) throws SLException {
        byte[] replacementValuesJson = (byte[]) context.getVariable(Constants.VAR_XS_PLACEHOLDER_REPLACEMENT_VALUES);
        return JsonUtil.convertJsonToMap(new String(replacementValuesJson, StandardCharsets.UTF_8));
    }

    static XsPlaceholderResolver getXsPlaceholderResolver(DelegateExecution context) throws SLException {
        Map<String, Object> replacementValues = getXsPlaceholderReplacementValues(context);
        XsPlaceholderResolver resolver = new XsPlaceholderResolver();
        resolver.setControllerEndpoint((String) replacementValues.get(SupportedParameters.XSA_CONTROLLER_ENDPOINT_PLACEHOLDER));
        resolver.setRouterPort(((Double) replacementValues.get(SupportedParameters.XSA_ROUTER_PORT_PLACEHOLDER)).intValue());
        resolver.setAuthorizationEndpoint((String) replacementValues.get(SupportedParameters.XSA_AUTHORIZATION_ENDPOINT_PLACEHOLDER));
        resolver.setDeployServiceUrl((String) replacementValues.get(SupportedParameters.XSA_DEPLOY_SERVICE_URL_PLACEHOLDER));
        resolver.setProtocol((String) replacementValues.get(SupportedParameters.XSA_PROTOCOL_PLACEHOLDER));
        resolver.setDefaultDomain((String) replacementValues.get(SupportedParameters.XSA_DEFAULT_DOMAIN_PLACEHOLDER));
        return resolver;
    }

    static DeploymentDescriptor getDeploymentDescriptor(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR);
        return parseDeploymentDescriptor(context, binaryJson);
    }

    static DeploymentDescriptor getUnresolvedDeploymentDescriptor(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_MTA_UNRESOLVED_DEPLOYMENT_DESCRIPTOR);
        return parseDeploymentDescriptor(context, binaryJson);
    }

    private static DeploymentDescriptor parseDeploymentDescriptor(DelegateExecution context, byte[] binaryJson) {
        int majorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
        int minorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION);
        switch (majorSchemaVersion) {
            case 1:
                return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor.class);
            case 2:
                return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor.class);
            case 3:
                switch (minorSchemaVersion) {
                    case 0:
                        return getBinaryJsonForMtaModel().unmarshal(binaryJson,
                            com.sap.cloud.lm.sl.mta.model.v3_0.DeploymentDescriptor.class);
                    case 1:
                        return getBinaryJsonForMtaModel().unmarshal(binaryJson,
                            com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor.class);
                }
            default:
                return null;
        }
    }

    static void setUnresolvedDeploymentDescriptor(DelegateExecution context, DeploymentDescriptor descriptor) {
        context.setVariable(Constants.VAR_MTA_UNRESOLVED_DEPLOYMENT_DESCRIPTOR, getBinaryJsonForMtaModel().marshal(descriptor));
    }

    static void setDeploymentDescriptor(DelegateExecution context, DeploymentDescriptor descriptor) {
        context.setVariable(Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR, getBinaryJsonForMtaModel().marshal(descriptor));
    }

    static String getDeploymentDescriptorString(DelegateExecution context) {
        return new String((byte[]) context.getVariable(Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR_STRING), Charset.forName("UTF-8"));
    }

    static void setDeploymentDescriptorString(DelegateExecution context, String descriptor) {
        context.setVariable(Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR_STRING, descriptor.getBytes(Charset.forName("UTF-8")));
    }

    static List<String> getExtensionDescriptorStrings(DelegateExecution context) {
        return ContextUtil.getArrayVariableAsList(context, Constants.VAR_MTA_EXTENSION_DESCRIPTOR_STRINGS);
    }

    static void setExtensionDescriptorStrings(DelegateExecution context, List<String> descriptors) {
        ContextUtil.setArrayVariableFromCollection(context, Constants.VAR_MTA_EXTENSION_DESCRIPTOR_STRINGS, descriptors);
    }

    static SystemParameters getSystemParameters(DelegateExecution context) {
        return GsonHelper.getFromBinaryJson((byte[]) context.getVariable(Constants.VAR_SYSTEM_PARAMETERS), SystemParameters.class);
    }

    static void setSystemParameters(DelegateExecution context, SystemParameters systemParameters) {
        ContextUtil.setAsBinaryJson(context, Constants.VAR_SYSTEM_PARAMETERS, systemParameters);
    }

    static void setAppPropertiesChanged(DelegateExecution context, boolean state) {
        context.setVariable(Constants.VAR_APP_PROPERTIES_CHANGED, state);
    }

    static boolean getAppPropertiesChanged(DelegateExecution context) {
        return (boolean) context.getVariable(Constants.VAR_APP_PROPERTIES_CHANGED);
    }

    static CloudApplicationExtended getApp(DelegateExecution context) {
        return JsonUtil.fromJson((String) context.getVariable(Constants.VAR_APP_TO_DEPLOY), CloudApplicationExtended.class);
    }

    static void setApp(DelegateExecution context, CloudApplicationExtended app) {
        context.setVariable(Constants.VAR_APP_TO_DEPLOY, JsonUtil.toJson(app));
    }

    static CloudTask getTask(DelegateExecution context) {
        List<CloudTask> tasks = StepsUtil.getTasksToExecute(context);
        int index = (Integer) context.getVariable(Constants.VAR_TASKS_INDEX);
        return tasks.get(index);
    }

    static CloudApplicationExtended getAppToRestart(DelegateExecution context) {
        List<String> appsToRestart = StepsUtil.getAppsToRestart(context);
        int index = (Integer) context.getVariable(Constants.VAR_APPS_INDEX);
        final String appToRestartName = appsToRestart.get(index);
        List<CloudApplicationExtended> appsToDeploy = StepsUtil.getAppsToDeploy(context);
        return appsToDeploy.stream().filter((app) -> app.getName().equals(appToRestartName)).findFirst().get();
    }

    static CloudApplication getExistingApp(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_EXISTING_APP);
        return (binaryJson != null) ? GsonHelper.getFromBinaryJson(binaryJson, CloudApplication.class) : null;
    }

    static void setExistingApp(DelegateExecution context, CloudApplication app) {
        byte[] binaryJson = (app != null) ? GsonHelper.getAsBinaryJson(app) : null;
        context.setVariable(Constants.VAR_EXISTING_APP, binaryJson);
    }

    static Set<ApplicationStateAction> getAppStateActionsToExecute(DelegateExecution context) {
        @SuppressWarnings("unchecked")
        Set<String> actionsAsStrings = (Set<String>) context.getVariable(Constants.VAR_APP_STATE_ACTIONS_TO_EXECUTE);
        return actionsAsStrings.stream().map(action -> ApplicationStateAction.valueOf(action)).collect(Collectors.toSet());
    }

    static void setAppStateActionsToExecute(DelegateExecution context, Set<ApplicationStateAction> actions) {
        Set<String> actionsAsStrings = actions.stream().map(action -> action.toString()).collect(Collectors.toSet());
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

    static void saveAppLogs(DelegateExecution context, CloudFoundryOperations client, RecentLogsRetriever recentLogsRetriever,
        CloudApplication app, Logger logger, ProcessLoggerProviderFactory processLoggerProviderFactory) {
        List<ApplicationLog> recentLogs = recentLogsRetriever.getRecentLogs(client, app.getName());
        if (recentLogs != null) {
            recentLogs.forEach(log -> appLog(context, app.getName(), log.toString(), logger, processLoggerProviderFactory));
        }
    }

    static boolean hasTimedOut(DelegateExecution context, Supplier<Long> currentTimeSupplier) {
        int timeout = (Integer) context.getVariable(Constants.PARAM_START_TIMEOUT);
        long startTime = (Long) context.getVariable(Constants.VAR_START_TIME);
        long currentTime = currentTimeSupplier.get();
        return (currentTime - startTime) > timeout * 1000;
    }

    static StartingInfo getStartingInfo(DelegateExecution context) {
        String className = (String) context.getVariable(Constants.VAR_STARTING_INFO_CLASSNAME);
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_STARTING_INFO);
        return (binaryJson != null) ? GsonHelper.getFromBinaryJson(binaryJson, getStartingInfoClass(className)) : null;
    }

    static void setStartingInfo(DelegateExecution context, StartingInfo startingInfo) {
        byte[] binaryJson = (startingInfo != null) ? GsonHelper.getAsBinaryJson(startingInfo) : null;
        context.setVariable(Constants.VAR_STARTING_INFO, binaryJson);
        String className = (startingInfo != null) ? startingInfo.getClass().getName() : StartingInfo.class.getName();
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
        return (binaryJson != null) ? GsonHelper.getFromBinaryJson(binaryJson, StreamingLogToken.class) : null;
    }

    static void setStreamingLogsToken(DelegateExecution context, StreamingLogToken streamingLogToken) {
        byte[] binaryJson = (streamingLogToken != null) ? GsonHelper.getAsBinaryJson(streamingLogToken) : null;
        context.setVariable(Constants.VAR_STREAMING_LOGS_TOKEN, binaryJson);
    }

    static void setMtaVersionAccepted(DelegateExecution context, boolean versionAccepted) {
        context.setVariable(Constants.VAR_MTA_VERSION_ACCEPTED, versionAccepted);
    }

    static void setMtaArchiveModules(DelegateExecution context, Set<String> mtaArchiveModules) {
        ContextUtil.setArrayVariableFromCollection(context, Constants.VAR_MTA_ARCHIVE_MODULES, mtaArchiveModules);
    }

    static Set<String> getMtaArchiveModules(DelegateExecution context) {
        return ContextUtil.getArrayVariableAsSet(context, Constants.VAR_MTA_ARCHIVE_MODULES);
    }

    static void setMtaModules(DelegateExecution context, Set<String> mtaModules) {
        ContextUtil.setArrayVariableFromCollection(context, Constants.VAR_MTA_MODULES, mtaModules);
    }

    static Set<String> getMtaModules(DelegateExecution context) {
        return ContextUtil.getArrayVariableAsSet(context, Constants.VAR_MTA_MODULES);
    }

    static String getUploadToken(DelegateExecution context) {
        return (String) context.getVariable(Constants.VAR_UPLOAD_TOKEN);
    }

    public static String getCorrelationId(DelegateExecution context) {
        return (String) context.getVariable(Constants.VAR_CORRELATION_ID);
    }

    public static String getIndexedStepName(DelegateExecution context) {
        return (String) context.getVariable(com.sap.cloud.lm.sl.cf.api.activiti.Constants.INDEXED_STEP_NAME);
    }

    static ErrorType getErrorType(String processId, ContextExtensionDao contextExtensionDao) {
        ContextExtension errorTypeExtension = contextExtensionDao.find(processId, Constants.VAR_ERROR_TYPE);
        if (errorTypeExtension == null || errorTypeExtension.getValue() == null) {
            return null;
        }
        return ErrorType.valueOf(errorTypeExtension.getValue());
    }

    static void setErrorType(String processId, ContextExtensionDao contextExtensionDao, ErrorType errorType) {
        if (errorType == null) {
            return;
        }
        contextExtensionDao.addOrUpdate(processId, Constants.VAR_ERROR_TYPE, errorType.toString());
    }

    static void appLog(DelegateExecution context, String appName, String message, Logger logger,
        ProcessLoggerProviderFactory processLoggerProviderFactory) {
        getAppLogger(context, appName, processLoggerProviderFactory).debug(getPrefix(logger) + "[" + appName + "] " + message);
    }

    static SLException createException(CloudFoundryException e) {
        return new SLException(e, Messages.CF_ERROR, e.getMessage());
    }

    private static String getPrefix(Logger logger) {
        String name = logger.getName();
        return "[" + name.substring(name.lastIndexOf('.') + 1) + "] ";
    }

    public static void setArchiveFileId(DelegateExecution context, String uploadedMtarId) {
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, uploadedMtarId);
    }

    public static ProcessType getProcessType(DelegateExecution context) throws SLException {
        String serviceId = getServiceId(context);
        switch (serviceId) {
            case Constants.UNDEPLOY_SERVICE_ID:
                return ProcessType.UNDEPLOY;
            case Constants.DEPLOY_SERVICE_ID:
                return ProcessType.DEPLOY;
            case Constants.BLUE_GREEN_DEPLOY_SERVICE_ID:
                return ProcessType.BLUE_GREEN_DEPLOY;
            default:
                throw new SLException(Messages.UNKNOWN_SERVICE_ID, serviceId);
        }
    }

    public static String getServiceId(DelegateExecution context) {
        return (String) context.getVariable(com.sap.cloud.lm.sl.cf.api.activiti.Constants.VARIABLE_NAME_SERVICE_ID);
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

    static DomainsCloudModelBuilder getDomainsCloudModelBuilder(DelegateExecution context) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);

        SystemParameters systemParameters = StepsUtil.getSystemParameters(context);

        XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(context);

        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);
        return handlerFactory.getDomainsCloudModelBuilder(systemParameters, xsPlaceholderResolver, deploymentDescriptor);
    }

    static ServicesCloudModelBuilder getServicesCloudModelBuilder(DelegateExecution context) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
        CloudModelConfiguration configuration = getCloudBuilderConfiguration(context, true);
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);
        return handlerFactory.getServicesCloudModelBuilder(deploymentDescriptor, handlerFactory.getPropertiesAccessor(), configuration);
    }

    static CloudModelConfiguration getCloudBuilderConfiguration(DelegateExecution context, boolean prettyPrinting) {
        Boolean allowInvalidEnvNames = ContextUtil.getVariable(context, Constants.PARAM_ALLOW_INVALID_ENV_NAMES, Boolean.FALSE);
        Boolean useNamespaces = ContextUtil.getVariable(context, Constants.PARAM_USE_NAMESPACES, Boolean.FALSE);
        Boolean useNamespacesForServices = ContextUtil.getVariable(context, Constants.PARAM_USE_NAMESPACES_FOR_SERVICES, Boolean.FALSE);
        Boolean portBasedRouting = ContextUtil.getVariable(context, Constants.VAR_PORT_BASED_ROUTING, Boolean.FALSE);
        CloudModelConfiguration configuration = new CloudModelConfiguration();
        configuration.setAllowInvalidEnvNames(allowInvalidEnvNames);
        configuration.setPortBasedRouting(portBasedRouting);
        configuration.setPrettyPrinting(prettyPrinting);
        configuration.setUseNamespaces(useNamespaces);
        configuration.setUseNamespacesForServices(useNamespacesForServices);
        return configuration;
    }

}
