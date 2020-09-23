package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.model.HistoricOperationEvent.EventType;
import org.cloudfoundry.multiapps.controller.core.persistence.service.OperationService;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;
import org.cloudfoundry.multiapps.controller.process.event.DynatraceEventPublisher;
import org.cloudfoundry.multiapps.controller.process.event.DynatraceProcessEvent;
import org.cloudfoundry.multiapps.controller.process.event.ImmutableDynatraceProcessEvent;
import org.cloudfoundry.multiapps.controller.process.util.HistoricOperationEventPersister;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class AbortProcessAction extends ProcessAction {

    public static final String ACTION_ID_ABORT = "abort";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortProcessAction.class);

    private HistoricOperationEventPersister historicEventPersister;
    private OperationService operationService;
    private final ProgressMessageService progressMessageService;
    private DynatraceEventPublisher dynatraceEventPublisher;

    @Inject
    public AbortProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                              HistoricOperationEventPersister historicEventPersister, OperationService operationService,
                              ProgressMessageService progressMessageService, DynatraceEventPublisher dynatraceEventPublisher) {
        super(flowableFacade, additionalProcessActions);
        this.historicEventPersister = historicEventPersister;
        this.operationService = operationService;
        this.progressMessageService = progressMessageService;
        this.dynatraceEventPublisher = dynatraceEventPublisher;
    }

    @Override
    public void executeActualProcessAction(String user, String superProcessInstanceId) {
        flowableFacade.setAbortVariable(superProcessInstanceId);
        releaseOperationLock(superProcessInstanceId, Operation.State.ABORTED);
        historicEventPersister.add(superProcessInstanceId, EventType.ABORTED);
        publishDynatraceEvent(superProcessInstanceId);
    }

    private void publishDynatraceEvent(String processId) {
        LOGGER.info("entering publishDynatraceEvent");
        List<ProgressMessage> errorProgressMessages = progressMessageService.createQuery()
                                                                            .processId(processId)
                                                                            .type(ProgressMessageType.ERROR)
                                                                            .list();
        if (errorProgressMessages.isEmpty()) {
            LOGGER.info("no error messages");
            return;
        }
        LOGGER.info("count of error messages: " + errorProgressMessages.size());
        Operation operation = operationService.createQuery()
                                              .processId(processId)
                                              .singleResult();
        DynatraceProcessEvent errorEvent = ImmutableDynatraceProcessEvent.builder()
                                                                         .processId(processId)
                                                                         .eventType(DynatraceProcessEvent.EventType.FAILED)
                                                                         .mtaId(operation.getMtaId())
                                                                         .processType(operation.getProcessType())
                                                                         .spaceId(operation.getSpaceId())
                                                                         .build();
        dynatraceEventPublisher.publish(errorEvent, getLogger());

    }

    private void releaseOperationLock(String superProcessInstanceId, Operation.State state) {
        getProcessConflictPreventer().releaseLock(superProcessInstanceId, state);
    }

    protected ProcessConflictPreventer getProcessConflictPreventer() {
        return new ProcessConflictPreventer(operationService);
    }

    @Override
    public String getActionId() {
        return ACTION_ID_ABORT;
    }

    private Logger getLogger() {
        return LOGGER;
    }

}
