package org.cloudfoundry.multiapps.controller.persistence.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NullProcessLoggerTest {

    private static final String SPACE_ID = "space-1";
    private static final String PROCESS_ID = "process-1";
    private static final String ACTIVITY_ID = "activity-1";

    @Test
    void testInfoIsSafeToCall() {
        NullProcessLogger logger = newLogger();

        Assertions.assertDoesNotThrow(() -> logger.info("anything"));
    }

    @Test
    void testDebugIsSafeToCall() {
        NullProcessLogger logger = newLogger();

        Assertions.assertDoesNotThrow(() -> logger.debug("anything"));
    }

    @Test
    void testErrorIsSafeToCall() {
        NullProcessLogger logger = newLogger();

        Assertions.assertDoesNotThrow(() -> logger.error("anything"));
    }

    @Test
    void testErrorWithThrowableIsSafeToCall() {
        NullProcessLogger logger = newLogger();

        Assertions.assertDoesNotThrow(() -> logger.error("anything", new RuntimeException()));
    }

    @Test
    void testTraceIsSafeToCall() {
        NullProcessLogger logger = newLogger();

        Assertions.assertDoesNotThrow(() -> logger.trace("anything"));
    }

    @Test
    void testWarnIsSafeToCall() {
        NullProcessLogger logger = newLogger();

        Assertions.assertDoesNotThrow(() -> logger.warn("anything"));
    }

    @Test
    void testWarnWithThrowableIsSafeToCall() {
        NullProcessLogger logger = newLogger();

        Assertions.assertDoesNotThrow(() -> logger.warn("anything", new RuntimeException()));
    }

    @Test
    void testLogNullCorrelationIdDoesNotThrow() {
        NullProcessLogger logger = newLogger();

        Assertions.assertDoesNotThrow(logger::logNullCorrelationId);
    }

    private NullProcessLogger newLogger() {
        return new NullProcessLogger(SPACE_ID, PROCESS_ID, ACTIVITY_ID);
    }
}
