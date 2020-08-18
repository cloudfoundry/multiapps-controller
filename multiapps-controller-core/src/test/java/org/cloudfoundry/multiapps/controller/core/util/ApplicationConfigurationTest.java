package org.cloudfoundry.multiapps.controller.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingFacade;
import org.cloudfoundry.multiapps.controller.core.configuration.Environment;
import org.cloudfoundry.multiapps.controller.core.health.model.HealthCheckConfiguration;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Maps;

class ApplicationConfigurationTest {

    private static final String VCAP_APPLICATION_WITHOUT_SPACE = "vcap-application-without-space.json";
    private static final String VCAP_APPLICATION_WITHOUT_URLS = "vcap-application-without-urls.json";
    private static final String VCAP_APPLICATION = "vcap-application.json";
    private static final String PLATFORM = "platform.json";
    private static final String CLOUD_COMPONENTS = "cloud-components.json";

    @Mock
    private Environment environment;
    @Mock
    private AuditLoggingFacade auditLoggingFacade;
    @InjectMocks
    private ApplicationConfiguration configuration;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testLoadDefaultsWithAnEmptyEnvironment() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> configuration.load());
        assertEquals(Messages.CONTROLLER_URL_NOT_SPECIFIED, e.getMessage());
    }

    @Test
    void testGetControllerUrl() throws Exception {
        URL expectedControllerUrl = new URL("https://api.example.com");
        Map<String, String> vcapApplication = MapUtil.asMap("cf_api", expectedControllerUrl.toString());
        assertEquals(expectedControllerUrl, getControllerUrlWithVcapApplication(vcapApplication));
    }

    @Test
    void testGetControllerUrlWithInvalidValue() {
        String invalidUrl = "blabla";
        Map<String, String> vcapApplication = MapUtil.asMap("cf_api", invalidUrl);
        Exception e = assertThrows(IllegalArgumentException.class, () -> getControllerUrlWithVcapApplication(vcapApplication));
        e.printStackTrace();
        assertEquals(MessageFormat.format(Messages.INVALID_CONTROLLER_URL, invalidUrl), e.getMessage());
    }

    private URL getControllerUrlWithVcapApplication(Map<String, String> vcapApplication) {
        String vcapApplicationJson = JsonUtil.toJson(vcapApplication);
        when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION)).thenReturn(vcapApplicationJson);
        ApplicationConfiguration testedConfiguration = new ApplicationConfiguration(environment);
        return testedConfiguration.getControllerUrl();
    }

    @Test
    void testGetSpaceGuidWithNull() {
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_GUID, configuration.getSpaceGuid());
    }

    @Test
    void testGetSpaceGuidWithEmptyString() {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
               .thenReturn("");
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_GUID, configuration.getSpaceGuid());
    }

    @Test
    void testGetSpaceGuidWithInvalidJson() {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
               .thenReturn("invalid");
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_GUID, configuration.getSpaceGuid());
    }

    @Test
    void testGetSpaceGuidWithEmptyMap() {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
               .thenReturn("{}");
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_GUID, configuration.getSpaceGuid());
    }

    @Test
    void testGetSpaceGuidWithMissingSpaceGuid() {
        injectFileInEnvironment(VCAP_APPLICATION_WITHOUT_SPACE, ApplicationConfiguration.CFG_VCAP_APPLICATION);
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_GUID, configuration.getSpaceGuid());
    }

    @Test
    void testGetSpaceGuid() {
        Map<String, Object> vcapApplication = injectFileInEnvironment(VCAP_APPLICATION, ApplicationConfiguration.CFG_VCAP_APPLICATION);
        String spaceId = configuration.getSpaceGuid();
        assertEquals(vcapApplication.get("space_id"), spaceId);
    }

    @Test
    void testGetPlatformValidPlatform() {
        Map<String, Object> platformMap = injectFileInEnvironment(PLATFORM, ApplicationConfiguration.CFG_PLATFORM);
        Platform platform = configuration.getPlatform();
        Assertions.assertEquals(platformMap.get("name"), platform.getName());
        Assertions.assertFalse(platform.getResourceTypes()
                                       .isEmpty());
        Assertions.assertFalse(platform.getModuleTypes()
                                       .isEmpty());
        Assertions.assertTrue(platform.getParameters()
                                      .isEmpty());
    }

    @Test
    void testGetPlatformNoPlatformInEnvironment() {
        Exception exception = assertThrows(IllegalStateException.class, () -> configuration.getPlatform());
        Assertions.assertEquals(Messages.PLATFORMS_NOT_SPECIFIED, exception.getMessage());
    }

    @Test
    void testGetMaxUploadSize() {
        Mockito.when(environment.getLong(ApplicationConfiguration.CFG_MAX_UPLOAD_SIZE, ApplicationConfiguration.DEFAULT_MAX_UPLOAD_SIZE))
               .thenReturn(ApplicationConfiguration.DEFAULT_MAX_UPLOAD_SIZE);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_MAX_UPLOAD_SIZE, configuration.getMaxUploadSize());
    }

    @Test
    void testGetMaxMtaDescriptorSize() {
        Mockito.when(environment.getLong(ApplicationConfiguration.CFG_MAX_MTA_DESCRIPTOR_SIZE,
                                         ApplicationConfiguration.DEFAULT_MAX_MTA_DESCRIPTOR_SIZE))
               .thenReturn(ApplicationConfiguration.DEFAULT_MAX_MTA_DESCRIPTOR_SIZE);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_MAX_MTA_DESCRIPTOR_SIZE, configuration.getMaxMtaDescriptorSize());
    }

    @Test
    void testGetMaxManifestFileSize() {
        Mockito.when(environment.getLong(ApplicationConfiguration.CFG_MAX_MANIFEST_SIZE,
                                         ApplicationConfiguration.DEFAULT_MAX_MANIFEST_SIZE))
               .thenReturn(ApplicationConfiguration.DEFAULT_MAX_MANIFEST_SIZE);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_MAX_MANIFEST_SIZE, configuration.getMaxManifestSize());
    }

    @Test
    void testGetMaxResourceFileSize() {
        Mockito.when(environment.getLong(ApplicationConfiguration.CFG_MAX_RESOURCE_FILE_SIZE,
                                         ApplicationConfiguration.DEFAULT_MAX_RESOURCE_FILE_SIZE))
               .thenReturn(ApplicationConfiguration.DEFAULT_MAX_RESOURCE_FILE_SIZE);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_MAX_RESOURCE_FILE_SIZE, configuration.getMaxResourceFileSize());
    }

    @Test
    void testGetCronExpressionForOldData() {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_CRON_EXPRESSION_FOR_OLD_DATA))
               .thenReturn(ApplicationConfiguration.DEFAULT_CRON_EXPRESSION_FOR_OLD_DATA);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_CRON_EXPRESSION_FOR_OLD_DATA, configuration.getCronExpressionForOldData());
    }

    @Test
    void testGetMaxTtlForOldDataFromEnvironment() {
        Mockito.when(environment.getLong(ApplicationConfiguration.CFG_MAX_TTL_FOR_OLD_DATA,
                                         ApplicationConfiguration.DEFAULT_MAX_TTL_FOR_OLD_DATA))
               .thenReturn(ApplicationConfiguration.DEFAULT_MAX_TTL_FOR_OLD_DATA);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_MAX_TTL_FOR_OLD_DATA, configuration.getMaxTtlForOldData());
    }

    @Test
    void testShouldUseXSAuditLogging() {
        Mockito.when(environment.getBoolean(ApplicationConfiguration.CFG_USE_XS_AUDIT_LOGGING,
                                            ApplicationConfiguration.DEFAULT_USE_XS_AUDIT_LOGGING))
               .thenReturn(ApplicationConfiguration.DEFAULT_USE_XS_AUDIT_LOGGING);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_USE_XS_AUDIT_LOGGING, configuration.shouldUseXSAuditLogging());
    }

    @Test
    void testGetOrgNameWithValidOrgName() {
        Map<String, Object> vcapApplication = injectFileInEnvironment(VCAP_APPLICATION, ApplicationConfiguration.CFG_VCAP_APPLICATION);
        Assertions.assertEquals(vcapApplication.get("organization_name"), configuration.getOrgName());
    }

    @Test
    void testGetOrgNameNameNotSet() {
        injectFileInEnvironment(VCAP_APPLICATION_WITHOUT_SPACE, ApplicationConfiguration.CFG_VCAP_APPLICATION);
        Assertions.assertNull(configuration.getOrgName());
    }

    @Test
    void testGetDeployServiceUrlUrlIsSet() {
        Map<String, Object> vcapApplication = injectFileInEnvironment(VCAP_APPLICATION, ApplicationConfiguration.CFG_VCAP_APPLICATION);
        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) vcapApplication.get("uris");
        Assertions.assertEquals(urls.get(0), configuration.getDeployServiceUrl());
    }

    @Test
    void testGetDeployServiceUrlUrlIsNotSet() {
        injectFileInEnvironment(VCAP_APPLICATION_WITHOUT_URLS, ApplicationConfiguration.CFG_VCAP_APPLICATION);
        Assertions.assertNull(configuration.getDeployServiceUrl());
    }

    @Test
    void testIsBasicAuthEnabled() {
        Mockito.when(environment.getBoolean(ApplicationConfiguration.CFG_BASIC_AUTH_ENABLED,
                                            ApplicationConfiguration.DEFAULT_BASIC_AUTH_ENABLED))
               .thenReturn(ApplicationConfiguration.DEFAULT_BASIC_AUTH_ENABLED);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_BASIC_AUTH_ENABLED, configuration.isBasicAuthEnabled());
    }

    @Test
    void testGetGlobalAuditorUser() {
        String globalAuditorUser = "globalAuditorUserName";
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_GLOBAL_AUDITOR_USER))
               .thenReturn(globalAuditorUser);
        Assertions.assertEquals(globalAuditorUser, configuration.getGlobalAuditorUser());
    }

    @Test
    void testGetGlobalAuditorPasswordFromEnvironment() {
        String globalAuditorPassword = "globalAuditorUserPassword";
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_GLOBAL_AUDITOR_PASSWORD))
               .thenReturn(globalAuditorPassword);
        Assertions.assertEquals(globalAuditorPassword, configuration.getGlobalAuditorPassword());
    }

    @Test
    void testGetDbConnectionThreadsFromEnvironment() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_DB_CONNECTION_THREADS,
                                                    ApplicationConfiguration.DEFAULT_DB_CONNECTION_THREADS))
               .thenReturn(ApplicationConfiguration.DEFAULT_DB_CONNECTION_THREADS);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_DB_CONNECTION_THREADS, configuration.getDbConnectionThreads());
    }

    @Test
    void testGetStepPollingIntervalInSeconds() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_STEP_POLLING_INTERVAL_IN_SECONDS,
                                                    ApplicationConfiguration.DEFAULT_STEP_POLLING_INTERVAL_IN_SECONDS))
               .thenReturn(ApplicationConfiguration.DEFAULT_STEP_POLLING_INTERVAL_IN_SECONDS);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_STEP_POLLING_INTERVAL_IN_SECONDS,
                                configuration.getStepPollingIntervalInSeconds());
    }

    @Test
    void testShouldSkipSslValidation() {
        Mockito.when(environment.getBoolean(ApplicationConfiguration.CFG_SKIP_SSL_VALIDATION,
                                            ApplicationConfiguration.DEFAULT_SKIP_SSL_VALIDATION))
               .thenReturn(ApplicationConfiguration.DEFAULT_SKIP_SSL_VALIDATION);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_SKIP_SSL_VALIDATION, configuration.shouldSkipSslValidation());
    }

    @Test
    void testGetVersionFromEnvironment() {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VERSION, ApplicationConfiguration.DEFAULT_VERSION))
               .thenReturn(ApplicationConfiguration.DEFAULT_VERSION);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_VERSION, configuration.getVersion());
    }

    @Test
    void testGetChangeLogLockPollRate() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_CHANGE_LOG_LOCK_POLL_RATE,
                                                    ApplicationConfiguration.DEFAULT_CHANGE_LOG_LOCK_POLL_RATE))
               .thenReturn(ApplicationConfiguration.DEFAULT_CHANGE_LOG_LOCK_POLL_RATE);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_CHANGE_LOG_LOCK_POLL_RATE, configuration.getChangeLogLockPollRate());
    }

    @Test
    void testGetControllerClientSslHandshakeTimeout() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_CONTROLLER_CLIENT_SSL_HANDSHAKE_TIMEOUT_IN_SECONDS,
                                                    ApplicationConfiguration.DEFAULT_CONTROLLER_CLIENT_SSL_HANDSHAKE_TIMEOUT_IN_SECONDS))
               .thenReturn(120);
        assertEquals(Duration.ofSeconds(120), configuration.getControllerClientSslHandshakeTimeout());
    }

    @Test
    void testGetControllerClientConnectTimeout() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_CONTROLLER_CLIENT_CONNECT_TIMEOUT_IN_SECONDS,
                                                    ApplicationConfiguration.DEFAULT_CONTROLLER_CLIENT_CONNECT_TIMEOUT_IN_SECONDS))
               .thenReturn(10);
        assertEquals(Duration.ofSeconds(10), configuration.getControllerClientConnectTimeout());
    }

    void testGetChangeLogLockDuration() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_CHANGE_LOG_LOCK_DURATION,
                                                    ApplicationConfiguration.DEFAULT_CHANGE_LOG_LOCK_DURATION))
               .thenReturn(ApplicationConfiguration.DEFAULT_CHANGE_LOG_LOCK_DURATION);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_CHANGE_LOG_LOCK_DURATION, configuration.getChangeLogLockDuration());
    }

    @Test
    void testGetChangeLogLockAttempts() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_CHANGE_LOG_LOCK_ATTEMPTS,
                                                    ApplicationConfiguration.DEFAULT_CHANGE_LOG_LOCK_ATTEMPTS))
               .thenReturn(ApplicationConfiguration.DEFAULT_CHANGE_LOG_LOCK_ATTEMPTS);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_CHANGE_LOG_LOCK_ATTEMPTS, configuration.getChangeLogLockAttempts());
    }

    @Test
    void testGetGlobalConfigSpace() {
        String globalConfigSpace = "globalConfigSpace";
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_GLOBAL_CONFIG_SPACE))
               .thenReturn(globalConfigSpace);
        Assertions.assertEquals(globalConfigSpace, configuration.getGlobalConfigSpace());
    }

    @Test
    void testGetHealthCheckConfigurationFromEnvironment() {
        String healthCheckSpaceId = "healthCheckSpaceId";
        String healthCheckMtaId = "healthCheckMtaId";
        String healthCheckUserName = "healthCheckUserId";
        int healthCheckTimeRange = 10;
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_HEALTH_CHECK_SPACE_GUID))
               .thenReturn(healthCheckSpaceId);
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_HEALTH_CHECK_MTA_ID))
               .thenReturn(healthCheckMtaId);
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_HEALTH_CHECK_USER))
               .thenReturn(healthCheckUserName);
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_HEALTH_CHECK_TIME_RANGE,
                                                    ApplicationConfiguration.DEFAULT_HEALTH_CHECK_TIME_RANGE))
               .thenReturn(healthCheckTimeRange);
        HealthCheckConfiguration healthCheckConfiguration = configuration.getHealthCheckConfiguration();
        Assertions.assertEquals(healthCheckSpaceId, healthCheckConfiguration.getSpaceId());
        Assertions.assertEquals(healthCheckMtaId, healthCheckConfiguration.getMtaId());
        Assertions.assertEquals(healthCheckUserName, healthCheckConfiguration.getUserName());
        Assertions.assertEquals(healthCheckTimeRange, healthCheckConfiguration.getTimeRangeInSeconds());
    }

    @Test
    void testGetApplicationGuid() {
        Map<String, Object> vcapApplication = injectFileInEnvironment(VCAP_APPLICATION, ApplicationConfiguration.CFG_VCAP_APPLICATION);
        Assertions.assertEquals(vcapApplication.get("application_id"), configuration.getApplicationGuid());
    }

    @Test
    void testGetApplicationInstanceIndex() {
        Integer instanceIndex = 1;
        Mockito.when(environment.getInteger(ApplicationConfiguration.CFG_CF_INSTANCE_INDEX))
               .thenReturn(instanceIndex);
        Assertions.assertEquals(instanceIndex, configuration.getApplicationInstanceIndex());
    }

    @Test
    void testGetAuditLogClientCoreThreads() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_AUDIT_LOG_CLIENT_CORE_THREADS,
                                                    ApplicationConfiguration.DEFAULT_AUDIT_LOG_CLIENT_CORE_THREADS))
               .thenReturn(ApplicationConfiguration.DEFAULT_AUDIT_LOG_CLIENT_CORE_THREADS);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_AUDIT_LOG_CLIENT_CORE_THREADS,
                                configuration.getAuditLogClientCoreThreads());
    }

    @Test
    void testGetAuditLogClientMaxThreads() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_AUDIT_LOG_CLIENT_MAX_THREADS,
                                                    ApplicationConfiguration.DEFAULT_AUDIT_LOG_CLIENT_MAX_THREADS))
               .thenReturn(ApplicationConfiguration.DEFAULT_AUDIT_LOG_CLIENT_MAX_THREADS);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_AUDIT_LOG_CLIENT_MAX_THREADS, configuration.getAuditLogClientMaxThreads());
    }

    @Test
    void testGetAuditLogClientQueueCapacity() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_AUDIT_LOG_CLIENT_QUEUE_CAPACITY,
                                                    ApplicationConfiguration.DEFAULT_AUDIT_LOG_CLIENT_QUEUE_CAPACITY))
               .thenReturn(ApplicationConfiguration.DEFAULT_AUDIT_LOG_CLIENT_QUEUE_CAPACITY);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_AUDIT_LOG_CLIENT_QUEUE_CAPACITY,
                                configuration.getAuditLogClientQueueCapacity());
    }

    @Test
    void testGetAuditLogClientKeepAlive() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_AUDIT_LOG_CLIENT_KEEP_ALIVE,
                                                    ApplicationConfiguration.DEFAULT_AUDIT_LOG_CLIENT_KEEP_ALIVE))
               .thenReturn(ApplicationConfiguration.DEFAULT_AUDIT_LOG_CLIENT_KEEP_ALIVE);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_AUDIT_LOG_CLIENT_KEEP_ALIVE, configuration.getAuditLogClientKeepAlive());
    }

    @Test
    void testGetFlowableJobExecutorCoreThreads() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_FLOWABLE_JOB_EXECUTOR_CORE_THREADS,
                                                    ApplicationConfiguration.DEFAULT_FLOWABLE_JOB_EXECUTOR_CORE_THREADS))
               .thenReturn(ApplicationConfiguration.DEFAULT_FLOWABLE_JOB_EXECUTOR_CORE_THREADS);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_FLOWABLE_JOB_EXECUTOR_CORE_THREADS,
                                configuration.getFlowableJobExecutorCoreThreads());
    }

    @Test
    void testGetFlowableJobExecutorMaxThreads() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_FLOWABLE_JOB_EXECUTOR_MAX_THREADS,
                                                    ApplicationConfiguration.DEFAULT_FLOWABLE_JOB_EXECUTOR_MAX_THREADS))
               .thenReturn(ApplicationConfiguration.DEFAULT_FLOWABLE_JOB_EXECUTOR_MAX_THREADS);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_FLOWABLE_JOB_EXECUTOR_MAX_THREADS,
                                configuration.getFlowableJobExecutorMaxThreads());
    }

    @Test
    void testGetFlowableJobExecutorQueueCapacity() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY,
                                                    ApplicationConfiguration.DEFAULT_FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY))
               .thenReturn(ApplicationConfiguration.DEFAULT_FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY,
                                configuration.getFlowableJobExecutorQueueCapacity());
    }

    @Test
    void testGetFssCacheUpdateTimeoutMinutes() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_FSS_CACHE_UPDATE_TIMEOUT_MINUTES,
                                                    ApplicationConfiguration.DEFAULT_FSS_CACHE_UPDATE_TIMEOUT_MINUTES))
               .thenReturn(ApplicationConfiguration.DEFAULT_FSS_CACHE_UPDATE_TIMEOUT_MINUTES);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_FSS_CACHE_UPDATE_TIMEOUT_MINUTES,
                                configuration.getFssCacheUpdateTimeoutMinutes());
    }

    @Test
    void testGetSpaceDeveloperCacheExpirationInSeconds() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS,
                                                    ApplicationConfiguration.DEFAULT_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS))
               .thenReturn(ApplicationConfiguration.DEFAULT_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS,
                                configuration.getSpaceDeveloperCacheExpirationInSeconds());
    }

    @Test
    void testGetControllerClientConnectionPoolSize() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_CONTROLLER_CLIENT_CONNECTION_POOL_SIZE,
                                                    ApplicationConfiguration.DEFAULT_CONTROLLER_CLIENT_CONNECTION_POOL_SIZE))
               .thenReturn(ApplicationConfiguration.DEFAULT_CONTROLLER_CLIENT_CONNECTION_POOL_SIZE);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_CONTROLLER_CLIENT_CONNECTION_POOL_SIZE,
                                configuration.getControllerClientConnectionPoolSize());
    }

    @Test
    void testGetControllerClientThreadPoolSize() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_CONTROLLER_CLIENT_THREAD_POOL_SIZE,
                                                    ApplicationConfiguration.DEFAULT_CONTROLLER_CLIENT_THREAD_POOL_SIZE))
               .thenReturn(ApplicationConfiguration.DEFAULT_CONTROLLER_CLIENT_THREAD_POOL_SIZE);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_CONTROLLER_CLIENT_THREAD_POOL_SIZE,
                                configuration.getControllerClientThreadPoolSize());
    }

    @Test
    void testGetSnakeyamlMaxAliasesForCollections() {
        Mockito.when(environment.getPositiveInteger(ApplicationConfiguration.CFG_SNAKEYAML_MAX_ALIASES_FOR_COLLECTIONS,
                                                    ApplicationConfiguration.DEFAULT_SNAKEYAML_MAX_ALIASES_FOR_COLLECTIONS))
               .thenReturn(ApplicationConfiguration.DEFAULT_SNAKEYAML_MAX_ALIASES_FOR_COLLECTIONS);
        Assertions.assertEquals(ApplicationConfiguration.DEFAULT_SNAKEYAML_MAX_ALIASES_FOR_COLLECTIONS,
                                configuration.getSnakeyamlMaxAliasesForCollections());
    }

    @Test
    void testIsInternalEnvironment() {
        Mockito.when(environment.getBoolean(ApplicationConfiguration.SAP_INTERNAL_DELIVERY,
                                            ApplicationConfiguration.DEFAULT_SAP_INTERNAL_DELIVERY))
               .thenReturn(true);
        Assertions.assertTrue(configuration.isInternalEnvironment());
    }

    @Test
    void testGetCloudComponentsInvalidJson() {
        Mockito.when(environment.getString(ApplicationConfiguration.SUPPORT_COMPONENTS))
               .thenReturn("Invalid json");
        Map<String, Object> cloudComponents = configuration.getCloudComponents();
        Assertions.assertEquals(0, cloudComponents.size());
    }

    @Test
    void testGetCloudComponentsValidJson() {
        Map<String, Object> cloudComponentsMap = injectFileInEnvironment(CLOUD_COMPONENTS, ApplicationConfiguration.SUPPORT_COMPONENTS);
        Map<String, Object> cloudComponents = configuration.getCloudComponents();
        Assertions.assertTrue(Maps.difference(cloudComponentsMap, cloudComponents)
                                  .areEqual());
    }

    @Test
    void testGetInternalSupportChannel() {
        String internalSupportChannel = "internal-support-channel";
        Mockito.when(environment.getString(ApplicationConfiguration.INTERNAL_SUPPORT_CHANNEL))
               .thenReturn(internalSupportChannel);
        Assertions.assertEquals(internalSupportChannel, configuration.getInternalSupportChannel());
    }

    @Test
    void testGetCertificateCN() {
        String certificateCN = "cert-cn";
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_CERTIFICATE_CN))
               .thenReturn(certificateCN);
        Assertions.assertEquals(certificateCN, configuration.getCertificateCN());
    }

    @Test
    void testGetFilteredEnv() {
        Map<String, String> filteredEnvironment = new HashMap<>();
        filteredEnvironment.put(ApplicationConfiguration.CFG_MAX_MTA_DESCRIPTOR_SIZE, "1024");
        Mockito.when(environment.getAllVariables())
               .thenReturn(filteredEnvironment);
        Map<String, String> filteredEnv = configuration.getNotSensitiveVariables();
        Assertions.assertTrue(filteredEnv.containsKey(ApplicationConfiguration.CFG_MAX_MTA_DESCRIPTOR_SIZE));
    }

    @Test
    void testLoad() {
        Map<String, Object> vcapApplication = injectFileInEnvironment(VCAP_APPLICATION, ApplicationConfiguration.CFG_VCAP_APPLICATION);
        configuration.load();
        Assertions.assertEquals(vcapApplication.get("cf_api"), configuration.getControllerUrl()
                                                                            .toString());
    }

    private Map<String, Object> injectFileInEnvironment(String filename, String envVariable) {
        String vcapApplicationJson = TestUtil.getResourceAsString(filename, getClass());
        Mockito.when(environment.getString(envVariable))
               .thenReturn(vcapApplicationJson);
        return JsonUtil.convertJsonToMap(vcapApplicationJson);
    }

}
