package com.sap.cloud.lm.sl.cf.process.flowable;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.flowable.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.common.SLException;

@Component
public class RetryProcessAdditionalAction implements AdditionalProcessAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryProcessAdditionalAction.class);

    private FlowableFacade flowableFacade;
    private ProgressMessageService progressMessageService;

    @Inject
    public RetryProcessAdditionalAction(FlowableFacade flowableFacade, ProgressMessageService progressMessageService) {
        this.progressMessageService = progressMessageService;
        this.flowableFacade = flowableFacade;
    }

    @Override
    public void executeAdditionalProcessAction(String processInstanceId) {
        List<String> failedActivityIds = findFailedActivityIds(processInstanceId);
        for (String failedActivityId : failedActivityIds) {
            try {
                progressMessageService.removeByProcessInstanceIdAndTaskIdAndType(processInstanceId, failedActivityId,
                    ProgressMessageType.ERROR);
            } catch (SLException e) {
                LOGGER.error(Messages.ERROR_DELETING_PROGRESS_MESSAGE, e);
            }
        }
    }

    private List<String> findFailedActivityIds(String superProcessInstanceId) {
        List<Execution> executionsForProcess = flowableFacade.getActiveProcessExecutions(superProcessInstanceId);

        return executionsForProcess.stream()
            .map(e -> e.getActivityId())
            .collect(Collectors.toList());
    }

    @Override
    public String getApplicableActionId() {
        return RetryProcessAction.ACTION_ID_RETRY;
    }

}
