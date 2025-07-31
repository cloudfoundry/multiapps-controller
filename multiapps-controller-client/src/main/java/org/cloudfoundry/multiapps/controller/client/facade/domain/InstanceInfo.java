package org.cloudfoundry.multiapps.controller.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableInstanceInfo.class)
@JsonDeserialize(as = ImmutableInstanceInfo.class)
public interface InstanceInfo {

    int getIndex();

    InstanceState getState();

}
