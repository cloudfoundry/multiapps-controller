package com.sap.cloud.lm.sl.cf.core.model;

import java.util.List;

import javax.ws.rs.QueryParam;

public class OperationsBean {

    @QueryParam("last")
    private Integer lastRequestedOperationsCount;

    @QueryParam("status")
    private List<String> statusList;

    public OperationsBean() {
    }

    public OperationsBean(Integer last, List<String> statusList) {
        this.lastRequestedOperationsCount = last;
        this.statusList = statusList;
    }

    public Integer getLastRequestedOperationsCount() {
        return lastRequestedOperationsCount;
    }

    public void setLastRequestedOperations(int last) {
        this.lastRequestedOperationsCount = last;
    }

    public List<String> getStatusList() {
        return statusList;
    }

    public void setStatusList(List<String> statusList) {
        this.statusList = statusList;
    }

}
