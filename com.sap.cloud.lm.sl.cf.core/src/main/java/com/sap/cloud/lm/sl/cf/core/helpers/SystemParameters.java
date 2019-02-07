package com.sap.cloud.lm.sl.cf.core.helpers;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.collections4.CollectionUtils;

import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.HostValidator;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class SystemParameters {

    public static final int GENERATED_CREDENTIALS_LENGTH = 16;
    public static final String IDLE_HOST_SUFFIX = "-idle";
    public static final String ROUTE_PATH_PLACEHOLDER = "${route-path}";
    public static final String DEFAULT_HOST_BASED_IDLE_URI = "${idle-host}.${idle-domain}";
    public static final String DEFAULT_PORT_BASED_IDLE_URI = "${idle-domain}:${idle-port}";
    public static final String DEFAULT_HOST_BASED_URI = "${host}.${domain}";
    public static final String DEFAULT_PORT_BASED_URI = "${domain}:${port}";
    public static final String DEFAULT_IDLE_URL = "${protocol}://${default-idle-uri}";
    public static final String DEFAULT_URL = "${protocol}://${default-uri}";

    private static final HostValidator HOST_VALIDATOR = new HostValidator();

    private final CredentialsGenerator credentialsGenerator;
    private final String targetName;
    private final String organization;
    private final String space;
    private final String user;
    private final String defaultDomain;
    private final PlatformType platformType;
    private final URL controllerUrl;
    private final String authorizationEndpoint;
    private final String deployServiceUrl;
    private final int routerPort;
    private final boolean portBasedRouting;
    private final PortAllocator portAllocator;
    private final DeployedMta deployedMta;
    private final boolean reserveTemporaryRoutes;
    private final boolean areXsPlaceholdersSupported;
    private final Supplier<String> timestampSupplier;
    private final ModuleToDeployHelper moduleToDeployHelper;

    public SystemParameters(Builder builder) {
        this.targetName = builder.organization + " " + builder.space;
        this.organization = builder.organization;
        this.space = builder.space;
        this.user = builder.user;
        this.defaultDomain = builder.defaultDomain;
        this.platformType = builder.platformType;
        this.controllerUrl = builder.controllerUrl;
        this.authorizationEndpoint = builder.authorizationEndpoint;
        this.deployServiceUrl = builder.deployServiceUrl;
        this.routerPort = builder.routerPort;
        this.portBasedRouting = builder.portBasedRouting;
        this.portAllocator = builder.portAllocator;
        this.deployedMta = builder.deployedMta;
        this.credentialsGenerator = builder.credentialsGenerator;
        this.reserveTemporaryRoutes = builder.reserveTemporaryRoutes;
        this.areXsPlaceholdersSupported = builder.areXsPlaceholdersSupported;
        this.timestampSupplier = builder.timestampSupplier;
        this.moduleToDeployHelper = builder.moduleToDeployHelper;
    }

    public void injectInto(DeploymentDescriptor descriptor) {
        for (Module module : descriptor.getModules2()) {
            Map<String, Object> moduleSystemParameters = getModuleParameters(module);
            module.setParameters(MapUtil.merge(moduleSystemParameters, module.getParameters()));
        }
        for (Resource resource : descriptor.getResources2()) {
            Map<String, Object> resourceSystemParameters = getResourceParameters(resource);
            resource.setParameters(MapUtil.merge(resourceSystemParameters, resource.getParameters()));
        }
        Map<String, Object> generalSystemParameters = getGeneralParameters();
        descriptor.setParameters(MapUtil.merge(generalSystemParameters, descriptor.getParameters()));
    }

    private Map<String, Object> getGeneralParameters() {
        Map<String, Object> systemParameters = new HashMap<>();

        systemParameters.put(SupportedParameters.DEPLOY_TARGET, targetName);
        systemParameters.put(SupportedParameters.ORG, organization);
        systemParameters.put(SupportedParameters.USER, user);
        systemParameters.put(SupportedParameters.SPACE, space);
        systemParameters.put(SupportedParameters.DEFAULT_DOMAIN, getDefaultDomain());
        if (shouldReserveTemporaryRoutes()) {
            systemParameters.put(SupportedParameters.DEFAULT_IDLE_DOMAIN, getDefaultDomain());
        }
        systemParameters.put(SupportedParameters.XS_TARGET_API_URL, getControllerUrl());
        systemParameters.put(SupportedParameters.CONTROLLER_URL, getControllerUrl());
        systemParameters.put(SupportedParameters.XS_TYPE, platformType.toString());
        systemParameters.put(SupportedParameters.XS_AUTHORIZATION_ENDPOINT, getAuthorizationEndpoint());
        systemParameters.put(SupportedParameters.AUTHORIZATION_URL, getAuthorizationEndpoint());
        systemParameters.put(SupportedParameters.DEPLOY_SERVICE_URL, getDeployServiceUrl());

        return systemParameters;
    }

    private Map<String, Object> getModuleParameters(Module module) {
        Map<String, Object> moduleSystemParameters = new HashMap<>();

        Map<String, Object> moduleParameters = Collections.unmodifiableMap(module.getParameters());
        moduleSystemParameters.put(SupportedParameters.DOMAIN, getDefaultDomain());
        if (shouldReserveTemporaryRoutes()) {
            moduleSystemParameters.put(SupportedParameters.IDLE_DOMAIN, getDefaultDomain());
        }
        moduleSystemParameters.put(SupportedParameters.APP_NAME, module.getName());
        moduleSystemParameters.put(SupportedParameters.INSTANCES, 1);
        moduleSystemParameters.put(SupportedParameters.TIMESTAMP, getDefaultTimestamp());

        putRoutingParameters(module, moduleParameters, moduleSystemParameters);

        moduleSystemParameters.put(SupportedParameters.GENERATED_USER, credentialsGenerator.next(GENERATED_CREDENTIALS_LENGTH));
        moduleSystemParameters.put(SupportedParameters.GENERATED_PASSWORD, credentialsGenerator.next(GENERATED_CREDENTIALS_LENGTH));

        return moduleSystemParameters;
    }

    private String getDefaultTimestamp() {
        return timestampSupplier.get();
    }

    private void putRoutingParameters(Module module, Map<String, Object> moduleParameters, Map<String, Object> moduleSystemParameters) {
        putHostRoutingParameters(module, moduleSystemParameters);
        String protocol = getProtocol(moduleParameters);
        if (portAllocator != null && moduleToDeployHelper.isApplication(module) && (portBasedRouting || isTcpOrTcpsProtocol(protocol))) {
            putPortRoutingParameters(module, moduleParameters, moduleSystemParameters);
        } else {
            boolean isStandardPort = UriUtil.isStandardPort(routerPort, controllerUrl.getProtocol());
            String defaultUri = isStandardPort ? DEFAULT_HOST_BASED_URI : DEFAULT_HOST_BASED_URI + ":" + getRouterPort();
            if (shouldReserveTemporaryRoutes()) {
                String defaultIdleUri = isStandardPort ? DEFAULT_HOST_BASED_IDLE_URI : DEFAULT_HOST_BASED_IDLE_URI + ":" + getRouterPort();
                moduleSystemParameters.put(SupportedParameters.DEFAULT_IDLE_URI,
                    appendRoutePathIfPresent(defaultIdleUri, moduleParameters));
                defaultUri = defaultIdleUri;
            }
            moduleSystemParameters.put(SupportedParameters.DEFAULT_URI, appendRoutePathIfPresent(defaultUri, moduleParameters));
        }

        String defaultUrl = DEFAULT_URL;
        if (shouldReserveTemporaryRoutes()) {
            String defaultIdleUrl = DEFAULT_IDLE_URL;
            moduleSystemParameters.put(SupportedParameters.DEFAULT_IDLE_URL, defaultIdleUrl);
            defaultUrl = defaultIdleUrl;
        }
        moduleSystemParameters.put(SupportedParameters.PROTOCOL, protocol);
        moduleSystemParameters.put(SupportedParameters.DEFAULT_URL, defaultUrl);
    }

    private boolean isTcpOrTcpsProtocol(String protocol) {
        return (UriUtil.TCP_PROTOCOL.equals(protocol) || UriUtil.TCPS_PROTOCOL.equals(protocol));
    }

    private void putHostRoutingParameters(Module module, Map<String, Object> moduleSystemParameters) {
        String defaultHost = getDefaultHost(module.getName());
        if (shouldReserveTemporaryRoutes()) {
            String idleHost = getDefaultHost(module.getName() + IDLE_HOST_SUFFIX);
            moduleSystemParameters.put(SupportedParameters.DEFAULT_IDLE_HOST, idleHost);
            moduleSystemParameters.put(SupportedParameters.IDLE_HOST, idleHost);
            defaultHost = idleHost;
        }
        moduleSystemParameters.put(SupportedParameters.DEFAULT_HOST, defaultHost);
        moduleSystemParameters.put(SupportedParameters.HOST, defaultHost);
    }

    private void putPortRoutingParameters(Module module, Map<String, Object> moduleParameters, Map<String, Object> moduleSystemParameters) {
        int defaultPort = getDefaultPort(module.getName(), moduleParameters);
        String defaultUri = appendRoutePathIfPresent(DEFAULT_PORT_BASED_URI, moduleParameters);
        if (shouldReserveTemporaryRoutes()) {
            int idlePort = allocatePort(module.getName(), moduleParameters);
            String idleUri = appendRoutePathIfPresent(DEFAULT_PORT_BASED_IDLE_URI, moduleParameters);
            moduleSystemParameters.put(SupportedParameters.DEFAULT_IDLE_PORT, idlePort);
            moduleSystemParameters.put(SupportedParameters.IDLE_PORT, idlePort);
            moduleSystemParameters.put(SupportedParameters.DEFAULT_IDLE_URI, idleUri);
            defaultPort = idlePort;
            defaultUri = idleUri;
        }
        moduleSystemParameters.put(SupportedParameters.DEFAULT_PORT, defaultPort);
        moduleSystemParameters.put(SupportedParameters.PORT, defaultPort);
        moduleSystemParameters.put(SupportedParameters.DEFAULT_URI, defaultUri);
    }

    private String appendRoutePathIfPresent(String uri, Map<String, Object> moduleParameters) {
        if (moduleParameters.containsKey(SupportedParameters.ROUTE_PATH)) {
            return uri + ROUTE_PATH_PLACEHOLDER;
        }
        return uri;
    }

    private Map<String, Object> getResourceParameters(Resource resource) {
        Map<String, Object> resourceSystemParameters = new HashMap<>();

        resourceSystemParameters.put(SupportedParameters.SERVICE_NAME, resource.getName());
        resourceSystemParameters.put(SupportedParameters.DEFAULT_CONTAINER_NAME,
            NameUtil.createValidContainerName(organization, space, resource.getName()));
        resourceSystemParameters.put(SupportedParameters.DEFAULT_XS_APP_NAME, NameUtil.createValidXsAppName(resource.getName()));

        resourceSystemParameters.put(SupportedParameters.GENERATED_USER, credentialsGenerator.next(GENERATED_CREDENTIALS_LENGTH));
        resourceSystemParameters.put(SupportedParameters.GENERATED_PASSWORD, credentialsGenerator.next(GENERATED_CREDENTIALS_LENGTH));

        return resourceSystemParameters;
    }

    private Integer getDefaultPort(String moduleName, Map<String, Object> moduleParameters) {
        DeployedMtaModule deployedModule = getDeployedModule(moduleName);
        if (deployedModule != null && !CollectionUtils.isEmpty(deployedModule.getUris())) {
            Integer usedPort = UriUtil.getPort(deployedModule.getUris()
                .get(0));
            if (usedPort != null) {
                return usedPort;
            }
        }
        return allocatePort(moduleName, moduleParameters);
    }

    private int allocatePort(String moduleName, Map<String, Object> moduleParameters) {
        boolean isTcpRoute = getBooleanParameter(moduleParameters, SupportedParameters.TCP);
        boolean isTcpsRoute = getBooleanParameter(moduleParameters, SupportedParameters.TCPS);
        if (isTcpRoute && isTcpsRoute) {
            throw new ContentException(Messages.INVALID_TCP_ROUTE);
        }

        if (isTcpRoute || isTcpsRoute) {
            return portAllocator.allocateTcpPort(moduleName, isTcpsRoute);
        } else {
            return portAllocator.allocatePort(moduleName);
        }
    }

    private boolean getBooleanParameter(Map<String, Object> moduleParameters, String parameterName) {
        return CommonUtil.cast(moduleParameters.getOrDefault(parameterName, false));
    }

    private DeployedMtaModule getDeployedModule(String moduleName) {
        return deployedMta == null ? null : deployedMta.findDeployedModule(moduleName);
    }

    private String getDefaultHost(String moduleName) {
        String host = (targetName + " " + moduleName).replaceAll("\\s", "-")
            .toLowerCase();
        if (!HOST_VALIDATOR.isValid(host)) {
            return HOST_VALIDATOR.attemptToCorrect(host);
        }
        return host;
    }

    private boolean shouldReserveTemporaryRoutes() {
        return reserveTemporaryRoutes;
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

    private String getControllerUrl() {
        if (shouldUseXsPlaceholders()) {
            return SupportedParameters.XSA_CONTROLLER_ENDPOINT_PLACEHOLDER;
        }
        return controllerUrl.toString();
    }

    private String getProtocol(Map<String, Object> moduleParameters) {
        boolean isTcpRoute = getBooleanParameter(moduleParameters, SupportedParameters.TCP);
        boolean isTcpsRoute = getBooleanParameter(moduleParameters, SupportedParameters.TCPS);
        if (isTcpRoute) {
            return UriUtil.TCP_PROTOCOL;
        }
        if (isTcpsRoute) {
            return UriUtil.TCPS_PROTOCOL;
        }
        if (shouldUseXsPlaceholders()) {
            return SupportedParameters.XSA_PROTOCOL_PLACEHOLDER;
        }
        return controllerUrl.getProtocol();
    }

    private boolean shouldUseXsPlaceholders() {
        return platformType.equals(PlatformType.XS2) && areXsPlaceholdersSupported;
    }

    public static class Builder {

        private CredentialsGenerator credentialsGenerator;
        private String organization;
        private String space;
        private String user;
        private String defaultDomain;
        private PlatformType platformType;
        private URL controllerUrl;
        private String authorizationEndpoint;
        private String deployServiceUrl;
        private int routerPort;
        private boolean portBasedRouting;
        private PortAllocator portAllocator;
        private DeployedMta deployedMta;
        private boolean reserveTemporaryRoutes;
        private boolean areXsPlaceholdersSupported;
        private Supplier<String> timestampSupplier;
        private ModuleToDeployHelper moduleToDeployHelper;

        public Builder credentialsGenerator(CredentialsGenerator credentialsGenerator) {
            this.credentialsGenerator = credentialsGenerator;
            return this;
        }

        public Builder organization(String organization) {
            this.organization = organization;
            return this;
        }

        public Builder space(String space) {
            this.space = space;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder defaultDomain(String defaultDomain) {
            this.defaultDomain = defaultDomain;
            return this;
        }

        public Builder platformType(PlatformType platformType) {
            this.platformType = platformType;
            return this;
        }

        public Builder controllerUrl(URL controllerUrl) {
            this.controllerUrl = controllerUrl;
            return this;
        }

        public Builder authorizationEndpoint(String authorizationEndpoint) {
            this.authorizationEndpoint = authorizationEndpoint;
            return this;
        }

        public Builder deployServiceUrl(String deployServiceUrl) {
            this.deployServiceUrl = deployServiceUrl;
            return this;
        }

        public Builder routerPort(int routerPort) {
            this.routerPort = routerPort;
            return this;
        }

        public Builder portBasedRouting(boolean portBasedRouting) {
            this.portBasedRouting = portBasedRouting;
            return this;
        }

        public Builder portAllocator(PortAllocator portAllocator) {
            this.portAllocator = portAllocator;
            return this;
        }

        public Builder deployedMta(DeployedMta deployedMta) {
            this.deployedMta = deployedMta;
            return this;
        }

        public Builder reserveTemporaryRoutes(boolean reserveTemporaryRoutes) {
            this.reserveTemporaryRoutes = reserveTemporaryRoutes;
            return this;
        }

        public Builder areXsPlaceholdersSupported(boolean areXsPlaceholdersSupported) {
            this.areXsPlaceholdersSupported = areXsPlaceholdersSupported;
            return this;
        }

        public Builder timestampSupplier(Supplier<String> timestampSupplier) {
            this.timestampSupplier = timestampSupplier;
            return this;
        }

        public Builder moduleToDeployHelper(ModuleToDeployHelper moduleToDeployHelper) {
            this.moduleToDeployHelper = moduleToDeployHelper;
            return this;
        }

        public SystemParameters build() {
            return new SystemParameters(this);
        }

    }

}
