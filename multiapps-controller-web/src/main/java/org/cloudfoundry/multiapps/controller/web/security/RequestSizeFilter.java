package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;

import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.web.util.ServletUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.GenericFilterBean;

@Named("requestSizeFilter")
public class RequestSizeFilter extends GenericFilterBean {

    private static final long MAX_REQUEST_SIZE_BYTES = 1024 * 1024L; // 1MB
    private static final long MAX_FILE_UPLOAD_REQUEST_SIZE_BYTES = 4096 * 1024 * 1024L; // 4GB

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        long requestSize = request.getContentLengthLong();
        String path = ServletUtil.decodeUri((HttpServletRequest) request);
        if (requestSize > MAX_REQUEST_SIZE_BYTES && !path.endsWith("/files")) {
            ((HttpServletResponse) response).sendError(HttpStatus.PAYLOAD_TOO_LARGE.value());
            return;
        }
        if (requestSize > MAX_FILE_UPLOAD_REQUEST_SIZE_BYTES && path.endsWith("files")) {
            ((HttpServletResponse) response).sendError(HttpStatus.PAYLOAD_TOO_LARGE.value());
            return;
        }
        chain.doFilter(request, response);
    }
}
