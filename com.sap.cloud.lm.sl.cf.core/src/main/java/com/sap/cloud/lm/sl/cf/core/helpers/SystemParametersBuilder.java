package com.sap.cloud.lm.sl.cf.core.helpers;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientFactory.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.HostValidator;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;

public class SystemParametersBuilder {

    public static final int GENERATED_CREDENTIALS_LENGTH = 16;
    public static final String TEMP_HOST_SUFFIX = "-temp";
    private static final String ROUTE_PATH_PLACEHOLDER = "${route-path}";

    private static final HostValidator HOST_VALIDATOR = new HostValidator();

    private final CredentialsGenerator credentialsGenerator;
    private final String platformName;
    private final String organization;
    private final String space;
    private final String user;
    private final String defaultDomain;
    private final PlatformType xsType;
    private final URL targetUrl;
    private final String authorizationEndpoint;
    private final String deployServiceUrl;
    private final int routerPort;
    private final boolean portBasedRouting;
    private final PortAllocator portAllocator;
    private final Map<String, List<Integer>> occupiedPorts;
    private final boolean useNamespaces;
    private final boolean useNamespacesForServices;
    private final DeployedMta deployedMta;
    private final boolean reserveTemporaryRoutes;
    private final boolean areXsPlaceholdersSupported;
    private final PropertiesAccessor propertiesAccessor;

    public SystemParametersBuilder(String platformName, String organization, String space, String user, String defaultDomain,
        PlatformType xsType, URL targetUrl, String authorizationEndpoint, String deployServiceUrl, int routerPort, boolean portBasedRouting,
        boolean reserveTemporaryRoutes, PortAllocator portAllocator, Map<String, List<Integer>> occupiedPorts, boolean useNamespaces,
        boolean useNamespacesForServices, DeployedMta deployedMta, int majorSchemaVersion, boolean areXsPlaceholdersSupported) {
        this(platformName, organization, space, user, defaultDomain, xsType, targetUrl, authorizationEndpoint, deployServiceUrl, routerPort,
            portBasedRouting, reserveTemporaryRoutes, portAllocator, occupiedPorts, useNamespaces, useNamespacesForServices, deployedMta,
            new CredentialsGenerator(), majorSchemaVersion, areXsPlaceholdersSupported);
    }

    public SystemParametersBuilder(String platformName, String organization, String space, String user, String defaultDomain,
        PlatformType xsType, URL targetUrl, String authorizationEndpoint, String deployServiceUrl, int routerPort, boolean portBasedRouting,
        boolean reserveTemporaryRoutes, PortAllocator portAllocator, Map<String, List<Integer>> occupiedPorts, boolean useNamespaces,
        boolean useNamespacesForServices, DeployedMta deployedMta, CredentialsGenerator credentialsGenerator, int majorSchemaVersion,
        boolean areXsPlaceholdersSupported) {
        this.platformName = platformName;
        this.organization = organization;
        this.space = space;
        this.user = user;
        this.defaultDomain = defaultDomain;
        this.xsType = xsType;
        this.targetUrl = targetUrl;
        this.authorizationEndpoint = authorizationEndpoint;
        this.deployServiceUrl = deployServiceUrl;
        this.routerPort = routerPort;
        this.portBasedRouting = portBasedRouting;
        this.portAllocator = portAllocator;
        this.occupiedPorts = occupiedPorts;
        this.useNamespacesForServices = useNamespacesForServices;
        this.useNamespaces = useNamespaces;
        this.deployedMta = deployedMta;
        this.credentialsGenerator = credentialsGenerator;
        this.reserveTemporaryRoutes = reserveTemporaryRoutes;
        this.areXsPlaceholdersSupported = areXsPlaceholdersSupported;
        this.propertiesAccessor = new HandlerFactory(majorSchemaVersion).getPropertiesAccessor();
    }

    public SystemParameters build(DeploymentDescriptor descriptor) throws SLException {
        Map<String, Map<String, Object>> moduleParameters = new HashMap<>();
        for (Module module : descriptor.getModules1_0()) {
            moduleParameters.put(module.getName(), getModuleParameters(module, descriptor.getId()));
        }

        Map<String, Map<String, Object>> resourceParameters = new HashMap<>();
        for (Resource resource : descriptor.getResources1_0()) {
            resourceParameters.put(resource.getName(), getResourceParameters(resource, descriptor.getId()));
        }
        return new SystemParameters(getGeneralParameters(), moduleParameters, resourceParameters,
            SupportedParameters.SINGULAR_PLURAL_MAPPING);
    }

    private Map<String, Object> getGeneralParameters() {
        Map<String, Object> systemParameters = new HashMap<>();

        systemParameters.put(SupportedParameters.PLATFORM, platformName);
        systemParameters.put(SupportedParameters.ORG, organization);
        systemParameters.put(SupportedParameters.USER, user);
        systemParameters.put(SupportedParameters.SPACE, space);
        systemParameters.put(SupportedParameters.DEFAULT_DOMAIN, getDefaultDomain());
        if (shouldReserveTemporaryRoutes()) {
            systemParameters.put(SupportedParameters.DEFAULT_TEMP_DOMAIN, getDefaultDomain());
        }
        systemParameters.put(SupportedParameters.XS_TARGET_API_URL, getTargetUrl());
        systemParameters.put(SupportedParameters.XS_TYPE, xsType.toString());
        systemParameters.put(SupportedParameters.PROTOCOL, getProtocol());
        systemParameters.put(SupportedParameters.XS_AUTHORIZATION_ENDPOINT, getAuthorizationEndpoint());
        systemParameters.put(SupportedParameters.DEPLOY_SERVICE_URL, getDeployServiceUrl());

        return systemParameters;
    }

    private Map<String, Object> getModuleParameters(Module module, String mtaId) throws SLException {
        Map<String, Object> moduleSystemParameters = new HashMap<>();

        Map<String, Object> moduleParameters = propertiesAccessor.getParameters(module);
        moduleSystemParameters.put(SupportedParameters.DOMAIN, getDefaultDomain());
        String appName = (String) moduleParameters.getOrDefault(SupportedParameters.APP_NAME, module.getName());
        moduleSystemParameters.put(SupportedParameters.APP_NAME, NameUtil.getApplicationName(appName, mtaId, useNamespaces));
        String defaultHost = getDefaultHost(module.getName());
        moduleSystemParameters.put(SupportedParameters.DEFAULT_HOST, defaultHost);
        moduleSystemParameters.put(SupportedParameters.HOST, defaultHost);
        if (shouldReserveTemporaryRoutes()) {
            String tempDefaultHost = getDefaultHost(module.getName() + TEMP_HOST_SUFFIX);
            moduleSystemParameters.put(SupportedParameters.DEFAULT_TEMP_HOST, tempDefaultHost);
            moduleSystemParameters.put(SupportedParameters.TEMP_HOST, tempDefaultHost);
        }
        if (portBasedRouting) {
            int defaultPort = getDefaultPort(getExistingApplicationName(module.getName()));
            moduleSystemParameters.put(SupportedParameters.DEFAULT_PORT, defaultPort);
            moduleSystemParameters.put(SupportedParameters.PORT, defaultPort);
            if (shouldReserveTemporaryRoutes()) {
                int tempDefaultPort = portAllocator.allocatePort();
                moduleSystemParameters.put(SupportedParameters.DEFAULT_TEMP_PORT, tempDefaultPort);
                moduleSystemParameters.put(SupportedParameters.TEMP_PORT, tempDefaultPort);
            }
            moduleSystemParameters.put(SupportedParameters.DEFAULT_URI, appendRoutePathIfPresent("${domain}:${port}", moduleParameters));
        } else {
            if (!UriUtil.isStandardPort(routerPort, targetUrl.getProtocol())) {
                moduleSystemParameters.put(SupportedParameters.DEFAULT_URI,
                    appendRoutePathIfPresent("${host}.${domain}:" + getRouterPort(), moduleParameters));
            } else {
                moduleSystemParameters.put(SupportedParameters.DEFAULT_URI,
                    appendRoutePathIfPresent("${host}.${domain}", moduleParameters));
            }
        }
        String defaulUrl = "${protocol}://${default-uri}";
        moduleSystemParameters.put(SupportedParameters.DEFAULT_URL, defaulUrl);
        moduleSystemParameters.put(SupportedParameters.COMMAND, "");
        moduleSystemParameters.put(SupportedParameters.BUILDPACK, "");
        moduleSystemParameters.put(SupportedParameters.DISK_QUOTA, -1);
        moduleSystemParameters.put(SupportedParameters.MEMORY, "256M");
        moduleSystemParameters.put(SupportedParameters.INSTANCES, 1);
        moduleSystemParameters.put(SupportedParameters.SERVICE, "");
        moduleSystemParameters.put(SupportedParameters.SERVICE_PLAN, "");

        moduleSystemParameters.put(SupportedParameters.GENERATED_USER, credentialsGenerator.next(GENERATED_CREDENTIALS_LENGTH));
        moduleSystemParameters.put(SupportedParameters.GENERATED_PASSWORD, credentialsGenerator.next(GENERATED_CREDENTIALS_LENGTH));

        return moduleSystemParameters;
    }

    private Object appendRoutePathIfPresent(String uri, Map<String, Object> moduleParameters) {
        if (moduleParameters.containsKey(SupportedParameters.ROUTE_PATH)) {
            return uri + ROUTE_PATH_PLACEHOLDER;
        }
        return uri;
    }

    private Map<String, Object> getResourceParameters(Resource resource, String mtaId) throws SLException {
        Map<String, Object> resourceSystemParameters = new HashMap<>();

        String serviceName = NameUtil.getServiceName(resource.getName(), mtaId, useNamespaces, useNamespacesForServices);
        resourceSystemParameters.put(SupportedParameters.SERVICE_NAME, serviceName);
        resourceSystemParameters.put(SupportedParameters.SERVICE, "");
        resourceSystemParameters.put(SupportedParameters.SERVICE_PLAN, "");
        resourceSystemParameters.put(SupportedParameters.DEFAULT_CONTAINER_NAME,
            NameUtil.createValidContainerName(organization, space, resource.getName()));
        resourceSystemParameters.put(SupportedParameters.DEFAULT_XS_APP_NAME, NameUtil.createValidXsAppName(resource.getName()));

        resourceSystemParameters.put(SupportedParameters.GENERATED_USER, credentialsGenerator.next(GENERATED_CREDENTIALS_LENGTH));
        resourceSystemParameters.put(SupportedParameters.GENERATED_PASSWORD, credentialsGenerator.next(GENERATED_CREDENTIALS_LENGTH));

        return resourceSystemParameters;
    }

    private String getExistingApplicationName(String moduleName) {
        if (deployedMta == null) {
            return null;
        }
        DeployedMtaModule deployedModule = deployedMta.findDeployedModule(moduleName);
        if (deployedModule != null) {
            return deployedModule.getAppName();
        }
        return null;
    }

    private Integer getDefaultPort(String existingApplicationName) {
        if (existingApplicationName != null && occupiedPorts.containsKey(existingApplicationName)) {
            List<Integer> ports = occupiedPorts.get(existingApplicationName);
            if (ports.size() > 0) {
                return ports.get(0);
            }
        }
        return portAllocator.allocatePort();
    }

    private String getDefaultHost(String moduleName) throws SLException {
        String host = (platformName + " " + moduleName).replaceAll("\\s", "-").toLowerCase();
        if (!HOST_VALIDATOR.validate(host)) {
            return HOST_VALIDATOR.attemptToCorrect(host);
        }
        return host;
    }

    private boolean shouldReserveTemporaryRoutes() {
        return reserveTemporaryRoutes && deployedMta != null;
    }

    private String getDeployServiceUrl() {
        if (shouldUseXsPlaceholders()) {
            return SupportedParameters.XSA_DEPLOY_SERVICE_URL_PLACEHOLDER;
        }
        return deployServiceUrl;
    }

    private String getDefaultDomain() {
        if (shouldUseXsPlaceholders()) {
            return SupportedParameters.XSA_DEFAULT_DOMAIN_PLACEHOLDER;
        }
        return defaultDomain;
    }

    private Object getAuthorizationEndpoint() {
        if (shouldUseXsPlaceholders()) {
            return SupportedParameters.XSA_AUTHORIZATION_ENDPOINT_PLACEHOLDER;
        }
        return authorizationEndpoint;
    }

    private String getRouterPort() {
        if (shouldUseXsPlaceholders()) {
            return SupportedParameters.XSA_ROUTER_PORT_PLACEHOLDER;
        }
        return Integer.toString(routerPort);
    }

    private String getTargetUrl() {
        if (shouldUseXsPlaceholders()) {
            return SupportedParameters.XSA_CONTROLLER_ENDPOINT_PLACEHOLDER;
        }
        return targetUrl.toString();
    }

    private Object getProtocol() {
        if (shouldUseXsPlaceholders()) {
            return SupportedParameters.XSA_PROTOCOL_PLACEHOLDER;
        }
        return targetUrl.getProtocol();
    }

    private boolean shouldUseXsPlaceholders() {
        return xsType.equals(PlatformType.XS2) && areXsPlaceholdersSupported;
    }

}
