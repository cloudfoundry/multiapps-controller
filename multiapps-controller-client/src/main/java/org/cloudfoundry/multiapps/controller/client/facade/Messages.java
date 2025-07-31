package org.cloudfoundry.multiapps.controller.client.facade;

public final class Messages {

    private Messages() {

    }

    // INFO messages
    public static final String CALLING_CF_ROOT_0_TO_ACCESS_LOG_CACHE_URL = "Calling CF root: {0} to access log-cache URL";
    public static final String CF_ROOT_REQUEST_FINISHED = "CF root request finished";
    public static final String TRYING_TO_GET_APP_LOGS = "Trying to get app logs";
    public static final String APP_LOGS_WERE_FETCHED_SUCCESSFULLY = "App logs were fetched successfully";

    // WARN messages
    public static final String RETRYING_OPERATION = "Retrying operation that failed with: {0}";
    public static final String CALL_TO_0_FAILED_WITH_1 = "Calling {0} failed with: {1}";

    // ERROR messages
    public static final String UNKNOWN_PACKAGE_TYPE = "Unknown package type: %s";
    public static final String CANT_CREATE_SERVICE_KEY_FOR_USER_PROVIDED_SERVICE = "Service keys can't be created for user-provided service instance \"%s\"";
    public static final String NO_SERVICE_PLAN_FOUND = "Service plan with guid \"{0}\" for service instance with name \"{1}\" was not found.";
    public static final String SERVICE_PLAN_WITH_GUID_0_NOT_AVAILABLE_FOR_SERVICE_INSTANCE_1 = "Service plan with guid \"{0}\" is not available for service instance \"{1}\".";
    public static final String SERVICE_OFFERING_WITH_GUID_0_IS_NOT_AVAILABLE = "Service offering with guid \"{0}\" is not available.";
    public static final String SERVICE_OFFERING_WITH_GUID_0_NOT_FOUND = "Service offering with guid \"{0}\" not found.";
    public static final String FAILED_TO_FETCH_APP_LOGS_FOR_APP = "Failed to fetch app logs for app: %s";

    public static final String BUILDPACKS_ARE_REQUIRED_FOR_CNB_LIFECYCLE_TYPE = "Buildpacks are required for CNB lifecycle type.";

}
