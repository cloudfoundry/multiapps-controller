package org.cloudfoundry.multiapps.controller.process.dynatrace;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.util.LoggingUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;

@Named
public class DynatracePublisher {

    public void publishProcessEvent(DynatraceProcessEvent dynatraceProcessEvent, Logger logger) {
        LoggingUtil.logWithCorrelationId(dynatraceProcessEvent.getProcessId(),
                                         () -> logger.info(MessageFormat.format(Messages.REGISTERING_PROCESS_EVENT_IN_DYNATRACE,
                                                                                dynatraceProcessEvent)));
    }

    public void publishProcessDuration(DynatraceProcessDuration dynatraceProcessDuration, Logger logger) {
        LoggingUtil.logWithCorrelationId(dynatraceProcessDuration.getProcessId(),
                                         () -> logger.info(MessageFormat.format(Messages.REGISTERING_PROCESS_DURATION_IN_DYNATRACE,
                                                                                dynatraceProcessDuration)));
    }
}
