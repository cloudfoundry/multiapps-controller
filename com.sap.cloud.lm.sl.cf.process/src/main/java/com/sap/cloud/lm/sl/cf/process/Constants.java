package com.sap.cloud.lm.sl.cf.process;

public class Constants {

    public static final String DEPLOY_SERVICE_ID = "xs2-deploy";
    public static final String BLUE_GREEN_DEPLOY_SERVICE_ID = "xs2-bg-deploy";
    public static final String UNDEPLOY_SERVICE_ID = "xs2-undeploy";
    public static final String CTS_DEPLOY_SERVICE_ID = "CTS_DEPLOY";
    public static final String DEPLOY_APP_SUB_PROCESS_ID = "deployAppSubProcess";
    public static final String EXECUTE_HOOK_TASKS_SUB_PROCESS_ID = "executeHookTasksSubProcess";
    public static final String SERVICE_VERSION_1_2 = "1.2";
    public static final String SERVICE_VERSION_1_1 = "1.1";
    public static final String SERVICE_VERSION_1_0 = "1.0";

    public static final String VAR_MTA_MODULE_CONTENT_PREFIX = "mtaModuleContent_";
    public static final String VAR_MTA_MODULE_FILE_NAME_PREFIX = "mtaModuleFileName_";
    public static final String VAR_MTA_REQUIRES_FILE_NAME_PREFIX = "mtaRequiresFileName_";
    public static final String VAR_MTA_RESOURCE_FILE_NAME_PREFIX = "mtaResourceFileName_";
    public static final String VAR_IS_SERVICE_UPDATED_VAR_PREFIX = "IS_SERVICE_UPDATED_";
    public static final String VAR_APP_SERVICE_BROKER_VAR_PREFIX = "APP_SERVICE_BROKER_";
    public static final String PROCESS_ABORTED = "__PROCESS_ABORTED";
    public static final String VAR_STEP_START_TIME = "stepStartTime_";
    public static final String VAR_EXECUTED_HOOKS_FOR_PREFIX = "executedHooksFor_";

    public static final String TOOL_TYPE = "tool_type";
    public static final String FEEDBACK_MAIL = "feedback_form";
    public static final String SYMANTEC_CERTIFICATE_FILE = "/symantec.crt";
    public static final String CERTIFICATE_TYPE_X_509 = "X.509";

    protected Constants() {
    }
}
