package org.cloudfoundry.multiapps.controller.web.resources;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CsrfTokenResourceTest {

    private CsrfTokenResource resource;

    @BeforeEach
    void setUp() {
        resource = new CsrfTokenResource();
    }

    @Test
    void testGetCsrfTokenReturnsNoContent() {
        ResponseEntity<Void> response = resource.getCsrfToken();

        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        Assertions.assertNull(response.getBody());
    }
}
