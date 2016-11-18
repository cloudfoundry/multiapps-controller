package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.identity.Authentication;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.activiti.common.util.GsonHelper;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKey;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyImpl;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.BinaryJson;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.model.json.PropertiesAdapterFactory;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.slp.services.ProcessLoggerProviderFactory;

public class StepsUtil {

    private static final String PARENT_LOGGER = "com.sap.cloud.lm.sl.xs2";

    // Logger
    static final org.apache.log4j.Logger getLogger(DelegateExecution context, ProcessLoggerProviderFactory processLoggerProviderFactory) {
        return processLoggerProviderFactory.getDefaultLoggerProvider().getLogger(context, PARENT_LOGGER);
    }

    private static org.apache.log4j.Logger getAppLogger(DelegateExecution context, String appName,
        ProcessLoggerProviderFactory processLoggerProviderFactory) {
        return processLoggerProviderFactory.getLoggerProvider(appName).getLogger(context, PARENT_LOGGER, appName);
    }

    static CloudFoundryOperations getCloudFoundryClient(DelegateExecution context, CloudFoundryClientProvider clientProvider, Logger logger,
        ProcessLoggerProviderFactory processLoggerProviderFactory) throws SLException {
        return getCloudFoundryClient(context, clientProvider, logger, processLoggerProviderFactory, getOrg(context), getSpace(context));
    }

    static CloudFoundryOperations getCloudFoundryClient(DelegateExecution context, CloudFoundryClientProvider clientProvider, Logger logger,
        ProcessLoggerProviderFactory processLoggerProviderFactory, String org, String space) throws SLException {
        // Determine the current user
        String userName = determineCurrentUser(context, logger, processLoggerProviderFactory);

        debug(context, format(Messages.CURRENT_USER, userName), logger, processLoggerProviderFactory);
        debug(context, format(Messages.CLIENT_SPACE, space), logger, processLoggerProviderFactory);
        debug(context, format(Messages.CLIENT_ORG, org), logger, processLoggerProviderFactory);

        return clientProvider.getCloudFoundryClient(userName, org, space, context.getProcessInstanceId());
    }

    static ClientExtensions getClientExtensions(DelegateExecution context, CloudFoundryClientProvider clientProvider, Logger logger,
        ProcessLoggerProviderFactory processLoggerProviderFactory) throws SLException {
        CloudFoundryOperations cloudFoundryClient = StepsUtil.getCloudFoundryClient(context, clientProvider, logger,
            processLoggerProviderFactory);
        if (cloudFoundryClient instanceof ClientExtensions) {
            return (ClientExtensions) cloudFoundryClient;
        }
        return null;
    }

    static ClientExtensions getClientExtensions(DelegateExecution context, CloudFoundryClientProvider clientProvider, Logger logger,
        ProcessLoggerProviderFactory processLoggerProviderFactory, String org, String space) throws SLException {
        CloudFoundryOperations cloudFoundryClient = StepsUtil.getCloudFoundryClient(context, clientProvider, logger,
            processLoggerProviderFactory, org, space);
        if (cloudFoundryClient instanceof ClientExtensions) {
            return (ClientExtensions) cloudFoundryClient;
        }
        return null;
    }

    public static String determineCurrentUser(DelegateExecution context, Logger logger,
        ProcessLoggerProviderFactory processLoggerProviderFactory) throws SLException {
        String userId = Authentication.getAuthenticatedUserId();

        // Determine the current user
        debug(context, format(Messages.AUTHENTICATED_USER_ID, userId), logger, processLoggerProviderFactory);
        if (userId == null) {
            // If the authenticated user cannot be determined,
            // use the user saved by the previous service task
            userId = (String) context.getVariable(Constants.VAR_USER);
            debug(context, format(Messages.PREVIOUS_USER, userId), logger, processLoggerProviderFactory);
            if (userId == null) {
                // If there is no previous user, this must be the first service task
                // Use the process initiator in this case
                userId = (String) context.getVariable(Constants.PARAM_INITIATOR);
                debug(context, format(Messages.PROCESS_INITIATOR, userId), logger, processLoggerProviderFactory);
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

    static TargetPlatform getPlatform(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_PLATFORM);

        int majorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
        int minorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION);
        switch (majorSchemaVersion) {
            case 1:
                return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform.class);
            case 2:
                return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatform.class);
            case 3:
                switch (minorSchemaVersion) {
                    case 0:
                        return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v3_0.TargetPlatform.class);
                    case 1:
                        return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v3_1.TargetPlatform.class);
                }
            default:
                return null;
        }
    }

    static void setPlatform(DelegateExecution context, TargetPlatform platform) {
        context.setVariable(Constants.VAR_PLATFORM, getBinaryJsonForMtaModel().marshal(platform));
    }

    static TargetPlatformType getPlatformType(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_PLATFORM_TYPE);

        int majorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
        int minorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION);
        switch (majorSchemaVersion) {
            case 1:
                return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType.class);
            case 2:
                return getBinaryJsonForMtaModel().unmarshal(binaryJson, com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatformType.class);
            case 3:
                switch (minorSchemaVersion) {
                    case 0:
                        return getBinaryJsonForMtaModel().unmarshal(binaryJson,
                            com.sap.cloud.lm.sl.mta.model.v3_0.TargetPlatformType.class);
                    case 1:
                        return getBinaryJsonForMtaModel().unmarshal(binaryJson,
                            com.sap.cloud.lm.sl.mta.model.v3_1.TargetPlatformType.class);
                }
            default:
                return null;
        }
    }

    static void setPlatformType(DelegateExecution context, TargetPlatformType platformType) {
        context.setVariable(Constants.VAR_PLATFORM_TYPE, getBinaryJsonForMtaModel().marshal(platformType));
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
        return (String) context.getVariable(com.sap.cloud.lm.sl.slp.Constants.VARIABLE_NAME_SPACE_ID);
    }

    public static void setSpaceId(DelegateExecution context, String spaceId) {
        context.setVariable(com.sap.cloud.lm.sl.slp.Constants.VARIABLE_NAME_SPACE_ID, spaceId);
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

    static List<String> getCustomDomains(DelegateExecution context) {
        return ContextUtil.getArrayVariableAsList(context, Constants.VAR_CUSTOM_DOMAINS);
    }

    static void setCustomDomains(DelegateExecution context, List<String> customDomains) {
        ContextUtil.setArrayVariableFromCollection(context, Constants.VAR_CUSTOM_DOMAINS, customDomains);
    }

    static List<CloudServiceExtended> getServicesToCreate(DelegateExecution context) {
        CloudServiceExtended[] cloudServices = GsonHelper.getFromBinaryJson((byte[]) context.getVariable(Constants.VAR_SERVICES_TO_CREATE),
            CloudServiceExtended[].class);
        return Arrays.asList(cloudServices);
    }

    static void setServicesToCreate(DelegateExecution context, List<CloudServiceExtended> services) {
        context.setVariable(Constants.VAR_SERVICES_TO_CREATE, GsonHelper.getAsBinaryJson(services.toArray(new CloudService[] {})));
    }

    static void setUpdatedServices(DelegateExecution context, Set<String> updatedServices) {
        ContextUtil.setArrayVariableFromCollection(context, Constants.VAR_UPDATED_SERVICES, updatedServices);
    }

    static Set<String> getUpdatedServices(DelegateExecution context) {
        return ContextUtil.getArrayVariableAsSet(context, Constants.VAR_UPDATED_SERVICES);
    }

    static Map<String, List<ServiceKey>> getServiceKeysToCreate(DelegateExecution context) {
        String json = new String((byte[]) context.getVariable(Constants.VAR_SERVICE_KEYS_TO_CREATE), StandardCharsets.UTF_8);
        return new Gson().fromJson(json, new TypeToken<Map<String, List<ServiceKeyImpl>>>() {
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

    static List<CloudApplicationExtended> getAppsToDeploy(DelegateExecution context) {
        CloudApplicationExtended[] apps = GsonHelper.getFromBinaryJson((byte[]) context.getVariable(Constants.VAR_APPS_TO_DEPLOY),
            CloudApplicationExtended[].class);
        return Arrays.asList(apps);
    }

    static void setAppsToDeploy(DelegateExecution context, List<CloudApplicationExtended> apps) {
        context.setVariable(Constants.VAR_APPS_TO_DEPLOY, GsonHelper.getAsBinaryJson(apps.toArray(new CloudApplicationExtended[] {})));
    }

    static List<CloudApplication> getAppsToUndeploy(DelegateExecution context) {
        CloudApplication[] apps = GsonHelper.getFromBinaryJson((byte[]) context.getVariable(Constants.VAR_APPS_TO_UNDEPLOY),
            CloudApplication[].class);
        return Arrays.asList(apps);
    }

    static void setAppsToUndeploy(DelegateExecution context, List<CloudApplication> apps) {
        context.setVariable(Constants.VAR_APPS_TO_UNDEPLOY, GsonHelper.getAsBinaryJson(apps.toArray(new CloudApplication[] {})));
    }

    static List<String> getServicesToDelete(DelegateExecution context) {
        return ContextUtil.getArrayVariableAsList(context, Constants.VAR_SERVICES_TO_DELETE);
    }

    static void setServicesToDelete(DelegateExecution context, List<String> services) {
        ContextUtil.setArrayVariableFromCollection(context, Constants.VAR_SERVICES_TO_DELETE, services);
    }

    static List<ConfigurationSubscription> getSubscriptionsToDelete(DelegateExecution context) {
        ConfigurationSubscription[] subscriptionsArray = GsonHelper.getFromBinaryJson(
            (byte[]) context.getVariable(Constants.VAR_SUBSCRIPTIONS_TO_DELETE), ConfigurationSubscription[].class);
        return Arrays.asList(subscriptionsArray);
    }

    static void setSubscriptionsToDelete(DelegateExecution context, List<ConfigurationSubscription> subscriptions) {
        byte[] subscriptionsByteArray = GsonHelper.getAsBinaryJson(subscriptions.toArray(new ConfigurationSubscription[] {}));
        context.setVariable(Constants.VAR_SUBSCRIPTIONS_TO_DELETE, subscriptionsByteArray);
    }

    static List<ConfigurationSubscription> getSubscriptionsToCreate(DelegateExecution context) {
        ConfigurationSubscription[] subscriptionsArray = GsonHelper.getFromBinaryJson(
            (byte[]) context.getVariable(Constants.VAR_SUBSCRIPTIONS_TO_CREATE), ConfigurationSubscription[].class);
        return Arrays.asList(subscriptionsArray);
    }

    static void setSubscriptionsToCreate(DelegateExecution context, List<ConfigurationSubscription> subscriptions) {
        byte[] subscriptionsByteArray = GsonHelper.getAsBinaryJson(subscriptions.toArray(new ConfigurationSubscription[] {}));
        context.setVariable(Constants.VAR_SUBSCRIPTIONS_TO_CREATE, subscriptionsByteArray);
    }

    static void setDependenciesToPublish(DelegateExecution context, List<ProvidedDependency> providedDependencies) {
        byte[] providedDependenciesByteArray = GsonHelper.getAsBinaryJson(providedDependencies.toArray(new ProvidedDependency[] {}));
        context.setVariable(Constants.VAR_DEPENDENCIES_TO_PUBLISH, providedDependenciesByteArray);
    }

    static List<ProvidedDependency> getDependenciesToPublish(DelegateExecution context) {
        ProvidedDependency[] providedDependenciesArray = GsonHelper.getFromBinaryJson(
            (byte[]) context.getVariable(Constants.VAR_DEPENDENCIES_TO_PUBLISH), ProvidedDependency[].class);
        return Arrays.asList(providedDependenciesArray);
    }

    static void setServiceBrokersToCreate(DelegateExecution context, List<CloudServiceBroker> serviceBrokers) {
        context.setVariable(Constants.VAR_SERVICE_BROKERS_TO_CREATE,
            GsonHelper.getAsBinaryJson(serviceBrokers.toArray(new CloudServiceBroker[] {})));
    }

    static List<CloudServiceBroker> getServiceBrokersToCreate(DelegateExecution context) {
        CloudServiceBroker[] serviceBrokers = GsonHelper.getFromBinaryJson(
            (byte[]) context.getVariable(Constants.VAR_SERVICE_BROKERS_TO_CREATE), CloudServiceBroker[].class);
        return Arrays.asList(serviceBrokers);
    }

    static List<ConfigurationEntry> getDeletedEntries(DelegateExecution context) {
        byte[] deletedEntriesByteArray = (byte[]) context.getVariable(Constants.VAR_DELETED_ENTRIES);
        if (deletedEntriesByteArray == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(GsonHelper.getFromBinaryJson(deletedEntriesByteArray, ConfigurationEntry[].class));
    }

    static void setDeletedEntries(DelegateExecution context, List<ConfigurationEntry> deletedEntries) {
        if (deletedEntries == null) {
            return;
        }
        byte[] deletedEntriesByteArray = GsonHelper.getAsBinaryJson(deletedEntries.toArray(new ConfigurationEntry[] {}));
        context.setVariable(Constants.VAR_DELETED_ENTRIES, deletedEntriesByteArray);
    }

    static List<ConfigurationEntry> getPublishedEntries(DelegateExecution context) {
        ConfigurationEntry[] publishedEntriesArray = GsonHelper.getFromBinaryJson(
            (byte[]) context.getVariable(Constants.VAR_PUBLISHED_ENTRIES), ConfigurationEntry[].class);
        return Arrays.asList(publishedEntriesArray);
    }

    static void setPublishedEntries(DelegateExecution context, List<ConfigurationEntry> publishedEntries) {
        byte[] publishedEntriesByteArray = GsonHelper.getAsBinaryJson(publishedEntries.toArray(new ConfigurationEntry[] {}));
        context.setVariable(Constants.VAR_PUBLISHED_ENTRIES, publishedEntriesByteArray);
    }

    static void setServiceUrlsToRegister(DelegateExecution context, List<ServiceUrl> serviceUrls) {
        context.setVariable(Constants.VAR_SERVICE_URLS_TO_REGISTER, GsonHelper.getAsBinaryJson(serviceUrls.toArray(new ServiceUrl[] {})));
    }

    static List<ServiceUrl> getServiceUrlsToRegister(DelegateExecution context) {
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
        List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(context);
        int index = (Integer) context.getVariable(Constants.VAR_APPS_INDEX);
        return apps.get(index);
    }

    static CloudApplication getExistingApp(DelegateExecution context) {
        byte[] binaryJson = (byte[]) context.getVariable(Constants.VAR_EXISTING_APP);
        return (binaryJson != null) ? GsonHelper.getFromBinaryJson(binaryJson, CloudApplication.class) : null;
    }

    static void setExistingApp(DelegateExecution context, CloudApplication app) {
        byte[] binaryJson = (app != null) ? GsonHelper.getAsBinaryJson(app) : null;
        context.setVariable(Constants.VAR_EXISTING_APP, binaryJson);
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

    static void setRestartApplication(DelegateExecution context, boolean restartApplication) {
        context.setVariable(Constants.VAR_RESTART_APPLICATION, restartApplication);
    }

    static boolean getRestartApplication(DelegateExecution context) {
        return (boolean) context.getVariable(Constants.VAR_RESTART_APPLICATION);
    }

    static Set<String> getMtaModules(DelegateExecution context) {
        return ContextUtil.getArrayVariableAsSet(context, Constants.VAR_MTA_MODULES);
    }

    static String getUploadToken(DelegateExecution context) {
        return (String) context.getVariable(Constants.VAR_UPLOAD_TOKEN);
    }

    static void logActivitiTask(DelegateExecution context, Logger logger, ProcessLoggerProviderFactory processLoggerProvider) {
        String message = format(Messages.EXECUTING_ACTIVITI_TASK, context.getId(), context.getCurrentActivityId());
        debug(context, message, logger, processLoggerProvider);
    }

    static void error(DelegateExecution context, String message, Exception e, Logger logger, ProgressMessageService progressMessageService,
        ProcessLoggerProviderFactory processLoggerProviderFactory) {
        error(context, getExtendedErrorMessage(message, e), logger, progressMessageService, processLoggerProviderFactory);
    }

    static void error(DelegateExecution context, String message, Logger logger, ProgressMessageService progressMessageService,
        ProcessLoggerProviderFactory processLoggerProviderFactory) {
        logger.error(message);
        sendProgressMessage(context, message, ProgressMessageType.ERROR, progressMessageService, processLoggerProviderFactory);
        getLogger(context, processLoggerProviderFactory).error(getPrefix(logger) + message);
    }

    static void warn(DelegateExecution context, String message, Exception e, Logger logger, ProgressMessageService progressMessageService,
        ProcessLoggerProviderFactory processLoggerProviderFactory) {
        logger.warn(message, e);
        sendProgressMessage(context, message, ProgressMessageType.WARNING, progressMessageService, processLoggerProviderFactory);
        getLogger(context, processLoggerProviderFactory).warn(getPrefix(logger) + message, e);
    }

    static void warn(DelegateExecution context, String message, Logger logger, ProgressMessageService progressMessageService,
        ProcessLoggerProviderFactory processLoggerProviderFactory) {
        logger.warn(message);
        sendProgressMessage(context, message, ProgressMessageType.WARNING, progressMessageService, processLoggerProviderFactory);
        getLogger(context, processLoggerProviderFactory).warn(getPrefix(logger) + message);
    }

    static void info(DelegateExecution context, String message, Logger logger, ProgressMessageService progressMessageService,
        ProcessLoggerProviderFactory processLoggerProviderFactory) {
        logger.info(message);
        sendProgressMessage(context, message, ProgressMessageType.INFO, progressMessageService, processLoggerProviderFactory);
        getLogger(context, processLoggerProviderFactory).info(getPrefix(logger) + message);
    }

    private static String getExtendedErrorMessage(String message, Exception e) {
        return message + ": " + e.getMessage();
    }

    static void debug(DelegateExecution context, String message, Logger logger, ProcessLoggerProviderFactory processLoggerProviderFactory) {
        logger.debug(message);
        getLogger(context, processLoggerProviderFactory).debug(getPrefix(logger) + message);
    }

    static void trace(DelegateExecution context, String message, Logger logger, ProcessLoggerProviderFactory processLoggerProviderFactory) {
        logger.trace(message);
        getLogger(context, processLoggerProviderFactory).trace(getPrefix(logger) + message);
    }

    static String getIndexedStepName(DelegateExecution context) {
        return (String) context.getVariable(com.sap.cloud.lm.sl.slp.Constants.INDEXED_STEP_NAME);
    }

    static void appLog(DelegateExecution context, String appName, String message, Logger logger,
        ProcessLoggerProviderFactory processLoggerProviderFactory) {
        getAppLogger(context, appName, processLoggerProviderFactory).debug(getPrefix(logger) + "[" + appName + "] " + message);
    }

    static SLException createException(CloudFoundryException e) {
        String message = e.getStatusText();
        if (e.getDescription() != null) {
            message += ": " + e.getDescription();
        }
        if (e.getCloudFoundryErrorCode() != -1) {
            message += " (" + e.getCloudFoundryErrorCode() + ")";
        }
        return new SLException(e, Messages.CF_ERROR, message);
    }

    @SuppressWarnings("unchecked")
    static <T> T getAppAttribute(CloudApplication app, String attribute, T defaultValue) throws SLException {
        String attributes = app.getEnvAsMap().get(CloudModelBuilder.ENV_DEPLOY_ATTRIBUTES);
        if (attributes != null) {
            Map<String, Object> map = JsonUtil.convertJsonToMap(attributes);
            if (map != null && map.containsKey(attribute)) {
                return (T) map.get(attribute);
            }
        }
        return defaultValue;
    }

    private static void sendProgressMessage(DelegateExecution context, String message, ProgressMessageType type,
        ProgressMessageService progressMessageService, ProcessLoggerProviderFactory processLoggerProviderFactory) {
        try {
            progressMessageService.add(new ProgressMessage(context.getProcessInstanceId(), getIndexedStepName(context), type, message,
                new Timestamp(System.currentTimeMillis())));
        } catch (SLException e) {
            getLogger(context, processLoggerProviderFactory).error(e);
        }
    }

    private static String getPrefix(Logger logger) {
        String name = logger.getName();
        return "[" + name.substring(name.lastIndexOf('.') + 1) + "] ";
    }

    public static void setArchiveFileId(DelegateExecution context, String uploadedMtarId) {
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, uploadedMtarId);
    }

    public static String getServiceId(DelegateExecution context) {
        return (String) context.getVariable(com.sap.cloud.lm.sl.slp.Constants.VARIABLE_NAME_SERVICE_ID);
    }

}
