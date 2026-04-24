package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.util.DataSourceFactory;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.cloudfoundry.multiapps.controller.web.configuration.bean.CloudDataSourceFactoryBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.pivotal.cfenv.jdbc.CfJdbcService;

class CloudDataSourceFactoryBeanTest {

    private static final String SERVICE_NAME = "my-service";
    private static final int DB_CONNECTION_THREADS = 100;

    @Mock
    private DataSource dataSource;
    @Mock
    private DataSourceFactory dataSourceFactory;
    @Mock
    private EnvironmentServicesFinder vcapServiceFinder;
    @Mock
    private ApplicationConfiguration configuration;

    private CloudDataSourceFactoryBean testedFactory;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        testedFactory = new CloudDataSourceFactoryBean(SERVICE_NAME, dataSourceFactory, vcapServiceFinder, configuration);
    }

    @Test
    void testWhenServiceExists() {
        CfJdbcService service = mock(CfJdbcService.class);
        when(vcapServiceFinder.findJdbcService(SERVICE_NAME))
               .thenReturn(service);
        when(configuration.getDbConnectionThreads())
               .thenReturn(DB_CONNECTION_THREADS);
        when(configuration.getApplicationGuid())
               .thenReturn("1");
        when(configuration.getApplicationInstanceIndex())
               .thenReturn(0);
        when(dataSourceFactory.createDataSource(service, DB_CONNECTION_THREADS, "ds-1/0"))
               .thenReturn(dataSource, null, null);

        testedFactory.afterPropertiesSet();

        assertEquals(dataSource, testedFactory.getObject());
    }

    @Test
    void testWhenServiceDoesNotExist() {
        testedFactory.afterPropertiesSet();
        assertNull(testedFactory.getObject());
    }

}
