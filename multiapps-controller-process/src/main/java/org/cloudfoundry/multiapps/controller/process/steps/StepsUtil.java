package org.cloudfoundry.multiapps.controller.process.steps;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.cf.detect.AppSuffixDeterminer;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.Phase;
import org.cloudfoundry.multiapps.controller.core.util.LogsOffset;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogger;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;
import com.sap.cloudfoundry.client.facade.domain.CloudTask;

public class StepsUtil {

    public static final String DEPLOY_ID_PREFIX = "deploy-";

    protected StepsUtil() {
    }

    public static String determineCurrentUser(VariableScope scope) {
        String user = VariableHandling.get(scope, Variables.USER);
        if (user == null) {
            throw new SLException(Messages.CANT_DETERMINE_CURRENT_USER);
        }
        return user;
    }

    public static CloudHandlerFactory getHandlerFactory(VariableScope scope) {
        int majorSchemaVersion = VariableHandling.get(scope, Variables.MTA_MAJOR_SCHEMA_VERSION);
        return CloudHandlerFactory.forSchemaVersion(majorSchemaVersion);
    }

    public static String getQualifiedMtaId(String mtaId, String namespace) {
        if (StringUtils.isNotEmpty(namespace)) {
            return namespace + org.cloudfoundry.multiapps.controller.core.Constants.NAMESPACE_SEPARATOR + mtaId;
        }
        return mtaId;
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

    public static boolean isServiceOptional(List<CloudServiceInstanceExtended> servicesCloudModel, String serviceName) {
        return servicesCloudModel.stream()
                                 .filter(service -> service.getName()
                                                           .equals(serviceName))
                                 .map(CloudServiceInstanceExtended::isOptional)
                                 .findAny()
                                 .orElse(false);
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

    static CloudTask getTask(ProcessContext context) {
        List<CloudTask> tasks = context.getVariable(Variables.TASKS_TO_EXECUTE);
        int index = context.getVariable(Variables.TASKS_INDEX);
        return tasks.get(index);
    }

    static void saveAppLogs(ProcessContext context, CloudControllerClient client, String appName, Logger logger,
                            ProcessLoggerProvider processLoggerProvider) {
        LocalDateTime offset = getLogOffsetAdapter(context);
        var recentLogs = getRecentLogsSafely(client, appName, offset, logger);
        if (recentLogs.isEmpty()) {
            return;
        }
        if (context.getVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY)) {
            appName = BlueGreenApplicationNameSuffix.removeSuffix(appName);
        }
        ProcessLogger processLogger = processLoggerProvider.getLogger(context.getExecution(), appName);
        var loggerPrefix = getLoggerPrefix(logger);
        for (ApplicationLog log : recentLogs) {
            processLogger.debug(loggerPrefix + "[" + appName + "] " + log.toString());
        }

        var lastLog = recentLogs.get(recentLogs.size() - 1);
        context.setVariable(Variables.LOGS_OFFSET, lastLog.getTimestamp());
    }

    //TODO remove this after next takt and use
    // context.getVariable(Variables.LOGS_OFFSET) in its place
    private static LocalDateTime getLogOffsetAdapter(ProcessContext context) {
        Object value = context.getExecution()
                              .getVariable(org.cloudfoundry.multiapps.controller.core.Constants.LOGS_OFFSET);
        if (value instanceof LogsOffset) {
            return LocalDateTime.ofInstant(((LogsOffset) value).getTimestamp()
                                                               .toInstant(), ZoneId.of("UTC"));
        }
        return context.getVariable(Variables.LOGS_OFFSET);
    }

    private static List<ApplicationLog> getRecentLogsSafely(CloudControllerClient client, String appName,
                                                            LocalDateTime offset, Logger logger) {
        try {
            return client.getRecentLogs(appName, offset);
        } catch (RuntimeException e) {
            logger.error(MessageFormat.format(Messages.COULD_NOT_GET_APP_LOGS, e.getMessage()), e);
            return Collections.emptyList();
        }
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
        CloudHandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());

        String deployId = DEPLOY_ID_PREFIX + context.getVariable(Variables.CORRELATION_ID);
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);

        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);

        DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);

        return handlerFactory.getApplicationCloudModelBuilder(deploymentDescriptor, true, deployedMta, deployId, namespace,
                                                              context.getStepLogger(), getAppSuffixDeterminer(context),
                                                              context.getControllerClient());
    }

    static AppSuffixDeterminer getAppSuffixDeterminer(ProcessContext context) {
        boolean keepOriginalNamesAfterDeploy = context.getVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY);
        boolean isAfterResumePhase = context.getVariable(Variables.PHASE) == Phase.AFTER_RESUME;
        return new AppSuffixDeterminer(keepOriginalNamesAfterDeploy, isAfterResumePhase);
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

    public static void setExecutedHooksForModule(VariableScope scope, String moduleName, Map<String, List<String>> moduleHooks) {
        setAsJsonBinary(scope, getExecutedHooksForModuleVariableName(moduleName), moduleHooks);
    }

    public static Map<String, List<String>> getExecutedHooksForModule(VariableScope scope, String moduleName) {
        TypeReference<Map<String, List<String>>> type = new TypeReference<>() {
        };
        return getFromJsonBinary(scope, getExecutedHooksForModuleVariableName(moduleName), type, Collections.emptyMap());
    }

    private static String getExecutedHooksForModuleVariableName(String moduleName) {
        return Constants.VAR_EXECUTED_HOOKS_FOR_PREFIX + moduleName;
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
        return getFromJsonBinary(scope, name, Variable.typeReference(classOfT));
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

}
