package org.cloudfoundry.multiapps.controller.api;

import org.springframework.http.ResponseEntity;

public interface CsrfTokenApiService {

    ResponseEntity<Void> getCsrfToken();

}
