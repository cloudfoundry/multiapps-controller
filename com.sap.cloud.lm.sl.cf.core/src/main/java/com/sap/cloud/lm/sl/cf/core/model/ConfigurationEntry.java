package com.sap.cloud.lm.sl.cf.core.model;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.sap.cloud.lm.sl.cf.core.model.adapter.VersionJsonAdapter;
import com.sap.cloud.lm.sl.mta.model.Version;

public class ConfigurationEntry {

    private long id;

    @Expose
    private String providerNid;

    @Expose
    private String providerId;

    @Expose
    @JsonAdapter(VersionJsonAdapter.class)
    private Version providerVersion;

    @Expose
    @SerializedName("targetSpace")
    private CloudTarget targetSpace;

    @Expose
    private String content;

    @Expose
    private List<CloudTarget> visibility;
    
    @Expose
    private String spaceId;

    public ConfigurationEntry(long id, String providerNid, String providerId, Version providerVersion, CloudTarget targetSpace, String content,
        List<CloudTarget> visibility, String spaceId) {
        this.id = id;
        this.providerNid = providerNid;
        this.providerId = providerId;
        this.providerVersion = providerVersion;
        this.targetSpace = targetSpace;
        this.content = content;
        this.visibility = visibility;
        this.spaceId = spaceId;
    }

    public ConfigurationEntry(String providerNid, String providerId, Version providerVersion, CloudTarget targetSpace, String content,
        List<CloudTarget> cloudTargets, String spaceId) {
        this(0, providerNid, providerId, providerVersion, targetSpace, content, cloudTargets, spaceId);
    }

    public long getId() {
        return id;
    }

    public String getProviderNid() {
        return providerNid;
    }

    public CloudTarget getTargetSpace() {
        return targetSpace;
    }

    public String getProviderId() {
        return providerId;
    }

    public Version getProviderVersion() {
        return providerVersion;
    }

    public String getContent() {
        return content;
    }

    public List<CloudTarget> getVisibility() {
        return visibility;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }
}
