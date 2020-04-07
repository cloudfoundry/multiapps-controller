package com.sap.cloud.lm.sl.cf.process.flowable;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.MapUtil;

@Named
public class ResumeProcessAction extends ProcessAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResumeProcessAction.class);

    public static final String ACTION_ID_RESUME = "resume";

    @Inject
    public ResumeProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions) {
        super(flowableFacade, additionalProcessActions);
    }

    @Override
    public void executeActualProcessAction(String user, String superProcessInstanceId) {
        List<String> activeProcessIds = getActiveExecutionIds(superProcessInstanceId);
        List<String> processesAtReceiveTask = activeProcessIds.stream()
                                                              .filter(processId -> !flowableFacade.findExecutionsAtReceiveTask(processId)
                                                                                                  .isEmpty())
                                                              .collect(Collectors.toList());

        updateUserIfNecessary(user, superProcessInstanceId);
        for (String processAtReceiveTask : processesAtReceiveTask) {
            triggerProcessInstance(user, processAtReceiveTask);
        }
    }

    private void triggerProcessInstance(String user, String processId) {
        List<Execution> executionsAtReceiveTask = flowableFacade.findExecutionsAtReceiveTask(processId);
        if (executionsAtReceiveTask.isEmpty()) {
            LOGGER.warn(MessageFormat.format("Process with id {0} is in undetermined process state", processId));
            return;
        }
        for (Execution execution : executionsAtReceiveTask) {
            flowableFacade.trigger(execution.getId(), MapUtil.asMap(Variables.USER.getName(), user));
        }
    }

    @Override
    public String getActionId() {
        return ACTION_ID_RESUME;
    }

}
