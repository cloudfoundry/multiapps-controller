package com.sap.cloud.lm.sl.cf.web.api;

public class Constants {

    public static class PathVariables {

        public static final String OPERATION_ID = "operationId";
        public static final String ACTION_ID = "actionId";
        public static final String LOG_ID = "logId";
        public static final String MTA_ID = "mtaId";
        public static final String SPACE_GUID = "spaceGuid";

    }

    public static class Resources {

        private static final String ROOT = "/api/v1";

        public static final String SPACE = ROOT + "/spaces/{" + PathVariables.SPACE_GUID + "}";
        public static final String OPERATIONS = SPACE + "/operations";
        public static final String FILES = SPACE + "/files";
        public static final String MTAS = SPACE + "/mtas";
        public static final String INFO = ROOT + "/info";
        public static final String CSRF = ROOT + "/csrf-token";

    }

    public static class Endpoints {

        public static final String MTA = "/{" + PathVariables.MTA_ID + "}";
        public static final String OPERATION = "/{" + PathVariables.OPERATION_ID + "}";
        public static final String OPERATION_LOGS = OPERATION + "/logs";
        public static final String OPERATION_LOG_CONTENT = OPERATION_LOGS + "/{" + PathVariables.LOG_ID + "}/content";
        public static final String OPERATION_ACTIONS = OPERATION + "/actions";

    }

}
