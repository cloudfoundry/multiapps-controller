package com.sap.cloud.lm.sl.cf.core.model;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
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
    private String targetSpace;

    @Expose
    private String content;

    @Expose
    private List<CloudTarget> visibility;

    public ConfigurationEntry(long id, String providerNid, String providerId, Version providerVersion, String targetSpace, String content,
        List<CloudTarget> visibility) {
        this.id = id;
        this.providerNid = providerNid;
        this.providerId = providerId;
        this.providerVersion = providerVersion;
        this.targetSpace = targetSpace;
        this.content = content;
        this.visibility = visibility;
    }

    public ConfigurationEntry(String providerNid, String providerId, Version providerVersion, String targetSpace, String content,
        List<CloudTarget> cloudTargets) {
        this(0, providerNid, providerId, providerVersion, targetSpace, content, cloudTargets);
    }

    public long getId() {
        return id;
    }

    public String getProviderNid() {
        return providerNid;
    }

    public String getTargetSpace() {
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

}
