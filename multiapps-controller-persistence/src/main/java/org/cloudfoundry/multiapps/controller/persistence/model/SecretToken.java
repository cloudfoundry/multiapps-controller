package org.cloudfoundry.multiapps.controller.persistence.model;

import java.time.LocalDateTime;

import org.immutables.value.Value;

@Value.Immutable
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
