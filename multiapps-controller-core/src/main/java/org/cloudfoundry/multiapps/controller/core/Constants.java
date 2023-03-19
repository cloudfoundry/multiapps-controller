package org.cloudfoundry.multiapps.controller.core;

public class Constants {

    public static final String DEPENDENCY_TYPE_SOFT = "soft";
    public static final String DEPENDENCY_TYPE_HARD = "hard";

    public static final String UNIX_PATH_SEPARATOR = "/";
    public static final String MTA_ELEMENT_SEPARATOR = "/";
    public static final String MODULE_SEPARATOR = ",";
    public static final String NAMESPACE_SEPARATOR = "-";

    // Metadata attributes:
    public static final String ATTR_ID = "id";
    public static final String ATTR_NAMESPACE = "namespace";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_VERSION = "version";
    public static final String ATTR_DESCRIPTION = "description";

    // Deploy attributes:
    public static final String ATTR_APP_CONTENT_DIGEST = "app-content-digest";

    // Metadata environment variables:
    public static final String ENV_DEPLOY_ATTRIBUTES = "DEPLOY_ATTRIBUTES";
    public static final String ENV_DEPLOY_ID = "DEPLOY_ID";

    // Variables
    public static final String ATTR_CORRELATION_ID = "correlation_id";

    public static final String B3_TRACE_ID_HEADER = "X-B3-TraceId";
    public static final String B3_SPAN_ID_HEADER = "X-B3-SpanId";

    protected Constants() {
    }
}
