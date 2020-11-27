package org.cloudfoundry.multiapps.controller.api;

public class Constants {

    private Constants() {
    }

    public static class PathVariables {

        private PathVariables() {
        }

        public static final String OPERATION_ID = "operationId";
        public static final String ACTION_ID = "actionId";
        public static final String LOG_ID = "logId";
        public static final String SPACE_GUID = "spaceGuid";

    }

    public static class RequestVariables {

        private RequestVariables() {
        }

        public static final String MTA_ID = "mtaId";
        public static final String NAMESPACE = "namespace";
        public static final String MTA_NAME = "name";
        public static final String FILE_URL = "file-url";
    }

    public static class QueryVariables {

        private QueryVariables() {
        }

        public static final String LAST = "last";
        public static final String STATE = "state";
    }

    public static class Resources {

        private Resources() {
        }

        private static final String ROOT = "/api/v1";
        private static final String ROOT_V2 = "/api/v2";

        public static final String SPACE = ROOT + "/spaces/{" + PathVariables.SPACE_GUID + "}";
        public static final String OPERATIONS = SPACE + "/operations";
        public static final String FILES = SPACE + "/files";
        public static final String MTAS = SPACE + "/mtas";
        public static final String INFO = ROOT + "/info";
        public static final String CSRF = ROOT + "/csrf-token";
        
        public static final String SPACE_V2 = Resources.ROOT_V2 + "/spaces/{" + PathVariables.SPACE_GUID + "}";
        public static final String MTAS_V2 = SPACE_V2 + "/mtas";
    }

    public static class Endpoints {

        private Endpoints() {
        }

        public static final String MTA = "/{" + RequestVariables.MTA_ID + "}";
        public static final String OPERATION = "/{" + PathVariables.OPERATION_ID + "}";
        public static final String OPERATION_LOGS = OPERATION + "/logs";
        public static final String OPERATION_LOG_CONTENT = OPERATION_LOGS + "/{" + PathVariables.LOG_ID + "}/content";
        public static final String OPERATION_ACTIONS = OPERATION + "/actions";

    }

}
