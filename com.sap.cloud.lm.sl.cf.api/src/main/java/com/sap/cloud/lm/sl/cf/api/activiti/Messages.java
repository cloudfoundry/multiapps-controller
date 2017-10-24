package com.sap.cloud.lm.sl.cf.api.activiti;

public class Messages {
    
    // Exception messages
    
    public static final String ABORT_OPERATION_TIMED_OUT = "Abort operation timed out";
    public static final String ACTIVITI_JOB_RETRY_FAILED = "Activiti job retry failed";
    
    // Warn messages
    
    public static final String RETRYING_PROCESS_ABORT = "Abort of process \"{0}\" failed due to an optimistic locking exception. Retrying abort...";

    // Debug messages

    public static final String SETTING_VARIABLE = "Setting variable \"{0}\" to \"{1}\"...";
    public static final String SET_SUCCESSFULLY = "Variable \"{0}\" set successfully";
}
