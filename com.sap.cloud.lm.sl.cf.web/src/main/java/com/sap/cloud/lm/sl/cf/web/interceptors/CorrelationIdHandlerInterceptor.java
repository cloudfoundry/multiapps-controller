package com.sap.cloud.lm.sl.cf.web.interceptors;

import java.util.UUID;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;

import com.sap.cloud.lm.sl.cf.core.Constants;

@Named
public class CorrelationIdHandlerInterceptor implements CustomHandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        MDC.put(Constants.ATTR_CORRELATION_ID, UUID.randomUUID()
                                                   .toString());
        return true;
    }

}
