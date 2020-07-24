package org.cloudfoundry.multiapps.controller.process.util;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableProgressMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.flowable.engine.delegate.DelegateExecution;

@Named("hookProcessGetter")
public class HookProcessGetter {

    private final ProgressMessageService progressMessageService;
    private final FlowableFacade flowableFacade;

    @Inject
    public HookProcessGetter(ProgressMessageService progressMessageService, FlowableFacade flowableFacade) {
        this.progressMessageService = progressMessageService;
        this.flowableFacade = flowableFacade;
    }

    public String get(String hookForExecutionString, DelegateExecution execution) {
        Hook hookForExecution = JsonUtil.fromJson(hookForExecutionString, Hook.class);
        if (TaskHookParser.HOOK_TYPE_TASK.equals(hookForExecution.getType())) {
            return Constants.EXECUTE_HOOK_TASKS_SUB_PROCESS_ID;
        }

        String errorMessage = MessageFormat.format("Unsupported hook type \"{0}\"", hookForExecution.getType());
        preserveErrorMessage(execution, errorMessage);

        throw new IllegalStateException(errorMessage);
    }

    private void preserveErrorMessage(DelegateExecution execution, String errorMessage) {
        progressMessageService.add(ImmutableProgressMessage.builder()
                                                           .processId(flowableFacade.getProcessInstanceId(execution.getId()))
                                                           .taskId(execution.getCurrentActivityId())
                                                           .type(ProgressMessageType.ERROR)
                                                           .text(errorMessage)
                                                           .timestamp(getCurrentTime())
                                                           .build());
    }

    protected Date getCurrentTime() {
        return new Timestamp(System.currentTimeMillis());
    }
}
