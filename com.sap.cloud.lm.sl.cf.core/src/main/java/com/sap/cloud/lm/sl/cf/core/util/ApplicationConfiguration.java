package com.sap.cloud.lm.sl.cf.core.util;

import static java.text.MessageFormat.format;

import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.configuration.Environment;
import com.sap.cloud.lm.sl.cf.core.health.model.HealthCheckConfiguration;
import com.sap.cloud.lm.sl.cf.core.health.model.ImmutableHealthCheckConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.util.Configuration;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MiscUtil;
import com.sap.cloud.lm.sl.mta.handlers.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.Platform;

@Named
@Lazy(false)
public class ApplicationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    // Environment variables:
    static final String CFG_TYPE = "XS_TYPE";
    static final String CFG_DB_TYPE = "DB_TYPE";
    static final String CFG_PLATFORM = "PLATFORM"; // Mandatory
    static final String CFG_MAX_UPLOAD_SIZE = "MAX_UPLOAD_SIZE";
    static final String CFG_MAX_MTA_DESCRIPTOR_SIZE = "MAX_MTA_DESCRIPTOR_SIZE";
    static final String CFG_MAX_MANIFEST_SIZE = "DEFAULT_MAX_MANIFEST_SIZE";
    static final String CFG_MAX_RESOURCE_FILE_SIZE = "DEFAULT_MAX_RESOURCE_FILE_SIZE";
    static final String CFG_CRON_EXPRESSION_FOR_OLD_DATA = "CRON_EXPRESSION_FOR_OLD_DATA";
    static final String CFG_MAX_TTL_FOR_OLD_DATA = "MAX_TTL_FOR_OLD_DATA";
    static final String CFG_USE_XS_AUDIT_LOGGING = "USE_XS_AUDIT_LOGGING";
    static final String CFG_VCAP_APPLICATION = "VCAP_APPLICATION"; // Mandatory
    static final String CFG_BASIC_AUTH_ENABLED = "BASIC_AUTH_ENABLED";
    static final String CFG_GLOBAL_AUDITOR_USER = "GLOBAL_AUDITOR_USER";
    static final String CFG_GLOBAL_AUDITOR_PASSWORD = "GLOBAL_AUDITOR_PASSWORD";
    static final String CFG_DB_CONNECTION_THREADS = "DB_CONNECTION_THREADS";
    static final String CFG_STEP_POLLING_INTERVAL_IN_SECONDS = "STEP_POLLING_INTERVAL_IN_SECONDS";
    static final String CFG_SKIP_SSL_VALIDATION = "SKIP_SSL_VALIDATION";
    static final String CFG_VERSION = "VERSION";
    static final String CFG_CHANGE_LOG_LOCK_POLL_RATE = "CHANGE_LOG_LOCK_POLL_RATE";
    static final String CFG_CHANGE_LOG_LOCK_DURATION = "CHANGE_LOG_LOCK_DURATION";
    static final String CFG_CHANGE_LOG_LOCK_ATTEMPTS = "CHANGE_LOG_LOCK_ATTEMPTS";
    static final String CFG_GLOBAL_CONFIG_SPACE = "GLOBAL_CONFIG_SPACE";
    static final String CFG_HEALTH_CHECK_SPACE_GUID = "HEALTH_CHECK_SPACE_ID";
    static final String CFG_HEALTH_CHECK_MTA_ID = "HEALTH_CHECK_MTA_ID";
    static final String CFG_HEALTH_CHECK_USER = "HEALTH_CHECK_USER";
    static final String CFG_HEALTH_CHECK_TIME_RANGE = "HEALTH_CHECK_TIME_RANGE";
    static final String CFG_AUDIT_LOG_CLIENT_CORE_THREADS = "AUDIT_LOG_CLIENT_CORE_THREADS";
    static final String CFG_AUDIT_LOG_CLIENT_MAX_THREADS = "AUDIT_LOG_CLIENT_MAX_THREADS";
    static final String CFG_AUDIT_LOG_CLIENT_QUEUE_CAPACITY = "AUDIT_LOG_CLIENT_QUEUE_CAPACITY";
    static final String CFG_AUDIT_LOG_CLIENT_KEEP_ALIVE = "AUDIT_LOG_CLIENT_KEEP_ALIVE";
    static final String CFG_FLOWABLE_JOB_EXECUTOR_CORE_THREADS = "FLOWABLE_JOB_EXECUTOR_CORE_THREADS";
    static final String CFG_FLOWABLE_JOB_EXECUTOR_MAX_THREADS = "FLOWABLE_JOB_EXECUTOR_MAX_THREADS";
    static final String CFG_FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY = "FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY";
    static final String CFG_FSS_CACHE_UPDATE_TIMEOUT_MINUTES = "FSS_CACHE_UPDATE_TIMEOUT_MINUTES";
    static final String CFG_THREAD_MONITOR_CACHE_UPDATE_IN_SECONDS = "THREAD_MONITOR_CACHE_UPDATE_IN_SECONDS";
    static final String CFG_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS = "SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS";
    static final String CFG_CONTROLLER_CLIENT_SSL_HANDSHAKE_TIMEOUT_IN_SECONDS = "CONTROLLER_CLIENT_SSL_HANDSHAKE_TIMEOUT_IN_SECONDS";
    static final String CFG_CONTROLLER_CLIENT_CONNECT_TIMEOUT_IN_SECONDS = "CONTROLLER_CLIENT_CONNECT_TIMEOUT_IN_SECONDS";
    static final String CFG_CONTROLLER_CLIENT_CONNECTION_POOL_SIZE = "CONTROLLER_CLIENT_CONNECTION_POOL_SIZE";
    static final String CFG_CONTROLLER_CLIENT_THREAD_POOL_SIZE = "CONTROLLER_CLIENT_THREAD_POOL_SIZE";
    static final String SAP_INTERNAL_DELIVERY = "SAP_INTERNAL_DELIVERY";
    static final String SUPPORT_COMPONENTS = "SUPPORT_COMPONENTS";
    static final String INTERNAL_SUPPORT_CHANNEL = "INTERNAL_SUPPORT_CHANNEL";
    static final String CFG_CF_INSTANCE_INDEX = "CF_INSTANCE_INDEX";
    static final String CFG_CERTIFICATE_CN = "CERTIFICATE_CN";
    static final String CFG_MICROMETER_STEP_IN_SECONDS = "MICROMETER_STEP_IN_SECONDS";
    static final String CFG_MICROMETER_BATCH_SIZE = "MICROMETER_BATCH_SIZE";

    private static final List<String> VCAP_APPLICATION_URIS_KEYS = Arrays.asList("full_application_uris", "application_uris", "uris");

    // Default values:
    public static final List<Platform> DEFAULT_PLATFORMS = Collections.emptyList();
    public static final List<Target> DEFAULT_TARGETS = Collections.emptyList();
    public static final long DEFAULT_MAX_UPLOAD_SIZE = 4 * 1024 * 1024 * 1024L; // 4 GB(s)
    public static final long DEFAULT_MAX_MTA_DESCRIPTOR_SIZE = 1024 * 1024L; // 1 MB(s)
    public static final long DEFAULT_MAX_MANIFEST_SIZE = 1024 * 1024L; // 1MB
    public static final long DEFAULT_MAX_RESOURCE_FILE_SIZE = 1024 * 1024 * 1024L; // 1GB

    public static final Boolean DEFAULT_USE_XS_AUDIT_LOGGING = true;
    public static final String DEFAULT_SPACE_GUID = "";
    public static final Boolean DEFAULT_BASIC_AUTH_ENABLED = false;
    public static final Integer DEFAULT_DB_CONNECTION_THREADS = 30;
    public static final String DEFAULT_CRON_EXPRESSION_FOR_OLD_DATA = "0 0 0/6 * * ?"; // every 6 hours
    public static final long DEFAULT_MAX_TTL_FOR_OLD_DATA = TimeUnit.DAYS.toSeconds(5); // 5 days
    public static final Integer DEFAULT_STEP_POLLING_INTERVAL_IN_SECONDS = 5;
    public static final Boolean DEFAULT_SKIP_SSL_VALIDATION = false;
    public static final String DEFAULT_VERSION = "N/A";
    public static final Integer DEFAULT_CHANGE_LOG_LOCK_POLL_RATE = 1; // 1 minute(s)
    public static final Integer DEFAULT_CHANGE_LOG_LOCK_DURATION = 1; // 1 minute(s)
    public static final Integer DEFAULT_CHANGE_LOG_LOCK_ATTEMPTS = 5; // 5 minute(s)
    public static final Integer DEFAULT_HEALTH_CHECK_TIME_RANGE = (int) TimeUnit.MINUTES.toSeconds(5);
    public static final Integer DEFAULT_AUDIT_LOG_CLIENT_CORE_THREADS = 2;
    public static final Integer DEFAULT_AUDIT_LOG_CLIENT_MAX_THREADS = 8;
    public static final Integer DEFAULT_AUDIT_LOG_CLIENT_QUEUE_CAPACITY = 8;
    public static final Integer DEFAULT_AUDIT_LOG_CLIENT_KEEP_ALIVE = 60;
    public static final Integer DEFAULT_FLOWABLE_JOB_EXECUTOR_CORE_THREADS = 8;
    public static final Integer DEFAULT_FLOWABLE_JOB_EXECUTOR_MAX_THREADS = 32;
    public static final Integer DEFAULT_FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY = 16;
    public static final Integer DEFAULT_FSS_CACHE_UPDATE_TIMEOUT_MINUTES = 30;
    public static final Integer DEFAULT_THREAD_MONITOR_CACHE_UPDATE_IN_SECONDS = 1;
    public static final Integer DEFAULT_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS = 20;
    public static final int DEFAULT_CONTROLLER_CLIENT_SSL_HANDSHAKE_TIMEOUT_IN_SECONDS = 30;
    // We've experimented with much smaller values (5, 15 seconds), but these lead to connection timeouts.
    public static final int DEFAULT_CONTROLLER_CLIENT_CONNECT_TIMEOUT_IN_SECONDS = (int) TimeUnit.MINUTES.toSeconds(2);
    public static final int DEFAULT_CONTROLLER_CLIENT_CONNECTION_POOL_SIZE = 192;
    public static final int DEFAULT_CONTROLLER_CLIENT_THREAD_POOL_SIZE = 64;
    public static final Boolean DEFAULT_SAP_INTERNAL_DELIVERY = false;
    protected final Environment environment;

    // Cached configuration settings:
    private Map<String, Object> vcapApplication;
    private URL controllerUrl;
    private Long maxUploadSize;
    private Long maxMtaDescriptorSize;
    private Long maxManifestSize;
    private Long maxResourceFileSize;
    private String cronExpressionForOldData;
    private Long maxTtlForOldData;
    private Boolean useXSAuditLogging;
    private String spaceGuid;
    private String orgName;
    private Boolean basicAuthEnabled;
    private String globalAuditorUser;
    private String globalAuditorPassword;
    private Integer dbConnectionThreads;
    private Integer stepPollingIntervalInSeconds;
    private Boolean skipSslValidation;
    private String version;
    private String deployServiceUrl;
    private Integer changeLogLockPollRate;
    private Integer changeLogLockDuration;
    private Integer changeLogLockAttempts;
    private String globalConfigSpace;
    private HealthCheckConfiguration healthCheckConfiguration;
    private String applicationGuid;
    private Integer applicationInstanceIndex;
    private Integer auditLogClientCoreThreads;
    private Integer auditLogClientMaxThreads;
    private Integer auditLogClientQueueCapacity;
    private Integer auditLogClientKeepAlive;
    private Integer flowableJobExecutorCoreThreads;
    private Integer flowableJobExecutorMaxThreads;
    private Integer flowableJobExecutorQueueCapacity;
    private Integer fssCacheUpdateTimeoutMinutes;
    private Integer threadMonitorCacheUpdateInSeconds;
    private Integer spaceDeveloperCacheTimeInSeconds;
    private Platform platform;
    private Duration controllerClientSslHandshakeTimeout;
    private Duration controllerClientConnectTimeout;
    private Integer controllerClientConnectionPoolSize;
    private Integer controllerClientThreadPoolSize;
    private String certificateCN;
    private Integer micrometerStepInSeconds;
    private Integer micrometerBatchSize;

    public ApplicationConfiguration() {
        this(new Environment());
    }

    @Inject
    public ApplicationConfiguration(Environment environment) {
        this.environment = environment;
    }

    public void load() {
        getControllerUrl();
        getMaxUploadSize();
        getMaxMtaDescriptorSize();
        getMaxManifestSize();
        getMaxResourceFileSize();
        shouldUseXSAuditLogging();
        getSpaceGuid();
        getOrgName();
        getDeployServiceUrl();
        isBasicAuthEnabled();
        getGlobalAuditorUser();
        getGlobalAuditorPassword();
        getDbConnectionThreads();
        getStepPollingIntervalInSeconds();
        shouldSkipSslValidation();
        getVersion();
        getChangeLogLockPollRate();
        getChangeLogLockDuration();
        getChangeLogLockAttempts();
        getGlobalConfigSpace();
        getHealthCheckConfiguration();
        getApplicationGuid();
        getApplicationInstanceIndex();
        getAuditLogClientCoreThreads();
        getAuditLogClientMaxThreads();
        getAuditLogClientQueueCapacity();
        getAuditLogClientKeepAlive();
        getFssCacheUpdateTimeoutMinutes();
    }

    protected AuditLoggingFacade getAuditLoggingFacade() {
        return AuditLoggingProvider.getFacade();
    }

    public Map<String, String> getNotSensitiveVariables() {
        Set<String> notSensitiveConfigVariables = getNotSensitiveConfigVariables();
        Map<String, String> env = environment.getAllVariables();
        return env.entrySet()
                  .stream()
                  .filter(envVariable -> notSensitiveConfigVariables.contains(envVariable.getKey()))
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> getNotSensitiveConfigVariables() {
        return new HashSet<>(Arrays.asList(CFG_TYPE, CFG_DB_TYPE, CFG_PLATFORM, CFG_MAX_UPLOAD_SIZE, CFG_MAX_MTA_DESCRIPTOR_SIZE,
                                           CFG_MAX_MANIFEST_SIZE, CFG_MAX_RESOURCE_FILE_SIZE, CFG_USE_XS_AUDIT_LOGGING,
                                           CFG_BASIC_AUTH_ENABLED, CFG_GLOBAL_AUDITOR_USER, CFG_STEP_POLLING_INTERVAL_IN_SECONDS,
                                           CFG_SKIP_SSL_VALIDATION, CFG_VERSION, CFG_CHANGE_LOG_LOCK_POLL_RATE,
                                           CFG_CHANGE_LOG_LOCK_DURATION, CFG_CHANGE_LOG_LOCK_ATTEMPTS, CFG_GLOBAL_CONFIG_SPACE,
                                           CFG_AUDIT_LOG_CLIENT_CORE_THREADS, CFG_AUDIT_LOG_CLIENT_MAX_THREADS,
                                           CFG_AUDIT_LOG_CLIENT_QUEUE_CAPACITY, CFG_FLOWABLE_JOB_EXECUTOR_CORE_THREADS,
                                           CFG_FLOWABLE_JOB_EXECUTOR_MAX_THREADS, CFG_FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY,
                                           CFG_AUDIT_LOG_CLIENT_KEEP_ALIVE, CFG_CONTROLLER_CLIENT_CONNECTION_POOL_SIZE,
                                           CFG_CONTROLLER_CLIENT_THREAD_POOL_SIZE));
    }

    public Configuration getFileConfiguration() {
        return new Configuration(getMaxUploadSize());
    }

    public URL getControllerUrl() {
        if (controllerUrl == null) {
            controllerUrl = getControllerUrlFromEnvironment();
        }
        return controllerUrl;
    }

    public Platform getPlatform() {
        if (platform == null) {
            platform = getPlatformFromEnvironment();
        }
        return platform;
    }

    public Long getMaxUploadSize() {
        if (maxUploadSize == null) {
            maxUploadSize = getMaxUploadSizeFromEnvironment();
        }
        return maxUploadSize;
    }

    public Long getMaxMtaDescriptorSize() {
        if (maxMtaDescriptorSize == null) {
            maxMtaDescriptorSize = getMaxMtaDescriptorSizeFromEnvironment();
        }
        return maxMtaDescriptorSize;
    }

    public Long getMaxManifestSize() {
        if (maxManifestSize == null) {
            maxManifestSize = getMaxManifestSizeFromEnvironment();
        }
        return maxManifestSize;
    }

    public Long getMaxResourceFileSize() {
        if (maxResourceFileSize == null) {
            maxResourceFileSize = getMaxResourceFileSizeFromEnvironment();
        }
        return maxResourceFileSize;
    }

    public String getCronExpressionForOldData() {
        if (cronExpressionForOldData == null) {
            cronExpressionForOldData = getCronExpressionForOldDataFromEnvironment();
        }
        return cronExpressionForOldData;
    }

    public Long getMaxTtlForOldData() {
        if (maxTtlForOldData == null) {
            maxTtlForOldData = getMaxTtlForOldDataFromEnvironment();
        }
        return maxTtlForOldData;
    }

    public Boolean shouldUseXSAuditLogging() {
        if (useXSAuditLogging == null) {
            useXSAuditLogging = shouldUseXSAuditLoggingFromEnvironment();
        }
        return useXSAuditLogging;
    }

    public String getSpaceGuid() {
        if (spaceGuid == null) {
            spaceGuid = getSpaceGuidFromEnvironment();
        }
        return spaceGuid;
    }

    public String getOrgName() {
        if (orgName == null) {
            orgName = getOrgNameFromEnvironment();
        }
        return orgName;
    }

    public String getDeployServiceUrl() {
        if (deployServiceUrl == null) {
            deployServiceUrl = getDeployServiceUrlFromEnvironment();
        }
        return deployServiceUrl;
    }

    public Boolean isBasicAuthEnabled() {
        if (basicAuthEnabled == null) {
            basicAuthEnabled = isBasicAuthEnabledThroughEnvironment();
        }
        return basicAuthEnabled;
    }

    public String getGlobalAuditorUser() {
        if (globalAuditorUser == null) {
            globalAuditorUser = getGlobalAuditorUserFromEnvironment();
        }
        return globalAuditorUser;
    }

    public String getGlobalAuditorPassword() {
        if (globalAuditorPassword == null) {
            globalAuditorPassword = getGlobalAuditorPasswordFromEnvironment();
        }
        return globalAuditorPassword;
    }

    public int getDbConnectionThreads() {
        if (dbConnectionThreads == null) {
            dbConnectionThreads = getDbConnectionThreadsFromEnvironment();
        }
        return dbConnectionThreads;
    }

    public int getStepPollingIntervalInSeconds() {
        if (stepPollingIntervalInSeconds == null) {
            stepPollingIntervalInSeconds = getStepPollingIntervalFromEnvironment();
        }
        return stepPollingIntervalInSeconds;
    }

    public Boolean shouldSkipSslValidation() {
        if (skipSslValidation == null) {
            skipSslValidation = shouldSkipSslValidationBasedOnEnvironment();
        }
        return skipSslValidation;
    }

    public String getVersion() {
        if (version == null) {
            version = getVersionFromEnvironment();
        }
        return version;
    }

    public Integer getChangeLogLockPollRate() {
        if (changeLogLockPollRate == null) {
            changeLogLockPollRate = getChangeLogLockPollRateFromEnvironment();
        }
        return changeLogLockPollRate;
    }

    public Integer getChangeLogLockDuration() {
        if (changeLogLockDuration == null) {
            changeLogLockDuration = getChangeLogLockDurationFromEnvironment();
        }
        return changeLogLockDuration;
    }

    public Integer getChangeLogLockAttempts() {
        if (changeLogLockAttempts == null) {
            changeLogLockAttempts = getChangeLogLockAttemptsFromEnvironment();
        }
        return changeLogLockAttempts;
    }

    public String getGlobalConfigSpace() {
        if (globalConfigSpace == null) {
            globalConfigSpace = getGlobalConfigSpaceFromEnvironment();
        }
        return globalConfigSpace;
    }

    public HealthCheckConfiguration getHealthCheckConfiguration() {
        if (healthCheckConfiguration == null) {
            healthCheckConfiguration = getHealthCheckConfigurationFromEnvironment();
        }
        return healthCheckConfiguration;
    }

    public String getApplicationGuid() {
        if (applicationGuid == null) {
            applicationGuid = getApplicationGuidFromEnvironment();
        }
        return applicationGuid;
    }

    public Integer getApplicationInstanceIndex() {
        if (applicationInstanceIndex == null) {
            applicationInstanceIndex = getApplicationInstanceIndexFromEnvironment();
        }
        return applicationInstanceIndex;
    }

    public Integer getAuditLogClientCoreThreads() {
        if (auditLogClientCoreThreads == null) {
            auditLogClientCoreThreads = getAuditLogClientCoreThreadsFromEnvironment();
        }
        return auditLogClientCoreThreads;
    }

    public Integer getAuditLogClientMaxThreads() {
        if (auditLogClientMaxThreads == null) {
            auditLogClientMaxThreads = getAuditLogClientMaxThreadsFromEnvironment();
        }
        return auditLogClientMaxThreads;
    }

    public Integer getAuditLogClientQueueCapacity() {
        if (auditLogClientQueueCapacity == null) {
            auditLogClientQueueCapacity = getAuditLogClientQueueCapacityFromEnvironment();
        }
        return auditLogClientQueueCapacity;
    }

    public Integer getAuditLogClientKeepAlive() {
        if (auditLogClientKeepAlive == null) {
            auditLogClientKeepAlive = getAuditLogClientKeepAliveFromEnvironment();
        }
        return auditLogClientKeepAlive;
    }

    public Integer getFlowableJobExecutorCoreThreads() {
        if (flowableJobExecutorCoreThreads == null) {
            flowableJobExecutorCoreThreads = getFlowableJobExecutorCoreThreadsFromEnvironment();
        }
        return flowableJobExecutorCoreThreads;
    }

    public Integer getFlowableJobExecutorMaxThreads() {
        if (flowableJobExecutorMaxThreads == null) {
            flowableJobExecutorMaxThreads = getFlowableJobExecutorMaxThreadsFromEnvironment();
        }
        return flowableJobExecutorMaxThreads;
    }

    public Integer getFlowableJobExecutorQueueCapacity() {
        if (flowableJobExecutorQueueCapacity == null) {
            flowableJobExecutorQueueCapacity = getFlowableJobExecutorQueueCapacityFromEnvironment();
        }
        return flowableJobExecutorQueueCapacity;
    }

    public Integer getFssCacheUpdateTimeoutMinutes() {
        if (fssCacheUpdateTimeoutMinutes == null) {
            fssCacheUpdateTimeoutMinutes = getFssCacheUpdateTimeoutMinutesFromEnvironment();
        }
        return fssCacheUpdateTimeoutMinutes;
    }

    public Integer getThreadMonitorCacheUpdateInSeconds() {
        if (threadMonitorCacheUpdateInSeconds == null) {
            threadMonitorCacheUpdateInSeconds = getThreadMonitorCacheUpdateInSecondsFromEnvironment();
        }
        return threadMonitorCacheUpdateInSeconds;
    }

    public Integer getSpaceDeveloperCacheExpirationInSeconds() {
        if (spaceDeveloperCacheTimeInSeconds == null) {
            spaceDeveloperCacheTimeInSeconds = getSpaceDeveloperCacheTimeInSecondsFromEnvironment();
        }
        return spaceDeveloperCacheTimeInSeconds;
    }

    public Duration getControllerClientSslHandshakeTimeout() {
        if (controllerClientSslHandshakeTimeout == null) {
            controllerClientSslHandshakeTimeout = getControllerClientSslHandshakeTimeoutFromEnvironment();
        }
        return controllerClientSslHandshakeTimeout;
    }

    public Duration getControllerClientConnectTimeout() {
        if (controllerClientConnectTimeout == null) {
            controllerClientConnectTimeout = getControllerClientConnectTimeoutFromEnvironment();
        }
        return controllerClientConnectTimeout;
    }

    public Integer getControllerClientConnectionPoolSize() {
        if (controllerClientConnectionPoolSize == null) {
            controllerClientConnectionPoolSize = getControllerClientConnectionPoolSizeFromEnvironment();
        }
        return controllerClientConnectionPoolSize;
    }

    public Integer getControllerClientThreadPoolSize() {
        if (controllerClientThreadPoolSize == null) {
            controllerClientThreadPoolSize = getControllerClientThreadPoolSizeFromEnvironment();
        }
        return controllerClientThreadPoolSize;
    }

    public String getCertificateCN() {
        if (certificateCN == null) {
            certificateCN = getCertificateCNFromEnvironment();
        }
        return certificateCN;
    }

    public Integer getMicrometerStepInSeconds() {
        if (micrometerStepInSeconds == null) {
            micrometerStepInSeconds = getMicrometerStepInSecondsFromEnvironment();
        }
        return micrometerStepInSeconds;
    }

    public Integer getMicrometerBatchSize() {
        if (micrometerBatchSize == null) {
            micrometerBatchSize = getMicrometerBatchSizeFromEnvironment();
        }
        return micrometerBatchSize;
    }

    private URL getControllerUrlFromEnvironment() {
        Map<String, Object> vcapApplicationMap = getVcapApplication();
        String controllerUrlString = getControllerUrl(vcapApplicationMap);
        try {
            URL parsedControllerUrl = MiscUtil.getURL(controllerUrlString);
            LOGGER.info(format(Messages.CONTROLLER_URL, parsedControllerUrl));
            return parsedControllerUrl;
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalArgumentException(format(Messages.INVALID_CONTROLLER_URL, controllerUrlString), e);
        }
    }

    private String getControllerUrl(Map<String, Object> vcapApplication) {
        String api = (String) vcapApplication.get("cf_api");
        if (api != null) {
            return api;
        }
        throw new IllegalArgumentException(Messages.CONTROLLER_URL_NOT_SPECIFIED);
    }

    private Long getMaxUploadSizeFromEnvironment() {
        Long value = environment.getLong(CFG_MAX_UPLOAD_SIZE, DEFAULT_MAX_UPLOAD_SIZE);
        LOGGER.info(format(Messages.MAX_UPLOAD_SIZE, value));
        return value;
    }

    private Long getMaxMtaDescriptorSizeFromEnvironment() {
        Long value = environment.getLong(CFG_MAX_MTA_DESCRIPTOR_SIZE, DEFAULT_MAX_MTA_DESCRIPTOR_SIZE);
        LOGGER.info(format(Messages.MAX_MTA_DESCRIPTOR_SIZE, value));
        return value;
    }

    private Platform getPlatformFromEnvironment() {
        String platformJson = environment.getString(CFG_PLATFORM);
        if (platformJson == null) {
            throw new IllegalStateException(Messages.PLATFORMS_NOT_SPECIFIED);
        }
        Platform parsedPlatform = new ConfigurationParser().parsePlatformJson(platformJson);
        LOGGER.debug(format(Messages.PLATFORM, JsonUtil.toJson(parsedPlatform, true)));
        return parsedPlatform;
    }

    private Long getMaxManifestSizeFromEnvironment() {
        Long value = environment.getLong(CFG_MAX_MANIFEST_SIZE, DEFAULT_MAX_MANIFEST_SIZE);
        LOGGER.info(format(Messages.MAX_MANIFEST_SIZE, value));
        return value;
    }

    private Long getMaxResourceFileSizeFromEnvironment() {
        Long value = environment.getLong(CFG_MAX_RESOURCE_FILE_SIZE, DEFAULT_MAX_RESOURCE_FILE_SIZE);
        LOGGER.info(format(Messages.MAX_RESOURCE_FILE_SIZE, value));
        return value;
    }

    private String getCronExpressionForOldDataFromEnvironment() {
        String value = getCronExpression(CFG_CRON_EXPRESSION_FOR_OLD_DATA, DEFAULT_CRON_EXPRESSION_FOR_OLD_DATA);
        LOGGER.info(format(Messages.CRON_EXPRESSION_FOR_OLD_DATA, value));
        return value;
    }

    private Long getMaxTtlForOldDataFromEnvironment() {
        Long value = environment.getLong(CFG_MAX_TTL_FOR_OLD_DATA, DEFAULT_MAX_TTL_FOR_OLD_DATA);
        LOGGER.info(format(Messages.MAX_TTL_FOR_OLD_DATA, value));
        return value;
    }

    private Boolean shouldUseXSAuditLoggingFromEnvironment() {
        Boolean value = environment.getBoolean(CFG_USE_XS_AUDIT_LOGGING, DEFAULT_USE_XS_AUDIT_LOGGING);
        LOGGER.info(format(Messages.USE_XS_AUDIT_LOGGING, value));
        return value;
    }

    private String getSpaceGuidFromEnvironment() {
        Map<String, Object> vcapApplicationMap = getVcapApplication();
        Object spaceGuidValue = vcapApplicationMap.get("space_id");
        if (spaceGuidValue != null) {
            LOGGER.info(format(Messages.SPACE_GUID, spaceGuidValue));
            return spaceGuidValue.toString();
        }
        LOGGER.warn(format(Messages.SPACE_GUID_NOT_SPECIFIED_USING_DEFAULT_0, DEFAULT_SPACE_GUID));
        return DEFAULT_SPACE_GUID;
    }

    private String getOrgNameFromEnvironment() {
        Map<String, Object> vcapApplicationMap = getVcapApplication();
        Object orgNameValue = vcapApplicationMap.get("organization_name");
        if (orgNameValue != null) {
            LOGGER.info(format(Messages.ORG_NAME, orgNameValue));
            return orgNameValue.toString();
        }
        LOGGER.debug(Messages.ORG_NAME_NOT_SPECIFIED);
        return null;
    }

    private String getDeployServiceUrlFromEnvironment() {
        Map<String, Object> vcapApplicationMap = getVcapApplication();
        List<String> uris = getApplicationUris(vcapApplicationMap);
        if (!CollectionUtils.isEmpty(uris)) {
            return uris.get(0);
        }
        LOGGER.warn(Messages.DEPLOY_SERVICE_URL_NOT_SPECIFIED);
        return null;
    }

    private Map<String, Object> getVcapApplication() {
        if (vcapApplication == null) {
            vcapApplication = getVcapApplicationFromEnvironment();
        }
        return vcapApplication;
    }

    private Map<String, Object> getVcapApplicationFromEnvironment() {
        String vcapApplicationFromEnvironment = environment.getString(CFG_VCAP_APPLICATION);
        try {
            return JsonUtil.convertJsonToMap(vcapApplicationFromEnvironment);
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_VCAP_APPLICATION, vcapApplicationFromEnvironment), e);
            return Collections.emptyMap();
        }
    }

    private List<String> getApplicationUris(Map<String, Object> vcapApplication) {
        for (String urisKey : VCAP_APPLICATION_URIS_KEYS) {
            List<String> uris = MiscUtil.cast(vcapApplication.get(urisKey));
            if (!CollectionUtils.isEmpty(uris)) {
                return uris;
            }
        }
        return Collections.emptyList();
    }

    private Boolean isBasicAuthEnabledThroughEnvironment() {
        Boolean value = environment.getBoolean(CFG_BASIC_AUTH_ENABLED, DEFAULT_BASIC_AUTH_ENABLED);
        LOGGER.info(format(Messages.BASIC_AUTH_ENABLED, value));
        return value;
    }

    private String getGlobalAuditorUserFromEnvironment() {
        String value = environment.getString(CFG_GLOBAL_AUDITOR_USER);
        LOGGER.info(format(Messages.GLOBAL_AUDITOR_USERNAME, value));
        return value;
    }

    private String getGlobalAuditorPasswordFromEnvironment() {
        return environment.getString(CFG_GLOBAL_AUDITOR_PASSWORD);
    }

    private Integer getDbConnectionThreadsFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_DB_CONNECTION_THREADS, DEFAULT_DB_CONNECTION_THREADS);
        LOGGER.info(format(Messages.DB_CONNECTION_THREADS, value));
        return value;
    }

    private int getStepPollingIntervalFromEnvironment() {
        int value = environment.getPositiveInteger(CFG_STEP_POLLING_INTERVAL_IN_SECONDS, DEFAULT_STEP_POLLING_INTERVAL_IN_SECONDS);
        LOGGER.info(format(Messages.STEP_POLLING_INTERVAL_IN_SECONDS, value));
        return value;
    }

    private Boolean shouldSkipSslValidationBasedOnEnvironment() {
        Boolean value = environment.getBoolean(CFG_SKIP_SSL_VALIDATION, DEFAULT_SKIP_SSL_VALIDATION);
        LOGGER.info(format(Messages.SKIP_SSL_VALIDATION, value));
        return value;
    }

    private String getVersionFromEnvironment() {
        String value = environment.getString(CFG_VERSION, DEFAULT_VERSION);
        LOGGER.info(format(Messages.DS_VERSION, value));
        return value;
    }

    private Integer getChangeLogLockPollRateFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_CHANGE_LOG_LOCK_POLL_RATE, DEFAULT_CHANGE_LOG_LOCK_POLL_RATE);
        LOGGER.info(format(Messages.CHANGE_LOG_LOCK_POLL_RATE, value));
        return value;
    }

    private Integer getChangeLogLockDurationFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_CHANGE_LOG_LOCK_DURATION, DEFAULT_CHANGE_LOG_LOCK_DURATION);
        LOGGER.info(format(Messages.CHANGE_LOG_LOCK_DURATION, value));
        return value;
    }

    private Integer getChangeLogLockAttemptsFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_CHANGE_LOG_LOCK_ATTEMPTS, DEFAULT_CHANGE_LOG_LOCK_ATTEMPTS);
        LOGGER.info(format(Messages.CHANGE_LOG_LOCK_ATTEMPTS, value));
        return value;
    }

    private String getGlobalConfigSpaceFromEnvironment() {
        String value = environment.getString(CFG_GLOBAL_CONFIG_SPACE);
        LOGGER.debug(format(Messages.GLOBAL_CONFIG_SPACE, value));
        return value;
    }

    private HealthCheckConfiguration getHealthCheckConfigurationFromEnvironment() {
        HealthCheckConfiguration healthCheckConfigurationFromEnvironment = ImmutableHealthCheckConfiguration.builder()
                                                                                                            .spaceId(getHealthCheckSpaceGuidFromEnvironment())
                                                                                                            .mtaId(getHealthCheckMtaIdFromEnvironment())
                                                                                                            .userName(getHealthCheckUserFromEnvironment())
                                                                                                            .timeRangeInSeconds(getHealthCheckTimeRangeFromEnvironment())
                                                                                                            .build();
        LOGGER.info(format(Messages.HEALTH_CHECK_CONFIGURATION, JsonUtil.toJson(healthCheckConfigurationFromEnvironment, true)));
        return healthCheckConfigurationFromEnvironment;
    }

    private String getHealthCheckSpaceGuidFromEnvironment() {
        return environment.getString(CFG_HEALTH_CHECK_SPACE_GUID);
    }

    private String getHealthCheckMtaIdFromEnvironment() {
        return environment.getString(CFG_HEALTH_CHECK_MTA_ID);
    }

    private String getHealthCheckUserFromEnvironment() {
        return environment.getString(CFG_HEALTH_CHECK_USER);
    }

    private Integer getHealthCheckTimeRangeFromEnvironment() {
        return environment.getPositiveInteger(CFG_HEALTH_CHECK_TIME_RANGE, DEFAULT_HEALTH_CHECK_TIME_RANGE);
    }

    private String getApplicationGuidFromEnvironment() {
        Map<String, Object> vcapApplicationMap = getVcapApplication();
        String applicationId = (String) vcapApplicationMap.get("application_id");
        LOGGER.info(format(Messages.APPLICATION_GUID, applicationId));
        return applicationId;
    }

    private Integer getApplicationInstanceIndexFromEnvironment() {
        Integer applicationInstanceIndexFromEnvironment = environment.getInteger(CFG_CF_INSTANCE_INDEX);
        LOGGER.info(format(Messages.APPLICATION_INSTANCE_INDEX, applicationInstanceIndexFromEnvironment));
        return applicationInstanceIndexFromEnvironment;
    }

    private Integer getAuditLogClientCoreThreadsFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_AUDIT_LOG_CLIENT_CORE_THREADS, DEFAULT_AUDIT_LOG_CLIENT_CORE_THREADS);
        LOGGER.info(format(Messages.AUDIT_LOG_CLIENT_CORE_THREADS, value));
        return value;
    }

    private Integer getAuditLogClientMaxThreadsFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_AUDIT_LOG_CLIENT_MAX_THREADS, DEFAULT_AUDIT_LOG_CLIENT_MAX_THREADS);
        LOGGER.info(format(Messages.AUDIT_LOG_CLIENT_MAX_THREADS, value));
        return value;
    }

    private Integer getAuditLogClientQueueCapacityFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_AUDIT_LOG_CLIENT_QUEUE_CAPACITY, DEFAULT_AUDIT_LOG_CLIENT_QUEUE_CAPACITY);
        LOGGER.info(format(Messages.AUDIT_LOG_CLIENT_QUEUE_CAPACITY, value));
        return value;
    }

    private Integer getAuditLogClientKeepAliveFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_AUDIT_LOG_CLIENT_KEEP_ALIVE, DEFAULT_AUDIT_LOG_CLIENT_KEEP_ALIVE);
        LOGGER.info(format(Messages.AUDIT_LOG_CLIENT_KEEP_ALIVE, value));
        return value;
    }

    private Integer getFlowableJobExecutorCoreThreadsFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_FLOWABLE_JOB_EXECUTOR_CORE_THREADS, DEFAULT_FLOWABLE_JOB_EXECUTOR_CORE_THREADS);
        LOGGER.info(format(Messages.FLOWABLE_JOB_EXECUTOR_CORE_THREADS, value));
        return value;
    }

    private Integer getFlowableJobExecutorMaxThreadsFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_FLOWABLE_JOB_EXECUTOR_MAX_THREADS, DEFAULT_FLOWABLE_JOB_EXECUTOR_MAX_THREADS);
        LOGGER.info(format(Messages.FLOWABLE_JOB_EXECUTOR_MAX_THREADS, value));
        return value;
    }

    private Integer getFlowableJobExecutorQueueCapacityFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY,
                                                       DEFAULT_FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY);
        LOGGER.info(format(Messages.FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY, value));
        return value;
    }

    private String getCronExpression(String name, String defaultValue) {
        String value = environment.getString(name);
        if (value != null && org.quartz.CronExpression.isValidExpression(value)) {
            return value;
        }
        LOGGER.info(format(Messages.ENVIRONMENT_VARIABLE_IS_NOT_SET_USING_DEFAULT, name, defaultValue));
        return defaultValue;
    }

    private Integer getFssCacheUpdateTimeoutMinutesFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_FSS_CACHE_UPDATE_TIMEOUT_MINUTES, DEFAULT_FSS_CACHE_UPDATE_TIMEOUT_MINUTES);
        LOGGER.info(format(Messages.FSS_CACHE_UPDATE_TIMEOUT, value));
        return value;
    }

    private Integer getThreadMonitorCacheUpdateInSecondsFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_THREAD_MONITOR_CACHE_UPDATE_IN_SECONDS,
                                                       DEFAULT_THREAD_MONITOR_CACHE_UPDATE_IN_SECONDS);
        LOGGER.info(format(Messages.THREAD_MONITOR_CACHE_TIMEOUT, value));
        return value;
    }

    private Integer getSpaceDeveloperCacheTimeInSecondsFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS,
                                                       DEFAULT_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS);
        LOGGER.info(format(Messages.SPACE_DEVELOPERS_CACHE_TIME_IN_SECONDS, value));
        return value;
    }

    private Duration getControllerClientSslHandshakeTimeoutFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_CONTROLLER_CLIENT_SSL_HANDSHAKE_TIMEOUT_IN_SECONDS,
                                                       DEFAULT_CONTROLLER_CLIENT_SSL_HANDSHAKE_TIMEOUT_IN_SECONDS);
        LOGGER.info(format(Messages.CONTROLLER_CLIENT_SSL_HANDSHAKE_TIMEOUT_IN_SECONDS, value));
        return Duration.ofSeconds(value);
    }

    private Duration getControllerClientConnectTimeoutFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_CONTROLLER_CLIENT_CONNECT_TIMEOUT_IN_SECONDS,
                                                       DEFAULT_CONTROLLER_CLIENT_CONNECT_TIMEOUT_IN_SECONDS);
        LOGGER.info(format(Messages.CONTROLLER_CLIENT_CONNECT_TIMEOUT_IN_SECONDS, value));
        return Duration.ofSeconds(value);
    }

    private Integer getControllerClientConnectionPoolSizeFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_CONTROLLER_CLIENT_CONNECTION_POOL_SIZE,
                                                       DEFAULT_CONTROLLER_CLIENT_CONNECTION_POOL_SIZE);
        LOGGER.info(format(Messages.CONTROLLER_CLIENT_CONNECTION_POOL_SIZE, value));
        return value;
    }

    private Integer getControllerClientThreadPoolSizeFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_CONTROLLER_CLIENT_THREAD_POOL_SIZE, DEFAULT_CONTROLLER_CLIENT_THREAD_POOL_SIZE);
        LOGGER.info(format(Messages.CONTROLLER_CLIENT_THREAD_POOL_SIZE, value));
        return value;
    }

    private Integer getMicrometerStepInSecondsFromEnvironment() {
        Integer micrometerStepInSeconds = environment.getInteger(CFG_MICROMETER_STEP_IN_SECONDS, null);
        LOGGER.info(format(Messages.MICROMETER_STEP_IN_SECONDS, micrometerStepInSeconds));
        return micrometerStepInSeconds;
    }

    private Integer getMicrometerBatchSizeFromEnvironment() {
        Integer micrometerBatchSize = environment.getInteger(CFG_MICROMETER_BATCH_SIZE, null);
        LOGGER.info(format(Messages.MICROMETER_BATCH_SIZE, micrometerBatchSize));
        return micrometerBatchSize;
    }

    public Boolean isInternalEnvironment() {
        return environment.getBoolean(SAP_INTERNAL_DELIVERY, DEFAULT_SAP_INTERNAL_DELIVERY);
    }

    public Map<String, Object> getCloudComponents() {
        String value = environment.getString(SUPPORT_COMPONENTS);
        try {
            return JsonUtil.convertJsonToMap(value);
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_SUPPORT_COMPONENTS, value), e);
            return Collections.emptyMap();
        }
    }

    public String getInternalSupportChannel() {
        return environment.getString(INTERNAL_SUPPORT_CHANNEL);
    }

    private String getCertificateCNFromEnvironment() {
        String value = environment.getString(CFG_CERTIFICATE_CN);
        LOGGER.info(format(Messages.CERTIFICATE_CN, value));
        return value;
    }

}