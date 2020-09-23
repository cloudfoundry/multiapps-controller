package org.cloudfoundry.multiapps.controller.process.listeners;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatraceProcessEvent;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatracePublisher;
import org.cloudfoundry.multiapps.controller.process.dynatrace.ImmutableDynatraceProcessEvent;
import org.cloudfoundry.multiapps.controller.process.util.OperationInFinalStateHandler;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

@Named("endProcessListener")
public class EndProcessListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 2L;

    private final OperationInFinalStateHandler eventHandler;
    protected final DynatracePublisher dynatracePublisher;
    protected final ProcessTypeParser processTypeParser;

    @Inject
    public EndProcessListener(OperationInFinalStateHandler eventHandler, DynatracePublisher dynatracePublisher, ProcessTypeParser processTypeParser) {
        this.eventHandler = eventHandler;
        this.dynatracePublisher = dynatracePublisher;
        this.processTypeParser = processTypeParser;
    }

    @Override
    protected void notifyInternal(DelegateExecution execution) {
        if (isRootProcess(execution)) {
            eventHandler.handle(execution, processTypeParser.getProcessType(execution), Operation.State.FINISHED);
            publishDynatraceEvent(execution,  processTypeParser.getProcessType(execution));
        }
    }

    private void publishDynatraceEvent(DelegateExecution execution, ProcessType processType) {
        DynatraceProcessEvent finishedEvent = ImmutableDynatraceProcessEvent.builder()
                                                                         .processId(VariableHandling.get(execution, Variables.CORRELATION_ID))
                                                                         .mtaId(VariableHandling.get(execution, Variables.MTA_ID))
                                                                         .spaceId(VariableHandling.get(execution, Variables.SPACE_GUID))
                                                                         .eventType(DynatraceProcessEvent.EventType.FINISHED)
                                                                         .processType(processType)
                                                                         .build();
        dynatracePublisher.publishProcessEvent(finishedEvent, getLogger());
    }
}
