package org.cloudfoundry.multiapps.controller;

public class Messages {

    // INFO messages
    public static final String WAITING_MS_BEFORE_RETRYING_WITH_TIMEOUT_OF_MS = "Waiting: {} ms before retrying with timeout of: {} ms.";
    public static final String RATE_LIMITED_BY_CC_WAITING_S = "CC returned 429 with Retry-After: {} s. Waiting {} s (capped) before retrying.";
    public static final String RATE_LIMITED_BY_CC_NO_HEADER_WAITING_MS = "CC returned 429 without Retry-After header. Waiting {} ms before retrying.";
    public static final String RANDOM_WAIT_BEFORE_RETRY_MS = "Waiting {} ms (randomized) before retrying failed CC operation.";

    // Exception messages
    public static final String ERROR_OCCURRED_SETTING_UP_DEFAULT_SSL_CONTEXT = "An error occurred while setting up the default SSLContext.";
    public static final String ERROR_OCCURRED_SETTING_UP_ALWAYS_APPROVING_SSL_CONTEXT = "An error occurred while setting up the always approving SSLContext.";
    public static final String COULD_NOT_DETERMINE_API_ORIGIN_FROM_0 = "Could not determine the API origin from \"{0}\".";
    public static final String COULD_NOT_RESOLVE_URL_FROM_0_1 = "Could not resolve URL from \"{0}\" \"{1}\".";
    public static final String COULD_NOT_RESOLVE_LOG_CACHE_URL_FROM_0 = "Could not derive the log-cache URL from \"{0}\".";
    public static final String CF_ROOT_DOCUMENT_REQUEST_TO_0_RETURNED_1 = "CF root request to \"{0}\" returned \"{1}\".";
    public static final String INTERRUPTED_WHILE_CALLING_THE_CF_ROOT_URL_AT_0 = "Interrupted while calling the CF root URL  at \"{0}\".";
    public static final String FAILED_TO_CALL_THE_CF_ROOT_AT_0_WITH_1 = "Failed to call the CF root at \"{0}\" with: \"{1}\".";
    public static final String SERVICE_KEY_0_NOT_FOUND = "Service key \"{0}\" not found.";
    public static final String SERVICE_INSTANCE_0_NOT_FOUND = "Service instance \"{0}\" not found.";
    public static final String SERVICE_BROKER_0_NOT_FOUND = "Service broker \"{0}\" not found.";
    public static final String APPLICATION_0_NOT_FOUND = "Application \"{0}\" not found.";
    public static final String DOMAIN_0_NOT_FOUND = "Domain \"{0}\" not found.";
    public static final String SERVICE_PLAN_0_NOT_FOUND = "Service plan \"{0}\" not found.";
    public static final String STACK_0_NOT_FOUND = "Stack \"{0}\" not found.";
    public static final String ORGANISATION_0_NOT_FOUND = "Organization \"{0}\" not found.";
    public static final String SPACE_WITH_GUID_0_NOT_FOUND = "Space with GUID \"{0}\" not found.";
    public static final String APPLICATION_WITH_GUID_0_DOES_NOT_HAVE_A_DROPLET = "Application with guid \"{0}\" does not have a droplet";
    public static final String HOST_0_NOT_FOUND_FOR_DOMAIN_1 = "Host \"{0}\" not found for domain \"{1}\".";
    public static final String UNABLE_TO_0_WITHOUT_SPECIFYING_ORGANIZATION_AND_SPACE_TO_USE = "Unable to \"{0}\" without specifying organization and space to use.";
    public static final String DOMAIN_0_NOT_FOUND_FOR_URI_1 = "Domain \"{0}\" not found for URI \"{1}\"";
    public static final String SERVICE_BINDING_BETWEEN_SERVICE_WITH_GUID_0_AND_APPLICATION_WITH_GUID_1_NOT_FOUND = "Service binding between service with GUID \"{0}\" and application with GUID \"{1}\" not found.";
    public static final String SERVICE_PLAN_WITH_GUID_0_NOT_AVAILABLE_FOR_SERVICE_INSTANCE_1 = "Service plan with guid \"{0}\" is not available for service instance \"{1}\".";
    public static final String NO_SERVICE_PLAN_FOUND = "Service plan with guid \"{0}\" for service instance with name \"{1}\" was not found.";
    public static final String SERVICE_OFFERING_WITH_GUID_0_IS_NOT_AVAILABLE = "Service offering with guid \"{0}\" is not available.";
    public static final String SERVICE_OFFERING_WITH_GUID_0_NOT_FOUND = "Service offering with guid \"{0}\" not found.";
    public static final String TARGET_SPACE_REQUIRED_TO_CREATE_AN_APPLICATION = "A target space is required to create an application.";
    public static final String JOB_0_DID_NOT_COMPLETE_WITHIN_1 = "Job \"{0}\" did not complete within \"{1}\"";
    public static final String ORGANIZATION_WITH_GUID_0_NOT_FOUND = "Organization with GUID \"{0}\" not found.";
    public static final String SPACE_0_NOT_FOUND_IN_ORGANIZATION_1 = "Space \"{0}\" not found in organization \"{1}\"";
    public static final String INTERRUPTED_WHILE_POLLING_ASYNC_JOB_0 = "Interrupted while polling an async job: \"{0}\"";
    public static final String UNKNOWN_JOB_STATE_0 = "Unknown job state: \"{0}\"";
    public static final String UNKNOWN_SERVICE_INSTANCE_TYPE_0 = "Unknown service instance type: \"{0}\"";
    public static final String UNKNOWN_USER_ROLE_0 = "Unknown user role: \"{0}\"";

    //Status texts
    public static final String NOT_FOUND = "Not Found";
    public static final String JOB_TIMEOUT = "Job Timeout";
    public static final String JOB_FAILED = "Job Failed";
    public static final String BAD_REQUEST = "Bad Request";
    public static final String FORBIDDEN = "Forbidden";

}
