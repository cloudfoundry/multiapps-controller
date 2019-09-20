package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.GenericFilterBean;

import com.sap.cloud.lm.sl.cf.core.Constants;

@Named("correlationIdFilter")
public class CorrelationIdFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        MDC.put(Constants.ATTR_CORRELATION_ID, UUID.randomUUID()
                                                   .toString());
        filterChain.doFilter(request, response);
    }

}
