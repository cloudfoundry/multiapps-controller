package org.cloudfoundry.multiapps.controller.web.api.impl;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.InfoApiService;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableInfo;
import org.cloudfoundry.multiapps.controller.api.model.Info;
import org.cloudfoundry.multiapps.controller.core.auditlogging.InfoApiServiceAuditLog;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.springframework.http.ResponseEntity;

@Named
public class InfoApiServiceImpl implements InfoApiService {

    @Override
    public ResponseEntity<Info> getInfo() {
        InfoApiServiceAuditLog.auditLogGetInfo(SecurityContextUtil.getUsername());
        Info info = ImmutableInfo.builder()
                                 .apiVersion(1)
                                 .build();
        return ResponseEntity.ok()
                             .body(info);
    }

}
