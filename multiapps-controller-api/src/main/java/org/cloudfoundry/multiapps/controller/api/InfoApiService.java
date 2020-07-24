package org.cloudfoundry.multiapps.controller.api;

import org.cloudfoundry.multiapps.controller.api.model.Info;
import org.springframework.http.ResponseEntity;

public interface InfoApiService {

    ResponseEntity<Info> getInfo();

}
