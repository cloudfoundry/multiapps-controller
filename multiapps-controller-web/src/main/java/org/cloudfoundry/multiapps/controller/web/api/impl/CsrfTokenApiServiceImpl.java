package org.cloudfoundry.multiapps.controller.web.api.impl;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.CsrfTokenApiService;
import org.springframework.http.ResponseEntity;

@Named
public class CsrfTokenApiServiceImpl implements CsrfTokenApiService {

    @Override
    public ResponseEntity<Void> getCsrfToken() {
        return ResponseEntity.noContent()
                             .build();
    }

}
