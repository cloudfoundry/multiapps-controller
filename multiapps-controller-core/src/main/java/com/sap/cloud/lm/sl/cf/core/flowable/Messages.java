package com.sap.cloud.lm.sl.cf.core.flowable;

public class Messages {

    // Exception messages

    public static final String ABORT_OPERATION_TIMED_OUT = "Abort operation timed out";
    public static final String FLOWABLE_JOB_RETRY_FAILED = "Flowable job retry failed";
    public static final String PROCESS_STEP_NOT_REACHED_BEFORE_TIMEOUT = "Step \"{0}\" of process \"{1}\" not reached before timeout";

    // Warn messages

    public static final String RETRYING_PROCESS_ABORT = "Abort of process \"{0}\" failed due to an optimistic locking exception. Retrying abort...";
    public static final String ERROR_REMOVING_HISTORIC_PROCESS_INSTANCE_WITH_ID_0 = "Error removing historic process instance with id {0}";
    public static final String ERROR_REMOVING_PROCESS_INSTANCE_WITH_ID_0 = "Error removing process instance with id {0}";

    // Debug messages

    public static final String SETTING_VARIABLE = "Setting variable \"{0}\" to \"{1}\"...";
    public static final String SET_SUCCESSFULLY = "Variable \"{0}\" set successfully";
    public static final String SETTING_SECONDS_TO_WAIT_BEFORE_FLOWABLE_JOB_EXECUTOR_SHUTDOWN = "Setting \"{0}\" seconds to wait before shutdown of Flowable job executor...";
    public static final String SHUTTING_DOWN_FLOWABLE_JOB_EXECUTOR = "Shutting down Flowable job executor...";

    // ERROR log messages:

    public static final String ERROR_DELETING_PROGRESS_MESSAGE = "Error deleting progress message";
}
