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
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.DeploymentMode;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
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

    private static String getModuleContentVariable(String moduleName) {
        return Constants.VAR_MTA_MODULE_CONTENT_PREFIX + moduleName;
    }

    public static HandlerFactory getHandlerFactory(VariableScope scope) {
        int majorSchemaVersion = VariableHandling.get(scope, Variables.MTA_MAJOR_SCHEMA_VERSION);
        return new HandlerFactory(majorSchemaVersion);
    }

    public static void setDeploymentMode(VariableScope scope, DeploymentMode deploymentMode) {
        scope.setVariable(Constants.VAR_DEPLOYMENT_MODE, deploymentMode);
    }

    static CloudApplication getUpdatedServiceBrokerSubscriber(ProcessContext context) {
        List<CloudApplication> apps = context.getVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS);
        int index = context.getVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX);
        return apps.get(index);
    }

    public static CloudServiceBroker getServiceBrokersToCreateForModule(VariableScope scope, String moduleName) {
        return getFromJsonBinary(scope, Constants.VAR_APP_SERVICE_BROKER_VAR_PREFIX + moduleName, CloudServiceBroker.class);
    }

    public static List<String> getCreatedOrUpdatedServiceBrokerNames(ProcessContext context) {
        List<Module> allModulesToDeploy = context.getVariable(Variables.ALL_MODULES_TO_DEPLOY);
        return allModulesToDeploy.stream()
                                 .map(module -> getServiceBrokersToCreateForModule(context.getExecution(), module.getName()))
                                 .filter(Objects::nonNull)
                                 .map(CloudServiceBroker::getName)
                                 .collect(Collectors.toList());
    }

    static List<ConfigurationEntry> getDeletedEntriesFromProcess(FlowableFacade flowableFacade, String processInstanceId) {
        HistoricVariableInstance deletedEntries = flowableFacade.getHistoricVariableInstance(processInstanceId,
                                                                                             Variables.DELETED_ENTRIES.getName());
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
                                                                                               Variables.PUBLISHED_ENTRIES.getName());
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

    public static Boolean getBoolean(VariableScope scope, String name, Boolean defaultValue) {
        return getObject(scope, name, defaultValue);
    }

    public static Integer getInteger(VariableScope scope, String name) {
        return getInteger(scope, name, null);
    }

    public static Integer getInteger(VariableScope scope, String name, Integer defaultValue) {
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

    public static void setAsJsonBinary(VariableScope scope, String name, Object value) {
        if (value == null) {
            scope.setVariable(name, null);
            return;
        }
        byte[] jsonBinary = JsonUtil.toJsonBinary(value);
        scope.setVariable(name, jsonBinary);
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
