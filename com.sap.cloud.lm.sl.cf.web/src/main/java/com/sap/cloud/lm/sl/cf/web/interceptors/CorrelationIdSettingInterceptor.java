package com.sap.cloud.lm.sl.cf.web.interceptors;

import java.util.UUID;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.web.api.Constants.PathVariables;
import com.sap.cloud.lm.sl.cf.web.util.ServletUtils;

@Named
public class CorrelationIdSettingInterceptor implements CustomHandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String correlationId = getOrGenerateCorrelationId(request);
        setCorrelationId(correlationId);
        return true;
    }

    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = ServletUtils.getPathVariable(request, PathVariables.OPERATION_ID);
        if (correlationId == null) {
            return generateCorrelationId();
        }
        return correlationId;
    }

    private String generateCorrelationId() {
        return UUID.randomUUID()
                   .toString();
    }

    private void setCorrelationId(String correlationId) {
        MDC.put(Constants.ATTR_CORRELATION_ID, correlationId);
    }

}
