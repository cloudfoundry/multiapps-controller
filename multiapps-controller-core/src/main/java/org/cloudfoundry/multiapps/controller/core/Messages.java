package org.cloudfoundry.multiapps.controller.core;

/**
 * A collection of string constants used for exception and logging messages.
 */
public final class Messages {

    // Exception messages
    public static final String NO_VALID_TOKEN_FOUND = "No valid access token was found for user guid \"{0}\"";
    public static final String CANT_CREATE_CLIENT = "Could not create client";
    public static final String CANT_CREATE_CLIENT_FOR_SPACE_ID = "Could not create client in space with guid \"{0}\"";
    public static final String UNAUTHORISED_OPERATION_ORG_SPACE = "Not authorized to perform operation \"{0}\" in organization \"{1}\" and space \"{2}\"";
    public static final String UNAUTHORISED_OPERATION_SPACE_ID = "Not authorized to perform operation \"{0}\" in space with ID \"{1}\"";
    public static final String PERMISSION_CHECK_FAILED_ORG_SPACE = "Could not check for permission to perform operation \"{0}\" in organization \"{1}\" and space \"{2}\"";
    public static final String PERMISSION_CHECK_FAILED_SPACE_ID = "Could not check for permission to perform operation \"{0}\" in space with ID \"{1}\"";
    public static final String ROUTE_NOT_FOUND = "Could not find route \"{0}\"";
    public static final String OPERATION_SPACE_MISMATCH = "MTA operation with ID \"{0}\" exists in space \"{1}\" but was queried from space \"{2}\"";
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
    public static final String NO_CONFIGURATION_ENTRIES_WERE_FOUND = "No configuration entries were found matching the filter specified in resource \"{0}\"";
    public static final String MULTIPLE_CONFIGURATION_ENTRIES_WERE_FOUND = "Multiple configuration entries were found matching the filter specified in resource \"{0}\"";
    public static final String CONFLICTING_APP_COLORS = "There are both blue and green applications already deployed for MTA \"{0}\"";
    public static final String COULD_NOT_COMPUTE_SPACE_ID = "Could not compute space ID for org \"{0}\" and space \"{1}\"";
    public static final String THE_DEPLOYMENT_DESCRIPTOR_0_SPECIFIES_NOT_SUPPORTED_MTA_VERSION_1 = "The deployment descriptor \"{0}\" specifies a non-supported MTA version \"{1}\"";
    public static final String CANNOT_CLEAN_MULTI_TARGET_APP_ASSEMBLY_TARGET_DIR_0 = "Cannot clean multi-target app assembly target dir \"{0}\"";
    public static final String FAILED_TO_COPY_FILE_0_TO_ASSEMBLY_DIRECTORY = "Failed to copy file \"{0}\" to assembly directory";
    public static final String PATH_IS_RESOLVED_TO_NOT_EXISTING_FILE = "Path \"{0}\" is resolved to a non-existing file \"{1}\"";
    public static final String FAILED_TO_READ_DEPLOYMENT_DESCRIPTOR_0 = "Failed to read deployment descriptor \"{0}\"";
    public static final String DIRECTORY_0_DOES_NOT_CONTAIN_MANDATORY_DEPLOYMENT_DESCRIPTOR_FILE_1 = "Directory \"{0}\" does not contain mandatory deployment descriptor file \"{1}\"";
    public static final String FAILED_TO_LIST_MULTI_TARGET_APP_DIRECTORY_0 = "Failed to list multi-target app directory \"{0}\"";
    public static final String CANNOT_SHORTEN_NAME_TO_N_CHARACTERS = "Cannot shorten name \"{0}\" to {1} characters";
    public static final String NAMESPACE_IS_TOO_LONG = "Cannot shorten \"{0}-{1}\" to {2} characters - the namespace is too long for a prefix";
    public static final String ERROR_GETTING_APPLICATIONS = "Error getting Cloud Foundry applications";
    public static final String COULD_NOT_PARSE_ATTRIBUTES_OF_APP_0 = "Could not parse attributes of application \"{0}\"";
    public static final String ATTRIBUTE_0_OF_APP_1_IS_OF_TYPE_2_INSTEAD_OF_3 = "Attribute \"{0}\" of application \"{1}\" is of type {2} instead of {3}!";
    public static final String ILLEGAL_DESIRED_STATE = "Illegal desired application state: {0}";
    public static final String ILLEGAL_SERVICE_OPERATION_STATE = "Illegal service operation state: {0}";
    public static final String ILLEGAL_SERVICE_OPERATION_TYPE = "Illegal service operation type: {0}";
    public static final String NO_SERVICE_PLAN_FOUND = "Could not create service instance \"{0}\". Service plan \"{1}\" from service offering \"{2}\" was not found.";
    public static final String EMPTY_SERVICE_PLANS_LIST_FOUND = "An empty service plans list was found for service with label \"{0}\" from broker \"{1}\" with plan \"{2}\"";
    public static final String UNABLE_TO_PARSE_MEMORY_STRING_0 = "Unable to parse memory string \"{0}\"";
    public static final String CANT_CREATE_SERVICE_NOT_MATCHING_OFFERINGS_OR_PLAN = "Service \"{0}\" could not be created because none of the service offering(s) \"{1}\" match with existing service offerings or provide service plan \"{2}\"";
    public static final String CANT_CREATE_SERVICE = "Service \"{0}\" could not be created because all attempt(s) to use service offerings \"{1}\" failed";
    public static final String CANT_PARSE_MTA_METADATA_VERSION_FOR_0 = "Cannot parse version from MTA metadata for \"{0}\". This indicates that MTA reserved variables in the entity''s metadata were modified manually. Either revert the changes or delete the entity.";
    public static final String CANT_PARSE_MTA_ENV_METADATA_VERSION_FOR_APP_0 = "Cannot parse version from MTA metadata for application \"{0}\". This indicates that MTA reserved variables in the application''s environment were modified manually. Either revert the changes or delete the application.";
    public static final String MTA_METADATA_FOR_APP_0_IS_INCOMPLETE = "MTA metadata for application \"{0}\" is incomplete. This indicates that MTA reserved variables in the application''s environment were modified manually. Either revert the changes or delete the application.";
    public static final String MTA_METADATA_FOR_0_IS_INCOMPLETE = "MTA metadata for entity \"{0}\" is incomplete. This indicates that MTA reserved variables in the entity''s metadata were modified manually. Either revert the changes or delete the entity.";
    public static final String ENV_OF_APP_0_CONTAINS_INVALID_VALUE_FOR_1 = "The environment of application \"{0}\" contains an invalid value for \"{0}\". This indicates that MTA reserved variables in the application''s environment were modified manually. Either revert the changes or delete the application.";
    public static final String METADATA_OF_0_CONTAINS_INVALID_VALUE_FOR_1 = "The metadata of \"{0}\" contains an invalid value for \"{0}\". This indicates that MTA reserved variables in the entity''s metadata were modified manually. Either revert the changes or delete the entity.";
    public static final String COULD_NOT_DELETE_SPACEIDS_LEFTOVERS = "Could not delete space ids leftovers";
    public static final String TIMEOUT_WHILE_CHECKING_DATABASE_HEALTH = "Timeout while checking database health";
    public static final String TIMEOUT_WHILE_CHECKING_FOR_INCREASED_LOCKS = "Timeout while checking for increased locks";
    public static final String TIMEOUT_WHILE_CHECKING_OBJECT_STORE_HEALTH = "Timeout while checking object store health";

    public static final String COULD_NOT_GET_FILE_CONTENT_FOR_0 = "Could not get file content for file \"{0}\"";
    public static final String SERVICE_MISSING_REQUIRED_PARAMETER = "Service \"{0}\" has missing required parameter: {1}";
    public static final String CONTROLLER_URL_NOT_SPECIFIED = "Controller URL is not specified in the environment.";
    public static final String INVALID_CONTROLLER_URL = "Invalid controller URL \"{0}\".";
    public static final String ERROR_SERVICE_INSTANCE_RESPONSE_WITH_MISSING_FIELD = "The response of finding a service instance should contain a \"{0}\" element";
    public static final String ERROR_SERVICE_INSTANCE_RESPONSE_WITH_MORE_THEN_ONE_RESULT = "The response of finding a service instance should not have more than one resource element";
    public static final String MISSING_GLOBAL_AUDITOR_CREDENTIALS = "Global Auditor credentials are missing from the application ENV.";
    public static final String DELETING_TEMP_FILE = "Deleting temp file: {0}";
    public static final String ERROR_DELETING_APP_TEMP_FILE = "Error deleting temp application file \"{0}\"";
    public static final String ERROR_DURING_DATA_TERMINATION_0 = "Error during data termination: {0}";
    public static final String INVALID_URL = "Invalid URL: {0}";
    public static final String INVALID_TOKEN_PROVIDED = "The provided JWT token is not valid!";
    public static final String UNSUPPORTED_ALGORITHM_PROVIDED = "Unsupported algorithm: \"{0}\"";
    public static final String NO_TOKEN_PARSER_FOUND_FOR_THE_CURRENT_TOKEN = "No token parser found for the current token";
    public static final String RESOURCE_0_CANNOT_BE_CREATED_DUE_TO_UNRESOLVED_DYNAMIC_PARAMETER = "Resouce \"{0}\" cannot be created due to unresolved dynamic parameter. Please specify \"{1}\" in the processed-after section!";
    public static final String ERROR_OCCURRED_DURING_OBJECT_STORE_HEALTH_CHECKING = "Error occurred during object store health checking";
    public static final String ERROR_OCCURRED_DURING_DATABASE_HEALTH_CHECKING = "Error occurred during database health checking";
    public static final String ERROR_OCCURRED_WHILE_CHECKING_FOR_INCREASED_LOCKS = "Error occurred while checking for increased locks";
    public static final String THREAD_WAS_INTERRUPTED_WHILE_WAITING_FOR_THE_RESULT_OF_A_FUTURE = "Thread was interrupted while waiting for the result of a future";
    public static final String ERROR_OCCURRED_DURING_HEALTH_CHECKING_FOR_INSTANCE_0_MESSAGE_1 = "Error occurred during health checking for instance: \"{0}\". Message: \"{1}\"";
    public static final String OBJECT_STORE_FILE_STORAGE_HEALTH_DATABASE_HEALTH = "Object store file storage health: \"{0}\", Database health: \"{1}\"";
    public static final String ERROR_OCCURRED_DURING_OBJECT_STORE_HEALTH_CHECKING_FOR_INSTANCE = "Error occurred during object store health checking for instance: \"{0}\"";
    public static final String ERROR_OCCURRED_WHILE_CHECKING_DATABASE_INSTANCE_0 = "Error occurred while checking database instance: \"{0}\"";
    public static final String DOCKER_INFO_NOT_ALLOWED_WITH_LIFECYCLE = "Docker information must not be provided when lifecycle is set to \"{0}\"";
    public static final String UNSUPPORTED_LIFECYCLE_VALUE = "Unsupported lifecycle value: \"{0}\"";
    public static final String BUILDPACKS_REQUIRED_FOR_CNB = "Buildpacks must be provided when lifecycle is set to 'cnb'.";
    public static final String DOCKER_INFO_REQUIRED = "Docker information must be provided when lifecycle is set to 'docker'.";
    public static final String BUILDPACKS_NOT_ALLOWED_WITH_DOCKER = "Buildpacks must not be provided when lifecycle is set to 'docker'.";

    // Warning messages
    public static final String ENVIRONMENT_VARIABLE_IS_NOT_SET_USING_DEFAULT = "Environment variable \"{0}\" is not set. Using default \"{1}\"...";
    public static final String OPTIONAL_RESOURCE_IS_NOT_SERVICE = "Optional resource \"{0}\" it will be not created because it''s not a service";
    public static final String SERVICE_IS_NOT_ACTIVE = "Service \"{0}\" is inactive and will not be processed";
    public static final String DETECTED_INCREASED_NUMBER_OF_PROCESSES_WAITING_FOR_LOCKS_FOR_INSTANCE = "Detected increased number of processes waiting for locks: \"{0}\" for instance: \"{1}\"";
    public static final String DETECTED_INCREASED_NUMBER_OF_PROCESSES_WAITING_FOR_LOCKS_FOR_INSTANCE_0_GETTING_THE_LOCKS = "Detected increased number of processes waiting for locks for instance {0}. Getting the locks...";

    public static final String INVALID_VCAP_APPLICATION = "Invalid VCAP_APPLICATION \"{0}\"";
    public static final String IGNORING_LABEL_FOR_USER_PROVIDED_SERVICE = "Ignoring label \"{0}\" for service \"{1}\", as user-provided services do not support labels!";
    public static final String NOT_DESCRIBED_MODULE = "MTA module \"{0}\" is found deployed, but it is not part of MTA manifest file";
    public static final String COULD_NOT_PARSE_PROVIDED_DEPENDENCY_NAMES_1_OF_APP_0 = "Could not parse provided dependency names of application \"{0}\". Assuming that they''re written in the \"old\" format. This is what they look like: {1}";
    public static final String SPACE_GUID_NOT_SPECIFIED_USING_DEFAULT_0 = "Space GUID is not specified in the environment. Using default: \"{0}\"";
    public static final String ORG_NAME_NOT_SPECIFIED = "Org name is not specified in the environment.";
    public static final String DEPLOY_SERVICE_URL_NOT_SPECIFIED = "Deploy service URL is not specified in the environment.";
    public static final String INVALID_SUPPORT_COMPONENTS = "Invalid SUPPORT_COMPONENTS \"{0}\"";
    public static final String INCOMPATIBLE_PARAMETERS = "Module \"{0}\" has parameters {1} that will be replaced by \"{2}\" due to inconsistency";
    public static final String MODULE_0_DEPENDS_ON_MODULE_1_WHICH_CANNOT_BE_RESOLVED = "Module \"{0}\" depends on module \"{1}\", which is not an application and its state cannot be calculated. This dependency will be ignored during deployment.";
    public static final String MODULE_0_WILL_BE_SKIPPED_DURING_DEPLOYMENT = "Module \"{0}\" will be skipped during deployment";

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
    public static final String MAX_RESOLVED_EXTERNAL_CONTENT_SIZE = "Max resolved external content size is set to: {0}";
    public static final String CRON_EXPRESSION_FOR_OLD_DATA = "Cron expression for old data: {0}";
    public static final String EXECUTION_TIME_FOR_FINISHED_PROCESSES = "Execution time for finished processes: {0}";
    public static final String MAX_TTL_FOR_OLD_DATA = "Max TTL for old data: {0}";
    public static final String SPACE_GUID = "Space GUID: {0}";
    public static final String ORG_NAME = "Org name: {0}";
    public static final String BASIC_AUTH_ENABLED = "Basic authentication enabled: {0}";
    public static final String USE_XS_AUDIT_LOGGING = "Use XSA audit logging: {0}";
    public static final String DB_CONNECTION_THREADS = "Database connection thread pool size: {0}";
    public static final String STEP_POLLING_INTERVAL_IN_SECONDS = "Step polling interval in seconds: {0}";
    public static final String SKIP_SSL_VALIDATION = "Skip SSL validation: {0}";
    public static final String DS_VERSION = "Deploy service version: {0}";
    public static final String CHANGE_LOG_LOCK_POLL_RATE = "Change log lock poll rate: {0}";
    public static final String CHANGE_LOG_LOCK_DURATION = "Change log lock duration: {0}";
    public static final String CHANGE_LOG_LOCK_ATTEMPTS = "Change log lock attempts: {0}";
    public static final String GLOBAL_CONFIG_SPACE = "Global config space: {0}";
    public static final String APPLICATION_GUID = "Application GUID: {0}";
    public static final String APPLICATION_INSTANCE_INDEX = "Application instance index: {0}";
    public static final String AUDIT_LOG_CLIENT_CORE_THREADS = "Audit log client core threads: {0}";
    public static final String AUDIT_LOG_CLIENT_MAX_THREADS = "Audit log client max threads: {0}";
    public static final String AUDIT_LOG_CLIENT_QUEUE_CAPACITY = "Audit log client queue capacity: {0}";
    public static final String AUDIT_LOG_CLIENT_KEEP_ALIVE = "Audit log client keep alive: {0}";
    public static final String FLOWABLE_JOB_EXECUTOR_CORE_THREADS = "Flowable job executor core threads: {0}";
    public static final String FLOWABLE_JOB_EXECUTOR_MAX_THREADS = "Flowable job executor max threads: {0}";
    public static final String FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY = "Flowable job executor queue capacity: {0}";
    public static final String GLOBAL_AUDITOR_ORIGIN = "Global auditor user origin: {0}";

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
    public static final String RECENT_DELETE_SPACE_REQUEST_EVENTS = "Recent delete space request events: {0}";
    public static final String RETRIEVED_TOKEN_FOR_USER_WITH_GUID_0_WITH_EXPIRATION_TIME_1 = "Retrieved token for user with GUID \"{0}\" with expiration time: {1} seconds";
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
    public static final String CONTROLLER_CLIENT_RESPONSE_TIMEOUT = "Controller client http response timeout: {0}";
    public static final String MICROMETER_STEP_IN_SECONDS = "Micrometer step in seconds: {0}";
    public static final String DB_TRANSACTION_TIMEOUT = "Database transaction timeout: {0} seconds";
    public static final String SNAKEYAML_MAX_ALIASES_FOR_COLLECTIONS = "SnakeYaml max aliases for collections: {0}";
    public static final String SERVICE_HANDLING_MAX_PARALLEL_THREADS = "Service handling max parallel threads: {0}";
    public static final String ABORTED_OPERATIONS_TTL_IN_SECONDS = "Aborted operations TTL in seconds: {0}";
    public static final String SPRING_SCHEDULER_TASK_EXECUTOR_THREADS = "Spring scheduler task executor threads: {0}";
    public static final String FILES_ASYNC_UPLOAD_EXECUTOR_MAX_THREADS = "Files async executor max threads: {0}";
    public static final String ON_START_FILES_CLEANER_WITHOUT_CONTENT_ENABLED_0 = "On start files cleaner without content enabled: {0}";
    public static final String THREADS_FOR_FILE_UPLOAD_TO_CONTROLLER_0 = "Threads for file upload to controller: {0}";
    public static final String THREADS_FOR_FILE_STORAGE_UPLOAD_0 = "Threads for file storage upload: {0}";
    public static final String DELETED_ORPHANED_MTA_DESCRIPTORS_COUNT = "Deleted orphaned mta descriptors count: {0}";
    public static final String IS_HEALTH_CHECK_ENABLED = "Is health check enabled: {0}";

    // Debug messages
    public static final String DEPLOYMENT_DESCRIPTOR = "Deployment descriptor: {0}";
    public static final String MERGED_DESCRIPTOR = "Merged deployment descriptor: {0}";
    public static final String DELETING_SUBSCRIPTION = "Deleting configuration subscription: {0}";
    public static final String DELETING_ENTRY = "Deleting configuration entry: {0}";
    public static final String CERTIFICATE_CN = "Certificate CN: \"{0}\"";
    public static final String PARSED_TOKEN_TYPE_0 = "Parsed token type: {0}";
    public static final String PARSED_TOKEN_EXPIRES_IN_0 = "Parsed token expires in: {0}";
    public static final String PARSER_CHAIN_0 = "Parser chain: {0}";
    public static final String VALUES_IN_INSTANCE_IN_THE_WAITING_FOR_LOCKS_SAMPLES = "Values in instance: \"{0}\" in the waiting for locks samples: {1}";
    public static final String INCREASING_OR_EQUAL_INDEX_0_1_2 = "Increasing or equal index: {0} / {1} = {2}";
    public static final String DECREASING_INDEX_0_1_2 = "Decreasing index: {0} / {1} = {2}";
    public static final String OBJECT_STORE_FILE_STORAGE_IS_NOT_AVAILABLE_FOR_INSTANCE = "Object store file storage is not available for instance: \"{0}\"";
    public static final String NOT_ENOUGH_SAMPLES_TO_DETECT_ANOMALY_0_1 = "Not enough samples to detect anomaly: {0} / {1}";
    public static final String CHECKING_DATABASE_HEALTH = "Checking database health...";
    public static final String CHECKING_OBJECT_STORE_HEALTH = "Checking object store health...";
    public static final String CHECKING_FOR_INCREASED_LOCKS = "Checking for increased locks...";

    // Audit log

    public static final String RETRIEVE_CSRF_TOKEN_AUDIT_LOG_MESSAGE = "Retrieve a CSRF token";

    public static final String LIST_FILES_AUDIT_LOG_MESSAGE = "List files in space with id: {0}";
    public static final String DELETE_SUBSCRIPTION_AUDIT_LOG_MESSAGE = "Delete subscription in space with id: {0}";
    public static final String DELETE_ENTRY_AUDIT_LOG_MESSAGE = "Delete entry in space with id: {0}";
    public static final String DELETE_OPERATION_AUDIT_LOG_MESSAGE = "Delete operation in space with id: {0}";
    public static final String DELETE_BACKUP_DESCRIPTOR_AUDIT_LOG_MESSAGE = "Delete backup descriptor in space with id: {0}";
    public static final String UPLOAD_FILE_AUDIT_LOG_MESSAGE = "Upload file in space with id: {0}";
    public static final String UPLOAD_FILE_FROM_URL_AUDIT_LOG_MESSAGE = "Upload file from url in space with id: {0}";
    public static final String GET_INFO_FOR_UPLOAD_URL_JOB_AUDIT_LOG_MESSAGE = "Get info for upload from url job in space with id: {0}";

    public static final String LIST_OPERATIONS_AUDIT_LOG_MESSAGE = "List operations for mta in space with id: {0}";
    public static final String LIST_OPERATION_ACTIONS_AUDIT_LOG_MESSAGE = "List operation action in space with id: {0}";
    public static final String EXECUTE_OPERATION_AUDIT_LOG_MESSAGE = "Execute operation in space with id: {0}";
    public static final String GET_OPERATION_LOGS_AUDIT_LOG_MESSAGE = "Get operation logs in space with id: {0}";
    public static final String GET_OPERATION_LOG_CONTENT_AUDIT_LOG_MESSAGE = "Get operation log content in space with id: {0}";
    public static final String START_OPERATION_AUDIT_LOG_MESSAGE = "Start {0} operation in space with id: {1}";
    public static final String GET_INFO_FOR_OPERATION = "Get info for operation in space with id: {0}";

    public static final String LIST_MTA_AUDIT_LOG_MESSAGE = "List MTA in space with id: {0}";
    public static final String GET_MTA_AUDIT_LOG_MESSAGE = "Get MTA in space with id: {0}";

    public static final String GET_INFO_FOR_API_AUDIT_LOG_CONFIG = "Get information for api";
    public static final String FETCH_TOKEN_AUDIT_LOG_MESSAGE = "Attempt to fetch access token for client: \"{0}\" in space: \"{1}\" for service \"{2}\"";
    public static final String FAILED_TO_FETCH_TOKEN_AUDIT_LOG_MESSAGE = "Failed to fetch access token for client: \"{0}\" in space: \"{1}\" for service \"{2}\"";

    public static final String FETCH_TOKEN_AUDIT_LOG_CONFIG = "Access token fetch";

    // Audit log configuration
    public static final String GET_CSRF_TOKEN_AUDIT_LOG_CONFIG = "CSRF token get ";

    public static final String FILE_INFO_AUDIT_LOG_CONFIG = "File list";
    public static final String SUBSCRIPTION_DELETE_AUDIT_LOG_CONFIG = "Subscription delete";
    public static final String ENTRY_DELETE_AUDIT_LOG_CONFIG = "Entry delete";
    public static final String OPERATION_DELETE_AUDIT_LOG_CONFIG = "Operation delete";
    public static final String FILE_UPLOAD_AUDIT_LOG_CONFIG = "File upload";
    public static final String FILE_UPLOAD_FROM_URL_AUDIT_LOG_CONFIG = "File upload from url";
    public static final String UPLOAD_FROM_URL_JOB_INFO_AUDIT_LOG_CONFIG = "Upload from url job info";
    public static final String MTA_DESCRIPTOR_DELETE_AUDIT_LOG_CONFIG = "Mta descriptor delete";

    public static final String OPERATION_LIST_AUDIT_LOG_CONFIG = "Operation list";
    public static final String OPERATION_ACTIONS_LIST_AUDIT_LOG_CONFIG = "Operation actions list";
    public static final String EXECUTE_OPERATION_AUDIT_LOG_CONFIG = "Operation action execute";
    public static final String LIST_OPERATION_LOGS_AUDIT_LOG_CONFIG = "Operation logs list";
    public static final String GET_OPERATION_LOG_CONTENT_AUDIT_LOG_CONFIG = "Operation log content info";
    public static final String START_OPERATION_AUDIT_LOG_CONFIG = "Operation start";
    public static final String GET_OPERATION_INFO_AUDIT_LOG_CONFIG = "Operation info";

    public static final String MTA_INFO_AUDIT_LOG_CONFIG = "MTA info";
    public static final String MTA_LIST_AUDIT_LOG_CONFIG = "MTA list";

    public static final String API_INFO_AUDIT_LOG_CONFIG = "Api info";
    public static final String IGNORING_NAMESPACE_PARAMETERS = "Ignoring parameter \"{0}\" , as the MTA is not deployed with namespace!";
    public static final String NAMESPACE_PARSING_ERROR_MESSAGE = "Cannot parse \"{0}\" flag - expected a boolean format.";

    private Messages() {
    }
}
