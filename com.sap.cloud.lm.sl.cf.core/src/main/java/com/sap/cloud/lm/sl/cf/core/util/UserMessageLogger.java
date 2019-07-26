package com.sap.cloud.lm.sl.cf.core.util;

public interface UserMessageLogger {

    void logFlowableTask();

    void infoWithoutProgressMessage(String pattern, Object... arguments);

    void infoWithoutProgressMessage(String message);

    void info(String pattern, Object... arguments);

    void info(String message);

    void errorWithoutProgressMessage(Exception e, String pattern, Object... arguments);

    void errorWithoutProgressMessage(Exception e, String message);

    void errorWithoutProgressMessage(String pattern, Object... arguments);

    void errorWithoutProgressMessage(String message);

    void error(Exception e, String pattern, Object... arguments);

    void error(Exception e, String message);

    void error(String pattern, Object... arguments);

    void error(String message);

    void warnWithoutProgressMessage(Exception e, String pattern, Object... arguments);

    void warnWithoutProgressMessage(Exception e, String message);

    void warnWithoutProgressMessage(String pattern, Object... arguments);

    void warnWithoutProgressMessage(String message);

    void warn(Exception e, String pattern, Object... arguments);

    void warn(Exception e, String message);

    void warn(String pattern, Object... arguments);

    void warn(String message);

    void debug(String pattern, Object... arguments);

    void debug(String message);

    void trace(String pattern, Object... arguments);

    void trace(String message);
}
