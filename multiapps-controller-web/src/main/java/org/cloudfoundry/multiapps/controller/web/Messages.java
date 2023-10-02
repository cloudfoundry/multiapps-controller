package org.cloudfoundry.multiapps.controller.web;

/**
 * A collection of string constants used for exception and logging messages.
 */
public final class Messages {

    // Exception messages
    public static final String ERROR_EXECUTING_REST_API_CALL = "Error occurred while executing REST API call1";
    public static final String MAX_UPLOAD_SIZE_EXCEEDED = "Cannot upload file, size is bigger than the configured maximum upload size \"{0}\" bytes";
    public static final String COULD_NOT_GET_FILES_0 = "Could not get files: {0}";
    public static final String COULD_NOT_UPLOAD_FILE_0 = "Could not upload file: {0}";
    public static final String NO_FILES_TO_UPLOAD = "Request has no files to upload!";
    public static final String ACTION_0_CANNOT_BE_EXECUTED_OVER_OPERATION_1_IN_STATE_2 = "Action \"{0}\" cannot be executed over operation \"{1}\" in state \"{2}\".";
    public static final String OPERATION_0_NOT_FOUND = "Operation \"{0}\" was not found.";
    public static final String TEMPORARY_PROBLEM_WITH_PERSISTENCE_LAYER = "Temporary problem with persistence layer of the service";
    public static final String FILE_URL_RESPONSE_DID_NOT_RETURN_CONTENT_LENGTH = "File URL response did not return Content-Length header";
    public static final String ERROR_FROM_REMOTE_MTAR_ENDPOINT = "Error from remote MTAR endpoint {0} with status code {1}, message: {2}";
    public static final String MTAR_ENDPOINT_NOT_SECURE = "Remote MTAR endpoint is not a secure connection. HTTPS required";
    public static final String CANNOT_PARSE_CONTAINER_URI_OF_OBJECT_STORE = "Cannot parse container_uri of object store";
    public static final String UNSUPPORTED_SERVICE_PLAN_FOR_OBJECT_STORE = "Unsupported service plan for object store!";

    // Audit log messages

    // ERROR log messages
    public static final String MTA_NOT_FOUND = "MTA with id \"{0}\" does not exist";
    public static final String MTAS_NOT_FOUND_BY_NAME = "MTAs with name \"{0}\" do not exist";
    public static final String MTAS_NOT_FOUND_BY_NAMESPACE = "MTAs with namespace \"{0}\" do not exist";
    public static final String SPECIFIC_MTA_NOT_FOUND = "MTA with name \"{0}\" and namespace \"{1}\" does not exist";
    public static final String MTA_SEARCH_NOT_UNIQUE_BY_NAME = "There are multiple MTAs with name \"{0}\" found. Specify namespace";
    public static final String ORG_AND_SPACE_MUST_BE_SPECIFIED = "Org and space must be specified!";
    public static final String NOT_AUTHORIZED_TO_OPERATE_IN_ORGANIZATION_0_AND_SPACE_1 = "You are not authorized to perform operations in organization \"{0}\", space \"{0}\". You need the SpaceDeveloper role to operate in that space.";
    public static final String NOT_AUTHORIZED_TO_OPERATE_IN_SPACE_WITH_GUID_0 = "You are not authorized to perform operations in space with GUID \"{0}\". You need the SpaceDeveloper role to operate in that space.";
    public static final String FILE_SERVICE_CLEANUP_FAILED = "FileService: Failed to delete files without content. Reason: \"{0}\"";
    public static final String BASIC_AUTHENTICATION_IS_NOT_ENABLED_USE_OAUTH_2 = "Basic authentication is not enabled, use OAuth2";
    public static final String INVALID_AUTHENTICATION_PROVIDED = "Invalid authentication provided";
    public static final String NO_AUTHORIZATION_HEADER_WAS_PROVIDED = "No Authorization header was provided!";
    public static final String INVALID_AUTHORIZATION_HEADER_WAS_PROVIDED = "Invalid Authorization header was provided!";
    public static final String THE_TOKEN_HAS_EXPIRED_ON_0 = "The token has expired on: \"{0}\"";
    public static final String UNSUPPORTED_TOKEN_TYPE = "Unsupported token type: \"{0}\".";
    public static final String CLEARING_FLOWABLE_LOCK_OWNER_THREW_AN_EXCEPTION_0 = "Clearing Flowable lock owner on JVM shutdown threw an exception: {0}";
    public static final String FETCHING_FILE_FAILED = "Fetching file {0} in space {1} failed with: {2}";
    public static final String ASYNC_UPLOAD_JOB_FAILED = "Async upload job {0} failed with: {1}";

    // WARN log messages

    // INFO log messages
    public static final String ALM_SERVICE_ENV_INITIALIZED = "Deploy service environment initialized";
    public static final String STORING_TOKEN_FOR_USER_0_WHICH_EXPIRES_AT_1 = "Storing token for user \"{0}\" which expires at: {1}";
    public static final String FILE_SERVICE_DELETING_FILES = "FileService: Deleting files without content.";
    public static final String FILE_SERVICE_DELETED_FILES = "FileService: Deleted {0} files without content.";
    public static final String DATABASE_FOR_BINARIES_STORAGE = "Database will be used for binaries storage";
    public static final String OBJECTSTORE_FOR_BINARIES_STORAGE = "Objectstore will be used for binaries storage";
    public static final String CLEARING_LOCK_OWNER = "Clearing lock owner {0}...";
    public static final String CLEARED_LOCK_OWNER = "Cleared lock owner {0}";

    // DEBUG log messages
    public static final String RECEIVED_UPLOAD_REQUEST = "Received upload request on URI: {}";
    public static final String RECEIVED_UPLOAD_FROM_URL_REQUEST = "Received upload from URL {} request";
    public static final String UPLOADED_FILE = "Uploaded file \"{}\" with name {}, size {} and digest {} (algorithm {}) for {} ms.";
    public static final String ASYNC_UPLOAD_JOB_EXISTS = "Async upload job for URL {} exists: {}";
    public static final String CREATING_ASYNC_UPLOAD_JOB = "Creating async upload job for URL {} with ID: {}";
    public static final String ASYNC_UPLOAD_JOB_REJECTED = "Async upload job {} rejected. Deleting entry";
    public static final String STARTING_DOWNLOAD_OF_MTAR = "Starting download of MTAR from remote endpoint: {}";
    public static final String UPLOADED_MTAR_FROM_REMOTE_ENDPOINT = "Uploaded MTAR from remote endpoint {} in {} ms";
    public static final String ASYNC_UPLOAD_JOB_FINISHED = "Async upload job {} finished";
    public static final String UPLOADING_MTAR_STREAM_FROM_REMOTE_ENDPOINT = "Uploading MTAR stream from remote endpoint: {}";
    public static final String CALLING_REMOTE_MTAR_ENDPOINT = "Calling remote MTAR endpoint {}";

    private Messages() {
    }
}
