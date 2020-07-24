package org.cloudfoundry.multiapps.controller.web;

public class Constants {

    private Constants() {
    }

    public static final String RATE_LIMIT = "X-RateLimit-Limit";
    public static final String RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String RATE_LIMIT_RESET = "X-RateLimit-Reset";
    public static final String CSRF_TOKEN = "X-CSRF-TOKEN";
    public static final String CSRF_PARAM_NAME = "X-CSRF-PARAM";
    public static final String CSRF_HEADER_NAME = "X-CSRF-HEADER";
}
