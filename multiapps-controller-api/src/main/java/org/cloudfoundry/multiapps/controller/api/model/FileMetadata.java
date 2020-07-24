package org.cloudfoundry.multiapps.controller.api.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;
import org.cloudfoundry.multiapps.mta.model.ConfigurationIdentifier;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableFileMetadata.class)
@JsonDeserialize(as = ImmutableFileMetadata.class)
public abstract class FileMetadata implements AuditableConfiguration {

    @Nullable
    @ApiModelProperty
    @JsonProperty("id")
    public abstract String getId();

    @Nullable
    @ApiModelProperty
    @JsonProperty("name")
    public abstract String getName();

    @Nullable
    @ApiModelProperty
    @JsonProperty("size")
    public abstract BigInteger getSize();

    @Nullable
    @ApiModelProperty
    @JsonProperty("digest")
    public abstract String getDigest();

    @Nullable
    @ApiModelProperty
    @JsonProperty("digestAlgorithm")
    public abstract String getDigestAlgorithm();

    @Nullable
    @ApiModelProperty
    @JsonProperty("space")
    public abstract String getSpace();

    @Override
    @ApiModelProperty(hidden = true)
    public String getConfigurationType() {
        return "file metadata";
    }

    @Override
    @ApiModelProperty(hidden = true)
    public String getConfigurationName() {
        return getName();
    }

    @Nullable
    @ApiModelProperty
    @JsonProperty("namespace")
    public abstract String getNamespace();

    @Override
    @ApiModelProperty(hidden = true)
    public List<ConfigurationIdentifier> getConfigurationIdentifiers() {
        List<ConfigurationIdentifier> configurationIdentifiers = new ArrayList<>();
        configurationIdentifiers.add(new ConfigurationIdentifier("id", getId()));
        configurationIdentifiers.add(new ConfigurationIdentifier("digest", getDigest()));
        configurationIdentifiers.add(new ConfigurationIdentifier("digestAlgorithm", getDigestAlgorithm()));
        configurationIdentifiers.add(new ConfigurationIdentifier("space", getSpace()));
        configurationIdentifiers.add(new ConfigurationIdentifier("size", Objects.toString(getSize())));
        configurationIdentifiers.add(new ConfigurationIdentifier("namespace", getNamespace()));
        return configurationIdentifiers;
    }
}
