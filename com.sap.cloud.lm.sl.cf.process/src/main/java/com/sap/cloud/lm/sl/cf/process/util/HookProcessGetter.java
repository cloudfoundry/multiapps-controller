package com.sap.cloud.lm.sl.cf.process.util;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Date;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.ProgressMessageDao;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Hook;

@Component("hookProcessGetter")
public class HookProcessGetter {

    private ProgressMessageDao progressMessageDao;
    private FlowableFacade flowableFacade;

    @Inject
    public HookProcessGetter(ProgressMessageDao progressMessageDao, FlowableFacade flowableFacade) {
        this.progressMessageDao = progressMessageDao;
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
        progressMessageDao.add(ImmutableProgressMessage.builder()
            .processId(flowableFacade.getProcessInstanceId(context.getId()))
            .taskId(context.getCurrentActivityId())
            .type(ProgressMessageType.ERROR)
            .text(errorMessage)
            .timestamp(getCurrentTime())
            .build());
    }

    protected Date getCurrentTime() {
        return new Timestamp(System.currentTimeMillis());
    }
}
