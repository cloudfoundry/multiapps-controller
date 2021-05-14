package org.cloudfoundry.multiapps.controller.persistence.services;

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessLoggerTest {

    private static final String CORRELATION_ID = "1234";
    private static final String TASK_ID = "1";
    private static final String SPACE_ID = "441cba52-fd99-4452-8c93-211ce1ad28e7";
    private static final String TEST_FILE_NAME = "testLogName";
    private static final String TEST_LOGGER_NAME = "testLoggerName";
    private static final String TRACE_MESSAGE = "This is TRACE test-message";
    private static final String INFO_MESSAGE = "This is INFO test-message";
    private static final String DEBUG_MESSAGE = "This is DEBUG test-message";
    private static final String WARN_MESSAGE = "This is WARN test-message";
    private static final String ERROR_MESSAGE = "This is ERROR test-message";

    private ProcessLogger processLogger;
    protected LoggerContext loggerContext;
    private Logger logger;
    private File log;
    private ProcessLogsPersistenceService processLogsPersistenceService;

    @BeforeEach
    void setUp() {
        loggerContext = Mockito.mock(LoggerContext.class);
        logger = Mockito.mock(Logger.class);
        log = Mockito.mock(File.class);
        processLogger = new ProcessLogger(loggerContext, logger, log, TEST_FILE_NAME, SPACE_ID, CORRELATION_ID, TASK_ID);
        processLogsPersistenceService = Mockito.mock(ProcessLogsPersistenceService.class);

    }

    @Test
    void testProcessLogger() {
        prepareContextForProtectedConstructor();
        processLogger = new ProcessLogger(loggerContext, SPACE_ID, CORRELATION_ID, TASK_ID);
        processLogger.info(INFO_MESSAGE);
        processLogger.debug(DEBUG_MESSAGE);
        processLogger.trace(TRACE_MESSAGE);
        processLogger.warn(WARN_MESSAGE);
        processLogger.error(ERROR_MESSAGE);

        verify(logger, times(1)).info((Object) INFO_MESSAGE);
        verify(logger, times(1)).debug((Object) DEBUG_MESSAGE);
        verify(logger, times(1)).trace((Object) TRACE_MESSAGE);
        verify(logger, times(1)).error((Object) ERROR_MESSAGE);
        verify(logger, times(1)).warn((Object) WARN_MESSAGE);
    }

    private void prepareContextForProtectedConstructor() {
        when(loggerContext.getRootLogger()).thenReturn(logger);
    }

    @Test
    void info() {
        processLogger.info(INFO_MESSAGE);
        verify(logger, times(1)).info((Object) INFO_MESSAGE);
    }

    @Test
    void debug() {
        processLogger.debug(DEBUG_MESSAGE);
        verify(logger, times(1)).debug((Object) DEBUG_MESSAGE);
    }

    @Test
    void testDebug() {
        Exception exception = new SLException(ERROR_MESSAGE);
        processLogger.debug(DEBUG_MESSAGE, exception);
        verify(logger, times(1)).debug((Object) DEBUG_MESSAGE, exception);
        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    void error() {
        processLogger.error(ERROR_MESSAGE);
        verify(logger, times(1)).error((Object) ERROR_MESSAGE);
    }

    @Test
    void testError() {
        Exception exception = new SLException(ERROR_MESSAGE);
        processLogger.error(ERROR_MESSAGE, exception);
        verify(logger, times(1)).error((Object) ERROR_MESSAGE, exception);
        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    void trace() {
        processLogger.trace(TRACE_MESSAGE);
        verify(logger, times(1)).trace((Object) TRACE_MESSAGE);
    }

    @Test
    void warn() {
        processLogger.warn(WARN_MESSAGE);
        verify(logger, times(1)).warn((Object) WARN_MESSAGE);
    }

    @Test
    void testWarn() {
        Exception exception = new SLException(ERROR_MESSAGE);
        processLogger.warn(WARN_MESSAGE, exception);
        verify(logger, times(1)).warn((Object) WARN_MESSAGE, exception);
        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    void getProcessId() {
        assertEquals(processLogger.getProcessId(), CORRELATION_ID);
    }

    @Test
    void getActivityId() {
        assertEquals(processLogger.getActivityId(), TASK_ID);
    }

    @Test
    void persistLogFile() {
        prepareContextForTruePersist();
        processLogger.persistLogFile(processLogsPersistenceService);
        verify(processLogsPersistenceService, times(1)).persistLog(SPACE_ID, CORRELATION_ID, log, TEST_FILE_NAME);
    }

    @Test
    void persistLogFileWithNullPassed() {
        prepareContextForFalsePersist();
        processLogger.persistLogFile(processLogsPersistenceService);
        verify(processLogsPersistenceService, times(0)).persistLog(SPACE_ID, CORRELATION_ID, log, TEST_FILE_NAME);
    }

    private void prepareContextForTruePersist() {
        when(log.exists()).thenReturn(true);
    }

    private void prepareContextForFalsePersist() {
        when(log.exists()).thenReturn(false);
    }

    @Test
    void getLoggerName() {
        prepareContextForGetLoggerName();
        assertEquals(TEST_LOGGER_NAME, processLogger.getLoggerName());
    }

    private void prepareContextForGetLoggerName() {
        when(logger.getName()).thenReturn(TEST_LOGGER_NAME);
    }

    @Test
    void closeLoggerContext() {
        processLogger.closeLoggerContext();
        verify(loggerContext, times(1)).close();
    }
}