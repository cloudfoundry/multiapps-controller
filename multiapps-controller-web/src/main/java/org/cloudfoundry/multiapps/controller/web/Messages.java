package org.cloudfoundry.multiapps.controller.web;

/**
 * A collection of string constants used for exception and logging messages.
 */
public final class Messages {

    // Exception messages
    public static final String ERROR_EXECUTING_REST_API_CALL = "Error occurred while executing REST API call";
    public static final String MAX_UPLOAD_SIZE_EXCEEDED = "Cannot upload file, size is bigger than the configured maximum upload size \"{0}\" bytes";
    public static final String COULD_NOT_GET_FILES_0 = "Could not get files: {0}";
    public static final String COULD_NOT_UPLOAD_FILE_0 = "Could not upload file: {0}";
    public static final String ACTION_0_CANNOT_BE_EXECUTED_OVER_OPERATION_1_IN_STATE_2 = "Action \"{0}\" cannot be executed over operation \"{1}\" in state \"{2}\".";
    public static final String OPERATION_0_NOT_FOUND = "Operation \"{0}\" was not found.";
    public static final String TEMPORARY_PROBLEM_WITH_PERSISTENCE_LAYER = "Temporary problem with persistence layer of the service";
    public static final String FILE_URL_RESPONSE_DID_NOT_RETURN_CONTENT_LENGTH = "File URL response did not return Content-Length header";

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
    public static final String THE_TOKEN_HAS_EXPIRED_ON_0 = "The token has expired on: \"{0}\"";

    // WARN log messages

    // INFO log messages
    public static final String OAUTH_TOKEN_STORE = "Using OAuth token store \"{0}\"";
    public static final String ALM_SERVICE_ENV_INITIALIZED = "Deploy service environment initialized";
    public static final String CANNOT_AUTHENTICATE_WITH_CLOUD_CONTROLLER = "Cannot authenticate with cloud controller";
    public static final String ASYNC_DATABASE_CHANGES_WILL_NOT_BE_EXECUTED_ON_THIS_INSTANCE = "Async database changes will not be executed on instance {0}.";
    public static final String STORING_TOKEN_FOR_USER_0_WHICH_EXPIRES_AT_1 = "Storing token for user \"{0}\" which expires at: {1}";
    public static final String REGISTERED_0_AS_LIQUIBASE_LOCK_SERVICE = "Registered {0} as a Liquibase lock service.";
    public static final String FILE_SERVICE_DELETED_FILES = "FileService: Deleted {0} files without content.";

    // DEBUG log messages
    public static final String ERROR_STORING_TOKEN_DUE_TO_INTEGRITY_VIOLATION = "Cannot store access token due to data integrity violation. The exception is ignored as the token and authentication are persisted by another client";

    private Messages() {
    }
}