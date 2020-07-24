package com.sap.cloud.lm.sl.cf.web.configuration.service;

import java.util.Map;

import org.springframework.cloud.cloudfoundry.CloudFoundryServiceInfoCreator;
import org.springframework.cloud.cloudfoundry.Tags;

public class DynatraceServiceInfoCreator extends CloudFoundryServiceInfoCreator<DynatraceServiceInfo> {

    private static final String API_PATH_ENDING = "/api";
    private static final String DEFAULT_DYNATRACE_SERVICE_LABEL = "user-provided";
    public static final String DEFAULT_DYNATRACE_SERVICE_ID = "deploy-service-dynatrace";


    public DynatraceServiceInfoCreator() {
        super(new Tags(DEFAULT_DYNATRACE_SERVICE_LABEL), "");
    }

    @Override
    public boolean accept(Map<String, Object> serviceData) {
        return serviceMatches(serviceData);
    }

    private boolean serviceMatches(Map<String, Object> serviceData) {
        String label = (String) serviceData.get("label");
        String name = (String) serviceData.get("name");
        return DEFAULT_DYNATRACE_SERVICE_LABEL.equals(label) && DEFAULT_DYNATRACE_SERVICE_ID.equals(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DynatraceServiceInfo createServiceInfo(Map<String, Object> serviceData) {
        Map<String, Object> credentials = (Map<String, Object>) serviceData.get("credentials");
        String apiUrl = ((String) credentials.get("apiurl"));
        String micrometerToken = (String) credentials.get("micrometertoken");
        String micrometerUrl = removeSlashApiAtEnd(apiUrl);
        return new DynatraceServiceInfo(DEFAULT_DYNATRACE_SERVICE_ID, micrometerUrl, micrometerToken);
    }

    private String removeSlashApiAtEnd(String apiUrl) {
        if (apiUrl.endsWith(API_PATH_ENDING)) {
            return apiUrl.substring(0, apiUrl.lastIndexOf(API_PATH_ENDING));
        }
        return apiUrl;
    }

}
