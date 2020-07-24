package org.cloudfoundry.multiapps.controller.client.util;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.compress.utils.Sets;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class ResilientCloudOperationExecutor extends ResilientOperationExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientCloudOperationExecutor.class);

    private static final Set<HttpStatus> DEFAULT_STATUSES_TO_IGNORE = Sets.newHashSet(HttpStatus.GATEWAY_TIMEOUT,
                                                                                      HttpStatus.REQUEST_TIMEOUT,
                                                                                      HttpStatus.INTERNAL_SERVER_ERROR,
                                                                                      HttpStatus.BAD_GATEWAY,
                                                                                      HttpStatus.SERVICE_UNAVAILABLE);

    private Set<HttpStatus> additionalStatusesToIgnore = Collections.emptySet();

    @Override
    public ResilientCloudOperationExecutor withRetryCount(long retryCount) {
        return (ResilientCloudOperationExecutor) super.withRetryCount(retryCount);
    }

    @Override
    public ResilientCloudOperationExecutor withWaitTimeBetweenRetriesInMillis(long waitTimeBetweenRetriesInMillis) {
        return (ResilientCloudOperationExecutor) super.withWaitTimeBetweenRetriesInMillis(waitTimeBetweenRetriesInMillis);
    }

    public ResilientCloudOperationExecutor withStatusesToIgnore(HttpStatus... statusesToIgnore) {
        this.additionalStatusesToIgnore = Sets.newHashSet(statusesToIgnore);
        return this;
    }

    @Override
    protected void handle(RuntimeException e) {
        if (e instanceof CloudOperationException) {
            handle((CloudOperationException) e);
        } else {
            super.handle(e);
        }
    }

    protected void handle(CloudOperationException e) {
        if (!shouldRetry(e)) {
            throw e;
        }
        LOGGER.warn("Retrying operation that failed with status {} and message: {}", e.getStatusCode(), e.getMessage());
    }

    private boolean shouldRetry(CloudOperationException e) {
        return getStatusesToIgnore().contains(e.getStatusCode());
    }

    private Set<HttpStatus> getStatusesToIgnore() {
        if (additionalStatusesToIgnore.isEmpty()) {
            return DEFAULT_STATUSES_TO_IGNORE;
        }
        return SetUtils.union(DEFAULT_STATUSES_TO_IGNORE, additionalStatusesToIgnore);
    }

}
