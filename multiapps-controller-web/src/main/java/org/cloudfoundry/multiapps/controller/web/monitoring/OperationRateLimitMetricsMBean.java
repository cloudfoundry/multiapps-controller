package org.cloudfoundry.multiapps.controller.web.monitoring;

public interface OperationRateLimitMetricsMBean {

    long getRateLimitRejectionCount();

    long getRateLimitRejectionCountInWindow();

}
