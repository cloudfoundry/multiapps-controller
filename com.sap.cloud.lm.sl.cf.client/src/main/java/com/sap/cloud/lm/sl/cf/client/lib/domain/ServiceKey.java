package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudService;

public interface ServiceKey {

    String getName();

    Map<String, Object> getParameters();

    Map<String, Object> getCredentials();

    String getGuid();

    CloudService getService();

}
