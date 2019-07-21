package com.sap.cloud.lm.sl.cf.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.core.model.adapter.VersionJsonDeserializer;
import com.sap.cloud.lm.sl.cf.core.model.adapter.VersionJsonSerializer;
import com.sap.cloud.lm.sl.mta.model.AuditableConfiguration;
import com.sap.cloud.lm.sl.mta.model.ConfigurationIdentifier;
import com.sap.cloud.lm.sl.mta.model.Version;

public class ConfigurationEntry implements AuditableConfiguration {

    private long id;
    private String providerNid;
    private String providerId;
    @JsonSerialize(using = VersionJsonSerializer.class)
    @JsonDeserialize(using = VersionJsonDeserializer.class)
    private Version providerVersion;
    private CloudTarget targetSpace;
    private String content;
    private List<CloudTarget> visibility;
    private String spaceId;

    // Required by Jackson.
    protected ConfigurationEntry() {
    }

    public ConfigurationEntry(long id, String providerNid, String providerId, Version providerVersion, CloudTarget targetSpace,
        String content, List<CloudTarget> visibility, String spaceId) {
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

    public void setId(long id) {
        this.id = id;
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

    @Override
    public String getConfigurationType() {
        return "configuration entry";
    }

    @Override
    public String getConfigurationName() {
        return String.valueOf(id);
    }

    @Override
    public List<ConfigurationIdentifier> getConfigurationIdentifiers() {
        List<ConfigurationIdentifier> configurationIdentifiers = new ArrayList<>();
        configurationIdentifiers.add(new ConfigurationIdentifier("provider namespace", providerNid));
        configurationIdentifiers.add(new ConfigurationIdentifier("provider id", providerId));
        configurationIdentifiers.add(new ConfigurationIdentifier("provider version", Objects.toString(providerVersion)));
        configurationIdentifiers.add(new ConfigurationIdentifier("provider target", targetSpace.getOrg() + "/" + targetSpace.getSpace()));
        configurationIdentifiers.add(new ConfigurationIdentifier("configuration content", content));
        return configurationIdentifiers;
    }

}
