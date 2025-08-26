package org.cloudfoundry.multiapps.controller.core.metering.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableEnvironment.class)
@JsonDeserialize(as = ImmutableEnvironment.class)
public interface Environment {

    default String getEnvironment() {
        return "CF";
    }

    String getSubAccount();
}
