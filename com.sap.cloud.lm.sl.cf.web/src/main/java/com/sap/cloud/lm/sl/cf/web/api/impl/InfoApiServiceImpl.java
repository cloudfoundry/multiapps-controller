package com.sap.cloud.lm.sl.cf.web.api.impl;

import javax.inject.Named;

import org.springframework.http.ResponseEntity;

import com.sap.cloud.lm.sl.cf.web.api.InfoApiService;
import com.sap.cloud.lm.sl.cf.web.api.model.Info;

@Named
public class InfoApiServiceImpl implements InfoApiService {

    @Override
    public ResponseEntity<Info> getInfo() {
        Info info = new Info();
        info.setApiVersion(1);
        return ResponseEntity.ok()
                             .body(info);
    }

}
