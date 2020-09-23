package org.cloudfoundry.multiapps.controller.process.event;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.util.LoggingUtil;
import org.slf4j.Logger;

@Named
public class DynatraceEventPublisher {
    
    public void publish(DynatraceProcessEvent dynatraceProcessEvent, Logger logger) {
//        if (logger.isDebugEnabled()) {
            LoggingUtil.logWithCorrelationId(dynatraceProcessEvent.getProcessId(),
                                             () -> logger.info("registering event"));
//        }
    }
}
