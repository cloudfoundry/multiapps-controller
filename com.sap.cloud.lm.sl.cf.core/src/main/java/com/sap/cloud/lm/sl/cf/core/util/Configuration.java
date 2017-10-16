package com.sap.cloud.lm.sl.cf.core.util;

import static java.text.MessageFormat.format;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.helpers.Environment;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MiscUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

@Component
public class Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private static final Configuration INSTANCE = new Configuration(new Environment());

    public static Configuration getInstance() {
        return INSTANCE;
    }

    private Environment environment;

    Configuration(Environment environment) {
        this.environment = environment;
    }

    public enum DatabaseType {
        DEFAULTDB, HANA, POSTGRESQL
    }

    // Environment variables
    static final String CFG_TYPE = "XS_TYPE";
    static final String CFG_TARGET_URL = "XS_TARGET_URL"; // Mandatory
    static final String CFG_DB_TYPE = "DB_TYPE";
    static final String CFG_PLATFORMS = "PLATFORMS"; // Mandatory
    static final String CFG_PLATFORMS_V2 = "PLATFORMS_V2"; // Mandatory
    static final String CFG_PLATFORMS_V3 = "PLATFORMS_V3"; // Mandatory
    static final String CFG_TARGETS = "TARGETS";
    static final String CFG_TARGETS_V2 = "TARGETS_V2";
    static final String CFG_TARGETS_V3 = "TARGETS_V3";
    static final String CFG_MAX_UPLOAD_SIZE = "MAX_UPLOAD_SIZE";
    static final String CFG_MAX_MTA_DESCRIPTOR_SIZE = "MAX_MTA_DESCRIPTOR_SIZE";
    static final String CFG_SCAN_UPLOADS = "SCAN_UPLOADS";
    static final String CFG_USE_XS_AUDIT_LOGGING = "USE_XS_AUDIT_LOGGING";
    static final String CFG_VCAP_APPLICATION = "VCAP_APPLICATION"; // Mandatory
    static final String CFG_DUMMY_TOKENS_ENABLED = "DUMMY_TOKENS_ENABLED";
    static final String CFG_BASIC_AUTH_ENABLED = "BASIC_AUTH_ENABLED";
    static final String CFG_ADMIN_USERNAME = "ADMIN_USERNAME";
    static final String CFG_XS_CLIENT_CORE_THREADS = "XS_CLIENT_CORE_THREADS";
    static final String CFG_XS_CLIENT_MAX_THREADS = "XS_CLIENT_MAX_THREADS";
    static final String CFG_XS_CLIENT_QUEUE_CAPACITY = "XS_CLIENT_QUEUE_CAPACITY";
    static final String CFG_XS_CLIENT_KEEP_ALIVE = "XS_CLIENT_KEEP_ALIVE";
    static final String CFG_ASYNC_EXECUTOR_CORE_THREADS = "ASYNC_EXECUTOR_CORE_THREADS";
    static final String CFG_CONTROLLER_POLLING_INTERVAL = "CONTROLLER_POLLING_INTERVAL";
    static final String CFG_UPLOAD_APP_TIMEOUT = "UPLOAD_APP_TIMEOUT";
    static final String CFG_SKIP_SSL_VALIDATION = "SKIP_SSL_VALIDATION";
    static final String CFG_XS_PLACEHOLDERS_SUPPORTED = "XS_PLACEHOLDER_SUPPORT_TEST";
    static final String CFG_VERSION = "VERSION";
    static final String CFG_CHANGE_LOG_LOCK_WAIT_TIME = "CHANGE_LOG_LOCK_WAIT_TIME";
    static final String CFG_CHANGE_LOG_LOCK_DURATION = "CHANGE_LOG_LOCK_DURATION";
    static final String CFG_CHANGE_LOG_LOCK_ATTEMPTS = "CHANGE_LOG_LOCK_ATTEMPTS";
    static final String CFG_GLOBAL_CONFIG_SPACE = "GLOBAL_CONFIG_SPACE";
    static final String CFG_GATHER_USAGE_STATISTICS = "GATHER_USAGE_STATISTICS";

    private static final List<String> VCAP_APPLICATION_URIS_KEYS = Arrays.asList("full_application_uris", "application_uris", "uris");

    // Default values
    public static final PlatformType DEFAULT_TYPE = PlatformType.XS2;
    public static final URL DEFAULT_TARGET_URL = url("http://localhost:9999");
    public static final DatabaseType DEFAULT_DB_TYPE = DatabaseType.DEFAULTDB;
    public static final List<Platform> DEFAULT_PLATFORMS = Collections.emptyList();
    public static final List<Target> DEFAULT_TARGETS = Collections.emptyList();
    public static final long DEFAULT_MAX_UPLOAD_SIZE = 4 * 1024 * 1024 * 1024l; // 4 GB(s)
    public static final long DEFAULT_MAX_MTA_DESCRIPTOR_SIZE = 1024 * 1024l; // 1 MB(s)
    public static final Boolean DEFAULT_SCAN_UPLOADS = false;
    public static final Boolean DEFAULT_USE_XS_AUDIT_LOGGING = true;
    public static final String DEFAULT_SPACE_ID = "";
    public static final int DEFAULT_HTTP_ROUTER_PORT = 80;
    public static final int DEFAULT_HTTPS_ROUTER_PORT = 443;
    public static final Boolean DEFAULT_DUMMY_TOKENS_ENABLED = false;
    public static final Boolean DEFAULT_BASIC_AUTH_ENABLED = false;
    public static final String DEFAULT_ADMIN_USERNAME = "";
    public static final Integer DEFAULT_XS_CLIENT_CORE_THREADS = 2;
    public static final Integer DEFAULT_XS_CLIENT_MAX_THREADS = 8;
    public static final Integer DEFAULT_XS_CLIENT_QUEUE_CAPACITY = 8;
    public static final Integer DEFAULT_XS_CLIENT_KEEP_ALIVE = 60;
    /*
     * In async local operations there are usually two threads. One does the actual work, while the other waits for a specific amount of
     * time and then terminates the first if it is still alive (thus introducing a time-out period for the entire operation).
     * 
     * There should be at least 2 core threads in the thread pool, used by the executor that starts these threads, in order to make sure
     * that the 'worker' thread and the 'killer' thread can be executed simultaneously. Otherwise, the time-out behaviour introduced by the
     * 'killer' thread would not work, as it would not be executed until after the 'worker' thread has already been executed.
     */
    public static final Integer DEFAULT_ASYNC_EXECUTOR_CORE_THREADS = 10;
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
    public static final Integer DEFAULT_UPLOAD_APP_TIMEOUT = 30 * 60; // 30 minute(s)
    public static final Boolean DEFAULT_SKIP_SSL_VALIDATION = false;
    public static final String DEFAULT_VERSION = "N/A";
    public static final Integer DEFAULT_CHANGE_LOG_LOCK_WAIT_TIME = 1; // 1 minute(s)
    public static final Integer DEFAULT_CHANGE_LOG_LOCK_DURATION = 1; // 1 minute(s)
    public static final Integer DEFAULT_CHANGE_LOG_LOCK_ATTEMPTS = 5; // 5 minute(s)
    public static final Boolean DEFAULT_GATHER_USAGE_STATISTICS = false;

    // Type names
    private static final Map<String, PlatformType> TYPE_NAMES = createTypeNames();

    private static Map<String, PlatformType> createTypeNames() {
        Map<String, PlatformType> result = new HashMap<>();
        result.put("XSA", PlatformType.XS2);
        return Collections.unmodifiableMap(result);
    }

    // Cached configuration settings
    private PlatformType platformType;
    private URL targetURL;
    private DatabaseType databaseType;
    private Long maxUploadSize;
    private Long maxMtaDescriptorSize;
    private Boolean scanUploads;
    private Boolean useXSAuditLogging;
    private String spaceGuid;
    private String orgName;
    private Integer routerPort;
    private Boolean dummyTokensEnabled;
    private Boolean basicAuthEnabled;
    private String adminUsername;
    private Integer xsClientCoreThreads;
    private Integer xsClientMaxThreads;
    private Integer xsClientQueueCapacity;
    private Integer xsClientKeepAlive;
    private Integer asyncExecutorCoreThreads;
    private Integer controllerPollingInterval;
    private Integer uploadAppTimeout;
    private Boolean skipSslValidation;
    private Boolean xsPlaceholdersSupported;
    private String version;
    private String deployServiceUrl;
    private Integer changeLogLockWaitTime;
    private Integer changeLogLockDuration;
    private Integer changeLogLockAttempts;
    private String globalConfigSpace;
    private Boolean gatherUsageStatistics;

    public void load() {
        getPlatformType();
        getTargetURL();
        getDatabaseType();
        getMaxUploadSize();
        getMaxMtaDescriptorSize();
        shouldScanUploads();
        shouldUseXSAuditLogging();
        getSpaceGuid();
        getOrgName();
        getRouterPort();
        getDeployServiceUrl();
        areDummyTokensEnabled();
        isBasicAuthEnabled();
        getAdminUsername();
        getXsClientCoreThreads();
        getXsClientMaxThreads();
        getXsClientQueueCapacity();
        getXsClientKeepAlive();
        getAsyncExecutorCoreThreads();
        getControllerPollingInterval();
        getUploadAppTimeout();
        shouldSkipSslValidation();
        areXsPlaceholdersSupported();
        getVersion();
        getChangeLogLockWaitTime();
        getChangeLogLockDuration();
        getChangeLogLockAttempts();
        getGlobalConfigSpace();
        shouldGatherUsageStatistics();
    }

    public void logFullConfig() {
        for (Map.Entry<String, String> envVariable : getFilteredEnv().entrySet()) {
            AuditLoggingProvider.getFacade().logConfig(envVariable.getKey(), envVariable.getValue());
        }
    }

    public Map<String, String> getFilteredEnv() {
        Set<String> notSensitiveConfigVariables = getNotSensitiveConfigVariables();
        Map<String, String> env = environment.getVariables();
        return env.entrySet().stream().filter(envVariable -> notSensitiveConfigVariables.contains(envVariable.getKey())).collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> getNotSensitiveConfigVariables() {
        return new HashSet<>(Arrays.asList(CFG_TYPE, CFG_TARGET_URL, CFG_DB_TYPE, CFG_PLATFORMS, CFG_PLATFORMS_V2, CFG_PLATFORMS_V3,
            CFG_TARGETS, CFG_TARGETS_V2, CFG_TARGETS_V3, CFG_MAX_UPLOAD_SIZE, CFG_MAX_MTA_DESCRIPTOR_SIZE, CFG_SCAN_UPLOADS,
            CFG_USE_XS_AUDIT_LOGGING, CFG_DUMMY_TOKENS_ENABLED, CFG_BASIC_AUTH_ENABLED, CFG_ADMIN_USERNAME, CFG_XS_CLIENT_CORE_THREADS,
            CFG_XS_CLIENT_MAX_THREADS, CFG_XS_CLIENT_QUEUE_CAPACITY, CFG_XS_CLIENT_KEEP_ALIVE, CFG_ASYNC_EXECUTOR_CORE_THREADS,
            CFG_CONTROLLER_POLLING_INTERVAL, CFG_UPLOAD_APP_TIMEOUT, CFG_SKIP_SSL_VALIDATION, CFG_XS_PLACEHOLDERS_SUPPORTED, CFG_VERSION,
            CFG_CHANGE_LOG_LOCK_WAIT_TIME, CFG_CHANGE_LOG_LOCK_DURATION, CFG_CHANGE_LOG_LOCK_ATTEMPTS, CFG_GLOBAL_CONFIG_SPACE,
            CFG_GATHER_USAGE_STATISTICS));
    }

    public com.sap.cloud.lm.sl.persistence.util.Configuration getFileConfiguration() {
        return new com.sap.cloud.lm.sl.persistence.util.DefaultConfiguration(getMaxUploadSize(), shouldScanUploads());
    }

    public PlatformType getPlatformType() {
        if (platformType == null) {
            platformType = getPlatformTypeFromEnvironment();
        }
        return platformType;
    }

    public URL getTargetURL() {
        if (targetURL == null) {
            targetURL = getTargetURLFromEnvironment();
        }
        return targetURL;
    }

    public DatabaseType getDatabaseType() {
        if (databaseType == null) {
            databaseType = getDatabaseTypeFromEnvironment();
        }
        return databaseType;
    }

    public List<Platform> getPlatforms() {
        return getPlatforms(null, 1);
    }

    public List<Platform> getPlatforms(ConfigurationParser parser, int majorVersion) {
        switch (majorVersion) {
            case 1:
                return parsePlatforms(environment.getVariable(CFG_PLATFORMS));
            case 2:
                return parsePlatforms(environment.getVariable(CFG_PLATFORMS_V2), parser);
            case 3:
                return parsePlatforms(environment.getVariable(CFG_PLATFORMS_V3), parser);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public List<? extends Target> getTargets() {
        return getTargets(null, 1);
    }

    public List<? extends Target> getTargets(ConfigurationParser parser, int majorVersion) {
        switch (majorVersion) {
            case 1:
                return parseTargets(environment.getVariable(CFG_TARGETS));
            case 2:
                return parseTargets(environment.getVariable(CFG_TARGETS_V2), parser);
            case 3:
                return parseTargets(environment.getVariable(CFG_TARGETS_V3), parser);
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

    public String getAdminUsername() {
        if (adminUsername == null) {
            adminUsername = getAdminUsernameFromEnvironment();
        }
        return adminUsername;
    }

    public int getXsClientCoreThreads() {
        if (xsClientCoreThreads == null) {
            xsClientCoreThreads = getXsClientCoreThreadsFromEnvironment();
        }
        return xsClientCoreThreads;
    }

    public int getXsClientMaxThreads() {
        if (xsClientMaxThreads == null) {
            xsClientMaxThreads = getXsClientMaxThreadsFromEnvironment();
        }
        return xsClientMaxThreads;
    }

    public int getXsClientQueueCapacity() {
        if (xsClientQueueCapacity == null) {
            xsClientQueueCapacity = getXsClientQueueCapacityFromEnvironment();
        }
        return xsClientQueueCapacity;
    }

    public int getXsClientKeepAlive() {
        if (xsClientKeepAlive == null) {
            xsClientKeepAlive = getXsClientKeepAliveFromEnvironment();
        }
        return xsClientKeepAlive;
    }

    public int getAsyncExecutorCoreThreads() {
        if (asyncExecutorCoreThreads == null) {
            asyncExecutorCoreThreads = getAsyncExecutorCoreThreadsFromEnvironment();
        }
        return asyncExecutorCoreThreads;
    }

    public int getControllerPollingInterval() {
        if (controllerPollingInterval == null) {
            controllerPollingInterval = getControllerPollingIntervalFromEnvironment();
        }
        return controllerPollingInterval;
    }

    public int getUploadAppTimeout() {
        if (uploadAppTimeout == null) {
            uploadAppTimeout = getUploadAppTimeoutFromEnvironment();
        }
        return uploadAppTimeout;
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

    public Integer getChangeLogLockWaitTime() {
        if (changeLogLockWaitTime == null) {
            changeLogLockWaitTime = getChangeLogLockWaitTimeFromEnvironment();
        }
        return changeLogLockWaitTime;
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

    private PlatformType getPlatformTypeFromEnvironment() {
        String type = environment.getVariable(CFG_TYPE);
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

    private URL getTargetURLFromEnvironment() {
        String targetURL = environment.getVariable(CFG_TARGET_URL);
        try {
            if (targetURL != null) {
                URL result = MiscUtil.getURL(targetURL);
                LOGGER.info(format(Messages.XS_TARGET_URL, result));
                return result;
            }
            LOGGER.warn(format(Messages.XS_TARGET_URL_NOT_SPECIFIED, DEFAULT_TARGET_URL));
        } catch (MalformedURLException e) {
            LOGGER.warn(format(Messages.INVALID_XS_TARGET_URL, targetURL, DEFAULT_TARGET_URL), e);
        }
        return DEFAULT_TARGET_URL;
    }

    private DatabaseType getDatabaseTypeFromEnvironment() {
        String type = environment.getVariable(CFG_DB_TYPE);
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

    private List<Platform> parsePlatforms(String json) {
        return parsePlatforms(json, new ConfigurationParser());
    }

    private List<Platform> parsePlatforms(String json, ConfigurationParser parser) {
        try {
            if (json != null) {
                List<Platform> result = parser.parsePlatformsJson(json);
                LOGGER.info(format(Messages.PLATFORMS, JsonUtil.toJson(result, true)));
                return result;
            }
            LOGGER.warn(format(Messages.PLATFORMS_NOT_SPECIFIED, DEFAULT_PLATFORMS));
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_PLATFORMS, json, DEFAULT_PLATFORMS), e);
        }
        return DEFAULT_PLATFORMS;
    }

    private List<Target> parseTargets(String json) {
        return parseTargets(json, new ConfigurationParser());
    }

    private List<Target> parseTargets(String json, ConfigurationParser parser) {
        try {
            if (json != null) {
                List<Target> result = parser.parseTargetsJson(json);
                LOGGER.info(format(Messages.TARGETS, new SecureSerializationFacade().toJson(result)));
                return result;
            }
            LOGGER.info(format(Messages.TARGETS_NOT_SPECIFIED, DEFAULT_TARGETS));
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_TARGETS, json, DEFAULT_TARGETS), e);
        }
        return DEFAULT_TARGETS;
    }

    private Long getMaxUploadSizeFromEnvironment() {
        String value = environment.getVariable(CFG_MAX_UPLOAD_SIZE);
        return getLong(value, DEFAULT_MAX_UPLOAD_SIZE, Messages.MAX_UPLOAD_SIZE, Messages.MAX_UPLOAD_SIZE_NOT_SPECIFIED,
            Messages.INVALID_MAX_UPLOAD_SIZE);
    }

    private Long getMaxMtaDescriptorSizeFromEnvironment() {
        String value = environment.getVariable(CFG_MAX_MTA_DESCRIPTOR_SIZE);
        return getLong(value, DEFAULT_MAX_MTA_DESCRIPTOR_SIZE, Messages.MAX_MTA_DESCRIPTOR_SIZE,
            Messages.MAX_MTA_DESCRIPTOR_SIZE_NOT_SPECIFIED, Messages.INVALID_MAX_MTA_DESCRIPTOR_SIZE);
    }

    private Boolean shouldScanUploadsFromEnvironment() {
        String value = environment.getVariable(CFG_SCAN_UPLOADS);
        return getBoolean(value, DEFAULT_SCAN_UPLOADS, Messages.SCAN_UPLOADS);
    }

    private Boolean shouldUseXSAuditLoggingFromEnvironment() {
        String value = environment.getVariable(CFG_USE_XS_AUDIT_LOGGING);
        return getBoolean(value, DEFAULT_USE_XS_AUDIT_LOGGING, Messages.USE_XS_AUDIT_LOGGING);
    }

    private String getSpaceGuidFromEnvironment() {
        String vcapApplication = environment.getVariable(CFG_VCAP_APPLICATION);
        try {
            Map<String, Object> parsedVcapApplication = JsonUtil.convertJsonToMap(vcapApplication);
            Object spaceId = parsedVcapApplication.get("space_id");
            if (spaceId != null) {
                LOGGER.info(format(Messages.SPACE_ID, spaceId));
                return spaceId.toString();
            }
            LOGGER.warn(format(Messages.SPACE_ID_NOT_SPECIFIED, DEFAULT_SPACE_ID));
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_VCAP_APPLICATION_SPACE_ID, vcapApplication, DEFAULT_SPACE_ID), e);
        }
        return DEFAULT_SPACE_ID;
    }

    private String getOrgNameFromEnvironment() {
        String vcapApplication = environment.getVariable(CFG_VCAP_APPLICATION);
        try {
            Map<String, Object> parsedVcapApplication = JsonUtil.convertJsonToMap(vcapApplication);
            Object orgName = parsedVcapApplication.get("organization_name");
            if (orgName != null) {
                LOGGER.info(format(Messages.ORG_NAME, orgName));
                return orgName.toString();
            }
            LOGGER.warn(format(Messages.SPACE_ID_NOT_SPECIFIED, DEFAULT_SPACE_ID));
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_VCAP_APPLICATION, vcapApplication), e);
        }
        return null;
    }

    private Integer getRouterPortFromEnvironment() {
        int defaultRouterPort = computeDefaultRouterPort();
        String vcapApplication = environment.getVariable(CFG_VCAP_APPLICATION);
        try {
            Map<String, Object> parsedVcapApplication = JsonUtil.convertJsonToMap(vcapApplication);
            List<String> uris = getApplicationUris(parsedVcapApplication);
            if (!CommonUtil.isNullOrEmpty(uris)) {
                Pair<String, String> portAndDomain = UriUtil.getHostAndDomain(uris.get(0));
                int port = Integer.parseInt(portAndDomain._1);
                if (UriUtil.isValidPort(port)) {
                    return port;
                }
            }
            LOGGER.info(format(Messages.NO_APPLICATION_URIS_SPECIFIED, defaultRouterPort));
        } catch (ParsingException | NumberFormatException e) {
            LOGGER.warn(format(Messages.INVALID_VCAP_APPLICATION_ROUTER_PORT, vcapApplication, defaultRouterPort), e);
        }
        return defaultRouterPort;
    }

    private int computeDefaultRouterPort() {
        return getTargetURL().getProtocol().equals("http") ? DEFAULT_HTTP_ROUTER_PORT : DEFAULT_HTTPS_ROUTER_PORT;
    }

    private String getDeployServiceUrlFromEnvironment() {
        String vcapApplication = environment.getVariable(CFG_VCAP_APPLICATION);
        try {
            Map<String, Object> parsedVcapApplication = JsonUtil.convertJsonToMap(vcapApplication);
            List<String> uris = getApplicationUris(parsedVcapApplication);
            if (!CommonUtil.isNullOrEmpty(uris)) {
                return uris.get(0);
            }
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_VCAP_APPLICATION_DEPLOY_SERVICE_URI, vcapApplication), e);
        }
        return null;
    }

    private List<String> getApplicationUris(Map<String, Object> vcapApplication) {
        for (String urisKey : VCAP_APPLICATION_URIS_KEYS) {
            List<String> uris = CommonUtil.cast(vcapApplication.get(urisKey));
            if (!CommonUtil.isNullOrEmpty(uris)) {
                return uris;
            }
        }
        return null;
    }

    private Boolean areDummyTokensEnabledThroughEnvironment() {
        String value = environment.getVariable(CFG_DUMMY_TOKENS_ENABLED);
        return getBoolean(value, DEFAULT_DUMMY_TOKENS_ENABLED, Messages.DUMMY_TOKENS_ENABLED);
    }

    private Boolean isBasicAuthEnabledThroughEnvironment() {
        String value = environment.getVariable(CFG_BASIC_AUTH_ENABLED);
        return getBoolean(value, DEFAULT_BASIC_AUTH_ENABLED, Messages.BASIC_AUTH_ENABLED);
    }

    private String getAdminUsernameFromEnvironment() {
        String value = environment.getVariable(CFG_ADMIN_USERNAME);
        return getString(value, DEFAULT_ADMIN_USERNAME, Messages.ADMIN_USERNAME);
    }

    private Integer getXsClientCoreThreadsFromEnvironment() {
        String value = environment.getVariable(CFG_XS_CLIENT_CORE_THREADS);
        return getPositiveInt(value, DEFAULT_XS_CLIENT_CORE_THREADS, Messages.XS_CLIENT_CORE_THREADS);
    }

    private Integer getXsClientMaxThreadsFromEnvironment() {
        String value = environment.getVariable(CFG_XS_CLIENT_MAX_THREADS);
        return getPositiveInt(value, DEFAULT_XS_CLIENT_MAX_THREADS, Messages.XS_CLIENT_MAX_THREADS);
    }

    private Integer getXsClientQueueCapacityFromEnvironment() {
        String value = environment.getVariable(CFG_XS_CLIENT_QUEUE_CAPACITY);
        return getPositiveInt(value, DEFAULT_XS_CLIENT_QUEUE_CAPACITY, Messages.XS_CLIENT_QUEUE_CAPACITY);
    }

    private int getXsClientKeepAliveFromEnvironment() {
        String value = environment.getVariable(CFG_XS_CLIENT_KEEP_ALIVE);
        return getPositiveInt(value, DEFAULT_XS_CLIENT_KEEP_ALIVE, Messages.XS_CLIENT_KEEP_ALIVE);
    }

    private Integer getAsyncExecutorCoreThreadsFromEnvironment() {
        String value = environment.getVariable(CFG_ASYNC_EXECUTOR_CORE_THREADS);
        return getPositiveInt(value, DEFAULT_ASYNC_EXECUTOR_CORE_THREADS, Messages.ASYNC_EXECUTOR_CORE_THREADS);
    }

    private int getControllerPollingIntervalFromEnvironment() {
        String value = environment.getVariable(CFG_CONTROLLER_POLLING_INTERVAL);
        return getPositiveInt(value, DEFAULT_CONTROLLER_POLLING_INTERVAL, Messages.CONTROLLER_POLLING_INTERVAL);
    }

    private int getUploadAppTimeoutFromEnvironment() {
        String value = environment.getVariable(CFG_UPLOAD_APP_TIMEOUT);
        return getPositiveInt(value, DEFAULT_UPLOAD_APP_TIMEOUT, Messages.UPLOAD_APP_TIMEOUT);
    }

    private Boolean shouldSkipSslValidationBasedOnEnvironment() {
        String value = environment.getVariable(CFG_SKIP_SSL_VALIDATION);
        return getBoolean(value, DEFAULT_SKIP_SSL_VALIDATION, Messages.SKIP_SSL_VALIDATION);
    }

    private Boolean areXsPlaceholdersSupportedBasedOnEnvironment() {
        String value = environment.getVariable(CFG_XS_PLACEHOLDERS_SUPPORTED);
        boolean result = (value != null) && (!value.equals(SupportedParameters.XSA_CONTROLLER_ENDPOINT_PLACEHOLDER));
        LOGGER.info(format(Messages.XS_PLACEHOLDERS_SUPPORTED, result));
        return result;
    }

    private String getVersionFromEnvironment() {
        String version = environment.getVariable(CFG_VERSION);
        return getString(version, DEFAULT_VERSION, Messages.DS_VERSION);
    }

    private Integer getChangeLogLockWaitTimeFromEnvironment() {
        String value = environment.getVariable(CFG_CHANGE_LOG_LOCK_WAIT_TIME);
        return getPositiveInt(value, DEFAULT_CHANGE_LOG_LOCK_WAIT_TIME, Messages.CHANGE_LOG_LOCK_WAIT_TIME);
    }

    private Integer getChangeLogLockDurationFromEnvironment() {
        String value = environment.getVariable(CFG_CHANGE_LOG_LOCK_DURATION);
        return getPositiveInt(value, DEFAULT_CHANGE_LOG_LOCK_DURATION, Messages.CHANGE_LOG_LOCK_DURATION);
    }

    private Integer getChangeLogLockAttemptsFromEnvironment() {
        String value = environment.getVariable(CFG_CHANGE_LOG_LOCK_ATTEMPTS);
        return getPositiveInt(value, DEFAULT_CHANGE_LOG_LOCK_ATTEMPTS, Messages.CHANGE_LOG_LOCK_ATTEMPTS);
    }

    private String getGlobalConfigSpaceFromEnvironment() {
        String value = environment.getVariable(CFG_GLOBAL_CONFIG_SPACE);
        return getString(value, null, Messages.GLOBAL_CONFIG_SPACE);
    }

    private Boolean shouldGatherUsageStatisticsBasedOnEnvironment() {
        String value = environment.getVariable(CFG_GATHER_USAGE_STATISTICS);
        return getBoolean(value, DEFAULT_GATHER_USAGE_STATISTICS, Messages.GATHER_STATISTICS);
    }

    private String getString(String value, String defaultValue, String message) {
        String result = (value != null) ? value : defaultValue;
        LOGGER.info(format(message, result));
        return result;
    }

    private Integer getPositiveInt(String value, Integer defaultValue, String message) {
        Integer result = (value != null) ? Integer.valueOf(value) : defaultValue;
        if (result <= 0)
            result = Integer.MAX_VALUE;
        LOGGER.info(format(message, result));
        return result;
    }

    private Boolean getBoolean(String value, Boolean defaultValue, String message) {
        Boolean result = (value != null) ? Boolean.valueOf(value) : defaultValue;
        LOGGER.info(format(message, result));
        return result;
    }

    private Long getLong(String value, Long defaultValue, String message, String notSpecifiedMessage, String invalidMessage) {
        try {
            if (value != null) {
                Long result = Long.valueOf(value);
                LOGGER.info(format(message, result));
                return result;
            }
            LOGGER.info(format(notSpecifiedMessage, defaultValue));
        } catch (NumberFormatException e) {
            LOGGER.warn(format(invalidMessage, value, defaultValue), e);
        }
        return defaultValue;
    }

    private static URL url(String s) {
        try {
            return MiscUtil.getURL(s);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

}
