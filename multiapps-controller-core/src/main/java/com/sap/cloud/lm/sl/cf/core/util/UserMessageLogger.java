package com.sap.cloud.lm.sl.cf.core.util;

public interface UserMessageLogger {

    public void logFlowableTask();

    public void infoWithoutProgressMessage(String pattern, Object... arguments);

    public void infoWithoutProgressMessage(String message);

    public void info(String pattern, Object... arguments);

    public void info(String message);

    public void errorWithoutProgressMessage(Exception e, String pattern, Object... arguments);

    public void errorWithoutProgressMessage(Exception e, String message);

    public void errorWithoutProgressMessage(String pattern, Object... arguments);

    public void errorWithoutProgressMessage(String message);

    public void error(Exception e, String pattern, Object... arguments);

    public void error(Exception e, String message);

    public void error(String pattern, Object... arguments);

    public void error(String message);

    public void warnWithoutProgressMessage(Exception e, String pattern, Object... arguments);

    public void warnWithoutProgressMessage(Exception e, String message);

    public void warnWithoutProgressMessage(String pattern, Object... arguments);

    public void warnWithoutProgressMessage(String message);

    public void warn(Exception e, String pattern, Object... arguments);

    public void warn(Exception e, String message);

    public void warn(String pattern, Object... arguments);

    public void warn(String message);

    public void debug(String pattern, Object... arguments);

    public void debug(String message);

    public void trace(String pattern, Object... arguments);

    public void trace(String message);
}
