package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.text.MessageFormat;
import java.util.Map;
import java.util.function.Supplier;

import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.util.ResilientCloudOperationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

import com.sap.cloudfoundry.client.facade.CloudOperationException;

public class CustomControllerClientErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomControllerClientErrorHandler.class);
    private Supplier<ResilientCloudOperationExecutor> executorFactory = ResilientCloudOperationExecutor::new;

    CustomControllerClientErrorHandler withExecutorFactory(Supplier<ResilientCloudOperationExecutor> executorFactory) {
        this.executorFactory = executorFactory;
        return this;
    }

    public void handleErrors(Runnable runnable) {
        handleErrorsOrReturnResult(() -> {
            runnable.run();
            return null;
        });
    }

    public <T> T handleErrorsOrReturnResult(Supplier<T> supplier, HttpStatus... statusesToIgnore) {
        return createExecutor(statusesToIgnore).execute((Supplier<T>) () -> {
            try {
                return supplier.get();
            } catch (HttpStatusCodeException e) {
                throw asCloudOperationException(e);
            }
        });
    }

    protected ResilientCloudOperationExecutor createExecutor(HttpStatus... statusesToIgnore) {
        return executorFactory.get()
                              .withStatusesToIgnore(statusesToIgnore);
    }

    private CloudOperationException asCloudOperationException(HttpStatusCodeException exception) {
        String description = getDescriptionFromResponseBody(exception.getResponseBodyAsString());
        return new CloudOperationException(exception.getStatusCode(), exception.getStatusText(), description);
    }

    private String getDescriptionFromResponseBody(String responseBody) {
        try {
            return tryParseDescriptionFromResponseBody(responseBody);
        } catch (ParsingException e) {
            LOGGER.warn(MessageFormat.format("Could not parse description from response body: {0}", responseBody), e);
        }
        return null;
    }

    private String tryParseDescriptionFromResponseBody(String responseBody) {
        Map<String, Object> responseEntity = JsonUtil.convertJsonToMap(responseBody);
        Object result = responseEntity.get("description");
        return result instanceof String ? (String) result : null;
    }

}
