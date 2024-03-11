package com.sap.cloud.lm.sl.cf.core.flowable;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ResumeProcessAction extends ProcessAction {

    public static final String ACTION_ID_RESUME = "resume";
    private static final Logger LOGGER = LoggerFactory.getLogger(ResumeProcessAction.class);

    @Inject
    public ResumeProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions) {
        super(flowableFacade, additionalProcessActions);
    }

    @Override
    public void executeActualProcessAction(String userId, String superProcessInstanceId) {
        List<String> activeProcessIds = getActiveExecutionIds(superProcessInstanceId);
        List<String> processesAtReceiveTask = activeProcessIds.stream()
                                                              .filter(processId -> !flowableFacade.findExecutionsAtReceiveTask(processId)
                                                                                                  .isEmpty())
                                                              .collect(Collectors.toList());

        for (String processAtReceiveTask : processesAtReceiveTask) {
            triggerProcessInstance(userId, processAtReceiveTask);
        }
    }

    private void triggerProcessInstance(String userId, String processId) {
        List<Execution> executionsAtReceiveTask = flowableFacade.findExecutionsAtReceiveTask(processId);
        if (!executionsAtReceiveTask.isEmpty()) {
            executionsAtReceiveTask.stream()
                                   .forEach(execution -> flowableFacade.trigger(userId, execution.getId()));
            return;
        }
        LOGGER.warn(MessageFormat.format("Process with id {0} is in undetermined process state", processId));
    }

    @Override
    public String getActionId() {
        return ACTION_ID_RESUME;
    }

}
