package org.cloudfoundry.multiapps.controller.client.util;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;

public class ResilientCloudOperationExecutor extends ResilientOperationExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientCloudOperationExecutor.class);

    private static final Set<HttpStatus> DEFAULT_STATUSES_TO_IGNORE = Set.of(HttpStatus.GATEWAY_TIMEOUT, HttpStatus.REQUEST_TIMEOUT,
                                                                             HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY,
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
        this.additionalStatusesToIgnore = new HashSet<>(Arrays.asList(statusesToIgnore));
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
        if (!shouldRetryWithWait(e)) {
            MiscUtil.sleep(getRandomWaitTime());
            throw e;
        }
        if (!shouldRetry(e)) {
            throw e;
        }
        LOGGER.warn(MessageFormat.format("Retrying operation that failed with status {0} and message: {1}", e.getStatusCode(),
                                         e.getMessage()),
                    e);
    }

    private boolean shouldRetryWithWait(CloudOperationException e) {
        return HttpStatus.TOO_MANY_REQUESTS.equals(e.getStatusCode());
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

    private long getRandomWaitTime() {
        return new Random().longs(0, MAX_WAIT_TIME_BETWEEN_RETRIES_IN_MILLIS)
                     .findFirst()
                     .getAsLong();
    }

}
