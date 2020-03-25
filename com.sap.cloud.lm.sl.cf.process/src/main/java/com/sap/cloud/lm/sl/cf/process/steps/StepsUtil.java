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
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.DeploymentMode;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.core.util.ImmutableLogsOffset;
import com.sap.cloud.lm.sl.cf.core.util.LogsOffset;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.analytics.model.ServiceAction;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.variables.VariableHandling;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.YamlUtil;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;

public class StepsUtil {

    public static final String DEPLOY_ID_PREFIX = "deploy-";

    protected StepsUtil() {
    }

    public static org.apache.log4j.Logger getLogger(DelegateExecution execution, String name, ProcessLoggerProvider processLoggerProvider) {
        return processLoggerProvider.getLogger(execution, name);
    }

    static CloudControllerClient getControllerClient(DelegateExecution execution, CloudControllerClientProvider clientProvider) {
        String userName = determineCurrentUser(execution);
        String spaceId = VariableHandling.get(execution, Variables.SPACE_ID);
        return clientProvider.getControllerClient(userName, spaceId);
    }

    static CloudControllerClient getControllerClient(DelegateExecution execution, CloudControllerClientProvider clientProvider, String org,
                                                     String space) {
        String userName = determineCurrentUser(execution);
        return clientProvider.getControllerClient(userName, org, space, execution.getProcessInstanceId());
    }

    public static String determineCurrentUser(VariableScope scope) {
        String user = VariableHandling.get(scope, Variables.USER);
        if (user == null) {
            throw new SLException(Messages.CANT_DETERMINE_CURRENT_USER);
        }
        return user;
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
        return getEnum(scope, Constants.VAR_LIVE_MTA_COLOR, ApplicationColor::valueOf);
    }

    public static void setLiveMtaColor(VariableScope scope, ApplicationColor liveMtaColor) {
        setEnum(scope, Constants.VAR_LIVE_MTA_COLOR, liveMtaColor);
    }

    public static ApplicationColor getIdleMtaColor(VariableScope scope) {
        return getEnum(scope, Constants.VAR_IDLE_MTA_COLOR, ApplicationColor::valueOf);
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
        int majorSchemaVersion = VariableHandling.get(scope, Variables.MTA_MAJOR_SCHEMA_VERSION);
        return new HandlerFactory(majorSchemaVersion);
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

    static CloudApplication getUpdatedServiceBrokerSubscriber(ProcessContext context) {
        List<CloudApplication> apps = context.getVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS);
        int index = context.getVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX);
        return apps.get(index);
    }

    public static List<CloudApplication> getAppsToUndeploy(VariableScope scope) {
        return getFromJsonStrings(scope, Constants.VAR_APPS_TO_UNDEPLOY, CloudApplication.class);
    }

    static void setAppsToUndeploy(VariableScope scope, List<CloudApplication> apps) {
        setAsJsonStrings(scope, Constants.VAR_APPS_TO_UNDEPLOY, apps);
    }

    /**
     * 
     * @deprecated This method should be used for backward compatibility for one release. After that it should be used only new mechanism
     *             with {@link Variables}
     */
    @Deprecated
    public static List<String> getServicesToDelete(VariableScope scope) {
        Object servicesToDelete = getObject(scope, Constants.VAR_SERVICES_TO_DELETE);
        if (servicesToDelete instanceof List) {
            return (List<String>) servicesToDelete;
        }
        TypeReference<List<String>> type = new TypeReference<List<String>>() {
        };
        return getFromJsonBinary(scope, Constants.VAR_SERVICES_TO_DELETE, type, Collections.emptyList());
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

    static List<ConfigurationEntry> getDeletedEntriesFromProcess(FlowableFacade flowableFacade, String processInstanceId) {
        HistoricVariableInstance deletedEntries = flowableFacade.getHistoricVariableInstance(processInstanceId,
                                                                                             Constants.VAR_DELETED_ENTRIES);
        if (deletedEntries == null) {
            return Collections.emptyList();
        }
        byte[] deletedEntriesByteArray = (byte[]) deletedEntries.getValue();
        return Arrays.asList(JsonUtil.fromJsonBinary(deletedEntriesByteArray, ConfigurationEntry[].class));
    }

    static List<ConfigurationEntry> getDeletedEntriesFromAllProcesses(ProcessContext context, FlowableFacade flowableFacade) {
        String correlationId = context.getVariable(Variables.CORRELATION_ID);
        List<ConfigurationEntry> configurationEntries = new ArrayList<>(StepsUtil.getDeletedEntriesFromProcess(flowableFacade,
                                                                                                               correlationId));
        List<String> subProcessIds = flowableFacade.getHistoricSubProcessIds(correlationId);
        for (String subProcessId : subProcessIds) {
            configurationEntries.addAll(getDeletedEntriesFromProcess(flowableFacade, subProcessId));
        }
        return configurationEntries;
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

    static List<ConfigurationEntry> getPublishedEntriesFromSubProcesses(ProcessContext context, FlowableFacade flowableFacade) {
        List<ConfigurationEntry> result = new ArrayList<>();
        List<String> subProcessIds = flowableFacade.getHistoricSubProcessIds(context.getVariable(Variables.CORRELATION_ID));
        for (String subProcessId : subProcessIds) {
            result.addAll(getPublishedEntriesFromProcess(flowableFacade, subProcessId));
        }
        return result;
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

    static CloudTask getTask(ProcessContext context) {
        List<CloudTask> tasks = context.getVariable(Variables.TASKS_TO_EXECUTE);
        int index = context.getVariable(Variables.TASKS_INDEX);
        return tasks.get(index);
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

    static void saveAppLogs(DelegateExecution execution, CloudControllerClient client, RecentLogsRetriever recentLogsRetriever,
                            CloudApplication app, Logger logger, ProcessLoggerProvider processLoggerProvider) {
        LogsOffset offset = getLogOffset(execution);
        List<ApplicationLog> recentLogs = recentLogsRetriever.getRecentLogsSafely(client, app.getName(), offset);
        if (!recentLogs.isEmpty()) {
            recentLogs.forEach(log -> appLog(execution, app.getName(), log.toString(), logger, processLoggerProvider));
            setLogOffset(recentLogs.get(recentLogs.size() - 1), execution);
        }
    }

    static void appLog(DelegateExecution execution, String appName, String message, Logger logger,
                       ProcessLoggerProvider processLoggerProvider) {
        getLogger(execution, appName, processLoggerProvider).debug(getLoggerPrefix(logger) + "[" + appName + "] " + message);
    }

    static LogsOffset getLogOffset(DelegateExecution execution) {
        return (LogsOffset) execution.getVariable(com.sap.cloud.lm.sl.cf.core.Constants.LOGS_OFFSET);
    }

    static void setLogOffset(ApplicationLog lastLog, DelegateExecution execution) {
        LogsOffset newOffset = ImmutableLogsOffset.builder()
                                                  .timestamp(lastLog.getTimestamp())
                                                  .message(lastLog.getMessage())
                                                  .build();
        execution.setVariable(com.sap.cloud.lm.sl.cf.core.Constants.LOGS_OFFSET, newOffset);
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

    public static void incrementVariable(VariableScope scope, String name) {
        int value = getInteger(scope, name);
        scope.setVariable(name, value + 1);
    }

    static ApplicationCloudModelBuilder getApplicationCloudModelBuilder(ProcessContext context) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());

        String deployId = DEPLOY_ID_PREFIX + context.getVariable(Variables.CORRELATION_ID);

        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);

        DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);

        return handlerFactory.getApplicationCloudModelBuilder(deploymentDescriptor, true, deployedMta, deployId, context.getStepLogger());
    }

    static String getGitRepoRef(ProcessContext context) {
        Map<String, String> gitRepoConfigMap = context.getVariable(Variables.GIT_REPOSITORY_CONFIG_MAP);
        if (gitRepoConfigMap != null) {
            return gitRepoConfigMap.get(Variables.GIT_REF.getName());
        }
        return context.getVariable(Variables.GIT_REF);
    }

    static String getGitRepoUri(ProcessContext context) {
        Map<String, String> gitRepoConfigMap = context.getVariable(Variables.GIT_REPOSITORY_CONFIG_MAP);
        if (gitRepoConfigMap != null) {
            return gitRepoConfigMap.get(Variables.GIT_URI.getName());
        }
        return context.getVariable(Variables.GIT_URI);
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

    public static void setServiceActionsToExecute(List<ServiceAction> actions, VariableScope scope) {
        List<String> actionsStrings = actions.stream()
                                             .map(ServiceAction::toString)
                                             .collect(Collectors.toList());
        scope.setVariable(Constants.VAR_SERVICE_ACTIONS_TO_EXCECUTE, actionsStrings);
    }

    @SuppressWarnings("unchecked")
    public static List<ServiceAction> getServiceActionsToExecute(VariableScope execution) {
        List<String> actionStrings = ListUtils.emptyIfNull((List<String>) execution.getVariable(Constants.VAR_SERVICE_ACTIONS_TO_EXCECUTE));
        return actionStrings.stream()
                            .map(ServiceAction::valueOf)
                            .collect(Collectors.toList());
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

    public static <E> E getEnum(VariableScope scope, String name, Function<String, E> factory) {
        String value = getObject(scope, name);
        return value == null ? null : factory.apply(value);
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

}
