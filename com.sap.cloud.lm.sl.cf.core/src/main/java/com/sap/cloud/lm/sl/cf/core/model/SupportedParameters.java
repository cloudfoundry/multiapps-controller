package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Arrays;
import java.util.Collections;
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
    @Deprecated
    public static final String DEPLOY_TARGET = "deploy-target";
    public static final String PROTOCOL = "protocol";
    public static final String XS_TYPE = "xs-type";
    @Deprecated
    public static final String XS_TARGET_API_URL = "xs-api-url";
    @Deprecated
    public static final String XS_AUTHORIZATION_ENDPOINT = "xs-auth-url";
    public static final String CONTROLLER_URL = "controller-url";
    public static final String AUTHORIZATION_URL = "authorization-url";
    public static final String DEPLOY_SERVICE_URL = "deploy-url";
    public static final String GENERATED_USER = "generated-user";
    public static final String GENERATED_PASSWORD = "generated-password";
    public static final String DEFAULT_IDLE_DOMAIN = "default-idle-domain";
    public static final String TIMESTAMP = "timestamp";
    public static final String ENABLE_PARALLEL_DEPLOYMENTS = "enable-parallel-deployments";

    // Module / module type parameters:
    public static final String APP_NAME = "app-name";
    public static final String DOMAIN = "domain";
    public static final String DOMAINS = "domains";
    public static final String DEFAULT_HOST = "default-host";
    public static final String HOST = "host";
    public static final String HOSTS = "hosts";
    public static final String DEFAULT_PORT = "default-port";
    public static final String PORT = "port";
    public static final String PORTS = "ports";
    public static final String KEEP_EXISTING_ROUTES = "keep-existing-routes";
    public static final String ROUTE = "route";
    public static final String ROUTES = "routes";
    public static final String TCP = "tcp";
    public static final String TCPS = "tcps";
    public static final String COMMAND = "command";
    public static final String BUILDPACK = "buildpack";
    public static final String STACK = "stack";
    public static final String HEALTH_CHECK_TIMEOUT = "health-check-timeout";
    public static final String HEALTH_CHECK_TYPE = "health-check-type";
    public static final String HEALTH_CHECK_HTTP_ENDPOINT = "health-check-http-endpoint";
    public static final String UPLOAD_TIMEOUT = "upload-timeout";
    public static final String DISK_QUOTA = "disk-quota";
    public static final String MEMORY = "memory";
    public static final String INSTANCES = "instances";
    public static final String ENABLE_SSH = "enable-ssh";
    public static final String NO_HOSTNAME = "no-hostname";
    public static final String NO_ROUTE = "no-route";
    public static final String DEFAULT_URI = "default-uri";
    public static final String DEFAULT_IDLE_URI = "default-idle-uri";
    public static final String DEFAULT_URL = "default-url";
    public static final String DEFAULT_IDLE_URL = "default-idle-url";
    public static final String ROUTE_PATH = "route-path";
    public static final String DEFAULT_IDLE_HOST = "default-idle-host";
    public static final String DEFAULT_IDLE_PORT = "default-idle-port";
    public static final String IDLE_PORT = "idle-port";
    public static final String IDLE_DOMAIN = "idle-domain";
    public static final String IDLE_HOST = "idle-host";
    public static final String IDLE_PORTS = "idle-ports";
    public static final String IDLE_DOMAINS = "idle-domains";
    public static final String IDLE_HOSTS = "idle-hosts";
    public static final String CREATE_USER_PROVIDED_SERVICE = "create-user-provided-service";
    public static final String USER_PROVIDED_SERVICE_NAME = "user-provided-service-name";
    public static final String USER_PROVIDED_SERVICE_CONFIG = "user-provided-service-config";
    public static final String DEPENDENCY_TYPE = "dependency-type";
    public static final String TASKS = "tasks";
    public static final String RESTART_ON_ENV_CHANGE = "restart-on-env-change";
    public static final String VCAP_APPLICATION_ENV = "vcap-application";
    public static final String VCAP_SERVICES_ENV = "vcap-services";
    public static final String USER_PROVIDED_ENV = "user-provided";
    public static final String DOCKER = "docker";

    public static final String EXECUTE_APP = "execute-app";
    public static final String SUCCESS_MARKER = "success-marker";
    public static final String FAILURE_MARKER = "failure-marker";
    public static final String STOP_APP = "stop-app";
    public static final String NO_START = "no-start";
    public static final String CHECK_DEPLOY_ID = "check-deploy-id";

    public static final String REGISTER_SERVICE_URL = "register-service-url";
    public static final String REGISTER_SERVICE_URL_SERVICE_NAME = "service-name";
    public static final String REGISTER_SERVICE_URL_SERVICE_URL = "service-url";

    public static final String CREATE_SERVICE_BROKER = "create-service-broker";
    public static final String SERVICE_BROKER_NAME = "service-broker-name";
    public static final String SERVICE_BROKER_USERNAME = "service-broker-user";
    public static final String SERVICE_BROKER_PASSWORD = "service-broker-password";
    public static final String SERVICE_BROKER_URL = "service-broker-url";
    public static final String SERVICE_BROKER_SPACE_SCOPED = "service-broker-space-scoped";

    public static final String DEFAULT_RT = "default-resource-type";

    // Required dependency parameters:
    public static final String SERVICE_BINDING_CONFIG = "config";
    public static final String SERVICE_BINDING_CONFIG_PATH = "config-path";
    public static final String MANAGED = "managed";
    public static final String ENV_VAR_NAME = "env-var-name";

    // Resource / resource type parameters:
    public static final String SERVICE_NAME = "service-name";
    public static final String SERVICE = "service";
    public static final String SERVICE_PLAN = "service-plan";
    public static final String SERVICE_ALTERNATIVES = "service-alternatives";
    public static final String SERVICE_PROVIDER = "service-provider";
    public static final String SERVICE_VERSION = "service-version";
    public static final String SERVICE_CONFIG = "config";
    public static final String SERVICE_CONFIG_PATH = "config-path";
    public static final String SERVICE_TAGS = "service-tags";
    public static final String SERVICE_KEYS = "service-keys";
    public static final String NAME = "name";
    public static final String SERVICE_KEY_CONFIG = "config";
    public static final String SERVICE_KEY_NAME = "service-key-name";
    public static final String DEFAULT_CONTAINER_NAME = "default-container-name";
    public static final String DEFAULT_XS_APP_NAME = "default-xsappname";
    public static final String TYPE = "type";
    public static final String IGNORE_UPDATE_ERRORS = "ignore-update-errors";

    // Configuration reference (new syntax):
    public static final String PROVIDER_NID = "provider-nid";
    public static final String VERSION = "version";
    public static final String PROVIDER_ID = "provider-id";
    public static final String TARGET = "target";
    public static final String FILTER = "filter";
    public static final String VISIBILITY = "visibility";
    // Configuration reference (old syntax):
    public static final String MTA_VERSION = "mta-version";
    public static final String MTA_ID = "mta-id";
    public static final String MTA_MODULE = "mta-module";
    public static final String MTA_PROVIDES_DEPENDENCY = "mta-provides-dependency";

    // Platform / platform type parameters:
    public static final String ORG = "org";
    public static final String SPACE = "space";

    public static final Set<String> CONFIGURATION_REFERENCE_PARAMETERS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(PROVIDER_NID, PROVIDER_ID, TARGET, VERSION, MTA_ID, MTA_VERSION, MTA_PROVIDES_DEPENDENCY)));

    public static final Set<String> APP_PROPS = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList(APP_NAME, HOST, HOSTS, DOMAIN, DOMAINS, PORT, PORTS, COMMAND, BUILDPACK,
            HEALTH_CHECK_TYPE, HEALTH_CHECK_HTTP_ENDPOINT, ENABLE_SSH, STACK, HEALTH_CHECK_TIMEOUT, IDLE_HOST, MEMORY, INSTANCES,
            NO_HOSTNAME, NO_ROUTE, IDLE_PORT, IDLE_DOMAIN, DISK_QUOTA, IDLE_PORTS, IDLE_DOMAINS, IDLE_HOSTS, TASKS, RESTART_ON_ENV_CHANGE,
            VCAP_APPLICATION_ENV, VCAP_SERVICES_ENV, USER_PROVIDED_ENV, KEEP_EXISTING_ROUTES)));

    public static final Set<String> SERVICE_PROPS = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList(SERVICE_NAME, SERVICE, SERVICE_PLAN, SERVICE_ALTERNATIVES, SERVICE_PROVIDER,
            SERVICE_VERSION, SERVICE_CONFIG, SERVICE_CONFIG_PATH, SERVICE_TAGS, SERVICE_KEY_NAME)));

    public static final Set<String> APP_ATTRIBUTES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EXECUTE_APP, SUCCESS_MARKER,
        FAILURE_MARKER, STOP_APP, CHECK_DEPLOY_ID, REGISTER_SERVICE_URL, REGISTER_SERVICE_URL_SERVICE_NAME,
        REGISTER_SERVICE_URL_SERVICE_URL, CREATE_SERVICE_BROKER, SERVICE_BROKER_NAME, SERVICE_BROKER_USERNAME, SERVICE_BROKER_PASSWORD,
        SERVICE_BROKER_URL, SERVICE_BROKER_SPACE_SCOPED, DEPENDENCY_TYPE, NO_START, UPLOAD_TIMEOUT)));

    public static final Set<String> SPECIAL_MT_PROPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(DEFAULT_RT)));

    public static final Set<String> SPECIAL_RT_PROPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(TYPE)));

    public static final Map<String, String> SINGULAR_PLURAL_MAPPING;

    static {
        Map<String, String> prototype = new HashMap<>();
        prototype.put(IDLE_HOST, IDLE_HOSTS);
        prototype.put(IDLE_DOMAIN, IDLE_DOMAINS);
        prototype.put(IDLE_PORT, IDLE_PORTS);

        prototype.put(ROUTE, ROUTES);
        prototype.put(HOST, HOSTS);
        prototype.put(DOMAIN, DOMAINS);
        prototype.put(PORT, PORTS);
        SINGULAR_PLURAL_MAPPING = Collections.unmodifiableMap(prototype);
    }

    public enum RoutingParameterSet {
        // @formatter:off
        ACTUAL(PORT,HOST,DOMAIN),
        DEFAULT(DEFAULT_PORT, DEFAULT_HOST, DEFAULT_DOMAIN),
        IDLE(IDLE_PORT, IDLE_HOST, IDLE_DOMAIN),
        DEFAULT_IDLE(DEFAULT_IDLE_PORT,DEFAULT_IDLE_HOST,DEFAULT_IDLE_DOMAIN);
        // @formatter:on
        public final String port;
        public final String domain;
        public final String host;

        RoutingParameterSet(String port, String host, String domain) {
            this.port = port;
            this.host = host;
            this.domain = domain;
        }
    }

}
