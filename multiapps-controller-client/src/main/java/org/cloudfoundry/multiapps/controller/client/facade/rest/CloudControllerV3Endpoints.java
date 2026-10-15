package org.cloudfoundry.multiapps.controller.client.facade.rest;

public class CloudControllerV3Endpoints {

    //leaving it like that for testing, then if all is alright with the performance, we can remove it and just not pass it since the default is 50 from cf controller side
    public static final int DEFAULT_PAGE_SIZE = 50;

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
    public static final String ORGANIZATION_DEFAULT_DOMAIN = "/v3/organizations/{guid}/domains/default";
    public static final String ORGANIZATION_DOMAINS = "/v3/organizations/{guid}/domains";
    public static final String ORGANIZATION_BY_GUID = "/v3/organizations/{guid}";
    public static final String SPACE_BY_GUID = "/v3/spaces/{guid}";
    public static final String JOB_BY_GUID = "/v3/jobs/{guid}";
    public static final String BUILD_BY_GUID = "/v3/builds/{guid}";
    public static final String PACKAGE_BY_GUID = "/v3/packages/{guid}";
    public static final String TASK_BY_GUID = "/v3/tasks/{guid}";
    public static final String SERVICE_PLAN_BY_GUID = "/v3/service_plans/{guid}";
    public static final String SERVICE_OFFERING_BY_GUID = "/v3/service_offerings/{guid}";
    public static final String SERVICE_INSTANCE_PARAMETERS = "/v3/service_instances/{guid}/parameters";
    public static final String SERVICE_INSTANCE_CREDENTIALS = "/v3/service_instances/{guid}/credentials";
    public static final String SERVICE_CREDENTIAL_BINDING_PARAMETERS = "/v3/service_credential_bindings/{guid}/parameters";
    public static final String SERVICE_CREDENTIAL_BINDING_DETAILS = "/v3/service_credential_bindings/{guid}/details";
    public static final String APP_WEB_PROCESS = "/v3/apps/{guid}/processes/web";
    public static final String APP_WEB_PROCESS_STATS = "/v3/apps/{guid}/processes/web/stats";
    public static final String APP_SSH_ENABLED = "/v3/apps/{guid}/ssh_enabled";
    public static final String APP_FEATURES = "/v3/apps/{guid}/features";
    public static final String APP_DROPLET_CURRENT = "/v3/apps/{guid}/droplets/current";
    public static final String APP_BUILDS = "/v3/apps/{guid}/builds";
    public static final String APP_PACKAGES = "/v3/apps/{guid}/packages";
    public static final String APP_ROUTES = "/v3/apps/{guid}/routes";

    private CloudControllerV3Endpoints() {

    }

}
