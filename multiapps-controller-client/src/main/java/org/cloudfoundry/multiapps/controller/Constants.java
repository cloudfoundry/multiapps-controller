package org.cloudfoundry.multiapps.controller;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Constants {

    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofMinutes(1);
    public static final Duration JOB_POLL_MIN_INTERVAL = Duration.ofSeconds(1);
    public static final Duration JOB_POLL_MAX_INTERVAL = Duration.ofSeconds(15);
    public static final Duration DELETE_JOB_TIMEOUT = Duration.ofMinutes(5);
    public static final Duration BINDING_OPERATIONS_TIMEOUT = Duration.ofMinutes(10);
    public static final long PACKAGE_UPLOAD_JOB_POLLING_PERIOD = TimeUnit.SECONDS.toMillis(5);

    public static final int DEFAULT_CONNECTION_POOL_SIZE = 192;
    public static final int UNDEFINED_PORT = -1;
    public static final int MAX_CONCURRENT_PAGES = 256;
    public static final int DEFAULT_CONCURRENT_TASKS = 256;
    public static final int MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST = 4000;

    public static final String CONNECTION_POOL_NAME = "cf-controller-client";
    public static final String LOOP_RESOURCES_SUFFIX = "-loop-resources";
    public static final String CF_API_V3 = "/v3";
    public static final String HREF = "href";
    public static final String CLOUD_CONTROLLER_CF_ROOT_DOCUMENT_NAME = "cloud_controller_v3";
    public static final String LOG_CACHE_CF_ROOT_DOCUMENT_NAME = "log_cache";
    public static final String API_HOST_PREFIX = "api.";
    public static final String LOG_CACHE_PREFIX = "log-cache.";
    public static final String ROOT_DOCUMENT_LINKS_LIST = "links";

    public static final String EMPTY_STRING = "";
    public static final String COLON = ":";
    public static final String PROTOCOL_SEPARATOR = "://";

    public static final String PACKAGE_LINK = "package";
    public static final String WEB_PROCESS_TYPE = "web";

}
