package org.cloudfoundry.multiapps.controller.web.api.impl;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.CsrfTokenApiService;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.auditlogging.CsrfTokenApiServiceAuditLog;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ExtentensionAuditLog;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.springframework.http.ResponseEntity;

@Named
public class CsrfTokenApiServiceImpl implements CsrfTokenApiService {

    @Override
    public ResponseEntity<Void> getCsrfToken() {
        CsrfTokenApiServiceAuditLog.auditLogGetInfo(SecurityContextUtil.getUsername());
        return ResponseEntity.noContent()
                             .build();
    }

}
