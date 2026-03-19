package org.cloudfoundry.multiapps.controller.web.monitoring;

import jakarta.servlet.http.HttpServletRequest;

public interface ApiUsageLogger {

    void logFilesMutatingCall(String spaceGuid, String namespace, String endpoint, HttpServletRequest request);

    void logFilesReadCall(String spaceGuid, String namespace, String endpoint, HttpServletRequest request);

    void logOperationsMutatingCall(String spaceGuid, String endpoint, String operationId, HttpServletRequest request);

    void logOperationsReadCall(String spaceGuid, String endpoint, String operationId, HttpServletRequest request);
}
