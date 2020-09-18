package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import static org.junit.Assert.assertEquals;

import org.cloudfoundry.multiapps.controller.web.util.EnvironmentServicesFinder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.zaxxer.hikari.HikariDataSource;

import io.pivotal.cfenv.jdbc.CfJdbcService;

public class CloudDataSourceFactoryBeanTest {

    private static final String SERVICE_NAME = "my-service";

    @Mock
    private HikariDataSource dataSource;
    @Mock
    private DataSourceFactory dataSourceFactory;
    @Mock
    private EnvironmentServicesFinder vcapServiceFinder;

    private CloudDataSourceFactoryBean testedFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        testedFactory = new CloudDataSourceFactoryBean(SERVICE_NAME, dataSourceFactory, vcapServiceFinder);
    }

    @Test
    public void testWhenServiceExists() {
        CfJdbcService service = Mockito.mock(CfJdbcService.class);
        Mockito.when(vcapServiceFinder.findJdbcService(SERVICE_NAME))
               .thenReturn(service);
        Mockito.when(dataSourceFactory.createHikariDataSource(service))
               .thenReturn(dataSource);

        testedFactory.afterPropertiesSet();

        assertEquals(dataSource, testedFactory.getObject());
    }

    @Test
    public void testWhenServiceDoesNotExist() {
        testedFactory.afterPropertiesSet();

        assertEquals(null, testedFactory.getObject());
    }

}
