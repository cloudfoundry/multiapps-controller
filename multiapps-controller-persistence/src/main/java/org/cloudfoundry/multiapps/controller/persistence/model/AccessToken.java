package org.cloudfoundry.multiapps.controller.persistence.model;

import java.time.LocalDateTime;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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

    LocalDateTime getExpiresAt();
}
