package com.sap.cloud.lm.sl.cf.process.steps;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("sendMessageCompleteEventStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SendCompleteMessageEventStep extends SyncFlowableStep {

    @Inject
    private FlowableFacade flowableFacade;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        String onCompleteMessageEvent = (String) execution.getContext()
            .getVariable(Constants.VAR_ON_COMPLETE_MESSAGE_EVENT_NAME);

        String parentExecutionId = (String) execution.getContext()
            .getVariable(Constants.VAR_PARENT_EXECUTION_ID);

        if (onCompleteMessageEvent == null || parentExecutionId == null) {
            return StepPhase.DONE;
        }

        flowableFacade.messageEventReceived(onCompleteMessageEvent, parentExecutionId);

        return StepPhase.DONE;
    }

}
