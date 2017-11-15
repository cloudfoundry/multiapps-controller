package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;

public class CloudApplicationExtended extends CloudApplication {

    private String moduleName;
    private Staging staging;
    private List<String> idleUris;
    private Map<String, Map<String, Object>> bindingParameters;
    private List<CloudTask> tasks;

    public CloudApplicationExtended(Meta meta, String name) {
        super(meta, name);
    }

    public CloudApplicationExtended(String name, String command, String buildpackUrl, int memory, int instances, List<String> uris,
        List<String> serviceNames, AppState state) {
        super(name, command, buildpackUrl, memory, instances, uris, serviceNames, state);
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

}
