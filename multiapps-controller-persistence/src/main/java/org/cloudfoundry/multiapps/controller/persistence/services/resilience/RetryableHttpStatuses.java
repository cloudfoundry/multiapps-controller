package org.cloudfoundry.multiapps.controller.persistence.services.resilience;

import java.util.Set;
import org.springframework.http.HttpStatus;

final class RetryableHttpStatuses {

    private static final Set<HttpStatus> VALUES = Set.of(HttpStatus.REQUEST_TIMEOUT,
                                                         HttpStatus.TOO_MANY_REQUESTS,
                                                         HttpStatus.INTERNAL_SERVER_ERROR,
                                                         HttpStatus.BAD_GATEWAY,
                                                         HttpStatus.SERVICE_UNAVAILABLE,
                                                         HttpStatus.GATEWAY_TIMEOUT);

    private RetryableHttpStatuses() {
    }

    static boolean contains(int statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode);
        return status != null && VALUES.contains(status);
    }

}
