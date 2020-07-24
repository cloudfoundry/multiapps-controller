package com.sap.cloud.lm.sl.cf.web.api;

import org.springframework.http.ResponseEntity;

import com.sap.cloud.lm.sl.cf.web.api.model.Info;

public interface InfoApiService {

    ResponseEntity<Info> getInfo();

}
