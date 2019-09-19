package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;

import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.GenericFilterBean;

@Named("requestSizeFilter")
public class RequestSizeFilter extends GenericFilterBean {

    private static final long MAX_REQUEST_SIZE_BYTES = 1024 * 1024L;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        long requestSize = request.getContentLengthLong();
        String path = ((HttpServletRequest) request).getRequestURI();
        if (requestSize > MAX_REQUEST_SIZE_BYTES && !path.endsWith("/files")) {
            ((HttpServletResponse) response).sendError(HttpStatus.PAYLOAD_TOO_LARGE.value());
            return;
        }
        chain.doFilter(request, response);
    }
}
