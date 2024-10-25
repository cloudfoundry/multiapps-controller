package org.cloudfoundry.multiapps.controller.process.flowable;

import static java.text.MessageFormat.format;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.util.SafeExecutor;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatraceProcessEvent;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatracePublisher;
import org.cloudfoundry.multiapps.controller.process.dynatrace.ImmutableDynatraceProcessEvent;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class AbortProcessAction extends ProcessAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortProcessAction.class);
    protected static final SafeExecutor SAFE_EXECUTOR = new SafeExecutor(AbortProcessAction::logDynatraceException);

    private final HistoricOperationEventService historicEventService;
    private final ProgressMessageService progressMessageService;
    private DynatracePublisher dynatracePublisher;

    @Inject
    public AbortProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                              HistoricOperationEventService historicEventService, OperationService operationService,
                              CloudControllerClientProvider cloudControllerClientProvider, ProgressMessageService progressMessageService,
                              DynatracePublisher dynatracePublisher) {
        super(flowableFacade, additionalProcessActions, operationService, cloudControllerClientProvider);
        this.historicEventService = historicEventService;
        this.progressMessageService = progressMessageService;
        this.dynatracePublisher = dynatracePublisher;
    }

    @Override
    public void executeActualProcessAction(String user, String superProcessInstanceId) {
        releaseOperationLock(superProcessInstanceId, Operation.State.ABORTED);
        historicEventService.add(ImmutableHistoricOperationEvent.of(superProcessInstanceId, HistoricOperationEvent.EventType.ABORTED));
        historicEventService.add(ImmutableHistoricOperationEvent.of(superProcessInstanceId,
                                                                    HistoricOperationEvent.EventType.ABORT_EXECUTED));
        SAFE_EXECUTOR.execute(() -> publishDynatraceEvent(superProcessInstanceId));
    }

    private void publishDynatraceEvent(String processId) {
        List<ProgressMessage> errorProgressMessages = progressMessageService.createQuery()
                                                                            .processId(processId)
                                                                            .type(ProgressMessageType.ERROR)
                                                                            .list();
        if (errorProgressMessages.isEmpty()) {
            return;
        }
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
        dynatracePublisher.publishProcessEvent(errorEvent, getLogger());

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

    private Logger getLogger() {
        return LOGGER;
    }

    private static void logDynatraceException(Exception e) {
        LOGGER.warn(format(Messages.WILL_NOT_REGISTER_EVENT_IN_DYNATRACE, e.getMessage()), e);
    }

}
