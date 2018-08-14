package com.sap.cloud.lm.sl.cf.persistence.services;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class FileLogCreator implements Callable<Logger> {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FileLogCreator.class);
    private static final String LOG_LAYOUT = "#2.0#%d{yyyy MM dd HH:mm:ss.SSS}#%d{XXX}#%p#%c#%n%X{MsgCode}#%X{CSNComponent}#%X{DCComponent}##%X{DSRCorrelationId}#%X{Application}#%C#%X{User}#%X{Session}#%X{Transaction}#%X{DSRRootContextId}#%X{DSRTransaction}#%X{DSRConnection}#%X{DSRCounter}#%t##%X{ResourceBundle}#%n%m#%n%n";

    private String name;
    private String logId;
    private String prefix;
    private String logDir;
    private Level customLoggingLevel;
    private PatternLayout customLayout;

    public FileLogCreator(String name, String logId, String prefix, String logDir, Level customLoggingLevel, PatternLayout customLayout) {
        this.name = name;
        this.logId = logId;
        this.prefix = prefix;
        this.logDir = logDir;
        this.customLoggingLevel = customLoggingLevel;
        this.customLayout = customLayout;
    }

    @Override
    public Logger call() {
        return getOrCreateLogger();
    }

    private Logger getOrCreateLogger() {
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
        Level level = logger.getParent()
            .getLevel();
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

    public static final String getLoggerName(String prefix, String logId, String name) {
        return new StringBuilder(prefix).append(".")
            .append(logId)
            .append(".")
            .append(name)
            .toString();
    }
}
