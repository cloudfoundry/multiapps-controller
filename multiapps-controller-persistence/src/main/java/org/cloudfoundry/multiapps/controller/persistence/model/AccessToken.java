package org.cloudfoundry.multiapps.controller.persistence.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableAccessToken.class)
@JsonDeserialize(as = ImmutableAccessToken.class)
public interface AccessToken {

    @Value.Default
    default long getId() {
        return 0;
    }

    byte[] getValue();

    String getUsername();

    @Nullable
    String getUserGuid();

    LocalDateTime getExpiresAt();
}
