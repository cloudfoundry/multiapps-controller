package org.cloudfoundry.multiapps.controller.api;

import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.Mta;
import org.cloudfoundry.multiapps.controller.api.model.MtaGraph;
import org.springframework.http.ResponseEntity;

public interface MtasApiService {

    ResponseEntity<List<Mta>> getMtas(String spaceGuid);

    ResponseEntity<Mta> getMta(String spaceGuid, String mtaId);

    ResponseEntity<MtaGraph> getMtaGraph(String spaceGuid, String mtaId, String namespace);

    ResponseEntity<MtaGraph> buildGraphFromDescriptor(String spaceGuid, String descriptorYaml);

    ResponseEntity<String> buildGraphFromDescriptorAsDot(String spaceGuid, String descriptorYaml);

    ResponseEntity<List<Mta>> getMtas(String spaceGuid, String mtaId, String namespace);

}
