package com.sap.cloud.lm.sl.cf.web.api.impl;

import javax.inject.Named;

import org.springframework.http.ResponseEntity;

import com.sap.cloud.lm.sl.cf.web.api.CsrfTokenApiService;

@Named
public class CsrfTokenApiServiceImpl implements CsrfTokenApiService {

    @Override
    public ResponseEntity<Void> getCsrfToken() {
        return ResponseEntity.noContent()
                             .build();
    }

}
