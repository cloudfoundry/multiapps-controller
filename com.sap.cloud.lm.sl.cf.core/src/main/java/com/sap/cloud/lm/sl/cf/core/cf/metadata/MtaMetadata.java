package com.sap.cloud.lm.sl.cf.core.cf.metadata;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.core.model.adapter.VersionJsonDeserializer;
import com.sap.cloud.lm.sl.cf.core.model.adapter.VersionJsonSerializer;
import com.sap.cloud.lm.sl.mta.model.Version;

@Value.Immutable
@JsonDeserialize(builder = ImmutableMtaMetadata.Builder.class)
public interface MtaMetadata {

    String getId();

    @JsonSerialize(using = VersionJsonSerializer.class)
    @JsonDeserialize(using = VersionJsonDeserializer.class)
    @Nullable
    Version getVersion();

    @Nullable
    String getNamespace();
}
