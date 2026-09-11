package org.cloudfoundry.multiapps.controller.web.monitoring;

public class ActiveOperationsCount implements ActiveOperationsCountMBean {

    private volatile long count;

    public ActiveOperationsCount(long count) {
        this.count = count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public long getCount() {
        return count;
    }

}
