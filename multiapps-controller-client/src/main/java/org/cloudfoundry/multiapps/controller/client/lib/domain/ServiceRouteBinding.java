package org.cloudfoundry.multiapps.controller.client.lib.domain;

import org.immutables.value.Value;

@Value.Immutable
public interface ServiceRouteBinding {

    String getRouteId();

    String getServiceInstanceId();

}
