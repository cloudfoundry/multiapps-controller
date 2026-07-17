package org.cloudfoundry.multiapps.controller;

public class Messages {

    // INFO messages
    public static final String WAITING_MS_BEFORE_RETRYING_WITH_TIMEOUT_OF_MS = "Waiting: {} ms before retrying with timeout of: {} ms";
    public static final String RATE_LIMITED_BY_CC_WAITING_S = "CC returned 429 with Retry-After: {} s. Waiting {} s (capped) before retrying.";
    public static final String RATE_LIMITED_BY_CC_NO_HEADER_WAITING_MS = "CC returned 429 without Retry-After header. Waiting {} ms before retrying.";
    public static final String RANDOM_WAIT_BEFORE_RETRY_S = "Waiting {} ms (randomized) before retrying failed CC operation.";

}
