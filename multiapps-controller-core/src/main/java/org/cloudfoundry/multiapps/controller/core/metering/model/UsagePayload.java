package org.cloudfoundry.multiapps.controller.core.metering.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableUsagePayload.class)
@JsonDeserialize(as = ImmutableUsagePayload.class)
public interface UsagePayload {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    default UUID getId() {
        return UUID.randomUUID();
    }

    default String getTimestamp() {
        return ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                            .format(formatter);
    }

    default Map<String, Object> getProduct() {
        return Map.of("service", Map.of("id", "TEST", "plan", "standard"));
    }

    default Map<String, Object> getMeasure() {
        return Map.of("id", "deploy-started", "value", 1);
    }

    Consumer getConsumer();

    default Map<String, String> getCustomDimensions() {
        return Map.of("dimension1", "test", "dimension2", "test");
    }
}
