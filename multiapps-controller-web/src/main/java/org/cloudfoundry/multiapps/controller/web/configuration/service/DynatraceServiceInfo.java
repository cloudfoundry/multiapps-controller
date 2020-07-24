package org.cloudfoundry.multiapps.controller.web.configuration.service;

import org.springframework.cloud.service.BaseServiceInfo;
import org.springframework.cloud.service.ServiceInfo.ServiceLabel;

@ServiceLabel("user-provided")
public class DynatraceServiceInfo extends BaseServiceInfo {

    private final String micrometerUrl;
    private final String micrometerToken;

    public DynatraceServiceInfo(String id, String micrometerUrl, String micrometerToken) {
        super(id);
        this.micrometerUrl = micrometerUrl;
        this.micrometerToken = micrometerToken;
    }

    public String getMicrometerUrl() {
        return micrometerUrl;
    }

    public String getMicrometerToken() {
        return micrometerToken;
    }

}
