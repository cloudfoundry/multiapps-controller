package com.sap.cloud.lm.sl.cf.process.flowable;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import org.flowable.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ResumeProcessAction extends ProcessAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResumeProcessAction.class);

    public static final String ACTION_ID_RESUME = "resume";

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

        updateUserIfNecessary(userId, superProcessInstanceId);
        for (String processAtReceiveTask : processesAtReceiveTask) {
            triggerProcessInstance(userId, processAtReceiveTask);
        }
    }

    private void triggerProcessInstance(String userId, String processId) {
        List<Execution> executionsAtReceiveTask = flowableFacade.findExecutionsAtReceiveTask(processId);
        if (executionsAtReceiveTask.isEmpty()) {
            LOGGER.warn(MessageFormat.format("Process with id {0} is in undetermined process state", processId));
            return;
        }
        for (Execution execution : executionsAtReceiveTask) {
            flowableFacade.trigger(userId, execution.getId(), MapUtil.asMap(Constants.VAR_USER, userId));
        }
    }

    @Override
    public String getActionId() {
        return ACTION_ID_RESUME;
    }

}
