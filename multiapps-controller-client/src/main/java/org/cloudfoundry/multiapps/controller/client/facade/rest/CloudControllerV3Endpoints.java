package org.cloudfoundry.multiapps.controller.client.facade.rest;

/**
 * The Cloud Controller v3 API surface used by the in-house CF client — resource roots, {@code {guid}} path templates, and query-parameter
 * fragments — in one place, so the {@code <Group>V3Operations} classes reference named constants instead of inline "magic string" URLs.
 * <p>
 * Two URI mechanisms are represented, matching how the operations classes issue requests:
 * <ul>
 * <li><b>Path templates</b> (e.g. {@link #APP_BY_GUID}) contain Spring {@code {placeholder}}s expanded by {@code RestClient.uri(template,
 * args...)} — the placeholders are intentionally kept literally.</li>
 * <li><b>Roots + query fragments</b> (e.g. {@link #APPS} + {@link #QUERY_PER_PAGE} + {@link #AMPERSAND_SPACE_GUIDS}) are concatenated by the caller
 * into a query string. The fragments include their leading {@code ?}/{@code &} and trailing {@code =} exactly as the callers write them,
 * so swapping a literal for a constant is a byte-identical substitution.</li>
 * </ul>
 */
public final class CloudControllerV3Endpoints {

    private CloudControllerV3Endpoints() {
    }

    public static final int DEFAULT_PAGE_SIZE = 5000;

    public static final String APPS = "/v3/apps";
    public static final String ROUTES = "/v3/routes";
    public static final String DOMAINS = "/v3/domains";
    public static final String SERVICE_INSTANCES = "/v3/service_instances";
    public static final String SERVICE_CREDENTIAL_BINDINGS = "/v3/service_credential_bindings";
    public static final String SERVICE_BROKERS = "/v3/service_brokers";
    public static final String SERVICE_OFFERINGS = "/v3/service_offerings";
    public static final String SERVICE_PLANS = "/v3/service_plans";
    public static final String PACKAGES = "/v3/packages";
    public static final String BUILDS = "/v3/builds";
    public static final String TASKS = "/v3/tasks";
    public static final String PROCESSES = "/v3/processes";
    public static final String STACKS = "/v3/stacks";
    public static final String AUDIT_EVENTS = "/v3/audit_events";
    public static final String JOBS = "/v3/jobs";
    public static final String ROLES = "/v3/roles";
    public static final String ORGANIZATIONS = "/v3/organizations";
    public static final String SPACES = "/v3/spaces";

    public static final String APP_BY_GUID = "/v3/apps/{guid}";
    public static final String APP_START = "/v3/apps/{guid}/actions/start";
    public static final String APP_STOP = "/v3/apps/{guid}/actions/stop";
    public static final String APP_ENV_VARS = "/v3/apps/{guid}/environment_variables";
    public static final String APP_FEATURE = "/v3/apps/{guid}/features/{name}";
    public static final String APP_WEB_PROCESS_SCALE = "/v3/apps/{guid}/processes/web/actions/scale";
    public static final String APP_CURRENT_DROPLET = "/v3/apps/{guid}/relationships/current_droplet";
    public static final String APP_TASKS = "/v3/apps/{guid}/tasks";
    public static final String ROUTE_BY_GUID = "/v3/routes/{guid}";
    public static final String ROUTE_DESTINATIONS = "/v3/routes/{guid}/destinations";
    public static final String ROUTE_DESTINATION_BY_GUID = "/v3/routes/{routeGuid}/destinations/{destinationGuid}";
    public static final String DOMAIN_BY_GUID = "/v3/domains/{guid}";
    public static final String SERVICE_INSTANCE_BY_GUID = "/v3/service_instances/{guid}";
    public static final String SERVICE_BROKER_BY_GUID = "/v3/service_brokers/{guid}";
    public static final String SERVICE_CREDENTIAL_BINDING_BY_GUID = "/v3/service_credential_bindings/{guid}";
    public static final String SERVICE_PLAN_VISIBILITY = "/v3/service_plans/{guid}/visibility";
    public static final String PROCESS_BY_GUID = "/v3/processes/{guid}";
    public static final String TASK_CANCEL = "/v3/tasks/{guid}/actions/cancel";
    public static final String PACKAGE_UPLOAD = "/v3/packages/{guid}/upload";
    public static final String SPACE_UNMAPPED_ROUTES = "/v3/spaces/{guid}/routes?unmapped=true";

    public static final String QUERY_PER_PAGE = "?per_page=";
    public static final String QUERY_NAMES = "?names=";
    public static final String QUERY_PACKAGE_GUIDS = "?package_guids=";
    public static final String QUERY_APP_GUIDS = "?app_guids=";
    public static final String QUERY_GUIDS = "?guids=";
    public static final String QUERY_SERVICE_INSTANCE_GUIDS = "?service_instance_guids=";
    public static final String QUERY_SERVICE_OFFERING_GUIDS = "?service_offering_guids=";
    public static final String QUERY_SERVICE_BROKER_GUIDS = "?service_broker_guids=";
    public static final String QUERY_ORGANIZATION_GUIDS = "?organization_guids=";

    public static final String AMPERSAND_PER_PAGE = "&per_page=";
    public static final String AMPERSAND_SPACE_GUIDS = "&space_guids=";
    public static final String AMPERSAND_NAMES = "&names=";
    public static final String AMPERSAND_TYPE = "&type=";
    public static final String AMPERSAND_LABEL_SELECTOR = "&label_selector=";
    public static final String AMPERSAND_DOMAIN_GUIDS = "&domain_guids=";
    public static final String AMPERSAND_SERVICE_INSTANCE_GUIDS = "&service_instance_guids=";
    public static final String AMPERSAND_SERVICE_OFFERING_GUIDS = "&service_offering_guids=";
    public static final String AMPERSAND_SERVICE_BROKER_NAMES = "&service_broker_names=";
    public static final String AMPERSAND_APP_GUIDS = "&app_guids=";
    public static final String AMPERSAND_USER_GUIDS = "&user_guids=";
    public static final String AMPERSAND_TARGET_GUIDS = "&target_guids=";
    public static final String AMPERSAND_HOSTS = "&hosts=";
    public static final String AMPERSAND_PATHS = "&paths=";

}
