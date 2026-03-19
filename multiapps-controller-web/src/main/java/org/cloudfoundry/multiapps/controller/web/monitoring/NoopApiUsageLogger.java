package org.cloudfoundry.multiapps.controller.web.monitoring;

import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named
public class NoopApiUsageLogger implements ApiUsageLogger {

    @Override
    public void logFilesMutatingCall(String spaceGuid, String namespace, String endpoint, HttpServletRequest request) {
        // no-op
    }

    @Override
    public void logFilesReadCall(String spaceGuid, String namespace, String endpoint, HttpServletRequest request) {
        // no-op
    }

    @Override
    public void logOperationsMutatingCall(String spaceGuid, String endpoint, String operationId, HttpServletRequest request) {
        // no-op
    }

    @Override
    public void logOperationsReadCall(String spaceGuid, String endpoint, String operationId, HttpServletRequest request) {
        // no-op
    }
}
