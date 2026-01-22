package org.cloudfoundry.multiapps.controller.persistence.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableSecretToken.class)
@JsonDeserialize(as = ImmutableSecretToken.class)
public interface SecretToken {

    @Value.Default
    default long getId() {
        return 0L;
    }

    String getProcessInstanceId();

    String getKeyId();

    String getVariableName();

    byte[] getContent();

    @Value.Default
    default LocalDateTime getTimestamp() {
        return LocalDateTime.now();
    }

}
