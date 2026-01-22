package org.cloudfoundry.multiapps.controller.process;

import java.util.regex.Pattern;

public class Constants {

    public static final String DEPLOY_SERVICE_ID = "xs2-deploy";
    public static final String BLUE_GREEN_DEPLOY_SERVICE_ID = "xs2-bg-deploy";
    public static final String UNDEPLOY_SERVICE_ID = "xs2-undeploy";
    public static final String ROLLBACK_MTA_SERVICE_ID = "rollback-mta";
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
    public static final String UNKNOWN_LABEL = "unknown label";
    public static final String UNKNOWN_PLAN = "unknown plan";
    public static final String MTA_BACKUP_NAMESPACE = "mta-backup";
    public static final String MTA_FOR_DELETION_PREFIX = "to-be-deleted";

    public static final String USER_PROVIDED_SERVICE_PREFIX_NAME_ENCRYPTION_DECRYPTION = "__mta-secure-";
    public static final String ENCRYPTION_KEY = "encryptionKey";
    public static final String KEY_ID = "keyId";
    public static final String PLACEHOLDER_PREFIX = "${";
    public static final String PLACEHOLDER_SUFFIX = "}";
    public static final String DOUBLE_APPENDED_STRING = "%s%s";
    public static final String TRIPLE_APPENDED_STRING = "%s%s%s";
    public static final String SECURE_EXTENSION_DESCRIPTOR_ID = "__mta.secure";

    public static final Long UNSET_LAST_LOG_TIMESTAMP_MS = 0L;
    public static final int LOG_STALLED_TASK_MINUTE_INTERVAL = 5;
    public static final long TTL_CACHE_ENTRY = 60_000L;
    public static final int MAX_CACHE_ENTRIES = 256;

    public static final Pattern STANDARD_INT_PATTERN = Pattern.compile("[+-]?[0-9]+");

    protected Constants() {
    }
}
