package org.cloudfoundry.multiapps.controller.process.jobs;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

@Named
@Order(30)
public class FlowableDataCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableDataCleaner.class);

    private FlowableFacade flowableFacade;

    @Inject
    public FlowableDataCleaner(FlowableFacade flowableFacade) {
        this.flowableFacade = flowableFacade;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.WILL_DELETE_FLOWABLE_PROCESSES_BEFORE_0, expirationTime));
        List<ProcessInstance> processInstances = flowableFacade.findAllRunningProcessInstanceStartedBefore(expirationTime);
        LOGGER.info(CleanUpJob.LOG_MARKER, MessageFormat.format(Messages.FLOWABLE_PROCESSES_TO_DELETE, processInstances.size()));
        processInstances.stream()
                        .map(ProcessInstance::getProcessInstanceId)
                        .forEach(this::deleteProcessInstance);
    }

    private void deleteProcessInstance(String processInstanceId) {
        try {
            LOGGER.info(CleanUpJob.LOG_MARKER, MessageFormat.format(Messages.DELETING_FLOWABLE_PROCESS_WITH_ID, processInstanceId));
            flowableFacade.deleteProcessInstance(processInstanceId, Operation.State.ABORTED.name());
        } catch (Exception e) {
            LOGGER.error(CleanUpJob.LOG_MARKER, MessageFormat.format(Messages.ERROR_DELETING_FLOWABLE_PROCESS_WITH_ID, processInstanceId),
                         e);
        }
    }

}
