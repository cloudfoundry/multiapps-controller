package com.sap.cloud.lm.sl.cf.web.resources;

import javax.ws.rs.core.Response;

public class RestResponse {

    private int status;
    private Object entity;
    private String errorMessage;

    public RestResponse(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public RestResponse(int status, Object entity) {
        this.status = status;
        this.entity = entity;
    }

    public RestResponse(Response response) {
        this.status = response.getStatus();
        this.entity = response.getEntity();
    }

    public int getStatus() {
        return status;
    }

    public Object getEntity() {
        return entity;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}