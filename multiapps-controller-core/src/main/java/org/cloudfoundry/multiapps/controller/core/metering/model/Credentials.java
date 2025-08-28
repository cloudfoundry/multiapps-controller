package org.cloudfoundry.multiapps.controller.core.metering.model;

import java.util.Map;

public record Credentials(Map<String, Object> meteringCredentials) {

    public String certificate() {
        return meteringCredentials.get("certificate")
                                  .toString();
    }

    public String key() {
        return meteringCredentials.get("key")
                                  .toString();
    }

    public String retrievalEndpoint() {
        return getEndpoint("retrieval");
    }

    public String syncRetrievalEndpoint() {
        return getEndpoint("syncRetrieval");
    }

    public String usageIngestionEndpoint() {
        return getEndpoint("usageIngestion");
    }

    public String meteringAccount() {
        return getMeteringAccount("name");
    }

    public String domain() {
        return getMeteringAccount("domain");
    }

    public String dataStreamName() {
        return "btp$1";
    }

    private String getEndpoint(String endpointName) {
        Map<String, String> endpoints = (Map<String, String>) meteringCredentials.get("endpoints");

        return endpoints.get(endpointName);
    }

    private String getMeteringAccount(String endpointName) {
        Map<String, String> endpoints = (Map<String, String>) meteringCredentials.get("meteringAccount");

        return endpoints.get(endpointName);
    }
}
