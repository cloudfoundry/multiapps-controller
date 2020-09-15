package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.model.HistoricOperationEvent.EventType;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.core.persistence.service.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.core.persistence.service.OperationService;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;

@Named
public class AbortProcessAction extends ProcessAction {

    private final HistoricOperationEventService historicEventService;
    private final OperationService operationService;

    @Inject
    public AbortProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                              HistoricOperationEventService historicEventService, OperationService operationService,
                              CloudControllerClientProvider cloudControllerClientProvider) {
        super(flowableFacade, additionalProcessActions, cloudControllerClientProvider);
        this.historicEventService = historicEventService;
        this.operationService = operationService;
    }

    @Override
    public void executeActualProcessAction(String user, String superProcessInstanceId) {
        releaseOperationLock(superProcessInstanceId, Operation.State.ABORTED);
        historicEventService.add(ImmutableHistoricOperationEvent.of(superProcessInstanceId, EventType.ABORTED));
        historicEventService.add(ImmutableHistoricOperationEvent.of(superProcessInstanceId, EventType.ABORT_EXECUTED));
    }

    private void releaseOperationLock(String superProcessInstanceId, Operation.State state) {
        getProcessConflictPreventer().releaseLock(superProcessInstanceId, state);
    }

    protected ProcessConflictPreventer getProcessConflictPreventer() {
        return new ProcessConflictPreventer(operationService);
    }

    @Override
    public Action getAction() {
        return Action.ABORT;
    }

}
