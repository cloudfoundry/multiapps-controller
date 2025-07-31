package org.cloudfoundry.multiapps.controller.client.facade.broker;

import java.util.List;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableFailConfiguration.class)
@JsonDeserialize(as = ImmutableFailConfiguration.class)
public interface FailConfiguration {

    Integer getStatus();

    String getOperationType();

    List<UUID> getInstanceIds();

    enum OperationType {
        CREATE, UPDATE, DELETE, BIND, UNBIND, CREATE_SERVICE_KEY, DELETE_SERVICE_KEY;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

}
