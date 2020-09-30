package org.cloudfoundry.multiapps.controller.persistence.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.cloudfoundry.multiapps.controller.persistence.model.adapter.VersionJsonDeserializer;
import org.cloudfoundry.multiapps.controller.persistence.model.adapter.VersionJsonSerializer;
import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;
import org.cloudfoundry.multiapps.mta.model.ConfigurationIdentifier;
import org.cloudfoundry.multiapps.mta.model.Version;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ConfigurationEntry implements AuditableConfiguration {

    private long id;
    private String providerNid;
    private String providerId;
    @JsonSerialize(using = VersionJsonSerializer.class)
    @JsonDeserialize(using = VersionJsonDeserializer.class)
    private Version providerVersion;
    private String providerNamespace;
    private CloudTarget targetSpace;
    private String content;
    private List<CloudTarget> visibility;
    private String spaceId;
    private String contentId;

    // Required by Jackson.
    protected ConfigurationEntry() {
    }

    public ConfigurationEntry(long id, String providerNid, String providerId, Version providerVersion, String providerNamespace,
                              CloudTarget targetSpace, String content, List<CloudTarget> visibility, String spaceId, String contentId) {
        this.id = id;
        this.providerNid = providerNid;
        this.providerId = providerId;
        this.providerVersion = providerVersion;
        this.providerNamespace = providerNamespace;
        this.targetSpace = targetSpace;
        this.content = content;
        this.visibility = visibility;
        this.spaceId = spaceId;
        this.contentId = contentId;
    }

    public ConfigurationEntry(String providerNid, String providerId, Version providerVersion, String providerNamespace,
                              CloudTarget targetSpace, String content, List<CloudTarget> cloudTargets, String spaceId, String contentId) {
        this(0, providerNid, providerId, providerVersion, providerNamespace, targetSpace, content, cloudTargets, spaceId, contentId);
    }

    public ConfigurationEntry(String providerId, Version providerVersion) {
        this(0, null, providerId, providerVersion, null, null, null, null, null, null);
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

    public String getProviderNamespace() {
        return providerNamespace;
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

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
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
        configurationIdentifiers.add(new ConfigurationIdentifier("provider id", providerId));
        configurationIdentifiers.add(new ConfigurationIdentifier("provider nid", providerNid));
        configurationIdentifiers.add(new ConfigurationIdentifier("provider version", Objects.toString(providerVersion)));
        configurationIdentifiers.add(new ConfigurationIdentifier("provider namespace", providerNamespace)); // welp
        configurationIdentifiers.add(new ConfigurationIdentifier("provider target",
                                                                 targetSpace.getOrganizationName() + "/" + targetSpace.getSpaceName()));
        configurationIdentifiers.add(new ConfigurationIdentifier("configuration content", content));
        configurationIdentifiers.add(new ConfigurationIdentifier("configuration content id", contentId));
        return configurationIdentifiers;
    }
}
