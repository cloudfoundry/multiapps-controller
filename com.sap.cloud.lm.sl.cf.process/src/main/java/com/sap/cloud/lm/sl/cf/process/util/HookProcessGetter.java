package com.sap.cloud.lm.sl.cf.process.util;

import java.text.MessageFormat;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Hook;

@Component("hookProcessGetter")
public class HookProcessGetter {

    public String get(String hookForExecutionString) {
        Hook hookForExecution = JsonUtil.fromJson(hookForExecutionString, Hook.class);
        if (TasksHookParser.HOOK_TYPE_TASKS.equals(hookForExecution.getType())) {
            return Constants.EXECUTE_HOOK_TASKS_SUB_PROCESS_ID;
        }

        throw new IllegalStateException(MessageFormat.format("Unsupported hook type \"{0}\"", hookForExecution.getType()));
    }
}
