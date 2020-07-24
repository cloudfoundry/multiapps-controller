package org.cloudfoundry.multiapps.controller.web.api.impl;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.InfoApiService;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableInfo;
import org.cloudfoundry.multiapps.controller.api.model.Info;
import org.springframework.http.ResponseEntity;

@Named
public class InfoApiServiceImpl implements InfoApiService {

    @Override
    public ResponseEntity<Info> getInfo() {
        Info info = ImmutableInfo.builder()
                                 .apiVersion(1)
                                 .build();
        return ResponseEntity.ok()
                             .body(info);
    }

}
