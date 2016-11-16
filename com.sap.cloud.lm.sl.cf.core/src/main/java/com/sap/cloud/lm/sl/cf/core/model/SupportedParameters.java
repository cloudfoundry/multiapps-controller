package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SupportedParameters {

    // XSA placeholders:
    public static final String XSA_CONTROLLER_ENDPOINT_PLACEHOLDER = "{xsa-placeholder-endpoint-controller}";
    public static final String XSA_DEFAULT_DOMAIN_PLACEHOLDER = "{xsa-placeholder-domain-default}";
    public static final String XSA_PROTOCOL_PLACEHOLDER = "{xsa-placeholder-protocol}";
    public static final String XSA_ROUTER_PORT_PLACEHOLDER = "{xsa-placeholder-router-port}";
    public static final String XSA_AUTHORIZATION_ENDPOINT_PLACEHOLDER = "{xsa-placeholder-endpoint-authorization}";
    public static final String XSA_DEPLOY_SERVICE_URL_PLACEHOLDER = "{xsa-placeholder-service-url-deploy-service}";

    // General parameters:
    public static final String USER = "user";
    public static final String DEFAULT_DOMAIN = "default-domain";
    public static final String PLATFORM = "platform";
    public static final String PROTOCOL = "protocol";
    public static final String XS_TYPE = "xs-type";
    public static final String XS_TARGET_API_URL = "xs-api-url";
    public static final String XS_AUTHORIZATION_ENDPOINT = "xs-auth-url";
    public static final String DEPLOY_SERVICE_URL = "deploy-url";
    public static final String GENERATED_USER = "generated-user";
    public static final String GENERATED_PASSWORD = "generated-password";
    public static final String DEFAULT_TEMP_DOMAIN = "default-temp-domain";

    // Module / module type parameters:
    public static final String APP_NAME = "app-name";
    public static final String DOMAIN = "domain";
    public static final String DEFAULT_HOST = "default-host";
    public static final String HOSTS = "hosts";
    public static final String DOMAINS = "domains";
    public static final String DEFAULT_PORT = "default-port";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String PORTS = "ports";
    public static final String COMMAND = "command";
    public static final String BUILDPACK = "buildpack";
    public static final String STACK = "stack";
    public static final String HEALTH_CHECK_TIMEOUT = "health-check-timeout";
    public static final String HEALTH_CHECK_TYPE = "health-check-type";
    public static final String DISK_QUOTA = "disk-quota";
    public static final String MEMORY = "memory";
    public static final String INSTANCES = "instances";
    public static final String NO_HOSTNAME = "no-hostname";
    public static final String NO_ROUTE = "no-route";
    public static final String DEFAULT_URI = "default-uri";
    public static final String DEFAULT_URL = "default-url";
    public static final String ROUTE_PATH = "route-path";
    public static final String DEFAULT_TEMP_HOST = "default-temp-host";
    public static final String DEFAULT_TEMP_PORT = "default-temp-port";
    public static final String TEMP_PORT = "temp-port";
    public static final String TEMP_DOMAIN = "temp-domain";
    public static final String TEMP_HOST = "temp-host";
    public static final String TEMP_PORTS = "temp-ports";
    public static final String TEMP_DOMAINS = "temp-domains";
    public static final String TEMP_HOSTS = "temp-hosts";
    public static final String CREATE_USER_PROVIDED_SERVICE = "create-user-provided-service";
    public static final String USER_PROVIDED_SERVICE_NAME = "user-provided-service-name";
    public static final String USER_PROVIDED_SERVICE_CONFIG = "user-provided-service-config";
    public static final String DEPENDENCY_TYPE = "dependency-type";

    public static final String EXECUTE_APP = "execute-app";
    public static final String SUCCESS_MARKER = "success-marker";
    public static final String FAILURE_MARKER = "failure-marker";
    public static final String STOP_APP = "stop-app";
    public static final String CHECK_DEPLOY_ID = "check-deploy-id";

    public static final String REGISTER_SERVICE_URL = "register-service-url";
    public static final String REGISTER_SERVICE_URL_SERVICE_NAME = "service-name";
    public static final String REGISTER_SERVICE_URL_SERVICE_URL = "service-url";

    public static final String CREATE_SERVICE_BROKER = "create-service-broker";
    public static final String SERVICE_BROKER_NAME = "service-broker-name";
    public static final String SERVICE_BROKER_USER = "service-broker-user";
    public static final String SERVICE_BROKER_PASSWORD = "service-broker-password";
    public static final String SERVICE_BROKER_URL = "service-broker-url";

    public static final String DEFAULT_RT = "default-resource-type";

    // Required dependency parameters:
    public static final String SERVICE_BINDING_CONFIG = "config";
    public static final String SERVICE_BINDING_CONFIG_PATH = "config-path";
    public static final String MANAGED = "managed";

    // Resource / resource type parameters:
    public static final String SERVICE_NAME = "service-name";
    public static final String SERVICE = "service";
    public static final String SERVICE_PLAN = "service-plan";
    public static final String SERVICE_PROVIDER = "service-provider";
    public static final String SERVICE_VERSION = "service-version";
    public static final String SERVICE_CONFIG = "config";
    public static final String SERVICE_CONFIG_PATH = "config-path";
    public static final String SERVICE_TAGS = "service-tags";
    public static final String SERVICE_KEYS = "service-keys";
    public static final String SERVICE_KEY_NAME = "name";
    public static final String SERVICE_KEY_CONFIG = "config";
    public static final String DEFAULT_CONTAINER_NAME = "default-container-name";
    public static final String DEFAULT_XS_APP_NAME = "default-xsappname";
    public static final String TYPE = "type";
    // Configuration reference (new syntax):
    public static final String PROVIDER_NID = "provider-nid";
    public static final String VERSION = "version";
    public static final String PROVIDER_ID = "provider-id";
    public static final String TARGET = "target";
    public static final String FILTER = "filter";
    // Configuration reference (old syntax):
    public static final String MTA_VERSION = "mta-version";
    public static final String MTA_ID = "mta-id";
    public static final String MTA_MODULE = "mta-module";
    public static final String MTA_PROVIDES_DEPENDENCY = "mta-provides-dependency";

    // Platform / platform type parameters:
    public static final String ORG = "org";
    public static final String SPACE = "space";

    public static final Map<String, String> SINGULAR_PLURAL_MAPPING = new HashMap<>();

    public static final Set<String> CONFIGURATION_REFERENCE_PARAMETERS = new HashSet<>(
        Arrays.asList(PROVIDER_NID, PROVIDER_ID, TARGET, VERSION, MTA_ID, MTA_VERSION, MTA_PROVIDES_DEPENDENCY));

    public static final Set<String> APP_PROPS = new HashSet<>(
        Arrays.asList(APP_NAME, HOST, HOSTS, DOMAIN, DOMAINS, PORT, PORTS, COMMAND, BUILDPACK, STACK, HEALTH_CHECK_TIMEOUT, TEMP_HOST,
            MEMORY, INSTANCES, NO_HOSTNAME, NO_ROUTE, TEMP_PORT, TEMP_DOMAIN, DISK_QUOTA, TEMP_PORTS, TEMP_DOMAINS, TEMP_HOSTS));

    public static final Set<String> SERVICE_PROPS = new HashSet<>(Arrays.asList(SERVICE_NAME, SERVICE, SERVICE_PLAN, SERVICE_PROVIDER,
        SERVICE_VERSION, SERVICE_CONFIG, SERVICE_CONFIG_PATH, SERVICE_TAGS));

    public static final Set<String> APP_ATTRIBUTES = new HashSet<>(Arrays.asList(EXECUTE_APP, SUCCESS_MARKER, FAILURE_MARKER, STOP_APP,
        CHECK_DEPLOY_ID, REGISTER_SERVICE_URL, REGISTER_SERVICE_URL_SERVICE_NAME, REGISTER_SERVICE_URL_SERVICE_URL, CREATE_SERVICE_BROKER,
        SERVICE_BROKER_NAME, SERVICE_BROKER_USER, SERVICE_BROKER_PASSWORD, SERVICE_BROKER_URL, DEPENDENCY_TYPE));

    public static final Set<String> SPECIAL_MT_PROPS = new HashSet<>(Arrays.asList(DEFAULT_RT));

    public static final Set<String> SPECIAL_RT_PROPS = new HashSet<>(Arrays.asList(TYPE));

    static {
        SINGULAR_PLURAL_MAPPING.put(TEMP_HOST, TEMP_HOSTS);
        SINGULAR_PLURAL_MAPPING.put(TEMP_DOMAIN, TEMP_DOMAINS);
        SINGULAR_PLURAL_MAPPING.put(TEMP_PORT, TEMP_PORTS);

        SINGULAR_PLURAL_MAPPING.put(HOST, HOSTS);
        SINGULAR_PLURAL_MAPPING.put(DOMAIN, DOMAINS);
        SINGULAR_PLURAL_MAPPING.put(PORT, PORTS);
    }

}
