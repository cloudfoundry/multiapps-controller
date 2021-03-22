package org.cloudfoundry.multiapps.controller.persistence.model;

public class PersistenceMetadata {

    private PersistenceMetadata() {
    }

    public static final String NOT_AVAILABLE = "N/A";

    public static class TableNames {

        private TableNames() {
        }

        public static final String CONFIGURATION_ENTRY_TABLE = "configuration_registry";
        public static final String CONFIGURATION_SUBSCRIPTION_TABLE = "configuration_subscription";
        public static final String PROGRESS_MESSAGE_TABLE = "progress_message";
        public static final String HISTORIC_OPERATION_EVENT_TABLE = "historic_operation_event";
        public static final String ACCESS_TOKEN_TABLE = "access_token";

    }

    public static class SequenceNames {

        private SequenceNames() {
        }

        public static final String DEPLOY_TARGET_SEQUENCE = "deploy_target_sequence";
        public static final String CONFIGURATION_ENTRY_SEQUENCE = "configuration_entry_sequence";
        public static final String CONFIGURATION_SUBSCRIPTION_SEQUENCE = "configuration_subscription_sequence";
        public static final String PROGRESS_MESSAGE_SEQUENCE = "ID_SEQ";
        public static final String HISTORIC_OPERATION_EVENT_SEQUENCE = "historic_operation_event_sequence";
        public static final String ACCESS_TOKEN_SEQUENCE = "access_token_sequence";

    }

    public static class TableColumnNames {

        private TableColumnNames() {
        }

        public static final String CONFIGURATION_ENTRY_ID = "id";
        public static final String CONFIGURATION_ENTRY_PROVIDER_NID = "provider_nid";
        public static final String CONFIGURATION_ENTRY_PROVIDER_ID = "provider_id";
        public static final String CONFIGURATION_ENTRY_PROVIDER_NAMESPACE = "provider_namespace";
        public static final String CONFIGURATION_ENTRY_PROVIDER_VERSION = "provider_version";
        public static final String CONFIGURATION_ENTRY_TARGET_ORG = "target_org";
        public static final String CONFIGURATION_ENTRY_TARGET_SPACE = "target_space";
        public static final String CONFIGURATION_ENTRY_SPACE_ID = "space_id";
        public static final String CONFIGURATION_ENTRY_CONTENT = "content";
        public static final String CONFIGURATION_CLOUD_TARGET = "visibility";
        public static final String CONFIGURATION_ENTRY_CONTENT_ID = "content_id";

        public static final String CONFIGURATION_SUBSCRIPTION_MTA_ID = "mta_id";
        public static final String CONFIGURATION_SUBSCRIPTION_ID = "id";
        public static final String CONFIGURATION_SUBSCRIPTION_SPACE_ID = "space_id";
        public static final String CONFIGURATION_SUBSCRIPTION_APP_NAME = "app_name";
        public static final String CONFIGURATION_SUBSCRIPTION_RESOURCE_PROP = "resource_properties";
        public static final String CONFIGURATION_SUBSCRIPTION_RESOURCE_NAME = "resource_name";
        public static final String CONFIGURATION_SUBSCRIPTION_MODULE = "module";
        public static final String CONFIGURATION_SUBSCRIPTION_FILTER = "filter";
        public static final String CONFIGURATION_SUBSCRIPTION_MODULE_ID = "module_id";
        public static final String CONFIGURATION_SUBSCRIPTION_RESOURCE_ID = "resource_id";

        public static final String ONGOING_OPERATION_SPACE_ID = "spaceId";
        public static final String ONGOING_OPERATION_STARTED_AT = "startedAt";
        public static final String ONGOING_OPERATION_FINAL_STATE = "finalState";
        public static final String ONGOING_OPERATION_MTA_ID = "mtaId";

        public static final String DEPLOY_TARGET_NAME = "name";

        public static final String PROGRESS_MESSAGE_ID = "id";
        public static final String PROGRESS_MESSAGE_PROCESS_ID = "process_id";
        public static final String PROGRESS_MESSAGE_TASK_ID = "task_id";
        public static final String PROGRESS_MESSAGE_TYPE = "type";
        public static final String PROGRESS_MESSAGE_TEXT = "text";
        public static final String PROGRESS_MESSAGE_TIMESTAMP = "timestamp";

        public static final String HISTORIC_OPERATION_EVENT_ID = "id";
        public static final String HISTORIC_OPERATION_EVENT_PROCESS_ID = "process_id";
        public static final String HISTORIC_OPERATION_EVENT_TYPE = "event";
        public static final String HISTORIC_OPERATION_EVENT_TIMESTAMP = "timestamp";

        public static final String ACCESS_TOKEN_ID = "id";
        public static final String ACCESS_TOKEN_VALUE = "value";
        public static final String ACCESS_TOKEN_USERNAME = "username";
        public static final String ACCESS_TOKEN_EXPIRES_AT = "expires_at";
    }

}
