package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProcessLoggerProviderTest {

    private static final String CORRELATION_ID = "1234";
    private static final String TASK_ID = "1";
    private static final String SPACE_ID = "441cba52-fd99-4452-8c93-211ce1ad28e7";
    private static final String TEST_FILE_NAME = "testLoggerFile";

    @Mock
    private DelegateExecution execution;

    private Path temporaryLogFile;
    private ProcessLoggerProvider processLoggerProvider;
    private ProcessLogger processLogger;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        temporaryLogFile = Files.createTempFile(TEST_FILE_NAME, null);
        processLoggerProvider = new ProcessLoggerProvider();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (processLogger != null && processLogger.getLoggerName() != null) {
            processLoggerProvider.removeLoggersCache(processLogger);

        }
        if (temporaryLogFile.toFile()
                            .exists()) {
            Files.delete(temporaryLogFile);
        }
    }

    @Test
    void testGetLogger() {
        prepareContext();

        processLogger = processLoggerProvider.getLogger(execution);

        assertEquals(CORRELATION_ID, processLogger.getProcessId());
        assertEquals(TASK_ID, processLogger.getActivityId());
        assertEquals(SPACE_ID, processLogger.spaceId);

    }

    private void prepareContext() {
        when(execution.getVariable(Constants.CORRELATION_ID)).thenReturn(CORRELATION_ID);
        when(execution.getVariable(Constants.TASK_ID)).thenReturn(TASK_ID);
        when(execution.getVariable(Constants.VARIABLE_NAME_SPACE_ID)).thenReturn(SPACE_ID);
    }

    @Test
    void testGetExistingLogger() {
        prepareContext();

        processLogger = processLoggerProvider.getLogger(execution);
        ProcessLogger existingLogger = processLoggerProvider.getExistingLoggers(CORRELATION_ID, TASK_ID)
                                                            .get(0);

        assertEquals(processLogger, existingLogger);

    }

    @Test
    void testGetNullProcessLogger() {
        processLogger = processLoggerProvider.getLogger(execution);
        assertTrue(processLogger instanceof NullProcessLogger,
                   MessageFormat.format("Expected NullProcessLogger but was {0}", processLogger.getClass()
                                                                                               .getSimpleName()));
    }
}
