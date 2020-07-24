package com.sap.cloud.lm.sl.cf.web.resources;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/csrf-token")
public class CsrfTokenResource {

    @GetMapping
    public ResponseEntity<Void> getCsrfToken() {
        return ResponseEntity.noContent()
                             .build();
    }

}
