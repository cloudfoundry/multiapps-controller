package org.cloudfoundry.multiapps.controller.web.util;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.pivotal.cfenv.core.CfService;
import io.pivotal.cfenv.jdbc.CfJdbcEnv;
import io.pivotal.cfenv.jdbc.CfJdbcService;

class EnvironmentServicesFinderTest {

    private static final String SERVICE_NAME = "my-service";

    private CfJdbcEnv env;
    private EnvironmentServicesFinder environmentServicesFinder;

    @BeforeEach
    void setUp() {
        this.env = Mockito.mock(CfJdbcEnv.class);
        this.environmentServicesFinder = new EnvironmentServicesFinder(env);
    }

    @Test
    void testFindJdbcService() {
        CfJdbcService expectedService = Mockito.mock(CfJdbcService.class);
        Mockito.when(env.findJdbcServiceByName(SERVICE_NAME))
               .thenReturn(expectedService);

        CfJdbcService service = environmentServicesFinder.findJdbcService(SERVICE_NAME);

        Assertions.assertEquals(expectedService, service);
    }

    @Test
    void testFindJdbcServiceWithZeroOrMultipleMatches() {
        Mockito.when(env.findJdbcServiceByName(SERVICE_NAME))
               .thenThrow(IllegalArgumentException.class);

        CfJdbcService service = environmentServicesFinder.findJdbcService(SERVICE_NAME);

        Assertions.assertNull(service);
    }

    @Test
    void testFindJdbcServiceWithNullOrEmptyServiceName() {
        Assertions.assertNull(environmentServicesFinder.findJdbcService(null));
        Assertions.assertNull(environmentServicesFinder.findJdbcService(""));
    }

    @Test
    void testFindService() {
        CfService expectedService = Mockito.mock(CfService.class);
        Mockito.when(env.findServicesByName(SERVICE_NAME))
               .thenReturn(List.of(expectedService));

        CfService service = environmentServicesFinder.findService(SERVICE_NAME);

        Assertions.assertEquals(expectedService, service);
    }

    @Test
    void testFindServiceWithMultipleMatches() {
        CfService expectedService1 = Mockito.mock(CfService.class);
        CfService expectedService2 = Mockito.mock(CfService.class);
        Mockito.when(env.findServicesByName(SERVICE_NAME))
               .thenReturn(List.of(expectedService1, expectedService2));

        CfService service = environmentServicesFinder.findService(SERVICE_NAME);

        Assertions.assertEquals(expectedService1, service);
    }

    @Test
    void testFindServiceWithZeroMatches() {
        Mockito.when(env.findServicesByName(SERVICE_NAME))
               .thenReturn(Collections.emptyList());

        CfService service = environmentServicesFinder.findService(SERVICE_NAME);

        Assertions.assertNull(service);
    }

    @Test
    void testFindServiceWithNullOrEmptyServiceName() {
        Assertions.assertNull(environmentServicesFinder.findService(null));
        Assertions.assertNull(environmentServicesFinder.findService(""));
    }

}
