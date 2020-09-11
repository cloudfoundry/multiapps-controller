package org.cloudfoundry.multiapps.controller.core.cf.metadata;

import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.controller.core.model.adapter.VersionJsonDeserializer;
import org.cloudfoundry.multiapps.controller.core.model.adapter.VersionJsonSerializer;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
