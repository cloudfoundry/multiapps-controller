package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.DockerInfo;

import com.google.gson.annotations.JsonAdapter;
import com.sap.cloud.lm.sl.common.model.json.MapWithNumbersAdapterFactory;

public class CloudApplicationExtended extends CloudApplication {

    private String moduleName;
    private List<String> idleUris;
    @JsonAdapter(MapWithNumbersAdapterFactory.class)
    private Map<String, Map<String, Object>> bindingParameters;
    private List<CloudTask> tasks;
    private List<CloudRoute> routes;
    private List<ServiceKeyToInject> serviceKeysToInject;
    private List<ApplicationPort> applicationPorts;
    private List<String> domains;
    private RestartParameters restartParameters;
    private DockerInfo dockerInfo;
    private AttributeUpdateStrategy applicationAttributesUpdateBehavior;

    public CloudApplicationExtended(Meta meta, String name) {
        super(meta, name);
    }

    public CloudApplicationExtended(String name, String command, String buildpackUrl, int memory, int instances, List<String> uris,
        List<String> serviceNames, AppState state, List<ApplicationPort> applicationPorts, List<String> domains, DockerInfo dockerInfo) {
        super(name, command, buildpackUrl, memory, instances, uris, serviceNames, state);
        this.applicationPorts = applicationPorts;
        this.domains = domains;
        this.dockerInfo = dockerInfo;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public List<String> getIdleUris() {
        return idleUris;
    }

    public void setIdleUris(List<String> idleUris) {
        this.idleUris = idleUris;
    }

    public Map<String, Map<String, Object>> getBindingParameters() {
        return bindingParameters;
    }

    public void setBindingParameters(Map<String, Map<String, Object>> bindingParameters) {
        this.bindingParameters = bindingParameters;
    }

    public List<CloudTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<CloudTask> tasks) {
        this.tasks = tasks;
    }

    public List<CloudRoute> getRoutes() {
        return routes;
    }

    public void setRoutes(List<CloudRoute> routes) {
        this.routes = routes;
    }

    public List<ServiceKeyToInject> getServiceKeysToInject() {
        return serviceKeysToInject;
    }

    public void setServiceKeysToInject(List<ServiceKeyToInject> serviceKeysToInject) {
        this.serviceKeysToInject = serviceKeysToInject;
    }

    public List<ApplicationPort> getApplicationPorts() {
        return applicationPorts;
    }

    public void setApplicationPorts(List<ApplicationPort> applicationPorts) {
        this.applicationPorts = applicationPorts;
    }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public RestartParameters getRestartParameters() {
        return restartParameters;
    }

    public void setRestartParameters(RestartParameters restartParameters) {
        this.restartParameters = restartParameters;
    }

    public DockerInfo getDockerInfo() {
        return dockerInfo;
    }

    public void setDockerInfo(DockerInfo dockerInfo) {
        this.dockerInfo = dockerInfo;
    }

    public AttributeUpdateStrategy getApplicationAttributesUpdateBehavior() {
        return applicationAttributesUpdateBehavior;
    }

    public void setApplicationAttributesUpdateBehavior(AttributeUpdateStrategy applicationAttributesUpdateBehavior) {
        this.applicationAttributesUpdateBehavior = applicationAttributesUpdateBehavior;
    }

    public static class AttributeUpdateStrategy {
        private boolean shouldKeepExistingEnv;
        private boolean shouldKeepExistingServiceBindings;
        private boolean shouldKeepExistingRoutes;

        public AttributeUpdateStrategy(Builder attrbiuteUpdateStrategyBuilder) {
            this.shouldKeepExistingEnv = attrbiuteUpdateStrategyBuilder.shouldKeepExistingEnv;
            this.shouldKeepExistingServiceBindings = attrbiuteUpdateStrategyBuilder.shouldKeepExistingServiceBindings;
            this.shouldKeepExistingRoutes = attrbiuteUpdateStrategyBuilder.shouldKeepExistingRoutes;
        }

        public boolean shouldKeepExistingEnv() {
            return shouldKeepExistingEnv;
        }

        public boolean shouldKeepExistingServiceBindings() {
            return shouldKeepExistingServiceBindings;
        }

        public boolean shouldKeepExistingRoutes() {
            return shouldKeepExistingRoutes;
        }

        public static class Builder {
            private boolean shouldKeepExistingEnv = false;
            private boolean shouldKeepExistingServiceBindings = false;
            private boolean shouldKeepExistingRoutes = false;

            public Builder shouldKeepExistingEnv(Boolean shouldKeepExistingEnv) {
                if (shouldKeepExistingEnv != null) {
                    this.shouldKeepExistingEnv = shouldKeepExistingEnv;
                }
                return this;
            }

            public Builder shouldKeepExistingServiceBindings(Boolean shouldKeepExistingServiceBindings) {
                if (shouldKeepExistingServiceBindings != null) {
                    this.shouldKeepExistingServiceBindings = shouldKeepExistingServiceBindings;
                }
                return this;
            }

            public Builder shouldKeepExistingRoutes(Boolean shouldKeepExistingRoutes) {
                if (shouldKeepExistingRoutes != null) {
                    this.shouldKeepExistingRoutes = shouldKeepExistingRoutes;
                }
                return this;
            }

            public AttributeUpdateStrategy build() {
                return new AttributeUpdateStrategy(this);
            }
        }

    }

}
