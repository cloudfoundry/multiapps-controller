package org.cloudfoundry.multiapps.controller.process;

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

    public static final String VAR_IS_SERVICE_UPDATED_VAR_PREFIX = "IS_SERVICE_UPDATED_";
    public static final String VAR_APP_SERVICE_BROKER_VAR_PREFIX = "APP_SERVICE_BROKER_";
    public static final String VAR_STEP_START_TIME = "stepStartTime_";
    public static final String VAR_EXECUTED_HOOKS_FOR_PREFIX = "executedHooksFor_";
    public static final String VAR_IS_APPLICATION_SERVICE_BINDING_UPDATED_VAR_PREFIX = "IS_APPLICATION_SERVICE_BINDING_UPDATED_";
    public static final String VAR_SERVICE_ACTIONS_TO_EXECUTE = "SERVICE_ACTIONS_TO_EXECUTE_";
    public static final String VAR_WAIT_AFTER_APP_STOP = "MTA_WAIT_AFTER_APP_STOP";
    public static final String VAR_SERVICE_INSTANCE_GUID_PREFIX = "SERVICE_INSTANCE_GUID_";

    protected Constants() {
    }
}
