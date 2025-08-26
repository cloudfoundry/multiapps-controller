package org.cloudfoundry.multiapps.controller.core.metering.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableConsumer.class)
@JsonDeserialize(as = ImmutableConsumer.class)
public interface Consumer {

    String getRegion();

    Environment getEnvironment();
}
