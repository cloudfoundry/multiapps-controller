package org.cloudfoundry.multiapps.controller.web.util;

import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import org.cloudfoundry.multiapps.controller.web.Constants;

/**
 * Resolves the client IP address for rate limiting. Prefers the {@code X-CF-True-Client-IP} header injected by the platform router and
 * falls back to the transport-level remote address when the header is absent or blank.
 */
@Named
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        // The header is trusted only because the platform edge router injects it and overwrites any
        // value sent by the client. If that guarantee does not hold, a caller could spoof this header to
        // evade per-IP rate limiting. Do not remove the getRemoteAddr() fallback, which is the only
        // non-spoofable source when the trusted header is absent.
        String trueClientIp = request.getHeader(Constants.X_CF_TRUE_CLIENT_IP_HEADER);
        if (trueClientIp != null && !trueClientIp.isBlank()) {
            return trueClientIp;
        }
        return request.getRemoteAddr();
    }
}
