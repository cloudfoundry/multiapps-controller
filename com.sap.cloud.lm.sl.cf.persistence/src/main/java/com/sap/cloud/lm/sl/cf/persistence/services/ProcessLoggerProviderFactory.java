package com.sap.cloud.lm.sl.cf.persistence.services;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.NOPLogger;
import org.apache.log4j.varia.NullAppender;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.message.Constants;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;

public class ProcessLoggerProviderFactory {

    static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ProcessLoggerProviderFactory.class);

    public static String LOG_DIR = "logs";

    // map between process id and logger
    private final Map<String, Map<String, Logger>> LOGGERS = new HashMap<String, Map<String, Logger>>();
    private final Map<String, ThreadLocalLogProvider> threadLocalProviderMap = new ConcurrentHashMap<String, ProcessLoggerProviderFactory.ThreadLocalLogProvider>();

    private static ProcessLoggerProviderFactory instance;

    @Inject
    private ProcessLogsPersistenceService processLogsPersistenceService;

    public static ProcessLoggerProviderFactory getInstance() {
        if (instance == null) {
            instance = new ProcessLoggerProviderFactory();
        }
        return instance;
    }

    public final ThreadLocalLogProvider getDefaultLoggerProvider() {
        return getLoggerProvider(DEFAULT_NAME);
    }

    public final ThreadLocalLogProvider getLoggerProvider(String name) {
        ThreadLocalLogProvider result = threadLocalProviderMap.get(name);
        if (result == null) {
            result = new ThreadLocalLogProvider();
            threadLocalProviderMap.put(name, result);
        }
        return result;
    }

    public final void removeAll() {
        for (ThreadLocal<Logger> threadLocal : threadLocalProviderMap.values()) {
            threadLocal.remove();
        }
        threadLocalProviderMap.clear();
    }

    public static final String DEFAULT_NAME = "MAIN_LOG";

    public class ThreadLocalLogProvider extends ThreadLocal<Logger> {

        private String prefix;
        private String name;
        private String processId;
        private Level customLoggingLevel;
        private PatternLayout customLayout;
        private String logDir;

        private ThreadLocalLogProvider() {
            super();
        }

        public synchronized Logger getLogger(String processId, String prefix) {
            this.processId = processId;
            this.prefix = prefix;
            this.name = DEFAULT_NAME;
            this.logDir = LOG_DIR;
            return get();
        }

        public synchronized Logger getLogger(String processId, String prefix, String name) {
            this.processId = processId;
            this.prefix = prefix;
            this.name = name;
            this.logDir = LOG_DIR;
            return get();
        }

        public synchronized Logger getLogger(String processId, String prefix, String name, PatternLayout customLayout) {
            this.processId = processId;
            this.prefix = prefix;
            this.customLayout = customLayout;
            this.name = name;
            this.logDir = LOG_DIR;
            return get();
        }

        public synchronized Logger getLogger(String processId, String prefix, Level customLoggingLevel, PatternLayout customLayout,
            String customLogDir) {
            this.processId = processId;
            this.prefix = prefix;
            this.customLoggingLevel = customLoggingLevel;
            this.customLayout = customLayout;
            this.name = DEFAULT_NAME;
            this.logDir = customLogDir;
            return get();
        }

        @Override
        protected Logger initialValue() {
            Map<String, Logger> nameMap = LOGGERS.get(processId);
            if (nameMap == null) {
                nameMap = new HashMap<String, Logger>();
                LOGGERS.put(processId, nameMap);
            }

            Logger logger = nameMap.get(name);
            if (logger == null) {
                logger = getLoggerInSeparateThread();
            }
            if (logger.getLevel() != null) {
                nameMap.put(name, logger);
            }
            return logger;
        }

        private Logger getLoggerInSeparateThread() {
            Logger logger;
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                logger = tryToGetLogger(executor);
                return logger;
            } finally {
                executor.shutdownNow();
            }
        }

        Logger tryToGetLogger(ExecutorService executor) {
            FileLogCreator logStorage = new FileLogCreator(name, processId, prefix, logDir, customLoggingLevel, customLayout);
            Future<Logger> logStorageTask = executor.submit(logStorage);
            try {
                return logStorageTask.get(30, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.warn("Get logger operation was interrupted");
            } catch (TimeoutException e) {
                LOGGER.warn("Get logger operation has timed out");
            }
            Logger nullLogger = NOPLogger.getLogger("NOOP_LOGGER");
            nullLogger.addAppender(new NullAppender());
            return nullLogger;
        }

    }

    public static String getProcessId(DelegateExecution context) {
        return context.getProcessInstanceId();
    }

    private static String getSpaceId(DelegateExecution context) {
        return (String) context.getVariable(Constants.VARIABLE_NAME_SPACE_ID);
    }

    public void flush(DelegateExecution context, String logDir) throws IOException, FileStorageException {
        String processId = getProcessId(context);
        String spaceId = getSpaceId(context);
        Map<String, Logger> nameMap = LOGGERS.remove(processId);
        if (nameMap != null) {
            LOGGER.debug(format(Messages.REMOVING_ALL_LOGGERS_FOR_PROCESS, processId, nameMap.keySet()));
            for (String name : nameMap.keySet()) {
                Logger logger = nameMap.get(name);
                LOGGER.debug(format(Messages.REMOVING_ALL_APPENDERS_FROM_LOGGER, logger.getName()));
                logger.removeAllAppenders();
                saveLog(logDir, processId, spaceId, name);
            }
        }
    }

    public void append(DelegateExecution context, String logDir) throws IOException, FileStorageException {
        String processId = getProcessId(context);
        String spaceId = getSpaceId(context);
        Map<String, Logger> nameMap = LOGGERS.remove(processId);
        if (nameMap != null) {
            LOGGER.debug(format(Messages.REMOVING_ALL_LOGGERS_FOR_PROCESS, processId, nameMap.keySet()));
            for (String name : nameMap.keySet()) {
                Logger logger = nameMap.get(name);
                LOGGER.debug(format(Messages.REMOVING_ALL_APPENDERS_FROM_LOGGER, logger.getName()));
                logger.removeAllAppenders();
                appendLog(logDir, processId, spaceId, name);
            }
        }
    }

    private void saveLog(String logDir, String processId, String spaceId, String name) throws IOException, FileStorageException {
        if (spaceId == null) {
            processLogsPersistenceService.saveLog(processId, name, logDir);
        } else {
            processLogsPersistenceService.saveLog(spaceId, processId, name, logDir);
        }
    }

    private void appendLog(String logDir, String processId, String spaceId, String name) throws IOException, FileStorageException {
        if (spaceId == null) {
            processLogsPersistenceService.appendLog(processId, name, logDir);
        } else {
            processLogsPersistenceService.appendLog(spaceId, processId, name, logDir);
        }
    }

}
