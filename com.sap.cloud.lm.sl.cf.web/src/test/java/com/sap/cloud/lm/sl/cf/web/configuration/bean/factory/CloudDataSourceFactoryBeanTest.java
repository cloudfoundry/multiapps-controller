package com.sap.cloud.lm.sl.cf.web.configuration.bean.factory;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.service.relational.DataSourceConfig;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.LambdaArgumentMatcher;

public class CloudDataSourceFactoryBeanTest {

    private final class TestedCloudDataSourceFactoryBean extends CloudDataSourceFactoryBean {

        @Override
        protected Cloud getSpringCloud() {
            return springCloudMock;
        }
    }

    @Mock
    private DataSource defaultDataSource;
    @Mock
    private DataSource createdDataSource;
    @Mock
    private ApplicationConfiguration configurationMock;
    @Mock
    private Cloud springCloudMock;

    private CloudDataSourceFactoryBean testedFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        testedFactory = new TestedCloudDataSourceFactoryBean();
    }

    @Test
    public void testDefaultProvisioning() {
        testedFactory.setDefaultDataSource(defaultDataSource);
        testedFactory.setServiceName("");
        testedFactory.setConfiguration(configurationMock);
        testedFactory.afterPropertiesSet();
        assertEquals(testedFactory.getObject(), defaultDataSource);
    }

    @Test
    public void testReadConfiguration() {
        final int DB_CONNECTIONS = 15;
        final String SERVICE_NAME = "abc";

        when(configurationMock.getDbConnectionThreads()).thenReturn(DB_CONNECTIONS);

        ArgumentMatcher<DataSourceConfig> dataSourceConfigMatcher = new LambdaArgumentMatcher<>((Object input) -> DB_CONNECTIONS == ((DataSourceConfig) input).getPoolConfig()
                                                                                                                                                              .getMaxTotal());
        when(springCloudMock.getServiceConnector(eq(SERVICE_NAME), eq(DataSource.class),
                                                 argThat(dataSourceConfigMatcher))).thenReturn(createdDataSource);

        testedFactory.setDefaultDataSource(defaultDataSource);
        testedFactory.setServiceName(SERVICE_NAME);
        testedFactory.setConfiguration(configurationMock);

        testedFactory.afterPropertiesSet();

        verify(springCloudMock, atLeastOnce()).getServiceConnector(any(), any(), any());
        assertEquals(testedFactory.getObject(), createdDataSource);
    }

    @Test
    public void testFallBackToDefault() {
        when(configurationMock.getDbConnectionThreads()).thenReturn(30);
        when(springCloudMock.getServiceConnector(any(), any(), any())).thenThrow(new CloudException("unknown service"));

        testedFactory.setDefaultDataSource(defaultDataSource);
        testedFactory.setServiceName("any");
        testedFactory.setConfiguration(configurationMock);
        testedFactory.afterPropertiesSet();

        verify(springCloudMock, atLeastOnce()).getServiceConnector(any(), any(), any());
        assertEquals(testedFactory.getObject(), defaultDataSource);
    }
}
