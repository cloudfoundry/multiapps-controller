package com.sap.cloud.lm.sl.cf.process;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.api.activiti.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;

public class ProcessLoggerProviderFactory {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ProcessLoggerProviderFactory.class);

    public static final String LOG_DIR = "logs";
    private static final String LOG_LAYOUT = "#2.0#%d{yyyy MM dd HH:mm:ss.SSS}#%d{XXX}#%p#%c#%n%X{MsgCode}#%X{CSNComponent}#%X{DCComponent}##%X{DSRCorrelationId}#%X{Application}#%C#%X{User}#%X{Session}#%X{Transaction}#%X{DSRRootContextId}#%X{DSRTransaction}#%X{DSRConnection}#%X{DSRCounter}#%t##%X{ResourceBundle}#%n%m#%n%n";

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
                logger = getOrCreateLogger(processId, name);
                nameMap.put(name, logger);
            }
            return logger;
        }

        private Logger getOrCreateLogger(String logId, String name) {
            String loggerName = getLoggerName(prefix, logId, name);
            Logger logger = LogManager.exists(loggerName);
            if (logger == null) {
                LOGGER.debug(format(Messages.CREATING_LOGGER, loggerName));
                logger = createLogger(loggerName);
            }
            File logFile = ProcessLogsPersistenceService.getFile(logId, name, logDir);
            if (logFile.exists()) {
                recreateLogFile(logFile);
            }
            LOGGER.debug(format(Messages.CREATING_APPENDER, loggerName));
            logger.addAppender(createAppender(logger.getLevel(), logFile));
            return logger;
        }

        private void recreateLogFile(File logFile) {
            logFile.delete();
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                throw new SLException(e);
            }
        }

        private Logger createLogger(String loggerName) {
            Logger logger;
            logger = Logger.getLogger(loggerName);
            Level level = logger.getParent().getLevel();
            if (customLoggingLevel != null) {
                level = customLoggingLevel;
            }
            logger.setLevel(level);
            return logger;
        }

        private Appender createAppender(Level level, File logFile) {
            FileAppender appender = new FileAppender();
            PatternLayout layout = new PatternLayout(LOG_LAYOUT);
            if (customLayout != null) {
                layout = customLayout;
            }
            appender.setLayout(layout);
            appender.setFile(logFile.getAbsolutePath());
            appender.setThreshold(level);
            appender.setAppend(true);
            appender.activateOptions();
            return appender;
        }

    }

    public static final String getLoggerName(String prefix, String logId, String name) {
        return new StringBuilder(prefix).append(".").append(logId).append(".").append(name).toString();
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
