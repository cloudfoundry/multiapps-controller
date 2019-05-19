package com.sap.cloud.lm.sl.cf.core.helpers;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.HostValidator;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class SystemParameters {

    public static final int GENERATED_CREDENTIALS_LENGTH = 16;
    public static final String IDLE_HOST_SUFFIX = "-idle";
    public static final String ROUTE_PATH_PLACEHOLDER = "${route-path}";
    public static final String DEFAULT_HOST_BASED_IDLE_URI = "${idle-host}.${idle-domain}";
    public static final String DEFAULT_HOST_BASED_URI = "${host}.${domain}";
    public static final String DEFAULT_IDLE_URL = "${protocol}://${default-idle-uri}";
    public static final String DEFAULT_URL = "${protocol}://${default-uri}";

    private static final HostValidator HOST_VALIDATOR = new HostValidator();

    private final CredentialsGenerator credentialsGenerator;
    private final String targetName;
    private final String organization;
    private final String space;
    private final String user;
    private final String defaultDomain;
    private final URL controllerUrl;
    private final String authorizationEndpoint;
    private final String deployServiceUrl;
    private final boolean reserveTemporaryRoutes;
    private final Supplier<String> timestampSupplier;

    public SystemParameters(Builder builder) {
        this.targetName = builder.organization + " " + builder.space;
        this.organization = builder.organization;
        this.space = builder.space;
        this.user = builder.user;
        this.defaultDomain = builder.defaultDomain;
        this.controllerUrl = builder.controllerUrl;
        this.authorizationEndpoint = builder.authorizationEndpoint;
        this.deployServiceUrl = builder.deployServiceUrl;
        this.credentialsGenerator = builder.credentialsGenerator;
        this.reserveTemporaryRoutes = builder.reserveTemporaryRoutes;
        this.timestampSupplier = builder.timestampSupplier;
    }

    public void injectInto(DeploymentDescriptor descriptor) {
        for (Module module : descriptor.getModules()) {
            Map<String, Object> moduleSystemParameters = getModuleParameters(module);
            module.setParameters(MapUtil.merge(moduleSystemParameters, module.getParameters()));
        }
        for (Resource resource : descriptor.getResources()) {
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
        systemParameters.put(SupportedParameters.DEFAULT_DOMAIN, defaultDomain);
        if (reserveTemporaryRoutes) {
            systemParameters.put(SupportedParameters.DEFAULT_IDLE_DOMAIN, defaultDomain);
        }
        systemParameters.put(SupportedParameters.XS_TARGET_API_URL, getControllerUrl());
        systemParameters.put(SupportedParameters.CONTROLLER_URL, getControllerUrl());
        systemParameters.put(SupportedParameters.XS_TYPE, "CF");
        systemParameters.put(SupportedParameters.XS_AUTHORIZATION_ENDPOINT, authorizationEndpoint);
        systemParameters.put(SupportedParameters.AUTHORIZATION_URL, authorizationEndpoint);
        systemParameters.put(SupportedParameters.DEPLOY_SERVICE_URL, deployServiceUrl);

        return systemParameters;
    }

    private Map<String, Object> getModuleParameters(Module module) {
        Map<String, Object> moduleSystemParameters = new HashMap<>();

        Map<String, Object> moduleParameters = Collections.unmodifiableMap(module.getParameters());
        moduleSystemParameters.put(SupportedParameters.DOMAIN, defaultDomain);
        if (reserveTemporaryRoutes) {
            moduleSystemParameters.put(SupportedParameters.IDLE_DOMAIN, defaultDomain);
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
        String protocol = getProtocol();
        String defaultUri = DEFAULT_HOST_BASED_URI;
        if (reserveTemporaryRoutes) {
            String defaultIdleUri = DEFAULT_HOST_BASED_IDLE_URI;
            moduleSystemParameters.put(SupportedParameters.DEFAULT_IDLE_URI, appendRoutePathIfPresent(defaultIdleUri, moduleParameters));
            defaultUri = defaultIdleUri;
        }
        moduleSystemParameters.put(SupportedParameters.DEFAULT_URI, appendRoutePathIfPresent(defaultUri, moduleParameters));

        String defaultUrl = DEFAULT_URL;
        if (reserveTemporaryRoutes) {
            String defaultIdleUrl = DEFAULT_IDLE_URL;
            moduleSystemParameters.put(SupportedParameters.DEFAULT_IDLE_URL, defaultIdleUrl);
            defaultUrl = defaultIdleUrl;
        }
        moduleSystemParameters.put(SupportedParameters.PROTOCOL, protocol);
        moduleSystemParameters.put(SupportedParameters.DEFAULT_URL, defaultUrl);
    }

    private void putHostRoutingParameters(Module module, Map<String, Object> moduleSystemParameters) {
        String defaultHost = getDefaultHost(module.getName());
        if (reserveTemporaryRoutes) {
            String idleHost = getDefaultHost(module.getName() + IDLE_HOST_SUFFIX);
            moduleSystemParameters.put(SupportedParameters.DEFAULT_IDLE_HOST, idleHost);
            moduleSystemParameters.put(SupportedParameters.IDLE_HOST, idleHost);
            defaultHost = idleHost;
        }
        moduleSystemParameters.put(SupportedParameters.DEFAULT_HOST, defaultHost);
        moduleSystemParameters.put(SupportedParameters.HOST, defaultHost);
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
            NameUtil.computeValidContainerName(organization, space, resource.getName()));
        resourceSystemParameters.put(SupportedParameters.DEFAULT_XS_APP_NAME, NameUtil.computeValidXsAppName(resource.getName()));

        resourceSystemParameters.put(SupportedParameters.GENERATED_USER, credentialsGenerator.next(GENERATED_CREDENTIALS_LENGTH));
        resourceSystemParameters.put(SupportedParameters.GENERATED_PASSWORD, credentialsGenerator.next(GENERATED_CREDENTIALS_LENGTH));

        return resourceSystemParameters;
    }

    private String getDefaultHost(String moduleName) {
        String host = (targetName + " " + moduleName).replaceAll("\\s", "-")
            .toLowerCase();
        if (!HOST_VALIDATOR.isValid(host)) {
            return HOST_VALIDATOR.attemptToCorrect(host);
        }
        return host;
    }

    private String getControllerUrl() {
        return controllerUrl.toString();
    }

    private String getProtocol() {
        return controllerUrl.getProtocol();
    }

    public static class Builder {

        private CredentialsGenerator credentialsGenerator;
        private String organization;
        private String space;
        private String user;
        private String defaultDomain;
        private URL controllerUrl;
        private String authorizationEndpoint;
        private String deployServiceUrl;
        private boolean reserveTemporaryRoutes;
        private Supplier<String> timestampSupplier;

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

        public Builder reserveTemporaryRoutes(boolean reserveTemporaryRoutes) {
            this.reserveTemporaryRoutes = reserveTemporaryRoutes;
            return this;
        }

        public Builder timestampSupplier(Supplier<String> timestampSupplier) {
            this.timestampSupplier = timestampSupplier;
            return this;
        }

        public SystemParameters build() {
            return new SystemParameters(this);
        }

    }

}
