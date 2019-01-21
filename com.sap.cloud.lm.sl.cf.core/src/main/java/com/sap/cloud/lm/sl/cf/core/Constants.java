package com.sap.cloud.lm.sl.cf.core;

public class Constants {

    public static final String DEPENDENCY_TYPE_SOFT = "soft";
    public static final String DEPENDENCY_TYPE_HARD = "hard";

    public static final String UNIX_PATH_SEPARATOR = "/";
    public static final String MTA_ELEMENT_SEPARATOR = "/";
    public static final String MODULE_SEPARATOR = ",";

    // Metadata attributes:
    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_VERSION = "version";
    public static final String ATTR_DESCRIPTION = "description";
    public static final String ATTR_PROVIDER = "provider";
    public static final String ATTR_COPYRIGHT = "copyright";

    // Deploy attributes:
    public static final String ATTR_APP_CONTENT_DIGEST = "app-content-digest";

    // Metadata environment variables:
    public static final String ENV_MTA_METADATA = "MTA_METADATA";
    public static final String ENV_MTA_MODULE_METADATA = "MTA_MODULE_METADATA";
    public static final String ENV_MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES = "MTA_MODULE_PROVIDED_DEPENDENCIES";
    public static final String ENV_MTA_SERVICES = "MTA_SERVICES";
    public static final String ENV_DEPLOY_ATTRIBUTES = "DEPLOY_ATTRIBUTES";
    public static final String ENV_DEPLOY_ID = "DEPLOY_ID";

    // Variables
    public static final String ATTR_CORRELATION_ID = "correlation-id";

    // Numeric constants
    public static final long DEFAULT_MAX_MTA_DESCRIPTOR_SIZE = 1024 * 1024l;
    
    public static final String SERVICE_INSTANCE_RESPONSE_RESOURCES = "resources";
    public static final String SERVICE_INSTANCE_RESPONSE_ENTITY = "entity";
    public static final String V2_QUERY_SEPARATOR = "&q=";
}
