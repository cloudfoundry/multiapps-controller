package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudTask;

import com.sap.cloud.lm.sl.cf.core.parser.TaskParametersParser;
import com.sap.cloud.lm.sl.mta.model.Hook;

public class TasksHookParser {

    public static final String HOOK_TYPE_TASKS = "tasks";

    public List<CloudTask> parse(Hook hook) {
        Map<String, Object> hookParameters = hook.getParameters();
        if (hookParameters.isEmpty()) {
            throw new IllegalStateException("Hook task parameters must not be empty");
        }
        return Arrays.asList(getHookTask(hookParameters));
    }

    private CloudTask getHookTask(Map<String, Object> hookParameters) {
        return new TaskParametersParser.CloudTaskMapper().toCloudTask(hookParameters);
    }

}
