package com.sap.cloud.lm.sl.cf.web.api;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.sap.cloud.lm.sl.cf.web.api.model.Mta;

public interface MtasApiService {
    
    ResponseEntity<List<Mta>> getMtas(String spaceGuid);

    ResponseEntity<Mta> getMta(String spaceGuid, String mtaId);

}
