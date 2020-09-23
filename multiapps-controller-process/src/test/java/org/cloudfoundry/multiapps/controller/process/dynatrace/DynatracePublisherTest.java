package org.cloudfoundry.multiapps.controller.process.dynatrace;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DynatracePublisherTest {

    private static final String DYNATRACE_PUBLISHER_CLASS_NAME = "org.cloudfoundry.multiapps.controller.process.dynatrace.DynatracePublisher";
    private static final String PUBLISH_PROCESS_EVENT_METHOD = "publishProcessEvent";
    private static final String PUBLISH_PROCESS_DURATION_METHOD = "publishProcessDuration";
    private static final String MISSING_REQUIRED_DYNATRACE_ENTRY_POINT = "MISSING REQUIRED DYNATRACE ENTRY POINT. IF IT IS CHANGED BY INTENTION, MAKE SURE THAT DYNATRACE CONFIGURATION IS UPDATED IN TIME";

    @Test
    void testDynatracePublisherClassExists() {
        try {
            Class.forName(DYNATRACE_PUBLISHER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            Assertions.fail(MISSING_REQUIRED_DYNATRACE_ENTRY_POINT, e);
        }
    }

    @Test
    void testPublishProcessEventMethodExists() {
        try {
            DynatracePublisher.class.getMethod(PUBLISH_PROCESS_EVENT_METHOD, DynatraceProcessEvent.class, org.slf4j.Logger.class);
        } catch (Exception e) {
            Assertions.fail(MISSING_REQUIRED_DYNATRACE_ENTRY_POINT, e);
        }
    }

    @Test
    void testPublishProcessDurationMethodExists() {
        try {
            DynatracePublisher.class.getMethod(PUBLISH_PROCESS_DURATION_METHOD, DynatraceProcessDuration.class, org.slf4j.Logger.class);
        } catch (Exception e) {
            Assertions.fail(MISSING_REQUIRED_DYNATRACE_ENTRY_POINT, e);
        }
    }
}
