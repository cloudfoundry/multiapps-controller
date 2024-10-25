package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.List;
import java.util.ListIterator;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RetryProcessAction extends ProcessAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryProcessAction.class);

    private final HistoricOperationEventService historicOperationEventService;

    @Inject
    public RetryProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                              HistoricOperationEventService historicOperationEventService, OperationService operationService,
                              CloudControllerClientProvider cloudControllerClientProvider) {
        super(flowableFacade, additionalProcessActions, operationService, cloudControllerClientProvider);
        this.historicOperationEventService = historicOperationEventService;
    }

    @Override
    protected void executeActualProcessAction(String user, String superProcessInstanceId) {
        List<String> subProcessIds = getActiveExecutionIds(superProcessInstanceId);
        ListIterator<String> subProcessesIdsIterator = subProcessIds.listIterator(subProcessIds.size());
        updateUserIfNecessary(user, superProcessInstanceId, subProcessIds);
        while (subProcessesIdsIterator.hasPrevious()) {
            String subProcessId = subProcessesIdsIterator.previous();
            retryProcess(subProcessId);
        }
        updateOperationState(superProcessInstanceId, Operation.State.RUNNING);
        historicOperationEventService.add(ImmutableHistoricOperationEvent.of(superProcessInstanceId,
                                                                             HistoricOperationEvent.EventType.RETRIED));
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
