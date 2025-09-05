package org.cloudfoundry.multiapps.controller.core.metering.model;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableUsagePayload.class)
@JsonDeserialize(as = ImmutableUsagePayload.class)
public interface UsagePayload {

    UUID getId();

    String getTimestamp();

    default Map<String, Object> getProduct() {
        return Map.of("service", Map.of("id", "deploy-service", "plan", "standard"));
    }

    Map<String, Object> getMeasure();

    Consumer getConsumer();

    Map<String, String> getCustomDimensions();
}
