package com.sap.cloud.lm.sl.cf.web.api.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.common.Nullable;
import com.sap.cloud.lm.sl.mta.model.AuditableConfiguration;
import com.sap.cloud.lm.sl.mta.model.ConfigurationIdentifier;

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
    public String getConfigurationType() {
        return "file metadata";
    }

    @Override
    public String getConfigurationName() {
        return getName();
    }

    @Override
    public List<ConfigurationIdentifier> getConfigurationIdentifiers() {
        List<ConfigurationIdentifier> configurationIdentifiers = new ArrayList<>();
        configurationIdentifiers.add(new ConfigurationIdentifier("id", getId()));
        configurationIdentifiers.add(new ConfigurationIdentifier("digest", getDigest()));
        configurationIdentifiers.add(new ConfigurationIdentifier("digestAlgorithm", getDigestAlgorithm()));
        configurationIdentifiers.add(new ConfigurationIdentifier("space", getSpace()));
        configurationIdentifiers.add(new ConfigurationIdentifier("size", Objects.toString(getSize())));
        return configurationIdentifiers;
    }
}
