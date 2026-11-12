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
        String trueClientIp = request.getHeader(Constants.X_CF_TRUE_CLIENT_IP_HEADER);
        if (trueClientIp != null && !trueClientIp.isBlank()) {
            return trueClientIp;
        }
        return request.getRemoteAddr();
    }
}
