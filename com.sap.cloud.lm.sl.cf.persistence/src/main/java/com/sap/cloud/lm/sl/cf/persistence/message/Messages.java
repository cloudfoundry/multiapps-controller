package com.sap.cloud.lm.sl.cf.persistence.message;

/**
 * A collection of string constants used for exception and logging messages.
 */
public final class Messages {

    // Exception messages:
    public static final String FILE_UPLOAD_FAILED = "Upload of file \"{0}\" to \"{1}\" failed";
    public static final String FILE_NOT_FOUND = "File \"{0}\" not found";
    public static final String ERROR_CALCULATING_FILE_DIGEST = "Error calculating digest for file {0}: {1}";
    public static final String ERROR_FINDING_FILE_TO_UPLOAD = "Error finding file to upload with name {0}: {1}";
    public static final String ERROR_READING_FILE_CONTENT = "Error reading content of file {0}: {1}";
    public static final String FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST = "File with ID \"{0}\" and space \"{1}\" does not exist.";
    public static final String ERROR_DELETING_FILE_WITH_ID = "Error deleting file with ID \"{0}\"";
    public static final String ERROR_GETTING_FILES_WITH_SPACE_AND_NAMESPACE = "Error getting files with space {0} and namespace {1}";
    public static final String ERROR_GETTING_FILES_WITH_SPACE_NAMESPACE_AND_NAME = "Error getting files with space {0} namespace {1} and file name {2}";
    public static final String ERROR_GETTING_ALL_FILES = "Error getting all files";
    public static final String ERROR_DELETING_PROCESS_LOGS_WITH_NAMESPACE = "Error deleting process logs with namespace \"{0}\"";
    public static final String ERROR_DELETING_DIRECTORY = "Error deleting directory \"{0}\"";
    public static final String ERROR_STORING_LOG_FILE = "Error storing log file \"{0}\"";
    public static final String ERROR_LOG_FILE_NOT_FOUND = "Log file with name \"{0}\" for operation \"{1}\" in space \"{2}\" was not found";
    public static final String ERROR_CORRELATION_ID_OR_ACTIVITY_ID_NULL = "Unable to retrieve correlation id or activity id for process \"{0}\" at activity \"{1}\" and space \"{2}\"";

    // ERROR log messages:
    public static final String UPLOAD_STREAM_FAILED_TO_CLOSE = "Cannot close file upload stream";

    // WARN log messages:
    public static final String COULD_NOT_CLOSE_RESULT_SET = "Could not close result set.";
    public static final String COULD_NOT_CLOSE_STATEMENT = "Could not close statement.";
    public static final String COULD_NOT_CLOSE_CONNECTION = "Could not close connection.";
    public static final String COULD_NOT_ROLLBACK_TRANSACTION = "Could not rollback transaction!";
    public static final String COULD_NOT_PERSIST_LOGS_FILE = "Could not persist logs file: {0}";
    public static final String ATTEMPT_TO_UPLOAD_BLOB_FAILED = "Attempt [{0}/{1}] to upload blob to ObjectStore failed with \"{2}\"";
    public static final String ATTEMPT_TO_DOWNLOAD_MISSING_BLOB = "Attempt [{0}/{1}] to download missing blob {2} from ObjectStore";

    // INFO log messages:
    public static final String FAILED_TO_DELETE_FILE = "Failed to delete file {0}";
    public static final String CREATING_INDEX_CONCURRENTLY = "Creating index {0} concurrently";
    public static final String INDEX_CREATED = "Index created.";

    // DEBUG log messages:
    public static final String DELETING_FILE_WITH_PATH_0 = "Deleting file with path \"{0}\"...";
    public static final String DELETED_FILE_0_SUCCESSFULLY_1 = "Deleted file with path \"{0}\": {1}";
    public static final String STORING_FILE_TO_PATH_0 = "Storing file to path \"{0}\"...";
    public static final String STORED_FILE_0 = "Stored file: \"{0}\"";
    public static final String STORED_FILE_0_WITH_SIZE_1_SUCCESSFULLY_2 = "Stored file \"{0}\" with size {1}";
    public static final String DELETED_0_FILES_WITH_SPACE_1 = "Deleted {0} files with space \"{1}\".";
    public static final String DELETED_0_FILES_WITH_SPACE_1_AND_NAMESPACE_2 = "Deleted {0} files with space \"{1}\" and namespace \"{2}\".";
    public static final String DELETED_0_FILES_MODIFIED_BEFORE_1 = "Deleted {0} files modified before \"{1}\".";
    public static final String DELETED_0_FILES_WITH_ID_1_AND_SPACE_2 = "Deleted {0} files with ID \"{1}\" and space \"{2}\".";
    public static final String DELETED_0_FILES_WITHOUT_CONTENT = "Deleted {0} files without content.";
    public static final String PROCESSING_FILE_0 = "Processing file \"{0}\"...";

    protected Messages() {
    }

}
