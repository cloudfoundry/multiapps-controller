package org.cloudfoundry.multiapps.controller.persistence.util;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CloudLoggingServiceUtilTest {

    private static final String MESSAGE = "boom";

    @Test
    void logErrorOrThrow_failSafeTrue_logsAndDoesNotThrow() {
        Logger logger = mock(Logger.class);

        CloudLoggingServiceUtil.logErrorOrThrowExceptionBasedOnFailSafe(buildConfig(true), logger, MESSAGE);

        verify(logger).error(MESSAGE);
    }

    @Test
    void logErrorOrThrow_failSafeFalse_throwsAndDoesNotLog() {
        Logger logger = mock(Logger.class);

        SLException thrown = assertThrows(SLException.class,
                                          () -> CloudLoggingServiceUtil.logErrorOrThrowExceptionBasedOnFailSafe(buildConfig(false), logger,
                                                                                                                MESSAGE));

        assertEquals(MESSAGE, thrown.getMessage());
        verify(logger, never()).error(MESSAGE);
    }

    private static LoggingConfiguration buildConfig(boolean failSafe) {
        return ImmutableLoggingConfiguration.builder()
                                            .operationId("op")
                                            .mtaSpaceId("space")
                                            .logLevel(LogLevel.INFO)
                                            .isFailSafe(failSafe)
                                            .endpointUrl("https://cls.example.com")
                                            .serverCa("ca")
                                            .clientCert("cert")
                                            .clientKey("key")
                                            .build();
    }
}
