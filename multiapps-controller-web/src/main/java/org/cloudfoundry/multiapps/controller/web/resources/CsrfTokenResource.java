package org.cloudfoundry.multiapps.controller.web.resources;

import org.cloudfoundry.multiapps.controller.web.Constants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = Constants.Resources.CSRF_TOKEN)
public class CsrfTokenResource {

    @GetMapping
    public ResponseEntity<Void> getCsrfToken() {
        return ResponseEntity.noContent()
                             .build();
    }

}
