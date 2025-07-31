package org.cloudfoundry.multiapps.controller.client.facade.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.util.CloudUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CloudControllerResponseErrorHandler extends DefaultResponseErrorHandler {

    private static CloudOperationException getException(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.valueOf(response.getStatusCode()
                                                           .value());
        String statusText = response.getStatusText();

        ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally

        if (response.getBody() != null) {
            try {
                @SuppressWarnings("unchecked") Map<String, Object> responseBody = mapper.readValue(response.getBody(), Map.class);
                String description = getTrimmedDescription(responseBody);
                return new CloudOperationException(statusCode, statusText, description);
            } catch (IOException e) {
                // Fall through. Handled below.
            }
        }
        return new CloudOperationException(statusCode, statusText);
    }

    private static String getTrimmedDescription(Map<String, Object> responseBody) {
        String description = getDescription(responseBody);
        return description == null ? null : description.trim();
    }

    private static String getDescription(Map<String, Object> responseBody) {
        String description = getV2Description(responseBody);
        return description == null ? getV3Description(responseBody) : description;
    }

    private static String getV2Description(Map<String, Object> responseBody) {
        return CloudUtil.parse(String.class, responseBody.get("description"));
    }

    @SuppressWarnings("unchecked")
    private static String getV3Description(Map<String, Object> responseBody) {
        List<Map<String, Object>> errors = (List<Map<String, Object>>) responseBody.get("errors");
        return errors == null ? null : concatenateErrorMessages(errors);
    }

    private static String concatenateErrorMessages(List<Map<String, Object>> errors) {
        return errors.stream()
                     .map(error -> (String) error.get("detail"))
                     .collect(Collectors.joining("\n"));
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.valueOf(response.getStatusCode()
                                                           .value());
        switch (statusCode.series()) {
            case CLIENT_ERROR:
            case SERVER_ERROR:
                throw getException(response);
            default:
                throw new RestClientException("Unknown status code [" + statusCode + "]");
        }
    }

}
