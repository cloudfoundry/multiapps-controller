package com.sap.cloud.lm.sl.cf.persistence.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.persistence.Constants;

public class ProcessLoggerProviderTest {

    private static final String CORRELATION_ID = "1234";
    private static final String TASK_ID = "1";
    private static final String SPACE_ID = "441cba52-fd99-4452-8c93-211ce1ad28e7";
    private static final String TEST_FILE_NAME = "testLoggerFile";

    @Mock
    private DelegateExecution context;

    private Path temporaryLogFile;
    private ProcessLoggerProvider processLoggerProvider;
    private ProcessLogger processLogger;

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        temporaryLogFile = Files.createTempFile(TEST_FILE_NAME, null);
        processLoggerProvider = new ProcessLoggerProvider() {

            @Override
            protected File getLocalFile(String loggerName) {
                return temporaryLogFile.toFile();
            }

        };
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (processLogger != null) {
            processLoggerProvider.remove(processLogger);

        }
        if (temporaryLogFile.toFile()
                            .exists()) {
            Files.delete(temporaryLogFile);
        }
    }

    @Test
    public void testGetLogger() {
        prepareContext();

        processLogger = processLoggerProvider.getLogger(context);

        assertEquals(CORRELATION_ID, processLogger.getProcessId());
        assertEquals(TASK_ID, processLogger.getActivityId());
        assertEquals(SPACE_ID, processLogger.spaceId);

    }

    private void prepareContext() {
        when(context.getVariable(Constants.CORRELATION_ID)).thenReturn(CORRELATION_ID);
        when(context.getVariable(Constants.TASK_ID)).thenReturn(TASK_ID);
        when(context.getVariable(Constants.VARIABLE_NAME_SPACE_ID)).thenReturn(SPACE_ID);
    }

    @Test
    public void testGetExistingLogger() {
        prepareContext();

        processLogger = processLoggerProvider.getLogger(context);
        ProcessLogger existingLogger = processLoggerProvider.getExistingLoggers(CORRELATION_ID, TASK_ID)
                                                            .get(0);

        assertEquals(processLogger, existingLogger);

    }

    @Test
    public void testGetNullProcessLogger() {
        processLogger = processLoggerProvider.getLogger(context);

        assertTrue(processLogger instanceof NullProcessLogger,
                   MessageFormat.format("Expected NullProcessLogger but was {0}", processLogger.getClass()
                                                                                               .getSimpleName()));
    }
}
