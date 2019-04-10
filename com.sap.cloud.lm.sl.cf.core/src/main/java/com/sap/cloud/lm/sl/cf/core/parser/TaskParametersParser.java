package com.sap.cloud.lm.sl.cf.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudTask;

import com.sap.cloud.lm.sl.cf.core.helpers.MapToEnvironmentConverter;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.TasksValidator;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class TaskParametersParser implements ParametersParser<List<CloudTask>> {

    private String parameterName;
    private CloudTaskMapper cloudTaskMapper;

    public TaskParametersParser(String parameterName, boolean prettyPrinting) {
        this.parameterName = parameterName;
        this.cloudTaskMapper = new CloudTaskMapper(prettyPrinting);
    }

    @Override
    public List<CloudTask> parse(List<Map<String, Object>> parametersList) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) PropertiesUtil.getPropertyValue(parametersList, parameterName,
            Collections.emptyList());

        return tasks.stream()
            .map(cloudTaskMapper::toCloudTask)
            .collect(Collectors.toList());
    }

    public static class CloudTaskMapper {

        private boolean prettyPrinting;

        public CloudTaskMapper() {
            this(false);
        }

        public CloudTaskMapper(boolean prettyPrinting) {
            this.prettyPrinting = prettyPrinting;
        }

        public CloudTask toCloudTask(Map<String, Object> rawTask) {
            CloudTask task = new CloudTask(null, getProperty(rawTask, TasksValidator.TASK_NAME_KEY));
            task.setCommand(getProperty(rawTask, TasksValidator.TASK_COMMAND_KEY));
            task.setEnvironmentVariables(getEnvironmentVariables(rawTask));
            task.setMemory(parseMemory(rawTask));
            task.setDiskQuota(parseDiskQuota(rawTask));
            return task;
        }

        private Map<String, String> getEnvironmentVariables(Map<String, Object> rawTask) {
            Map<String, Object> env = getProperty(rawTask, TasksValidator.TASK_ENV_KEY);
            return env == null ? null : new MapToEnvironmentConverter(prettyPrinting).asEnv(env);
        }

        private Integer parseMemory(Map<String, Object> rawTask) {
            return MemoryParametersParser.parseMemory(getProperty(rawTask, TasksValidator.TASK_MEMORY_KEY));
        }

        private Integer parseDiskQuota(Map<String, Object> rawTask) {
            return MemoryParametersParser.parseMemory(getProperty(rawTask, TasksValidator.TASK_DISK_QUOTA_KEY));
        }

        @SuppressWarnings("unchecked")
        private <T> T getProperty(Map<String, Object> rawTask, String key) {
            return (T) rawTask.get(key);
        }

    }

}
