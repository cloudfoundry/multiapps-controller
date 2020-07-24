package org.cloudfoundry.multiapps.controller.api;

import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.Mta;
import org.springframework.http.ResponseEntity;

public interface MtasApiService {

    ResponseEntity<List<Mta>> getMtas(String spaceGuid);

    ResponseEntity<Mta> getMta(String spaceGuid, String mtaId);

    ResponseEntity<List<Mta>> getMtas(String spaceGuid, String mtaId, String namespace);

}
