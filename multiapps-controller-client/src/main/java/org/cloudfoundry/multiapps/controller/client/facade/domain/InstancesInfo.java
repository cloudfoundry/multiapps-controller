package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.Collections;
import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableInstancesInfo.class)
@JsonDeserialize(as = ImmutableInstancesInfo.class)
public interface InstancesInfo {

    @Value.Default
    default List<InstanceInfo> getInstances() {
        return Collections.emptyList();
    }

}
