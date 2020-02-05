package com.sap.cloud.lm.sl.cf.core.message;

/**
 * A collection of string constants used for exception and logging messages.
 */
public final class Messages {

    // Exception messages
    public static final String NO_VALID_TOKEN_FOUND = "No valid access token was found for user \"{0}\"";
    public static final String TOKEN_EXPIRED = "The access token associated with user \"{0}\" has expired";
    public static final String CANT_CREATE_CLIENT = "Could not create client";
    public static final String CANT_CREATE_CLIENT_2 = "Could not create client in organization \"{0}\" and space \"{1}\"";
    public static final String CANT_CREATE_CLIENT_FOR_SPACE_ID = "Could not create client in space with guid \"{0}\"";
    public static final String UNAUTHORISED_OPERATION_ORG_SPACE = "Not authorized to perform operation \"{0}\" in organization \"{1}\" and space \"{2}\"";
    public static final String UNAUTHORISED_OPERATION_SPACE_ID = "Not authorized to perform operation \"{0}\" in space with ID \"{1}\"";
    public static final String PERMISSION_CHECK_FAILED_ORG_SPACE = "Could not check for permission to perform operation \"{0}\" in organization \"{1}\" and space \"{2}\"";
    public static final String PERMISSION_CHECK_FAILED_SPACE_ID = "Could not check for permission to perform operation \"{0}\" in space with ID \"{1}\"";
    public static final String ROUTE_NOT_FOUND = "Could not find route \"{0}\"";
    public static final String OPERATION_NOT_FOUND = "MTA operation with ID \"{0}\" does not exist";
    public static final String OPERATION_SPACE_MISMATCH = "MTA operation with ID \"{0}\" exists in space \"{1}\" but was queried from space \"{2}\"";
    public static final String OPERATION_ALREADY_EXISTS = "MTA operation with ID \"{0}\" already exists";
    public static final String PATH_SHOULD_NOT_BE_ABSOLUTE = "Path \"{0}\" should not be absolute";
    public static final String PATH_SHOULD_BE_NORMALIZED = "Path \"{0}\" should be normalized (should not contain any \".\", \"..\" or \"//\" path segments)!";
    public static final String PATH_MUST_NOT_CONTAIN_WINDOWS_SEPARATORS = "Path \"{0}\" must use only Unix separators \"/\", but contains Windows separators \"\\\"";
    public static final String UNRESOLVED_MTA_MODULES = "Unresolved MTA modules {0}, these modules are neither part of MTA archive, nor already deployed";
    public static final String UNRESOLVED_MODULE_DEPENDENCIES = "Modules {0} have dependencies which are neither part of MTA archive, nor already deployed";
    public static final String COULD_NOT_CREATE_VALID_DOMAIN = "Could not create a valid domain from \"{0}\"";
    public static final String COULD_NOT_CREATE_VALID_HOST = "Could not create a valid host from \"{0}\"";
    public static final String COULD_NOT_CREATE_VALID_ROUTE = "Could not create a valid route from \"{0}\"";
    public static final String COULD_NOT_CREATE_VALID_APPLICATION_NAME_FROM_0 = "Could not create a valid application name from \"{0}\"";
    public static final String COULD_NOT_CREATE_VALID_SERVICE_NAME_FROM_0 = "Could not create a valid service name from \"{0}\"";
    public static final String COULD_NOT_PARSE_ROUTE = "Cannot parse 'routes' property - check documentation for correct format.";
    public static final String COULD_NOT_CREATE_VALID_ROUTE_NESTED_EXCEPTION = "Invalid route \"{0}\" : {1}";
    public static final String CANNOT_CORRECT_PARAMETER = "Value for parameter \"{0}\" is not valid and cannot be corrected";
    public static final String CONFIGURATION_ENTRY_ALREADY_EXISTS = "Configuration entry with namespace ID \"{0}\", ID \"{1}\", version \"{2}\", target org \"{3}\" and target space \"{4}\", already exists";
    public static final String CONFIGURATION_SUBSCRIPTION_ALREADY_EXISTS = "Configuration subscription for MTA \"{0}\", app \"{1}\" and resource \"{2}\" already exists in space \"{3}\"";
    public static final String CONFIGURATION_ENTRY_NOT_FOUND = "Configuration entry with ID \"{0}\" does not exist";
    public static final String CONFIGURATION_ENTRY_SATISFYING_VERSION_AND_VIS_NOT_FOUND = "Configuration entry that satisfies version requirement \"{0}\" and/or visibility targets in format ('org', 'space'): (\"{1}\") has not been found";
    public static final String CONFIGURATION_SUBSCRIPTION_NOT_FOUND = "Configuration subscription with ID \"{0}\" does not exist";
    public static final String CONFIGURATION_SUBSCRIPTION_MATCHING_ENTRIES_NOT_FOUND_BY_QUERY = "Configuration subscription that matches the specified entries could not be found";
    public static final String NO_CONFIGURATION_ENTRIES_WERE_FOUND = "No configuration entries were found matching the filter specified in resource \"{0}\"";
    public static final String MULTIPLE_CONFIGURATION_ENTRIES_WERE_FOUND = "Multiple configuration entries were found matching the filter specified in resource \"{0}\"";
    public static final String CONFLICTING_APP_COLORS = "There are both blue and green applications already deployed for MTA \"{0}\"";
    public static final String UNABLE_TO_PARSE_SUBSCRIPTION = "Unable to parse configuration subscription: {0}";
    public static final String COLUMN_VALUE_SHOULD_NOT_BE_NULL = "Configuration subscription''s \"{0}\" column value should not be null";
    public static final String COULD_NOT_COMPUTE_SPACE_ID = "Could not compute space ID for org \"{0}\" and space \"{1}\"";
    public static final String THE_DEPLOYMENT_DESCRIPTOR_0_SPECIFIES_NOT_SUPPORTED_MTA_VERSION_1 = "The deployment descriptor \"{0}\" specifies a non-supported MTA version \"{1}\"";
    public static final String CANNOT_CLEAN_MULTI_TARGET_APP_ASSEMBLY_TARGET_DIR_0 = "Cannot clean multi-target app assembly target dir \"{0}\"";
    public static final String FAILED_TO_COPY_FILE_0_TO_ASSEMBLY_DIRECTORY = "Failed to copy file \"{0}\" to assembly directory";
    public static final String PATH_IS_RESOLVED_TO_NOT_EXISTING_FILE = "Path \"{0}\" is resolved to a non-existing file \"{1}\"";
    public static final String FAILED_TO_READ_DEPLOYMENT_DESCRIPTOR_0 = "Failed to read deployment descriptor \"{0}\"";
    public static final String DIRECTORY_0_DOES_NOT_CONTAIN_MANDATORY_DEPLOYMENT_DESCRIPTOR_FILE_1 = "Directory \"{0}\" does not contain mandatory deployment descriptor file \"{1}\"";
    public static final String FAILED_TO_LIST_MULTI_TARGET_APP_DIRECTORY_0 = "Failed to list multi-target app directory \"{0}\"";
    public static final String CANNOT_SHORTEN_NAME_TO_N_CHARACTERS = "Cannot shorten name \"{0}\" to {1} characters";
    public static final String ERROR_GETTING_APPLICATIONS = "Error getting Cloud Foundry applications";
    public static final String COULD_NOT_PARSE_ATTRIBUTES_OF_APP_0 = "Could not parse attributes of application \"{0}\"";
    public static final String ATTRIBUTE_0_OF_APP_1_IS_OF_TYPE_2_INSTEAD_OF_3 = "Attribute \"{0}\" of application \"{1}\" is of type {2} instead of {3}!";
    public static final String ILLEGAL_DESIRED_STATE = "Illegal desired application state: {0}";
    public static final String ILLEGAL_SERVICE_OPERATION_STATE = "Illegal service operation state: {0}";
    public static final String ILLEGAL_SERVICE_OPERATION_TYPE = "Illegal service operation type: {0}";
    public static final String SERVICE_ALREADY_EXISTS = "Service \"{0}\" already exists.";
    public static final String NO_SERVICE_PLAN_FOUND = "Could not create service instance \"{0}\". Service plan \"{1}\" from service offering \"{2}\" was not found.";
    public static final String EMPTY_SERVICE_PLANS_LIST_FOUND = "An empty service plans list was found for service \"{0}\"";
    public static final String UNABLE_TO_PARSE_MEMORY_STRING_0 = "Unable to parse memory string \"{0}\"";
    public static final String CANT_CREATE_SERVICE_NOT_MATCHING_OFFERINGS_OR_PLAN = "Service \"{0}\" could not be created because none of the service offering(s) \"{1}\" match with existing service offerings or provide service plan \"{2}\"";
    public static final String CANT_CREATE_SERVICE = "Service \"{0}\" could not be created because all attempt(s) to use service offerings \"{1}\" failed";
    public static final String CANT_PARSE_MTA_METADATA_VERSION_FOR_0 = "Cannot parse version from MTA metadata for \"{0}\". This indicates that MTA reserved variables in the entity's metadata were modified manually. Either revert the changes or delete the entity.";
    public static final String CANT_PARSE_MTA_ENV_METADATA_VERSION_FOR_APP_0 = "Cannot parse version from MTA metadata for application \"{0}\". This indicates that MTA reserved variables in the application's environment were modified manually. Either revert the changes or delete the application.";
    public static final String MTA_METADATA_FOR_APP_0_IS_INCOMPLETE = "MTA metadata for application \"{0}\" is incomplete. This indicates that MTA reserved variables in the application's environment were modified manually. Either revert the changes or delete the application.";
    public static final String MTA_METADATA_FOR_0_IS_INCOMPLETE = "MTA metadata for entity \"{0}\" is incomplete. This indicates that MTA reserved variables in the entity's metadata were modified manually. Either revert the changes or delete the entity.";
    public static final String ENV_OF_APP_0_CONTAINS_INVALID_VALUE_FOR_1 = "The environment of application \"{0}\" contains an invalid value for \"{0}\". This indicates that MTA reserved variables in the application's environment were modified manually. Either revert the changes or delete the application.";
    public static final String METADATA_OF_0_CONTAINS_INVALID_VALUE_FOR_1 = "The metadata of \"{0}\" contains an invalid value for \"{0}\". This indicates that MTA reserved variables in the entity's metadata were modified manually. Either revert the changes or delete the entity.";
    public static final String COULD_NOT_DELETE_SPACE_LEFTOVERS = "Could not delete space leftovers";
    public static final String SERVICE_MISSING_REQUIRED_PARAMETER = "Service \"{0}\" has missing required parameter: {1}";
    public static final String CONTROLLER_URL_NOT_SPECIFIED = "Controller URL is not specified in the environment.";
    public static final String INVALID_CONTROLLER_URL = "Invalid controller URL \"{0}\".";
    public static final String ERROR_SERVICE_INSTANCE_RESPONSE_WITH_MISSING_FIELD = "The response of finding a service instance should contain a \"{0}\" element";
    public static final String ERROR_SERVICE_INSTANCE_RESPONSE_WITH_MORE_THEN_ONE_RESULT = "The response of finding a service instance should not have more than one resource element";
    public static final String MISSING_GLOBAL_AUDITOR_CREDENTIALS = "Global Auditor credentials are missing from the application ENV.";
    public static final String PROGRESS_MESSAGE_NOT_FOUND = "Progress message with ID \"{0}\" does not exist";
    public static final String PROGRESS_MESSAGE_ALREADY_EXISTS = "Progress message for process \"{0}\" with ID \"{1}\" already exist";
    public static final String HISTORIC_OPERATION_EVENT_NOT_FOUND = "Historic operation event with ID \"{0}\" does not exist";
    public static final String HISTORIC_OPERATION_EVENT_ALREADY_EXISTS = "Historic operation event for process \"{0}\" with ID \"{1}\" already exist";

    // Warning messages
    public static final String ENVIRONMENT_VARIABLE_IS_NOT_SET_USING_DEFAULT = "Environment variable \"{0}\" is not set. Using default \"{1}\"...";
    public static final String OPTIONAL_RESOURCE_IS_NOT_SERVICE = "Optional resource \"{0}\" it will be not created because it's not a service";
    public static final String SERVICE_IS_NOT_ACTIVE = "Service \"{0}\" is inactive and will not be processed";

    public static final String INVALID_VCAP_APPLICATION = "Invalid VCAP_APPLICATION \"{0}\"";
    public static final String IGNORING_LABEL_FOR_USER_PROVIDED_SERVICE = "Ignoring label \"{0}\" for service \"{1}\", as user-provided services do not support labels!";
    public static final String NOT_DESCRIBED_MODULE = "MTA module \"{0}\" is found deployed, but it is not part of MTA manifest file";
    public static final String COULD_NOT_PARSE_PROVIDED_DEPENDENCY_NAMES_1_OF_APP_0 = "Could not parse provided dependency names of application \"{0}\". Assuming that they're written in the \"old\" format. This is what they look like: {1}";
    public static final String SPACE_GUID_NOT_SPECIFIED_USING_DEFAULT_0 = "Space GUID is not specified in the environment. Using default: \"{0}\"";
    public static final String ORG_NAME_NOT_SPECIFIED = "Org name is not specified in the environment.";
    public static final String DEPLOY_SERVICE_URL_NOT_SPECIFIED = "Deploy service URL is not specified in the environment.";
    public static final String INVALID_SUPPORT_COMPONENTS = "Invalid SUPPORT_COMPONENTS \"{0}\"";
    public static final String INCOMPATIBLE_PARAMETERS = "Module \"{0}\" has parameters {1} that will be replaced by \"{2}\" due to inconsistency";

    // Info messages
    public static final String PLATFORMS_NOT_SPECIFIED = "No platforms are specified in the environment.";
    public static final String ATTEMPTING_TO_RELEASE_STUCK_LOCK = "Change log lock is presumed to be stuck. Attempting to release it...";
    public static final String CURRENT_LOCK = "Change log lock was acquired at {0} by {1}";
    public static final String CURRENT_DATE = "The current date is {0}";
    public static final String DROPPED_UNNAMED_UNIQUE_CONSTRAINT_FOR_CONFIGURATION_REGISTRY = "Dropped unnamed unique constraint for configuration registry.";
    public static final String ALTERED_DATA_TYPES_FOR_OPERATION_TABLE = "Altered data types for 'started_at' and 'ended_at' columns in 'operation' table to 'timestamp'.";
    public static final String SPLIT_TARGET_SPACE_COLUMN = "Split target space column in configuration registry";
    public static final String TRANSFORMED_FILTER_COLUMN = "Transformed filter column in configuration subscription table";

    public static final String CONTROLLER_URL = "Controller URL: {0}";
    public static final String PLATFORM = "Platform: {0}";
    public static final String MAX_UPLOAD_SIZE = "Max upload size: {0}";
    public static final String MAX_MTA_DESCRIPTOR_SIZE = "Max mta descriptor size: {0}";
    public static final String MAX_MANIFEST_SIZE = "Max manifest size is set to: {0}";
    public static final String MAX_RESOURCE_FILE_SIZE = "Max resource file size is set to: {0}";
    public static final String CRON_EXPRESSION_FOR_OLD_DATA = "Cron expression for old data: {0}";
    public static final String MAX_TTL_FOR_OLD_DATA = "Max TTL for old data: {0}";
    public static final String SPACE_GUID = "Space GUID: {0}";
    public static final String ORG_NAME = "Org name: {0}";
    public static final String DUMMY_TOKENS_ENABLED = "Dummy tokens enabled: {0}";
    public static final String BASIC_AUTH_ENABLED = "Basic authentication enabled: {0}";
    public static final String GLOBAL_AUDITOR_USERNAME = "Global Auditor username: {0}";
    public static final String USE_XS_AUDIT_LOGGING = "Use XSA audit logging: {0}";
    public static final String DB_CONNECTION_THREADS = "Database connection thread pool size: {0}";
    public static final String STEP_POLLING_INTERVAL_IN_SECONDS = "Step polling interval in seconds: {0}";
    public static final String SKIP_SSL_VALIDATION = "Skip SSL validation: {0}";
    public static final String DS_VERSION = "Deploy service version: {0}";
    public static final String CHANGE_LOG_LOCK_POLL_RATE = "Change log lock poll rate: {0}";
    public static final String CHANGE_LOG_LOCK_DURATION = "Change log lock duration: {0}";
    public static final String CHANGE_LOG_LOCK_ATTEMPTS = "Change log lock attempts: {0}";
    public static final String GLOBAL_CONFIG_SPACE = "Global config space: {0}";
    public static final String GATHER_STATISTICS = "Gather statistics: {0}";
    public static final String HEALTH_CHECK_CONFIGURATION = "Health check configuration: {0}";
    public static final String MAIL_API_URL = "Mail API URL: {0}";
    public static final String APPLICATION_GUID = "Application GUID: {0}";
    public static final String APPLICATION_INSTANCE_INDEX = "Application instance index: {0}";
    public static final String AUDIT_LOG_CLIENT_CORE_THREADS = "Audit log client core threads: {0}";
    public static final String AUDIT_LOG_CLIENT_MAX_THREADS = "Audit log client max threads: {0}";
    public static final String AUDIT_LOG_CLIENT_QUEUE_CAPACITY = "Audit log client queue capacity: {0}";
    public static final String AUDIT_LOG_CLIENT_KEEP_ALIVE = "Audit log client keep alive: {0}";
    public static final String FLOWABLE_JOB_EXECUTOR_CORE_THREADS = "Flowable job executor core threads: {0}";
    public static final String FLOWABLE_JOB_EXECUTOR_MAX_THREADS = "Flowable job executor max threads: {0}";
    public static final String FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY = "Flowable job executor queue capacity: {0}";

    public static final String AUDIT_LOG_ABOUT_TO_PERFORM_ACTION = "About to perform action \"{0}\"";
    public static final String AUDIT_LOG_ABOUT_TO_PERFORM_ACTION_WITH_PARAMS = "About to perform action \"{0}\" with parameters \"{1}\"";
    public static final String AUDIT_LOG_ACTION_SUCCESS = "Successfully performed action \"{0}\"";
    public static final String AUDIT_LOG_ACTION_FAILURE = "Failed to perform action \"{0}\"";
    public static final String AUDIT_LOGGING_FAILED = "Failed to write message to the audit log";
    public static final String AUDIT_LOG_CONFIG = "Deploy service configuration \"{0}\": {1}";
    public static final String AUDIT_LOG_UPDATE_CONFIG = "Updating configuration \"{0}\"";
    public static final String AUDIT_LOG_CREATE_CONFIG = "Creating configuration \"{0}\" \"{1}\"";
    public static final String AUDIT_LOG_DELETE_CONFIG = "Deleting configuration \"{0}\"";
    public static final String AUDIT_LOG_CONFIG_UPDATED = "Configuration was updated";
    public static final String AUDIT_LOG_CONFIG_UPDATE_FAILED = "Configuration update failed";
    public static final String PURGING_SUBSCRIPTIONS = "Purging configuration subscriptions for target: {0}";
    public static final String PURGING_ENTRIES = "Purging entries for target: {0}";
    public static final String PURGE_DELETE_REQUEST_SPACE_FROM_CONFIGURATION_TABLES = "All delete request spaces after date: {0} will be deleted from configuration tables.";
    public static final String RETRIEVED_TOKEN_FOR_USER_0_WITH_EXPIRATION_TIME_1 = "Retrieved token for user \"{0}\" with expiration time: {1} seconds";
    public static final String FSS_CACHE_UPDATE_TIMEOUT = "Fss cache update timeout: {0} minutes";
    public static final String THREAD_MONITOR_CACHE_TIMEOUT = "Flowable thread monitor cache timeout: {0} seconds";
    public static final String SPACE_DEVELOPERS_CACHE_TIME_IN_SECONDS = "Cache for list of space developers per SpaceGUID: {0} seconds";
    public static final String APP_SHUTDOWN_REQUEST = "Application with id:\"{0}\", instance id:\"{1}\", instance index:\"{2}\", is requested to shutdown. Timeout to wait before shutdown of Flowable job executor:\"{3}\" seconds.";
    public static final String APP_SHUTDOWNED = "Application with id:\"{0}\", instance id:\"{1}\", instance index:\"{2}\", is shutdowned. Timeout to wait before shutdown of Flowable job executor:\"{3}\" seconds.";
    public static final String APP_SHUTDOWN_STATUS_MONITOR = "Monitor shutdown status of application with id:\"{0}\", instance id:\"{1}\", instance index:\"{2}\". Status:\"{3}\".";
    public static final String CONTROLLER_CLIENT_SSL_HANDSHAKE_TIMEOUT_IN_SECONDS = "Controller client SSL handshake timeout in seconds: {0}";
    public static final String CONTROLLER_CLIENT_CONNECT_TIMEOUT_IN_SECONDS = "Controller client connect timeout in seconds: {0}";
    public static final String CONTROLLER_CLIENT_CONNECTION_POOL_SIZE = "Controller client connection pool size: {0}";
    public static final String CONTROLLER_CLIENT_THREAD_POOL_SIZE = "Controller client thread pool size: {0}";

    // Debug messages
    public static final String DEPLOYMENT_DESCRIPTOR = "Deployment descriptor: {0}";
    public static final String MERGED_DESCRIPTOR = "Merged deployment descriptor: {0}";
    public static final String DELETING_SUBSCRIPTION = "Deleting configuration subscription: {0}";
    public static final String DELETING_ENTRY = "Deleting configuration entry: {0}";
    public static final String CERTIFICATE_CN = "Certificate CN: \"{0}\"";

    private Messages() {
    }
}
