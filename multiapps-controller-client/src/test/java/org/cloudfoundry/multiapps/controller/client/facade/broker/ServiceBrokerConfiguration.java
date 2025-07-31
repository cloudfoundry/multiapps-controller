package org.cloudfoundry.multiapps.controller.client.facade.broker;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableServiceBrokerConfiguration.class)
@JsonDeserialize(as = ImmutableServiceBrokerConfiguration.class)
public interface ServiceBrokerConfiguration {

    @Nullable
    @JsonProperty("asyncDuration")
    Integer getAsyncDurationInMillis();

    @Nullable
    @JsonProperty("syncDuration")
    Integer getSyncDurationInMillis();

    @Nullable
    @JsonProperty("asyncDurationForServiceCredentialBindings")
    Integer getAsyncDurationForServiceCredentialBindingsInMillis();

    @Nullable
    @JsonProperty("syncDurationForServiceCredentialBindings")
    Integer getSyncDurationForServiceCredentialBindingsInMillis();

    List<FailConfiguration> getFailConfigurations();

}
