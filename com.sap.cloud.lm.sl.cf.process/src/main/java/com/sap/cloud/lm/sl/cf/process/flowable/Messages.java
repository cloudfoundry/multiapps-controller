package com.sap.cloud.lm.sl.cf.process.flowable;

public class Messages {

    private Messages() {
    }

    // Info messages

    public static final String NEW_ACTIVE_EXECUTIONS_WITHOUT_CHILDREN_0_FOR_PROCESS_1 = "New active executions without children \"{0}\" for process: \"{1}\"";
    public static final String TIMEOUT_OF_0_FOR_PROCESS_1_HAS_BEEN_REACHED = "Timeout of \"{0}\" for process: \"{1}\" has been reached";
    public static final String CURRENT_EXECUTION_WITHOUT_CHILDREN_0 = "Current execution without children: \"{0}\"";
    public static final String PARENT_EXECUTION_WILL_NOT_BE_WAITED_TO_FINISH = "Parent execution: \"{0}\" will not be waited to finish";
    public static final String TIMER_EXECUTION_WILL_NOT_BE_WAITED_TO_FINISH = "Timer execution: \"{0}\" will not be waited to finish";
    public static final String EXECUTION_0_DOES_NOT_HAVE_DEAD_LETTER_JOBS = "Execution: \"{0}\" does not have dead letter jobs";
    public static final String PROCESS_0_HAS_BEEN_SUSPENDED_SUCCESSFULLY = "Process \"{0}\" has been suspended successfully";
    public static final String PROCESS_0_HAS_BEEN_DELETED_SUCCESSFULLY = "Process \"{0}\" has been deleted successfully";
    public static final String PROCESS_0_HAS_ALREADY_BEEN_SUSPENDED = "Process: \"{0}\" has already been suspended";
    public static final String PROCESS_INSTANCE_0_IS_AT_RECEIVE_TASK = "Process instance: \"{0}\" is at receive task";
    public static final String NO_DEAD_LETTER_JOBS_FOUND_FOR_PROCESS_WITH_ID_0 = "No dead letter jobs found for process with id \"{0}\"";
    public static final String DEAD_LETTER_JOBS_FOR_PROCESS_0_1 = "Dead letter jobs for process: \"{0}\": \"{1}\"";


    // Exception messages

    public static final String ABORT_OPERATION_FOR_PROCESS_0_FAILED = "Abort operation for process: \"{0}\" failed";
    public static final String ABORT_OPERATION_FAILED = "Abort operation failed: {0}";
    public static final String FLOWABLE_JOB_RETRY_FAILED = "Flowable job retry failed";
    public static final String PROCESS_STEP_NOT_REACHED_BEFORE_TIMEOUT = "Step \"{0}\" of process \"{1}\" not reached before timeout";
    public static final String PROCESS_WITH_ID_0_NOT_FOUND_WHILE_WAITING_FOR_EXECUTION_1_TO_FINISH = "Process with id: \"{0}\" not found while waiting for execution: \"{1}\" to finish";

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
