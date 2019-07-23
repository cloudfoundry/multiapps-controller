package com.sap.cloud.lm.sl.cf.process.util;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Date;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Hook;

@Component("hookProcessGetter")
public class HookProcessGetter {

    private ProgressMessageService progressMessageService;
    private FlowableFacade flowableFacade;

    @Inject
    public HookProcessGetter(ProgressMessageService progressMessageService, FlowableFacade flowableFacade) {
        this.progressMessageService = progressMessageService;
        this.flowableFacade = flowableFacade;
    }

    public String get(String hookForExecutionString, DelegateExecution context) {
        Hook hookForExecution = JsonUtil.fromJson(hookForExecutionString, Hook.class);
        if (TaskHookParser.HOOK_TYPE_TASK.equals(hookForExecution.getType())) {
            return Constants.EXECUTE_HOOK_TASKS_SUB_PROCESS_ID;
        }

        String errorMessage = MessageFormat.format("Unsupported hook type \"{0}\"", hookForExecution.getType());
        preserveErrorMessage(context, errorMessage);

        throw new IllegalStateException(errorMessage);
    }

    private void preserveErrorMessage(DelegateExecution context, String errorMessage) {
        progressMessageService.add(new ProgressMessage(flowableFacade.getProcessInstanceId(context.getId()), context.getCurrentActivityId(),
            ProgressMessageType.ERROR, errorMessage, getCurrentTime()));
    }

    protected Date getCurrentTime() {
        return new Timestamp(System.currentTimeMillis());
    }
}
