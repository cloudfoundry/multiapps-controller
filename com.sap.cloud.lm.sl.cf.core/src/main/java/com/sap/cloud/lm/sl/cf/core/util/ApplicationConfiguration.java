package com.sap.cloud.lm.sl.cf.core.util;

import static java.text.MessageFormat.format;

import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.configuration.Environment;
import com.sap.cloud.lm.sl.cf.core.health.model.HealthCheckConfiguration;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.persistence.util.Configuration;
import com.sap.cloud.lm.sl.cf.persistence.util.DefaultConfiguration;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MiscUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.handlers.v2.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;

@Component
@Lazy(false)
public class ApplicationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    public enum DatabaseType {
        DEFAULTDB, HANA, POSTGRESQL
    }

    // Environment variables
    static final String CFG_TYPE = "XS_TYPE";
    static final String CFG_DB_TYPE = "DB_TYPE";
    static final String CFG_PLATFORM = "PLATFORM"; // Mandatory
    static final String CFG_PLATFORM_V2 = "PLATFORM_V2"; // Mandatory
    static final String CFG_PLATFORM_V3 = "PLATFORM_V3"; // Mandatory
    static final String CFG_MAX_UPLOAD_SIZE = "MAX_UPLOAD_SIZE";
    static final String CFG_MAX_MTA_DESCRIPTOR_SIZE = "MAX_MTA_DESCRIPTOR_SIZE";
    static final String CFG_MAX_MANIFEST_SIZE = "DEFAULT_MAX_MANIFEST_SIZE";
    static final String CFG_MAX_RESOURCE_FILE_SIZE = "DEFAULT_MAX_RESOURCE_FILE_SIZE";
    static final String CFG_CRON_EXPRESSION_FOR_OLD_DATA = "CRON_EXPRESSION_FOR_OLD_DATA";
    static final String CFG_MAX_TTL_FOR_OLD_DATA = "MAX_TTL_FOR_OLD_DATA";
    static final String CFG_SCAN_UPLOADS = "SCAN_UPLOADS";
    static final String CFG_USE_XS_AUDIT_LOGGING = "USE_XS_AUDIT_LOGGING";
    static final String CFG_VCAP_APPLICATION = "VCAP_APPLICATION"; // Mandatory
    static final String CFG_DUMMY_TOKENS_ENABLED = "DUMMY_TOKENS_ENABLED";
    static final String CFG_BASIC_AUTH_ENABLED = "BASIC_AUTH_ENABLED";
    static final String CFG_GLOBAL_AUDITOR_USER = "GLOBAL_AUDITOR_USER";
    static final String CFG_GLOBAL_AUDITOR_PASSWORD = "GLOBAL_AUDITOR_PASSWORD";
    static final String CFG_DB_CONNECTION_THREADS = "DB_CONNECTION_THREADS";
    static final String CFG_CONTROLLER_POLLING_INTERVAL = "CONTROLLER_POLLING_INTERVAL";
    static final String CFG_SKIP_SSL_VALIDATION = "SKIP_SSL_VALIDATION";
    static final String CFG_XS_PLACEHOLDERS_SUPPORTED = "XS_PLACEHOLDER_SUPPORT_TEST";
    static final String CFG_VERSION = "VERSION";
    static final String CFG_CHANGE_LOG_LOCK_POLL_RATE = "CHANGE_LOG_LOCK_POLL_RATE";
    static final String CFG_CHANGE_LOG_LOCK_DURATION = "CHANGE_LOG_LOCK_DURATION";
    static final String CFG_CHANGE_LOG_LOCK_ATTEMPTS = "CHANGE_LOG_LOCK_ATTEMPTS";
    static final String CFG_GLOBAL_CONFIG_SPACE = "GLOBAL_CONFIG_SPACE";
    static final String CFG_GATHER_USAGE_STATISTICS = "GATHER_USAGE_STATISTICS";
    static final String CFG_HEALTH_CHECK_SPACE_ID = "HEALTH_CHECK_SPACE_ID";
    static final String CFG_HEALTH_CHECK_MTA_ID = "HEALTH_CHECK_MTA_ID";
    static final String CFG_HEALTH_CHECK_USER = "HEALTH_CHECK_USER";
    static final String CFG_HEALTH_CHECK_TIME_RANGE = "HEALTH_CHECK_TIME_RANGE";
    static final String CFG_MAIL_API_URL = "MAIL_API_URL";
    static final String CFG_AUDIT_LOG_CLIENT_CORE_THREADS = "AUDIT_LOG_CLIENT_CORE_THREADS";
    static final String CFG_AUDIT_LOG_CLIENT_MAX_THREADS = "AUDIT_LOG_CLIENT_MAX_THREADS";
    static final String CFG_AUDIT_LOG_CLIENT_QUEUE_CAPACITY = "AUDIT_LOG_CLIENT_QUEUE_CAPACITY";
    static final String CFG_AUDIT_LOG_CLIENT_KEEP_ALIVE = "AUDIT_LOG_CLIENT_KEEP_ALIVE";
    static final String CFG_FLOWABLE_JOB_EXECUTOR_CORE_THREADS = "FLOWABLE_JOB_EXECUTOR_CORE_THREADS";
    static final String CFG_FLOWABLE_JOB_EXECUTOR_MAX_THREADS = "FLOWABLE_JOB_EXECUTOR_MAX_THREADS";
    static final String CFG_FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY = "FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY";
    static final String CFG_FSS_CACHE_UPDATE_TIMEOUT_MINUTES = "FSS_CACHE_UPDATE_TIMEOUT_MINUTES";
    static final String CFG_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS = "SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS";

    private static final List<String> VCAP_APPLICATION_URIS_KEYS = Arrays.asList("full_application_uris", "application_uris", "uris");

    // Default values
    public static final PlatformType DEFAULT_TYPE = PlatformType.XS2;
    public static final DatabaseType DEFAULT_DB_TYPE = DatabaseType.DEFAULTDB;
    public static final List<Platform> DEFAULT_PLATFORMS = Collections.emptyList();
    public static final List<Target> DEFAULT_TARGETS = Collections.emptyList();
    public static final long DEFAULT_MAX_UPLOAD_SIZE = 4 * 1024 * 1024 * 1024l; // 4 GB(s)
    public static final long DEFAULT_MAX_MTA_DESCRIPTOR_SIZE = 1024 * 1024l; // 1 MB(s)
    public static final long DEFAULT_MAX_MANIFEST_SIZE = 1024 * 1024l; // 1MB
    public static final long DEFAULT_MAX_RESOURCE_FILE_SIZE = 1024 * 1024 * 1024l; // 1GB
    public static final Boolean DEFAULT_SCAN_UPLOADS = false;
    public static final Boolean DEFAULT_USE_XS_AUDIT_LOGGING = true;
    public static final String DEFAULT_SPACE_ID = "";
    public static final int DEFAULT_HTTP_ROUTER_PORT = 80;
    public static final int DEFAULT_HTTPS_ROUTER_PORT = 443;
    public static final Boolean DEFAULT_DUMMY_TOKENS_ENABLED = false;
    public static final Boolean DEFAULT_BASIC_AUTH_ENABLED = false;
    public static final String DEFAULT_GLOBAL_AUDITOR_USER = "";
    public static final String DEFAULT_GLOBAL_AUDITOR_PASSWORD = "";
    public static final Integer DEFAULT_DB_CONNECTION_THREADS = 30;
    public static final String DEFAULT_CRON_EXPRESSION_FOR_OLD_DATA = "0 0 0/6 * * ?"; // every 6 hours
    public static final long DEFAULT_MAX_TTL_FOR_OLD_DATA = TimeUnit.DAYS.toSeconds(5); // 5 days
    /**
     * The minimum duration for an Activiti timer is 5 seconds, because when the job manager schedules a new timer, it checks whether that
     * timer should fire in the next 5 seconds. If so, it hints the job executor that it should execute that timer ASAP. However, there is
     * some delay between the time when Activiti creates the timer object and when it checks whether its due date is in the next 5 seconds.
     * This is why we set the controller polling interval to 6 seconds.
     * 
     * TODO Revert this back to 3 seconds when we adopt version 5.16.0 of Activiti, where the behaviour described above is fixed.
     * 
     * @see org.activiti.engine.impl.persistence.entity.JobEntityManager#schedule()
     */
    public static final Integer DEFAULT_CONTROLLER_POLLING_INTERVAL = 6; // 6 second(s)
    public static final Boolean DEFAULT_SKIP_SSL_VALIDATION = false;
    public static final String DEFAULT_VERSION = "N/A";
    public static final Integer DEFAULT_CHANGE_LOG_LOCK_POLL_RATE = 1; // 1 minute(s)
    public static final Integer DEFAULT_CHANGE_LOG_LOCK_DURATION = 1; // 1 minute(s)
    public static final Integer DEFAULT_CHANGE_LOG_LOCK_ATTEMPTS = 5; // 5 minute(s)
    public static final Boolean DEFAULT_GATHER_USAGE_STATISTICS = false;
    public static final Integer DEFAULT_HEALTH_CHECK_TIME_RANGE = (int) TimeUnit.MINUTES.toSeconds(5);
    public static final Integer DEFAULT_AUDIT_LOG_CLIENT_CORE_THREADS = 2;
    public static final Integer DEFAULT_AUDIT_LOG_CLIENT_MAX_THREADS = 8;
    public static final Integer DEFAULT_AUDIT_LOG_CLIENT_QUEUE_CAPACITY = 8;
    public static final Integer DEFAULT_AUDIT_LOG_CLIENT_KEEP_ALIVE = 60;
    public static final Integer DEFAULT_FLOWABLE_JOB_EXECUTOR_CORE_THREADS = 8;
    public static final Integer DEFAULT_FLOWABLE_JOB_EXECUTOR_MAX_THREADS = 32;
    public static final Integer DEFAULT_FLOWABLE_JOB_EXECUTOR_QUEUE_CAPACITY = 16;
    public static final Integer DEFAULT_FSS_CACHE_UPDATE_TIMEOUT_MINUTES = 30;
    public static final Integer DEFAULT_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS = 20;

    // Type names
    private static final Map<String, PlatformType> TYPE_NAMES = createTypeNames();

    private static Map<String, PlatformType> createTypeNames() {
        Map<String, PlatformType> result = new HashMap<>();
        result.put("XSA", PlatformType.XS2);
        return Collections.unmodifiableMap(result);
    }

    private final Environment environment;

    // Cached configuration settings:
    private Map<String, Object> vcapApplication;
    private PlatformType platformType;
    private URL controllerUrl;
    private DatabaseType databaseType;
    private Long maxUploadSize;
    private Long maxMtaDescriptorSize;
    private Long maxManifestSize;
    private Long maxResourceFileSize;
    private String cronExpressionForOldData;
    private Long maxTtlForOldData;
    private Boolean scanUploads;
    private Boolean useXSAuditLogging;
    private String spaceId;
    private String orgName;
    private Integer routerPort;
    private Boolean dummyTokensEnabled;
    private Boolean basicAuthEnabled;
    private String globalAuditorUser;
    private String globalAuditorPassword;
    private Integer dbConnectionThreads;
    private Integer controllerPollingInterval;
    private Boolean skipSslValidation;
    private Boolean xsPlaceholdersSupported;
    private String version;
    private String deployServiceUrl;
    private Integer changeLogLockPollRate;
    private Integer changeLogLockDuration;
    private Integer changeLogLockAttempts;
    private String globalConfigSpace;
    private Boolean gatherUsageStatistics;
    private HealthCheckConfiguration healthCheckConfiguration;
    private String mailApiUrl;
    private String applicationId;
    private Integer applicationInstanceIndex;
    private Integer auditLogClientCoreThreads;
    private Integer auditLogClientMaxThreads;
    private Integer auditLogClientQueueCapacity;
    private Integer auditLogClientKeepAlive;
    private Integer flowableJobExecutorCoreThreads;
    private Integer flowableJobExecutorMaxThreads;
    private Integer flowableJobExecutorQueueCapacity;
    private Integer fssCacheUpdateTimeoutMinutes;
    private Integer spaceDeveloperCacheTimeInSeconds;

    public ApplicationConfiguration() {
        this(new Environment());
    }

    @Inject
    public ApplicationConfiguration(Environment environment) {
        this.environment = environment;
    }

    public void load() {
        getPlatformType();
        getControllerUrl();
        getDatabaseType();
        getMaxUploadSize();
        getMaxMtaDescriptorSize();
        getMaxManifestSize();
        getMaxResourceFileSize();
        shouldScanUploads();
        shouldUseXSAuditLogging();
        getSpaceId();
        getOrgName();
        getRouterPort();
        getDeployServiceUrl();
        areDummyTokensEnabled();
        isBasicAuthEnabled();
        getGlobalAuditorUser();
        getGlobalAuditorPassword();
        getDbConnectionThreads();
        getControllerPollingInterval();
        shouldSkipSslValidation();
        areXsPlaceholdersSupported();
        getVersion();
        getChangeLogLockPollRate();
        getChangeLogLockDuration();
        getChangeLogLockAttempts();
        getGlobalConfigSpace();
        shouldGatherUsageStatistics();
        getHealthCheckConfiguration();
        getMailApiUrl();
        getApplicationId();
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

    public Map<String, String> getFilteredEnv() {
        Set<String> notSensitiveConfigVariables = getNotSensitiveConfigVariables();
        Map<String, String> env = environment.getAllVariables();
        return env.entrySet()
            .stream()
            .filter(envVariable -> notSensitiveConfigVariables.contains(envVariable.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> getNotSensitiveConfigVariables() {
        return new HashSet<>(Arrays.asList(CFG_TYPE, CFG_DB_TYPE, CFG_PLATFORM, CFG_PLATFORM_V2, CFG_PLATFORM_V3, CFG_MAX_UPLOAD_SIZE,
            CFG_MAX_MTA_DESCRIPTOR_SIZE, CFG_MAX_MANIFEST_SIZE, CFG_MAX_RESOURCE_FILE_SIZE, CFG_SCAN_UPLOADS, CFG_USE_XS_AUDIT_LOGGING,
            CFG_DUMMY_TOKENS_ENABLED, CFG_BASIC_AUTH_ENABLED, CFG_GLOBAL_AUDITOR_USER, CFG_CONTROLLER_POLLING_INTERVAL,
            CFG_SKIP_SSL_VALIDATION, CFG_XS_PLACEHOLDERS_SUPPORTED, CFG_VERSION, CFG_CHANGE_LOG_LOCK_POLL_RATE,
            CFG_CHANGE_LOG_LOCK_DURATION, CFG_CHANGE_LOG_LOCK_ATTEMPTS, CFG_GLOBAL_CONFIG_SPACE, CFG_GATHER_USAGE_STATISTICS,
            CFG_MAIL_API_URL, CFG_AUDIT_LOG_CLIENT_CORE_THREADS, CFG_AUDIT_LOG_CLIENT_MAX_THREADS, CFG_AUDIT_LOG_CLIENT_QUEUE_CAPACITY,
            CFG_AUDIT_LOG_CLIENT_KEEP_ALIVE));
    }

    public Configuration getFileConfiguration() {
        return new DefaultConfiguration(getMaxUploadSize(), shouldScanUploads());
    }

    public PlatformType getPlatformType() {
        if (platformType == null) {
            platformType = getPlatformTypeFromEnvironment();
        }
        return platformType;
    }

    public URL getControllerUrl() {
        if (controllerUrl == null) {
            controllerUrl = getControllerUrlFromEnvironment();
        }
        return controllerUrl;
    }

    public DatabaseType getDatabaseType() {
        if (databaseType == null) {
            databaseType = getDatabaseTypeFromEnvironment();
        }
        return databaseType;
    }

    public Platform getPlatform() {
        return getPlatform(null, 1);
    }

    public Platform getPlatform(ConfigurationParser parser, int majorVersion) {
        switch (majorVersion) {
            case 1:
                return parsePlatform(environment.getString(CFG_PLATFORM));
            case 2:
                return parsePlatform(environment.getString(CFG_PLATFORM_V2), parser);
            case 3:
                return parsePlatform(environment.getString(CFG_PLATFORM_V3), parser);
            default:
                throw new UnsupportedOperationException();
        }
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
            maxManifestSize = getMaxManifestSizeFromEnviroment();
        }
        return maxManifestSize;
    }

    public Long getMaxResourceFileSize() {
        if (maxResourceFileSize == null) {
            maxResourceFileSize = getMaxResourceFileSizeFromEnviroment();
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

    public Boolean shouldScanUploads() {
        if (scanUploads == null) {
            scanUploads = shouldScanUploadsFromEnvironment();
        }
        return scanUploads;
    }

    public Boolean shouldUseXSAuditLogging() {
        if (useXSAuditLogging == null) {
            useXSAuditLogging = shouldUseXSAuditLoggingFromEnvironment();
        }
        return useXSAuditLogging;
    }

    public String getSpaceId() {
        if (spaceId == null) {
            spaceId = getSpaceIdFromEnvironment();
        }
        return spaceId;
    }

    public String getOrgName() {
        if (orgName == null) {
            orgName = getOrgNameFromEnvironment();
        }
        return orgName;
    }

    public int getRouterPort() {
        if (routerPort == null) {
            routerPort = getRouterPortFromEnvironment();
        }
        return routerPort;
    }

    public String getDeployServiceUrl() {
        if (deployServiceUrl == null) {
            deployServiceUrl = getDeployServiceUrlFromEnvironment();
        }
        return deployServiceUrl;
    }

    public Boolean areDummyTokensEnabled() {
        if (dummyTokensEnabled == null) {
            dummyTokensEnabled = areDummyTokensEnabledThroughEnvironment();
        }
        return dummyTokensEnabled;
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

    public int getControllerPollingInterval() {
        if (controllerPollingInterval == null) {
            controllerPollingInterval = getControllerPollingIntervalFromEnvironment();
        }
        return controllerPollingInterval;
    }

    public Boolean shouldSkipSslValidation() {
        if (skipSslValidation == null) {
            skipSslValidation = shouldSkipSslValidationBasedOnEnvironment();
        }
        return skipSslValidation;
    }

    public Boolean areXsPlaceholdersSupported() {
        if (xsPlaceholdersSupported == null) {
            xsPlaceholdersSupported = areXsPlaceholdersSupportedBasedOnEnvironment();
        }
        return xsPlaceholdersSupported;
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

    public Boolean shouldGatherUsageStatistics() {
        if (gatherUsageStatistics == null) {
            gatherUsageStatistics = shouldGatherUsageStatisticsBasedOnEnvironment();
        }
        return gatherUsageStatistics;
    }

    public HealthCheckConfiguration getHealthCheckConfiguration() {
        if (healthCheckConfiguration == null) {
            healthCheckConfiguration = getHealthCheckConfigurationFromEnvironment();
        }
        return healthCheckConfiguration;
    }

    public String getMailApiUrl() {
        if (mailApiUrl == null) {
            mailApiUrl = getMailApiUrlFromEnvironment();
        }
        return mailApiUrl;
    }

    public String getApplicationId() {
        if (applicationId == null) {
            applicationId = getApplicationIdFromEnvironment();
        }
        return applicationId;
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

    public Integer getSpaceDeveloperCacheExpirationInSeconds() {
        if (spaceDeveloperCacheTimeInSeconds == null) {
            spaceDeveloperCacheTimeInSeconds = getSpaceDeveloperCacheTimeInSecondsFromEnvironment();
        }
        return spaceDeveloperCacheTimeInSeconds;
    }

    private PlatformType getPlatformTypeFromEnvironment() {
        String type = environment.getString(CFG_TYPE);
        try {
            if (type != null) {
                PlatformType result = TYPE_NAMES.containsKey(type) ? TYPE_NAMES.get(type) : PlatformType.valueOf(type);
                LOGGER.info(format(Messages.XS_TYPE, result));
                return result;
            }
            LOGGER.info(format(Messages.XS_TYPE_NOT_SPECIFIED, DEFAULT_TYPE));
        } catch (IllegalArgumentException e) {
            LOGGER.warn(format(Messages.UNKNOWN_XS_TYPE, type, DEFAULT_TYPE), e);
        }
        return DEFAULT_TYPE;
    }

    private URL getControllerUrlFromEnvironment() {
        Map<String, Object> vcapApplication = getVcapApplication();
        String controllerUrl = getControllerUrl(vcapApplication);
        try {
            URL parsedControllerUrl = MiscUtil.getURL(controllerUrl);
            LOGGER.info(format(Messages.CONTROLLER_URL, parsedControllerUrl));
            return parsedControllerUrl;
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalArgumentException(format(Messages.INVALID_CONTROLLER_URL, controllerUrl), e);
        }
    }

    private String getControllerUrl(Map<String, Object> vcapApplication) {
        String cfApi = (String) vcapApplication.get("cf_api");
        if (cfApi != null) {
            return cfApi;
        }
        String xsApi = (String) vcapApplication.get("xs_api");
        if (xsApi != null) {
            return xsApi;
        }
        throw new IllegalArgumentException(Messages.CONTROLLER_URL_NOT_SPECIFIED);
    }

    private DatabaseType getDatabaseTypeFromEnvironment() {
        String type = environment.getString(CFG_DB_TYPE);
        try {
            if (type != null) {
                DatabaseType result = DatabaseType.valueOf(type);
                LOGGER.info(format(Messages.DB_TYPE, result));
                return result;
            }
            LOGGER.info(format(Messages.DB_TYPE_NOT_SPECIFIED, DEFAULT_DB_TYPE));
        } catch (IllegalArgumentException e) {
            LOGGER.warn(format(Messages.UNKNOWN_DB_TYPE, type, DEFAULT_DB_TYPE), e);
        }
        return DEFAULT_DB_TYPE;
    }

    private Platform parsePlatform(String json) {
        return parsePlatform(json, new ConfigurationParser());
    }

    private Platform parsePlatform(String json, ConfigurationParser parser) {
        if (json == null) {
            throw new IllegalStateException(Messages.PLATFORMS_NOT_SPECIFIED);
        }
        Platform result = parser.parsePlatformJson(json);
        LOGGER.info(format(Messages.PLATFORMS, JsonUtil.toJson(result, true)));
        return result;
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

    private Long getMaxManifestSizeFromEnviroment() {
        Long value = environment.getLong(CFG_MAX_MANIFEST_SIZE, DEFAULT_MAX_MANIFEST_SIZE);
        LOGGER.info(format(Messages.MAX_MANIFEST_SIZE, value));
        return value;
    }

    private Long getMaxResourceFileSizeFromEnviroment() {
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

    private Boolean shouldScanUploadsFromEnvironment() {
        Boolean value = environment.getBoolean(CFG_SCAN_UPLOADS, DEFAULT_SCAN_UPLOADS);
        LOGGER.info(format(Messages.SCAN_UPLOADS, value));
        return value;
    }

    private Boolean shouldUseXSAuditLoggingFromEnvironment() {
        Boolean value = environment.getBoolean(CFG_USE_XS_AUDIT_LOGGING, DEFAULT_USE_XS_AUDIT_LOGGING);
        LOGGER.info(format(Messages.USE_XS_AUDIT_LOGGING, value));
        return value;
    }

    private String getSpaceIdFromEnvironment() {
        Map<String, Object> vcapApplication = getVcapApplication();
        Object spaceId = vcapApplication.get("space_id");
        if (spaceId != null) {
            LOGGER.info(format(Messages.SPACE_ID, spaceId));
            return spaceId.toString();
        }
        LOGGER.warn(format(Messages.SPACE_ID_NOT_SPECIFIED_USING_DEFAULT_0, DEFAULT_SPACE_ID));
        return DEFAULT_SPACE_ID;
    }

    private String getOrgNameFromEnvironment() {
        Map<String, Object> vcapApplication = getVcapApplication();
        Object orgName = vcapApplication.get("organization_name");
        if (orgName != null) {
            LOGGER.info(format(Messages.ORG_NAME, orgName));
            return orgName.toString();
        }
        LOGGER.warn(Messages.ORG_NAME_NOT_SPECIFIED);
        return null;
    }

    private Integer getRouterPortFromEnvironment() {
        int defaultRouterPort = computeDefaultRouterPort();
        Map<String, Object> vcapApplication = getVcapApplication();
        List<String> uris = getApplicationUris(vcapApplication);
        if (CollectionUtils.isEmpty(uris)) {
            LOGGER.info(format(Messages.NO_APPLICATION_URIS_SPECIFIED, defaultRouterPort));
            return defaultRouterPort;
        }
        int routerPort = extractRouterPort(uris, defaultRouterPort);
        LOGGER.info(format(Messages.ROUTER_PORT, routerPort));
        return routerPort;
    }

    private Integer extractRouterPort(List<String> uris, int defaultRouterPort) {
        Pair<String, String> portAndDomain = UriUtil.getHostAndDomain(uris.get(0));
        try {
            int port = Integer.parseInt(portAndDomain._1);
            if (UriUtil.isValidPort(port)) {
                return port;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn(format(Messages.COULD_NOT_PARSE_ROUTER_PORT_0_USING_DEFAULT_1, portAndDomain._1, defaultRouterPort), e);
        }
        return defaultRouterPort;
    }

    private int computeDefaultRouterPort() {
        return getControllerUrl().getProtocol()
            .equals("http") ? DEFAULT_HTTP_ROUTER_PORT : DEFAULT_HTTPS_ROUTER_PORT;
    }

    private String getDeployServiceUrlFromEnvironment() {
        Map<String, Object> vcapApplication = getVcapApplication();
        List<String> uris = getApplicationUris(vcapApplication);
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
        String vcapApplication = environment.getString(CFG_VCAP_APPLICATION);
        try {
            return JsonUtil.convertJsonToMap(vcapApplication);
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_VCAP_APPLICATION, vcapApplication), e);
            return Collections.emptyMap();
        }
    }

    private List<String> getApplicationUris(Map<String, Object> vcapApplication) {
        for (String urisKey : VCAP_APPLICATION_URIS_KEYS) {
            List<String> uris = CommonUtil.cast(vcapApplication.get(urisKey));
            if (!CollectionUtils.isEmpty(uris)) {
                return uris;
            }
        }
        return null;
    }

    private Boolean areDummyTokensEnabledThroughEnvironment() {
        Boolean value = environment.getBoolean(CFG_DUMMY_TOKENS_ENABLED, DEFAULT_DUMMY_TOKENS_ENABLED);
        LOGGER.info(format(Messages.DUMMY_TOKENS_ENABLED, value));
        return value;
    }

    private Boolean isBasicAuthEnabledThroughEnvironment() {
        Boolean value = environment.getBoolean(CFG_BASIC_AUTH_ENABLED, DEFAULT_BASIC_AUTH_ENABLED);
        LOGGER.info(format(Messages.BASIC_AUTH_ENABLED, value));
        return value;
    }

    private String getGlobalAuditorUserFromEnvironment() {
        String value = environment.getString(CFG_GLOBAL_AUDITOR_USER, DEFAULT_GLOBAL_AUDITOR_USER);
        LOGGER.info(format(Messages.ADMIN_USERNAME, value));
        return value;
    }

    private String getGlobalAuditorPasswordFromEnvironment() {
        return environment.getString(CFG_GLOBAL_AUDITOR_PASSWORD, DEFAULT_GLOBAL_AUDITOR_PASSWORD);
    }

    private Integer getDbConnectionThreadsFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_DB_CONNECTION_THREADS, DEFAULT_DB_CONNECTION_THREADS);
        LOGGER.info(format(Messages.DB_CONNECTION_THREADS, value));
        return value;
    }

    private int getControllerPollingIntervalFromEnvironment() {
        int value = environment.getPositiveInteger(CFG_CONTROLLER_POLLING_INTERVAL, DEFAULT_CONTROLLER_POLLING_INTERVAL);
        LOGGER.info(format(Messages.CONTROLLER_POLLING_INTERVAL, value));
        return value;
    }

    private Boolean shouldSkipSslValidationBasedOnEnvironment() {
        Boolean value = environment.getBoolean(CFG_SKIP_SSL_VALIDATION, DEFAULT_SKIP_SSL_VALIDATION);
        LOGGER.info(format(Messages.SKIP_SSL_VALIDATION, value));
        return value;
    }

    private Boolean areXsPlaceholdersSupportedBasedOnEnvironment() {
        String value = environment.getString(CFG_XS_PLACEHOLDERS_SUPPORTED);
        boolean result = (value != null) && (!value.equals(SupportedParameters.XSA_CONTROLLER_ENDPOINT_PLACEHOLDER));
        LOGGER.info(format(Messages.XS_PLACEHOLDERS_SUPPORTED, result));
        return result;
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
        LOGGER.info(format(Messages.GLOBAL_CONFIG_SPACE, value));
        return value;
    }

    private Boolean shouldGatherUsageStatisticsBasedOnEnvironment() {
        Boolean value = environment.getBoolean(CFG_GATHER_USAGE_STATISTICS, DEFAULT_GATHER_USAGE_STATISTICS);
        LOGGER.info(format(Messages.GATHER_STATISTICS, value));
        return value;
    }

    private HealthCheckConfiguration getHealthCheckConfigurationFromEnvironment() {
        HealthCheckConfiguration healthCheckConfiguration = new HealthCheckConfiguration.Builder()
            .spaceId(getHealthCheckSpaceIdFromEnvironment())
            .mtaId(getHealthCheckMtaIdFromEnvironment())
            .userName(getHealthCheckUserFromEnvironment())
            .timeRangeInSeconds(getHealthCheckTimeRangeFromEnvironment())
            .build();
        LOGGER.info(format(Messages.HEALTH_CHECK_CONFIGURATION, JsonUtil.toJson(healthCheckConfiguration, true)));
        return healthCheckConfiguration;
    }

    private String getHealthCheckSpaceIdFromEnvironment() {
        return environment.getString(CFG_HEALTH_CHECK_SPACE_ID);
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

    private String getMailApiUrlFromEnvironment() {
        String value = environment.getString(CFG_MAIL_API_URL);
        LOGGER.info(format(Messages.MAIL_API_URL, value));
        return value;
    }

    private String getApplicationIdFromEnvironment() {
        Map<String, Object> vcapApplication = getVcapApplication();
        String applicationGuid = (String) vcapApplication.get("application_id");
        LOGGER.info(format(Messages.APPLICATION_ID, applicationGuid));
        return applicationGuid;
    }

    private Integer getApplicationInstanceIndexFromEnvironment() {
        Integer applicationInstanceIndex = environment.getInteger("CF_INSTANCE_INDEX");
        LOGGER.info(format(Messages.APPLICATION_INSTANCE_INDEX, applicationInstanceIndex));
        return applicationInstanceIndex;
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

    private Integer getSpaceDeveloperCacheTimeInSecondsFromEnvironment() {
        Integer value = environment.getPositiveInteger(CFG_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS,
            DEFAULT_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS);
        LOGGER.info(format(Messages.SPACE_DEVELOPERS_CACHE_TIME_IN_SECONDS, value));
        return value;
    }

}
