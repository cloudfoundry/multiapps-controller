package com.sap.cloud.lm.sl.cf.core.cf.metadata;

import javax.annotation.Nullable;

import org.cloudfoundry.multiapps.mta.model.Version;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.core.model.adapter.VersionJsonDeserializer;
import com.sap.cloud.lm.sl.cf.core.model.adapter.VersionJsonSerializer;

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
