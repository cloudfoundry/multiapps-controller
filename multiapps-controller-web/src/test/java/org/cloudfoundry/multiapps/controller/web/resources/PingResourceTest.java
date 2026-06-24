package org.cloudfoundry.multiapps.controller.web.resources;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PingResourceTest {

    private PingResource resource;

    @BeforeEach
    void setUp() {
        resource = new PingResource();
    }

    @Test
    void testPingReturnsPongWithOkStatus() {
        ResponseEntity<String> response = resource.ping();

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("pong", response.getBody());
    }
}
