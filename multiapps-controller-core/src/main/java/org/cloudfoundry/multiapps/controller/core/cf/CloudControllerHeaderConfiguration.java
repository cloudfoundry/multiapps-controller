package org.cloudfoundry.multiapps.controller.core.cf;

import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.cloudfoundry.multiapps.controller.core.Constants;

public class CloudControllerHeaderConfiguration {

    public static final String TAG_HEADER_CORRELATION_ID = "x-correlation-id";
    private static final String USER_AGENT_PRODUCT = "MtaDeployService";
    private static final String USER_AGENT_UNKNOWN_VERSION = "unknown";

    private String productVersion;

    public CloudControllerHeaderConfiguration(String productVersion) {
        this.productVersion = productVersion;
    }

    public Map<String, String> generateHeaders(String correlationId) {
        if (StringUtils.isEmpty(correlationId)) {
            return Map.of(HttpHeaders.USER_AGENT, getUserAgent());
        }
        String spanId = RandomStringUtils.randomAlphanumeric(16);
        return Map.of(TAG_HEADER_CORRELATION_ID, correlationId, Constants.B3_TRACE_ID_HEADER, correlationId, Constants.B3_SPAN_ID_HEADER,
                      spanId, HttpHeaders.USER_AGENT, getUserAgent());
    }

    private String getUserAgent() {
        if (StringUtils.isEmpty(productVersion)) {
            return USER_AGENT_PRODUCT + "/" + USER_AGENT_UNKNOWN_VERSION;
        }
        return USER_AGENT_PRODUCT + "/" + productVersion;
    }
}
