package com.sap.cloud.lm.sl.cf.web.api;

import org.springframework.http.ResponseEntity;

public interface CsrfTokenApiService {

    ResponseEntity<Void> getCsrfToken();

}
