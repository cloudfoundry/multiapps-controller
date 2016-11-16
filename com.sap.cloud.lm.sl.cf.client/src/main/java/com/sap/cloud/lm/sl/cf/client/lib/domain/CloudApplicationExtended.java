package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;

public class CloudApplicationExtended extends CloudApplication {

    private String moduleName;
    private StagingExtended extendedStaging;
    private List<String> tempUris;
    private Map<String, Map<String, Object>> bindingParameters;

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

    public List<String> getTempUris() {
        return tempUris;
    }

    public void setTempUris(List<String> tempUris) {
        this.tempUris = tempUris;
    }

    public Map<String, Map<String, Object>> getBindingParameters() {
        return bindingParameters;
    }

    public void setBindingParameters(Map<String, Map<String, Object>> bindingParameters) {
        this.bindingParameters = bindingParameters;
    }


    @Override
    public void setStaging(Staging staging) {
        this.extendedStaging = (StagingExtended) staging;
    }

    @Override
    public StagingExtended getStaging() {
        return this.extendedStaging;
    }
}
