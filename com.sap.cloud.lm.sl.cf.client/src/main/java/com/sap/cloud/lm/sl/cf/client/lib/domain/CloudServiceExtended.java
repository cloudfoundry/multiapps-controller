package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudService;

import com.google.gson.annotations.JsonAdapter;
import com.sap.cloud.lm.sl.common.model.json.MapWithNumbersAdapterFactory;

public class CloudServiceExtended extends CloudService {

    private String resourceName;
    @JsonAdapter(MapWithNumbersAdapterFactory.class)
    private Map<String, Object> credentials;
    private List<String> tags;
    private boolean isOptional;
    private boolean isManaged;
    private boolean shouldIgnoreUpdateErrors;

    public CloudServiceExtended() {
        super();
    }

    public CloudServiceExtended(Meta meta, String name) {
        super(meta, name);
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, Object> credentials) {
        this.credentials = credentials;
    }

    public List<String> getTags() {
        return this.tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public void setOptional(boolean isOptional) {
        this.isOptional = isOptional;
    }

    public boolean isManaged() {
        return isManaged;
    }

    public void setManaged(boolean isManaged) {
        this.isManaged = isManaged;
    }

    public boolean shouldIgnoreUpdateErrors() {
        return shouldIgnoreUpdateErrors;
    }

    public void setIgnoreUpdateErrors(boolean shouldIgnoreUpdateErrors) {
        this.shouldIgnoreUpdateErrors = shouldIgnoreUpdateErrors;
    }

}
