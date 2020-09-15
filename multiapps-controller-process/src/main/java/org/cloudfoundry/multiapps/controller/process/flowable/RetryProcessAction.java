package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.model.HistoricOperationEvent.EventType;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.core.persistence.service.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RetryProcessAction extends ProcessAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryProcessAction.class);

    private final HistoricOperationEventService historicOperationEventService;

    @Inject
    public RetryProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                              HistoricOperationEventService historicOperationEventService,
                              CloudControllerClientProvider cloudControllerClientProvider) {
        super(flowableFacade, additionalProcessActions, cloudControllerClientProvider);
        this.historicOperationEventService = historicOperationEventService;
    }

    @Override
    protected void executeActualProcessAction(String user, String superProcessInstanceId) {
        List<String> subProcessIds = getActiveExecutionIds(superProcessInstanceId);
        ListIterator<String> subProcessesIdsIterator = subProcessIds.listIterator(subProcessIds.size());

        updateUserIfNecessary(user, superProcessInstanceId);
        while (subProcessesIdsIterator.hasPrevious()) {
            String subProcessId = subProcessesIdsIterator.previous();
            retryProcess(subProcessId);
        }
        historicOperationEventService.add(ImmutableHistoricOperationEvent.of(superProcessInstanceId, EventType.RETRIED));
    }

    private void retryProcess(String subProcessId) {
        try {
            flowableFacade.executeJob(subProcessId);
        } catch (RuntimeException e) {
            // Consider the retry as successful. The execution error could be later obtained through
            // the getError() method.
            LOGGER.error(Messages.FLOWABLE_JOB_RETRY_FAILED, e);
        }
    }

    @Override
    public Action getAction() {
        return Action.RETRY;
    }

}
