package org.cloudfoundry.multiapps.controller.persistence;

/**
 * A collection of string constants used for exception and logging messages.
 */
public final class Messages {

    // Exception messages:
    public static final String FILE_UPLOAD_FAILED = "Upload of file \"{0}\" to \"{1}\" failed";
    public static final String FILE_NOT_FOUND = "File \"{0}\" not found";
    public static final String ERROR_FINDING_FILE_TO_UPLOAD = "Error finding file to upload with name {0}: {1}";
    public static final String ERROR_READING_FILE_CONTENT = "Error reading content of file {0}: {1}";
    public static final String FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST = "File with ID \"{0}\" and space \"{1}\" does not exist.";
    public static final String ERROR_DELETING_FILE_WITH_ID = "Error deleting file with ID \"{0}\"";
    public static final String ERROR_GETTING_FILES_WITH_SPACE_AND_NAMESPACE = "Error getting files with space {0} and namespace {1}";
    public static final String ERROR_GETTING_FILES_WITH_SPACE_NAMESPACE_AND_NAME = "Error getting files with space {0} namespace {1} and file name {2}";
    public static final String ERROR_GETTING_ALL_FILES = "Error getting all files";
    public static final String ERROR_DELETING_PROCESS_LOGS_WITH_NAMESPACE = "Error deleting process logs with namespace \"{0}\"";
    public static final String ERROR_DELETING_DIRECTORY = "Error deleting directory \"{0}\"";
    public static final String ERROR_DELETING_DIRECTORIES = "Error deleting directires {0}";
    public static final String ERROR_STORING_LOG_FILE = "Error storing log file \"{0}\"";
    public static final String ERROR_LOG_FILE_NOT_FOUND = "Log file with name \"{0}\" for operation \"{1}\" in space \"{2}\" was not found";
    public static final String ERROR_CORRELATION_ID_OR_ACTIVITY_ID_NULL = "Unable to retrieve correlation id or activity id for process \"{0}\" at activity \"{1}\" and space \"{2}\"";
    public static final String COLUMN_VALUE_SHOULD_NOT_BE_NULL = "Configuration subscription''s \"{0}\" column value should not be null";
    public static final String ERROR_WHILE_EXECUTING_TRANSACTION = "Error while executing database transaction \"{0}\"";
    public static final String PROGRESS_MESSAGE_NOT_FOUND = "Progress message with ID \"{0}\" does not exist";
    public static final String PROGRESS_MESSAGE_ALREADY_EXISTS = "Progress message for process \"{0}\" with ID \"{1}\" already exist";
    public static final String HISTORIC_OPERATION_EVENT_NOT_FOUND = "Historic operation event with ID \"{0}\" does not exist";
    public static final String HISTORIC_OPERATION_EVENT_ALREADY_EXISTS = "Historic operation event for process \"{0}\" with ID \"{1}\" already exist";
    public static final String CONFIGURATION_ENTRY_ALREADY_EXISTS = "Configuration entry with nid \"{0}\", ID \"{1}\", version \"{2}\", namespace \"{3}\", target org \"{4}\" and target space \"{5}\", already exists";
    public static final String CONFIGURATION_ENTRY_NOT_FOUND = "Configuration entry with ID \"{0}\" does not exist";
    public static final String CONFIGURATION_ENTRY_SATISFYING_VERSION_AND_VIS_NOT_FOUND = "Configuration entry that satisfies version requirement \"{0}\" and/or visibility targets in format ('org', 'space'): (\"{1}\") has not been found";
    public static final String CONFIGURATION_SUBSCRIPTION_ALREADY_EXISTS = "Configuration subscription for MTA \"{0}\", app \"{1}\" and resource \"{2}\" already exists in space \"{3}\"";
    public static final String CONFIGURATION_SUBSCRIPTION_NOT_FOUND = "Configuration subscription with ID \"{0}\" does not exist";
    public static final String CONFIGURATION_SUBSCRIPTION_MATCHING_ENTRIES_NOT_FOUND_BY_QUERY = "Configuration subscription that matches the specified entries could not be found";
    public static final String UNABLE_TO_PARSE_SUBSCRIPTION = "Unable to parse configuration subscription: {0}";
    public static final String OPERATION_NOT_FOUND = "MTA operation with ID \"{0}\" does not exist";
    public static final String OPERATION_ALREADY_EXISTS = "MTA operation with ID \"{0}\" already exists";
    public static final String ACCESS_TOKEN_NOT_FOUND = "Access token with ID \"{0}\" does not exist";
    public static final String ACCESS_TOKEN_ALREADY_EXISTS = "Access token with ID \"{0}\" already exist";
    public static final String LOCK_OWNER_NOT_FOUND = "Lock owner entry with ID \"{0}\" not found";
    public static final String LOCK_OWNER_ALREADY_EXISTS = "Lock owner entry with ID \"{0}\" already exists";
    public static final String INVALID_KEY_FORMAT = "Invalid key format: {0}";
    public static final String GENERATING_KEY_FILE_FAILED = "Generating key failed: {0}";
    public static final String ASYNC_UPLOAD_JOB_NOT_FOUND = "Async upload job entry with ID \"{0}\" not found";
    public static final String ASYNC_UPLOAD_JOB_ALREADY_EXISTS = "Async upload job entry with ID \"{0}\" already exists";

    // ERROR log messages:
    public static final String UPLOAD_STREAM_FAILED_TO_CLOSE = "Cannot close file upload stream";

    // WARN log messages:
    public static final String COULD_NOT_CLOSE_RESULT_SET = "Could not close result set.";
    public static final String COULD_NOT_CLOSE_STATEMENT = "Could not close statement.";
    public static final String COULD_NOT_CLOSE_CONNECTION = "Could not close connection.";
    public static final String COULD_NOT_CLOSE_LOGGER_CONTEXT = "Could not close logger context";
    public static final String COULD_NOT_ROLLBACK_TRANSACTION = "Could not rollback transaction!";
    public static final String COULD_NOT_PERSIST_LOGS_FILE = "Could not persist logs file: {0}";
    public static final String ATTEMPT_TO_UPLOAD_BLOB_FAILED = "Attempt [{0}/{1}] to upload blob to ObjectStore failed with \"{2}\"";
    public static final String ATTEMPT_TO_DOWNLOAD_MISSING_BLOB = "Attempt [{0}/{1}] to download missing blob {2} from ObjectStore";

    // INFO log messages:
    public static final String DEFAULT_CONSOLE = "DefaultConsole";

    // DEBUG log messages:
    public static final String STORED_FILE_0 = "Stored file: \"{0}\"";
    public static final String STORED_FILE_0_WITH_SIZE_1 = "Stored file \"{0}\" with size {1}";
    public static final String DELETED_0_FILES_WITH_SPACEIDS_1 = "Deleted {0} files with space ids \"{1}\".";
    public static final String DELETED_0_FILES_WITH_SPACE_1_AND_NAMESPACE_2 = "Deleted {0} files with space \"{1}\" and namespace \"{2}\".";
    public static final String DELETED_0_FILES_MODIFIED_BEFORE_1 = "Deleted {0} files modified before \"{1}\".";
    public static final String DELETED_0_FILES_WITH_ID_1_AND_SPACE_2 = "Deleted {0} files with ID \"{1}\" and space \"{2}\".";
    public static final String DELETED_0_FILES_WITHOUT_CONTENT = "Deleted {0} files without content.";

    protected Messages() {
    }

}
