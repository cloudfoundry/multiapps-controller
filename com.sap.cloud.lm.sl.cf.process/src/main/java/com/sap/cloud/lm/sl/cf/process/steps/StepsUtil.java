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
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.DeploymentMode;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveElements;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ImmutableLogsOffset;
import com.sap.cloud.lm.sl.cf.core.util.LogsOffset;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.ServiceAction;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.YamlUtil;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;

public class StepsUtil {

    public static final String DEPLOY_ID_PREFIX = "deploy-";

    protected StepsUtil() {
    }

    public static org.apache.log4j.Logger getLogger(DelegateExecution context, String name, ProcessLoggerProvider processLoggerProvider) {
        return processLoggerProvider.getLogger(context, name);
    }

    static CloudControllerClient getControllerClient(DelegateExecution context, CloudControllerClientProvider clientProvider) {
        String userName = determineCurrentUser(context);
        String spaceId = getSpaceId(context);
        return clientProvider.getControllerClient(userName, spaceId);
    }

    static CloudControllerClient getControllerClient(DelegateExecution context, CloudControllerClientProvider clientProvider, String org,
                                                     String space) {
        String userName = determineCurrentUser(context);
        return clientProvider.getControllerClient(userName, org, space, context.getProcessInstanceId());
    }

    public static String determineCurrentUser(VariableScope scope) {
        String user = (String) scope.getVariable(Constants.VAR_USER);
        if (user == null) {
            throw new SLException(Messages.CANT_DETERMINE_CURRENT_USER);
        }
        return user;
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

    public static ApplicationColor getLiveMtaColor(VariableScope scope) {
        ApplicationColor liveMtaColor = getEnum(scope, Constants.VAR_LIVE_MTA_COLOR, ApplicationColor::valueOf);
        return liveMtaColor != null ? liveMtaColor : getEnum(scope, Constants.VAR_DEPLOYED_MTA_COLOR, ApplicationColor::valueOf);
    }

    public static void setLiveMtaColor(VariableScope scope, ApplicationColor liveMtaColor) {
        setEnum(scope, Constants.VAR_LIVE_MTA_COLOR, liveMtaColor);
    }

    public static ApplicationColor getMtaColor(VariableScope scope) {
        ApplicationColor idleMtaColor = getEnum(scope, Constants.VAR_IDLE_MTA_COLOR, ApplicationColor::valueOf);
        return idleMtaColor != null ? idleMtaColor : getEnum(scope, Constants.VAR_MTA_COLOR, ApplicationColor::valueOf);
    }

    public static void setIdleMtaColor(VariableScope scope, ApplicationColor mtaColor) {
        setEnum(scope, Constants.VAR_IDLE_MTA_COLOR, mtaColor);
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

    public static String getOrgId(VariableScope scope) {
        return getString(scope, Constants.VAR_ORG_ID);
    }

    public static String getSpaceId(VariableScope scope) {
        return getString(scope, com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID);
    }

    public static void setSpaceId(VariableScope scope, String spaceId) {
        scope.setVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID, spaceId);
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
        TypeReference<List<String>> type = new TypeReference<List<String>>() {
        };
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
        TypeReference<List<CloudServiceExtended>> type = new TypeReference<List<CloudServiceExtended>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_SERVICES_TO_POLL, type);
    }

    static void setTriggeredServiceOperations(VariableScope scope, Map<String, ServiceOperation.Type> triggeredServiceOperations) {
        setAsJsonBinary(scope, Constants.VAR_TRIGGERED_SERVICE_OPERATIONS, triggeredServiceOperations);
    }

    public static Map<String, ServiceOperation.Type> getTriggeredServiceOperations(VariableScope scope) {
        TypeReference<Map<String, ServiceOperation.Type>> type = new TypeReference<Map<String, ServiceOperation.Type>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_TRIGGERED_SERVICE_OPERATIONS, type);
    }

    public static Map<String, List<CloudServiceKey>> getServiceKeysToCreate(VariableScope scope) {
        TypeReference<Map<String, List<CloudServiceKey>>> type = new TypeReference<Map<String, List<CloudServiceKey>>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_SERVICE_KEYS_TO_CREATE, type);
    }

    static void setServiceKeysToCreate(VariableScope scope, Map<String, List<CloudServiceKey>> serviceKeys) {
        setAsJsonBinary(scope, Constants.VAR_SERVICE_KEYS_TO_CREATE, serviceKeys);
    }

    static List<CloudApplication> getDeployedApps(VariableScope scope) {
        TypeReference<List<CloudApplication>> type = new TypeReference<List<CloudApplication>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_DEPLOYED_APPS, type);
    }

    static void setDeployedApps(VariableScope scope, List<CloudApplication> apps) {
        setAsJsonBinary(scope, Constants.VAR_DEPLOYED_APPS, apps);
    }

    public static List<String> getAppsToDeploy(VariableScope scope) {
        TypeReference<List<String>> type = new TypeReference<List<String>>() {
        };
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
        TypeReference<Map<String, Map<String, String>>> type = new TypeReference<Map<String, Map<String, String>>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT, type);
    }

    public static List<CloudApplication> getUpdatedSubscribers(VariableScope scope) {
        TypeReference<List<CloudApplication>> type = new TypeReference<List<CloudApplication>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_UPDATED_SUBSCRIBERS, type);
    }

    static void setUpdatedSubscribers(VariableScope scope, List<CloudApplication> apps) {
        setAsJsonBinary(scope, Constants.VAR_UPDATED_SUBSCRIBERS, apps);
    }

    static CloudApplication getServiceBrokerSubscriberToRestart(VariableScope scope) {
        List<CloudApplication> apps = getServiceBrokerSubscribersToRestart(scope);
        int index = (Integer) scope.getVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX);
        return apps.get(index);
    }

    public static List<CloudApplication> getServiceBrokerSubscribersToRestart(VariableScope scope) {
        TypeReference<List<CloudApplication>> type = new TypeReference<List<CloudApplication>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS, type);
    }

    static void setUpdatedServiceBrokerSubscribers(VariableScope scope, List<CloudApplication> apps) {
        setAsJsonBinary(scope, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS, apps);
    }

    static List<CloudTask> getTasksToExecute(VariableScope scope) {
        TypeReference<List<CloudTask>> type = new TypeReference<List<CloudTask>>() {
        };
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
        return getFromJsonStrings(scope, Constants.VAR_APPS_TO_UNDEPLOY, CloudApplication.class);
    }

    static void setAppsToUndeploy(VariableScope scope, List<CloudApplication> apps) {
        setAsJsonStrings(scope, Constants.VAR_APPS_TO_UNDEPLOY, apps);
    }

    public static List<String> getServicesToDelete(VariableScope scope) {
        TypeReference<List<String>> type = new TypeReference<List<String>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_SERVICES_TO_DELETE, type);
    }

    public static void setServicesToDelete(VariableScope scope, List<String> services) {
        setAsJsonBinary(scope, Constants.VAR_SERVICES_TO_DELETE, services);
    }

    public static List<ConfigurationSubscription> getSubscriptionsToDelete(VariableScope scope) {
        TypeReference<List<ConfigurationSubscription>> type = new TypeReference<List<ConfigurationSubscription>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_SUBSCRIPTIONS_TO_DELETE, type);
    }

    static void setSubscriptionsToDelete(VariableScope scope, List<ConfigurationSubscription> subscriptions) {
        setAsJsonBinary(scope, Constants.VAR_SUBSCRIPTIONS_TO_DELETE, subscriptions);
    }

    public static List<ConfigurationSubscription> getSubscriptionsToCreate(VariableScope scope) {
        TypeReference<List<ConfigurationSubscription>> type = new TypeReference<List<ConfigurationSubscription>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_SUBSCRIPTIONS_TO_CREATE, type);
    }

    static void setSubscriptionsToCreate(VariableScope scope, List<ConfigurationSubscription> subscriptions) {
        setAsJsonBinary(scope, Constants.VAR_SUBSCRIPTIONS_TO_CREATE, subscriptions);
    }

    static void setConfigurationEntriesToPublish(VariableScope scope, List<ConfigurationEntry> configurationEntries) {
        setAsJsonBinary(scope, Constants.VAR_CONFIGURATION_ENTRIES_TO_PUBLISH, configurationEntries);
    }

    static List<ConfigurationEntry> getConfigurationEntriesToPublish(VariableScope scope) {
        TypeReference<List<ConfigurationEntry>> type = new TypeReference<List<ConfigurationEntry>>() {
        };
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
        TypeReference<List<ConfigurationEntry>> type = new TypeReference<List<ConfigurationEntry>>() {
        };
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
        List<ConfigurationEntry> configurationEntries = new ArrayList<>(StepsUtil.getDeletedEntriesFromProcess(flowableFacade,
                                                                                                               StepsUtil.getCorrelationId(scope)));
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
        TypeReference<List<ConfigurationEntry>> type = new TypeReference<List<ConfigurationEntry>>() {
        };
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

    public static void setVariableInParentProcess(DelegateExecution context, String variablePrefix, Object variableValue) {
        CloudApplicationExtended cloudApplication = StepsUtil.getApp(context);
        if (cloudApplication == null) {
            throw new IllegalStateException(Messages.CANNOT_DETERMINE_CURRENT_APPLICATION);
        }

        String moduleName = cloudApplication.getModuleName();
        if (moduleName == null) {
            throw new IllegalStateException(Messages.CANNOT_DETERMINE_MODULE_NAME);
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

    static void setDeployedMta(VariableScope scope, DeployedMta deployedMta) {
        setAsJsonBinary(scope, Constants.VAR_DEPLOYED_MTA, deployedMta);
    }

    protected static DeployedMta getDeployedMta(VariableScope scope) {
        return getFromJsonBinary(scope, Constants.VAR_DEPLOYED_MTA, DeployedMta.class);
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
        return getFromJsonString(scope, Constants.VAR_APP_TO_PROCESS, CloudApplicationExtended.class);
    }

    static void setApp(VariableScope scope, CloudApplicationExtended app) {
        setAsJsonString(scope, Constants.VAR_APP_TO_PROCESS, app);
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
        LogsOffset offset = getLogOffset(context);
        List<ApplicationLog> recentLogs = recentLogsRetriever.getRecentLogsSafely(client, app.getName(), offset);
        if (!recentLogs.isEmpty()) {
            recentLogs.forEach(log -> appLog(context, app.getName(), log.toString(), logger, processLoggerProvider));
            setLogOffset(recentLogs.get(recentLogs.size() - 1), context);
        }
    }

    static void appLog(DelegateExecution context, String appName, String message, Logger logger,
                       ProcessLoggerProvider processLoggerProvider) {
        getLogger(context, appName, processLoggerProvider).debug(getLoggerPrefix(logger) + "[" + appName + "] " + message);
    }

    static LogsOffset getLogOffset(DelegateExecution context) {
        return (LogsOffset) context.getVariable(com.sap.cloud.lm.sl.cf.core.Constants.LOGS_OFFSET);
    }

    static void setLogOffset(ApplicationLog lastLog, DelegateExecution context) {
        LogsOffset newOffset = ImmutableLogsOffset.builder()
                                                  .timestamp(lastLog.getTimestamp())
                                                  .message(lastLog.getMessage())
                                                  .build();
        context.setVariable(com.sap.cloud.lm.sl.cf.core.Constants.LOGS_OFFSET, newOffset);
    }

    public static StartingInfo getStartingInfo(VariableScope scope) {
        String className = getString(scope, Constants.VAR_STARTING_INFO_CLASSNAME);
        return getFromJsonBinary(scope, Constants.VAR_STARTING_INFO, getStartingInfoClass(className));
    }

    public static void setStartingInfo(VariableScope scope, StartingInfo startingInfo) {
        setAsJsonBinary(scope, Constants.VAR_STARTING_INFO, startingInfo);
        String className = startingInfo != null ? startingInfo.getClass()
                                                              .getName()
            : StartingInfo.class.getName();
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

    static void setMtaArchiveModules(VariableScope scope, Set<String> mtaArchiveModules) {
        setAsJsonBinary(scope, Constants.VAR_MTA_ARCHIVE_MODULES, mtaArchiveModules);
    }

    static Set<String> getMtaArchiveModules(VariableScope scope) {
        TypeReference<Set<String>> type = new TypeReference<Set<String>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_MTA_ARCHIVE_MODULES, type);
    }

    static void setMtaModules(VariableScope scope, Set<String> mtaModules) {
        setAsJsonBinary(scope, Constants.VAR_MTA_MODULES, mtaModules);
    }

    static Set<String> getMtaModules(VariableScope scope) {
        TypeReference<Set<String>> type = new TypeReference<Set<String>>() {
        };
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
        return getString(scope, com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SERVICE_ID);
    }

    public static void incrementVariable(VariableScope scope, String name) {
        int value = getInteger(scope, name);
        scope.setVariable(name, value + 1);
    }

    static ApplicationCloudModelBuilder getApplicationCloudModelBuilder(VariableScope scope, UserMessageLogger stepLogger) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(scope);

        String deployId = DEPLOY_ID_PREFIX + getCorrelationId(scope);

        DeploymentDescriptor deploymentDescriptor = StepsUtil.getCompleteDeploymentDescriptor(scope);

        DeployedMta deployedMta = StepsUtil.getDeployedMta(scope);

        return handlerFactory.getApplicationCloudModelBuilder(deploymentDescriptor, true, deployedMta, deployId, stepLogger);
    }

    static List<String> getDomainsFromApps(VariableScope scope, DeploymentDescriptor descriptor,
                                           ApplicationCloudModelBuilder applicationCloudModelBuilder, List<? extends Module> modules,
                                           ModuleToDeployHelper moduleToDeployHelper) {

        String defaultDomain = (String) descriptor.getParameters()
                                                  .get(SupportedParameters.DEFAULT_DOMAIN);

        Set<String> domains = new TreeSet<>();
        for (Module module : modules) {
            if (!moduleToDeployHelper.isApplication(module)) {
                continue;
            }
            ParametersChainBuilder parametersChainBuilder = new ParametersChainBuilder(StepsUtil.getCompleteDeploymentDescriptor(scope));
            List<String> appDomains = applicationCloudModelBuilder.getApplicationDomains(parametersChainBuilder.buildModuleChain(module.getName()),
                                                                                         module);
            if (appDomains != null) {
                domains.addAll(appDomains);
            }
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

    public static void setServicesData(VariableScope scope, List<CloudServiceExtended> servicesData) {
        scope.setVariable(Constants.VAR_SERVICES_DATA, JsonUtil.toJsonBinary(servicesData));
    }

    public static List<CloudServiceExtended> getServicesData(VariableScope scope) {
        TypeReference<List<CloudServiceExtended>> type = new TypeReference<List<CloudServiceExtended>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_SERVICES_DATA, type, Collections.emptyList());
    }

    public static CloudApplication getBoundApplication(List<CloudApplication> applications, UUID appGuid) {
        return applications.stream()
                           .filter(app -> hasGuid(app, appGuid))
                           .findFirst()
                           .orElse(null);
    }

    private static boolean hasGuid(CloudApplication app, UUID appGuid) {
        return app.getMetadata()
                  .getGuid()
                  .equals(appGuid);
    }

    public static boolean shouldDeleteServices(VariableScope scope) {
        return getBoolean(scope, Constants.PARAM_DELETE_SERVICES, false);
    }

    public static boolean shouldVerifyArchiveSignature(VariableScope scope) {
        return getBoolean(scope, Constants.PARAM_VERIFY_ARCHIVE_SIGNATURE);
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
        return getVariableWithCommaSeparator(scope, Constants.PARAM_MODULES_FOR_DEPLOYMENT);
    }

    public static List<String> getResourcesForDeployment(VariableScope scope) {
        return getVariableWithCommaSeparator(scope, Constants.PARAM_RESOURCES_FOR_DEPLOYMENT);
    }

    private static List<String> getVariableWithCommaSeparator(VariableScope scope, String variableName) {
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
        TypeReference<Map<String, List<String>>> type = new TypeReference<Map<String, List<String>>>() {
        };
        return getFromJsonBinary(scope, getExecutedHooksForModuleVariableName(moduleName), type, Collections.emptyMap());
    }

    private static String getExecutedHooksForModuleVariableName(String moduleName) {
        return Constants.VAR_EXECUTED_HOOKS_FOR_PREFIX + moduleName;
    }

    static void setHooksForExecution(VariableScope scope, List<Hook> hooksForExecution) {
        setAsJsonStrings(scope, Constants.VAR_HOOKS_FOR_EXECUTION, hooksForExecution);
    }

    static Hook getHookForExecution(VariableScope scope) {
        return getFromJsonString(scope, Constants.VAR_HOOK_FOR_EXECUTION, Hook.class);
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

    public static <T> T getFromJsonString(VariableScope scope, String name, Class<T> classOfT) {
        return getFromJsonString(scope, name, toTypeReference(classOfT));
    }

    public static <T> T getFromJsonString(VariableScope scope, String name, Class<T> classOfT, T defaultValue) {
        return getFromJsonString(scope, name, toTypeReference(classOfT), defaultValue);
    }

    public static <T> T getFromJsonString(VariableScope scope, String name, TypeReference<T> type) {
        return getFromJsonString(scope, name, type, null);
    }

    public static <T> T getFromJsonString(VariableScope scope, String name, TypeReference<T> type, T defaultValue) {
        String stringJson = getString(scope, name);
        if (stringJson == null) {
            return defaultValue;
        }
        return JsonUtil.fromJson(stringJson, type);
    }

    public static <T> T getFromJsonBinary(VariableScope scope, String name, Class<T> classOfT) {
        return getFromJsonBinary(scope, name, toTypeReference(classOfT));
    }

    public static <T> T getFromJsonBinary(VariableScope scope, String name, Class<T> classOfT, T defaultValue) {
        return getFromJsonBinary(scope, name, toTypeReference(classOfT), defaultValue);
    }

    public static <T> T getFromJsonBinary(VariableScope scope, String name, TypeReference<T> type) {
        return getFromJsonBinary(scope, name, type, null);
    }

    public static <T> T getFromJsonBinary(VariableScope scope, String name, TypeReference<T> type, T defaultValue) {
        byte[] jsonBinary = getObject(scope, name);
        if (jsonBinary == null) {
            return defaultValue;
        }
        String jsonString = new String(jsonBinary, StandardCharsets.UTF_8);
        return JsonUtil.fromJson(jsonString, type);
    }

    public static <T> List<T> getFromJsonStrings(VariableScope scope, String name, Class<T> classOfT) {
        return getFromJsonStrings(scope, name, toTypeReference(classOfT));
    }

    public static <T> List<T> getFromJsonStrings(VariableScope scope, String name, Class<T> classOfT, List<T> defaultValue) {
        return getFromJsonStrings(scope, name, toTypeReference(classOfT), defaultValue);
    }

    public static <T> List<T> getFromJsonStrings(VariableScope scope, String name, TypeReference<T> type) {
        return getFromJsonStrings(scope, name, type, Collections.emptyList());
    }

    public static <T> List<T> getFromJsonStrings(VariableScope scope, String name, TypeReference<T> type, List<T> defaultValue) {
        List<String> jsonStrings = getObject(scope, name);
        if (jsonStrings == null) {
            return defaultValue;
        }
        return jsonStrings.stream()
                          .map(jsonString -> JsonUtil.fromJson(jsonString, type))
                          .collect(Collectors.toList());
    }

    public static <T> List<T> getFromJsonBinaries(VariableScope scope, String name, Class<T> classOfT) {
        return getFromJsonBinaries(scope, name, toTypeReference(classOfT));
    }

    public static <T> List<T> getFromJsonBinaries(VariableScope scope, String name, Class<T> classOfT, List<T> defaultValue) {
        return getFromJsonBinaries(scope, name, toTypeReference(classOfT), defaultValue);
    }

    public static <T> List<T> getFromJsonBinaries(VariableScope scope, String name, TypeReference<T> type) {
        return getFromJsonBinaries(scope, name, type, Collections.emptyList());
    }

    public static <T> List<T> getFromJsonBinaries(VariableScope scope, String name, TypeReference<T> type, List<T> defaultValue) {
        List<byte[]> jsonBinaries = getObject(scope, name);
        if (jsonBinaries == null) {
            return defaultValue;
        }
        return jsonBinaries.stream()
                           .map(jsonBinary -> JsonUtil.fromJsonBinary(jsonBinary, type))
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

    public static void setAsJsonStrings(VariableScope scope, String name, List<?> values) {
        if (values == null) {
            scope.setVariable(name, null);
            return;
        }
        List<String> jsonStrings = values.stream()
                                         .map(JsonUtil::toJson)
                                         .collect(Collectors.toList());
        scope.setVariable(name, jsonStrings);
    }

    public static void setAsJsonBinaries(VariableScope scope, String name, List<?> values) {
        if (values == null) {
            scope.setVariable(name, null);
            return;
        }
        List<byte[]> jsonBinaries = values.stream()
                                          .map(JsonUtil::toJsonBinary)
                                          .collect(Collectors.toList());
        scope.setVariable(name, jsonBinaries);
    }

    private static <T> TypeReference<T> toTypeReference(Class<T> classOfT) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return classOfT;
            }
        };
    }

    static String getServiceOffering(VariableScope scope) {
        return (String) scope.getVariable(Constants.VAR_SERVICE_OFFERING);
    }

    static void setServiceOffering(VariableScope scope, String variableName, String value) {
        scope.setVariable(variableName, value);
    }

}
