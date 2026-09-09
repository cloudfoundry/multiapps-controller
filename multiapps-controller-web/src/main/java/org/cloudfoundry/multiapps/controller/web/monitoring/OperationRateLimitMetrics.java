package org.cloudfoundry.multiapps.controller.web.monitoring;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import jakarta.inject.Named;

@Named
public class OperationRateLimitMetrics implements OperationRateLimitMetricsMBean {

    private final LongAdder totalRejections = new LongAdder();
    private final AtomicLong rejectionsInWindow = new AtomicLong(0);

    public void recordRejection() {
        totalRejections.increment();
        rejectionsInWindow.incrementAndGet();
    }

    @Override
    public long getRateLimitRejectionCount() {
        return totalRejections.sum();
    }

    @Override
    public long getRateLimitRejectionCountInWindow() {
        return rejectionsInWindow.getAndSet(0);
    }

}
