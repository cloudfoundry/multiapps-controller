package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudMetadata.class)
@JsonDeserialize(as = ImmutableCloudMetadata.class)
public interface CloudMetadata {

    @Nullable
    @Value.Parameter
    UUID getGuid();

    @Nullable
    LocalDateTime getCreatedAt();

    @Nullable
    LocalDateTime getUpdatedAt();

    @Nullable
    String getUrl();

    static CloudMetadata defaultMetadata() {
        return ImmutableCloudMetadata.builder()
                                     .build();
    }

}
