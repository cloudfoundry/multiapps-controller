package com.sap.cloud.lm.sl.cf.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.CloudTask.Limits;
import org.cloudfoundry.client.lib.domain.ImmutableCloudTask;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.TasksValidator;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class TaskParametersParser implements ParametersParser<List<CloudTask>> {

    private String parameterName;
    private CloudTaskMapper cloudTaskMapper = new CloudTaskMapper();

    public TaskParametersParser(String parameterName) {
        this.parameterName = parameterName;
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

        public CloudTask toCloudTask(Map<String, Object> rawTask) {
            return ImmutableCloudTask.builder()
                                     .name(getProperty(rawTask, TasksValidator.TASK_NAME_KEY))
                                     .command(getProperty(rawTask, TasksValidator.TASK_COMMAND_KEY))
                                     .limits(parseLimits(rawTask))
                                     .build();
        }

        private Limits parseLimits(Map<String, Object> rawTask) {
            return ImmutableCloudTask.ImmutableLimits.builder()
                                                     .memory(parseMemory(rawTask))
                                                     .disk(parseDiskQuota(rawTask))
                                                     .build();
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
