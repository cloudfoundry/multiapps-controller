package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Map;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.parser.TaskParametersParser;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.mta.model.Hook;

import com.sap.cloudfoundry.client.facade.domain.CloudTask;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudTask;

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
