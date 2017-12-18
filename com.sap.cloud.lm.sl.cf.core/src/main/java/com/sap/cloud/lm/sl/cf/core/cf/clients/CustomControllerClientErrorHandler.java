package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.text.MessageFormat;
import java.util.Map;
import java.util.function.Supplier;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

import com.sap.cloud.lm.sl.cf.client.util.ExecutionRetrier;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class CustomControllerClientErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomControllerClientErrorHandler.class);

    private ExecutionRetrier retrier;

    public CustomControllerClientErrorHandler() {
        this(new ExecutionRetrier());
    }

    public CustomControllerClientErrorHandler(ExecutionRetrier retrier) {
        this.retrier = retrier;
    }

    public <T> T handleErrorsOrReturnResult(Supplier<T> supplier, HttpStatus... httpStatusesToIgnore) {
        return retrier.executeWithRetry(() -> {
            try {
                return supplier.get();
            } catch (HttpStatusCodeException e) {
                throw asCloudFoundryException(e);
            }
        }, httpStatusesToIgnore);
    }

    public void handleErrors(Runnable runnable) {
        handleErrorsOrReturnResult(() -> {
            runnable.run();
            return null;
        });
    }

    private CloudFoundryException asCloudFoundryException(HttpStatusCodeException exception) {
        String description = getDescriptionFromResponseBody(exception.getResponseBodyAsString());
        return new CloudFoundryException(exception.getStatusCode(), exception.getStatusText(), description);
    }

    private String getDescriptionFromResponseBody(String responseBody) {
        try {
            return attemptToParseDescriptionFromResponseBody(responseBody);
        } catch (ParsingException e) {
            LOGGER.warn(MessageFormat.format("Could not parse description from response body: {0}", responseBody), e);
        }
        return null;
    }

    private String attemptToParseDescriptionFromResponseBody(String responseBody) {
        Map<String, Object> responseEntity = JsonUtil.convertJsonToMap(responseBody);
        Object result = responseEntity.get("description");
        return result instanceof String ? (String) result : null;
    }

}
