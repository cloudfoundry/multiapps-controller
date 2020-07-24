package org.cloudfoundry.multiapps.controller.web.resources;

import org.springframework.http.ResponseEntity;

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

    public RestResponse(ResponseEntity<?> response) {
        this.status = response.getStatusCodeValue();
        this.entity = response.getBody();
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