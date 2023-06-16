package org.cloudfoundry.multiapps.controller.core.cf;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.core.Constants;

public class CloudControllerHeaderConfiguration {

    public static final String TAG_HEADER_CORRELATION_ID = "x-correlation-id";

    public Map<String, String> generateHeaders(String correlationId) {
        if (StringUtils.isEmpty(correlationId)) {
            return Collections.emptyMap();
        }
        String spanId = RandomStringUtils.randomAlphanumeric(16);
        return Map.of(TAG_HEADER_CORRELATION_ID, correlationId,
                      Constants.B3_TRACE_ID_HEADER, correlationId,
                      Constants.B3_SPAN_ID_HEADER, spanId);
    }

}
