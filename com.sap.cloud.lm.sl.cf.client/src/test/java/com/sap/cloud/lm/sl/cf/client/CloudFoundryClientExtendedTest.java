package com.sap.cloud.lm.sl.cf.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CloudFoundryClientExtendedTest {

    @Test
    void testFromNonCFE() {
        Throwable cause = new Throwable("a");
        CloudFoundryException newException = CloudFoundryClientExtended.fromException("test", cause, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(cause, newException.getCause());
    }

    @Test
    void testFromCFE() {
        Throwable source = new CloudFoundryException(HttpStatus.NOT_FOUND);
        CloudFoundryException newException = CloudFoundryClientExtended.fromException("test", source, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(source, newException);
        assertEquals(HttpStatus.NOT_FOUND, newException.getStatusCode());
    }

}
