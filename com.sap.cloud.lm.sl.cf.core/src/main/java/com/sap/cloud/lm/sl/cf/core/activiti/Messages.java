package com.sap.cloud.lm.sl.cf.core.activiti;

public class Messages {

    // Exception messages

    public static final String ABORT_OPERATION_TIMED_OUT = "Abort operation timed out";
    public static final String ACTIVITI_JOB_RETRY_FAILED = "Activiti job retry failed";
    public static final String PROCESS_STEP_NOT_REACHED_BEFORE_TIMEOUT = "Step \"{0}\" of process \"{1}\" not reached before timeout";
    public static final String MISSING_REQUIRED_PARAMETERS = "Service \"{0}\" has missing required parameters: {1}";
    // Warn messages

    public static final String RETRYING_PROCESS_ABORT = "Abort of process \"{0}\" failed due to an optimistic locking exception. Retrying abort...";
    public static final String ERROR_REMOVING_HISTORIC_PROCESS_INSTANCE_WITH_ID_0 = "Error removing historic process instance with id {0}";
    public static final String ERROR_REMOVING_PROCESS_INSTANCE_WITH_ID_0 = "Error removing process instance with id {0}";

    // Debug messages

    public static final String SETTING_VARIABLE = "Setting variable \"{0}\" to \"{1}\"...";
    public static final String SET_SUCCESSFULLY = "Variable \"{0}\" set successfully";

    // ERROR log messages:

    public static final String ERROR_DELETING_PROGRESS_MESSAGE = "Error deleting progress message";
}
