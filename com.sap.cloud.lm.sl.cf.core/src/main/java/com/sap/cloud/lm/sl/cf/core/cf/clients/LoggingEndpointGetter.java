package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.util.CloudUtil;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class LoggingEndpointGetter extends CustomControllerClient {

    private static final String V2_INFO_ENDPOINT = "/v2/info";

    public String getLoggingEndpoint(CloudFoundryOperations client) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> attmeptToGetLoggingEndpoint(client));
    }

    private String attmeptToGetLoggingEndpoint(CloudFoundryOperations client) {
        RestTemplate restTemplate = getRestTemplate(client);
        String controllerUrl = client.getCloudControllerUrl()
            .toString();
        String infoV2Url = getUrl(controllerUrl, V2_INFO_ENDPOINT);

        String infoV2Json = restTemplate.getForObject(infoV2Url, String.class);
        Map<String, Object> infoV2Map = JsonUtil.convertJsonToMap(infoV2Json);

        // The standard CloudFoundryOperations.getCloudInfo() method tries to retrieve the logging endpoint from a field named
        // 'logging_endpoint', which was replaced with 'doppler_logging_endpoint' in
        // https://github.com/cloudfoundry/cloud_controller_ng/commit/b02a23c38ebda79a3bbd2c78716c627147bf042f.
        return CloudUtil.parse(String.class, infoV2Map.get("doppler_logging_endpoint"));
    }

}
