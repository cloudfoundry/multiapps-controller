package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

class ProcessLoggerProviderTest {

    private final static String TEST_LOG_NAME = "test-log-name";
    private final static String TEST_LOG_NAME_WITH_EXTENSION = TEST_LOG_NAME + ".log";
    private final static String DEFAULT_LOG_NAME = "OPERATION.log";
    private final static String TEST_CORRELATION_ID = "test-correlation-id";
    private final static String TEST_SECOND_CORRELATION_ID = "test-second-correlation-id";
    private final static String TEST_TASK_ID = "test-task-id";
    private final static String TEST_SPACE_ID = "test-space-id";
    private final static LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
    private final static PatternLayout patternLayout = PatternLayout.newBuilder()
                                                                    .withPattern(EMPTY)
                                                                    .withConfiguration(loggerContext.getConfiguration())
                                                                    .build();

    @Spy
    private ProcessLoggerProvider processLoggerProvider;

    @Mock
    private DelegateExecution delegateExecution;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(delegateExecution.getVariable(Constants.CORRELATION_ID)).thenReturn(TEST_CORRELATION_ID);
        when(delegateExecution.getVariable(Constants.TASK_ID)).thenReturn(TEST_TASK_ID);
        when(delegateExecution.getCurrentActivityId()).thenReturn(TEST_TASK_ID);
        when(delegateExecution.getVariable(Constants.VARIABLE_NAME_SPACE_ID)).thenReturn(TEST_SPACE_ID);
        when(delegateExecution.getProcessInstanceId()).thenReturn(TEST_TASK_ID);
    }

    @Test
    void testCorrectlyCreatedProcessLoggerWithProvidedPatternLayoutAndLogName() {
        ProcessLogger processLogger = processLoggerProvider.getLogger(delegateExecution, TEST_LOG_NAME, A -> patternLayout);
        Assertions.assertEquals(processLogger.getActivityId(), TEST_TASK_ID);
        Assertions.assertEquals(TEST_LOG_NAME_WITH_EXTENSION, processLogger.getOperationLogEntry()
                                                                           .getOperationLogName());
        Assertions.assertEquals(TEST_SPACE_ID, processLogger.getOperationLogEntry()
                                                            .getSpace());
        Assertions.assertEquals(TEST_CORRELATION_ID, processLogger.getOperationLogEntry()
                                                                  .getOperationId());
    }

    @Test
    void testCorrectlyCreatedProcessLoggerWithLogName() {
        ProcessLogger processLogger = processLoggerProvider.getLogger(delegateExecution, TEST_LOG_NAME);
        Assertions.assertEquals(processLogger.getActivityId(), TEST_TASK_ID);
        Assertions.assertEquals(TEST_LOG_NAME_WITH_EXTENSION, processLogger.getOperationLogEntry()
                                                                           .getOperationLogName());
        Assertions.assertEquals(TEST_SPACE_ID, processLogger.getOperationLogEntry()
                                                            .getSpace());
        Assertions.assertEquals(TEST_CORRELATION_ID, processLogger.getOperationLogEntry()
                                                                  .getOperationId());
    }

    @Test
    void testCorrectlyCreatedProcessLogger() {
        ProcessLogger processLogger = processLoggerProvider.getLogger(delegateExecution);
        Assertions.assertEquals(processLogger.getActivityId(), TEST_TASK_ID);
        Assertions.assertEquals(DEFAULT_LOG_NAME, processLogger.getOperationLogEntry()
                                                               .getOperationLogName());
        Assertions.assertEquals(TEST_SPACE_ID, processLogger.getOperationLogEntry()
                                                            .getSpace());
        Assertions.assertEquals(TEST_CORRELATION_ID, processLogger.getOperationLogEntry()
                                                                  .getOperationId());
    }

    @Test
    void testExistingLoggers() {
        processLoggerProvider.getLogger(delegateExecution);

        when(delegateExecution.getVariable(Constants.CORRELATION_ID)).thenReturn(TEST_SECOND_CORRELATION_ID);
        processLoggerProvider.getLogger(delegateExecution);

        List<ProcessLogger> processLoggers = processLoggerProvider.getExistingLoggers(TEST_CORRELATION_ID, TEST_TASK_ID);
        Assertions.assertEquals(1, processLoggers.size());
    }

    @Test
    void testRemoveExistingLogger() {
        processLoggerProvider.getLogger(delegateExecution);

        List<ProcessLogger> processLoggers = processLoggerProvider.getExistingLoggers(TEST_CORRELATION_ID, TEST_TASK_ID);
        Assertions.assertEquals(processLoggers.size(), 1);

        processLoggerProvider.removeProcessLoggerFromCache(processLoggers.get(0));

        Assertions.assertEquals(0, processLoggerProvider.getExistingLoggers(TEST_CORRELATION_ID, TEST_TASK_ID)
                                                        .size());
    }
}
