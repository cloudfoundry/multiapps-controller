package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.ImmutableCloudTask;

import com.sap.cloud.lm.sl.cf.core.parser.TaskParametersParser;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.Hook;

public class TaskHookParser {

    public static final String HOOK_TYPE_TASK = "task";

    public CloudTask parse(Hook hook) {
        Map<String, Object> hookParameters = hook.getParameters();
        if (hookParameters.isEmpty()) {
            throw new ContentException(Messages.PARAMETERS_OF_TASK_HOOK_0_ARE_INCOMPLETE, hook.getName());
        }
        return getHookTask(hook);
    }

    private CloudTask getHookTask(Hook hook) {
        CloudTask task = new TaskParametersParser.CloudTaskMapper().toCloudTask(hook.getParameters());
        if (task.getName() == null) {
            return ImmutableCloudTask.copyOf(task)
                                     .withName(hook.getName());
        }
        return task;
    }

}
