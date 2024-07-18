package org.cloudfoundry.multiapps.controller.web.api.impl;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.api.CsrfTokenApiService;
import org.cloudfoundry.multiapps.controller.core.auditlogging.CsrfTokenApiServiceAuditLog;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.springframework.http.ResponseEntity;

@Named
public class CsrfTokenApiServiceImpl implements CsrfTokenApiService {

    @Inject
    private CsrfTokenApiServiceAuditLog csrfTokenApiServiceAuditLog;

    @Override
    public ResponseEntity<Void> getCsrfToken() {
        csrfTokenApiServiceAuditLog.logGetInfo(SecurityContextUtil.getUsername());
        return ResponseEntity.noContent()
                             .build();
    }

}
