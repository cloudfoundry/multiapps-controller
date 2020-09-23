package org.cloudfoundry.multiapps.controller.process.dynatrace;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DynatraceProcessEventTest {

    private static final String DYNATRACE_PROCESS_EVENT_CLASS_NAME = "org.cloudfoundry.multiapps.controller.process.dynatrace.DynatraceProcessEvent";
    private static final String GET_PROCESS_ID_METHOD_NAME = "getProcessId";
    private static final String GET_PROCESS_TYPE_METHOD_NAME = "getProcessType";
    private static final String GET_SPACE_ID_METHOD_NAME = "getSpaceId";
    private static final String GET_MTA_ID_METHOD_NAME = "getMtaId";
    private static final String GET_EVENT_TYPE_METHOD_NAME = "getEventType";
    private static final String MISSING_REQUIRED_DYNATRACE_ENTRY_POINT = "MISSING REQUIRED DYNATRACE ENTRY POINT. IF IT IS CHANGED BY INTENTION, MAKE SURE THAT DYNATRACE CONFIGURATION IS UPDATED IN TIME";

    @Test
    void testDynatraceProcessEventClassExists() {
        try {
            Class.forName(DYNATRACE_PROCESS_EVENT_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            Assertions.fail(MISSING_REQUIRED_DYNATRACE_ENTRY_POINT, e);
        }
    }

    @Test
    void testDynatraceProcessDurationHasAllRequiredMethods() {
        try {
            DynatraceProcessEvent.class.getMethod(GET_PROCESS_ID_METHOD_NAME);
            DynatraceProcessEvent.class.getMethod(GET_PROCESS_TYPE_METHOD_NAME);
            DynatraceProcessEvent.class.getMethod(GET_SPACE_ID_METHOD_NAME);
            DynatraceProcessEvent.class.getMethod(GET_MTA_ID_METHOD_NAME);
            DynatraceProcessEvent.class.getMethod(GET_EVENT_TYPE_METHOD_NAME);
        } catch (Exception e) {
            Assertions.fail(MISSING_REQUIRED_DYNATRACE_ENTRY_POINT, e);
        }
    }

}
