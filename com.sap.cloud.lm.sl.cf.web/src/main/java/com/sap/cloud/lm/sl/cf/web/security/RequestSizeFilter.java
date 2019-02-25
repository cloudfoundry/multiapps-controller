package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.GenericFilterBean;

public class RequestSizeFilter extends GenericFilterBean {

    private static final long MAX_REQUEST_SIZE_BYTES = 1 * 1024 * 1024L;
    private static final int PAYLOAD_TOO_LARGE_HTTP_STATUS_CODE = 413;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        long requestSize = request.getContentLengthLong();
        String path = ((HttpServletRequest) request).getRequestURI();
        if (requestSize > MAX_REQUEST_SIZE_BYTES && !path.endsWith("/files")) {
            ((HttpServletResponse) response).sendError(PAYLOAD_TOO_LARGE_HTTP_STATUS_CODE);
            return;
        }
        chain.doFilter(request, response);
    }
}
