package org.cloudfoundry.multiapps.controller.persistence.services;

import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessLogsPersisterTest {

    private static final String CORRELATION_ID = "1234";
    private static final String TASK_ID = "1";
    private static final String SPACE_ID = "441cba52-fd99-4452-8c93-211ce1ad28e7";
    private static final String TEST_FILE_NAME = "testLoggerFile";
    private static final String LOG_EXTENSION = ".log";

    @Mock
    private final ProcessLogsPersistenceService processLogsPersistenceService = mock(ProcessLogsPersistenceService.class);
    @Mock
    private final ProcessLoggerProvider processLoggerProvider = mock(ProcessLoggerProvider.class);
    @Mock
    private ProcessLogger processLogger;
    @InjectMocks
    private ProcessLogsPersister processLogsPersister = new ProcessLogsPersister();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                .close();
        processLogger = mock(ProcessLogger.class);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void persistLogs() {
        prepareContext();
        processLogsPersister.persistLogs(CORRELATION_ID,TASK_ID);
        verify(processLogger,times(1)).persistLogFile(processLogsPersistenceService);
        verify(processLoggerProvider,times(1)).removeLoggersCache(processLogger);
        verify(processLogger,times(1)).closeLoggerContext();
        verify(processLogger,times(1)).deleteLogFile();
    }

    private void prepareContext() {
        List<ProcessLogger> processLoggerList = new ArrayList<>();
        processLoggerList.add(processLogger);
        when(processLoggerProvider.getExistingLoggers(CORRELATION_ID,TASK_ID)).thenReturn(processLoggerList);
    }
}