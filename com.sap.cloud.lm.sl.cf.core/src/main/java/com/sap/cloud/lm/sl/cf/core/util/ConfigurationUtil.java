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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
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
import com.sap.cloud.lm.sl.persistence.util.Configuration;
import com.sap.cloud.lm.sl.persistence.util.DefaultConfiguration;

public class ConfigurationUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationUtil.class);

    public enum DatabaseType {
        DEFAULTDB, HANA, POSTGRESQL
    }

    // Environment variables
    private static final String CFG_TYPE = "XS_TYPE";
    private static final String CFG_TARGET_URL = "XS_TARGET_URL"; // Mandatory
    private static final String CFG_DB_TYPE = "DB_TYPE";
    private static final String CFG_PLATFORMS = "PLATFORMS"; // Mandatory
    private static final String CFG_PLATFORMS_V2 = "PLATFORMS_V2"; // Mandatory
    private static final String CFG_PLATFORMS_V3 = "PLATFORMS_V3"; // Mandatory
    private static final String CFG_TARGETS = "TARGETS";
    private static final String CFG_TARGETS_V2 = "TARGETS_V2";
    private static final String CFG_TARGETS_V3 = "TARGETS_V3";
    private static final String CFG_MAX_UPLOAD_SIZE = "MAX_UPLOAD_SIZE";
    private static final String CFG_MAX_MTA_DESCRIPTOR_SIZE = "MAX_MTA_DESCRIPTOR_SIZE";
    private static final String CFG_SCAN_UPLOADS = "SCAN_UPLOADS";
    private static final String CFG_USE_XS_AUDIT_LOGGING = "USE_XS_AUDIT_LOGGING";
    private static final String CFG_VCAP_APPLICATION = "VCAP_APPLICATION"; // Mandatory
    private static final String CFG_DUMMY_TOKENS_ENABLED = "DUMMY_TOKENS_ENABLED";
    private static final String CFG_BASIC_AUTH_ENABLED = "BASIC_AUTH_ENABLED";
    private static final String CFG_ADMIN_USERNAME = "ADMIN_USERNAME";
    private static final String CFG_XS_CLIENT_CORE_THREADS = "XS_CLIENT_CORE_THREADS";
    private static final String CFG_XS_CLIENT_MAX_THREADS = "XS_CLIENT_MAX_THREADS";
    private static final String CFG_XS_CLIENT_QUEUE_CAPACITY = "XS_CLIENT_QUEUE_CAPACITY";
    private static final String CFG_XS_CLIENT_KEEP_ALIVE = "XS_CLIENT_KEEP_ALIVE";
    private static final String CFG_ASYNC_EXECUTOR_CORE_THREADS = "ASYNC_EXECUTOR_CORE_THREADS";
    private static final String CFG_CONTROLLER_POLLING_INTERVAL = "CONTROLLER_POLLING_INTERVAL";
    private static final String CFG_UPLOAD_APP_TIMEOUT = "UPLOAD_APP_TIMEOUT";
    private static final String CFG_SKIP_SSL_VALIDATION = "SKIP_SSL_VALIDATION";
    private static final String CFG_XS_PLACEHOLDERS_SUPPORTED = "XS_PLACEHOLDER_SUPPORT_TEST";
    private static final String CFG_VERSION = "VERSION";
    private static final String CFG_CHANGE_LOG_LOCK_WAIT_TIME = "CHANGE_LOG_LOCK_WAIT_TIME";
    private static final String CFG_CHANGE_LOG_LOCK_DURATION = "CHANGE_LOG_LOCK_DURATION";
    private static final String CFG_CHANGE_LOG_LOCK_ATTEMPTS = "CHANGE_LOG_LOCK_ATTEMPTS";
    private static final String CFG_GLOBAL_CONFIG_SPACE = "GLOBAL_CONFIG_SPACE";
    private static final String CFG_GATHER_USAGE_STATISTICS = "GATHER_USAGE_STATISTICS";

    private static final List<String> VCAP_APPLICATION_URIS_KEYS = Arrays.asList("full_application_uris", "application_uris", "uris");

    // Default values
    static final PlatformType DEFAULT_TYPE = PlatformType.XS2;
    static final URL DEFAULT_TARGET_URL = url("http://localhost:9999");
    static final DatabaseType DEFAULT_DB_TYPE = DatabaseType.DEFAULTDB;
    static final List<Platform> DEFAULT_PLATFORM_TYPES = Collections.emptyList();
    static final List<Target> DEFAULT_PLATFORMS = Collections.emptyList();
    static final long DEFAULT_MAX_UPLOAD_SIZE = 4 * 1024 * 1024 * 1024l; // 4GB
    static final long DEFAULT_MAX_MTA_DESCRIPTOR_SIZE = 1024 * 1024l; // 1MB
    static final Boolean DEFAULT_SCAN_UPLOADS = false;
    static final String DEFAULT_SPACE_ID = "";
    static final Integer DEFAULT_ROUTER_PORT = ConfigurationUtil.getTargetURL().getProtocol().equals("http") ? 80 : 443;
    private static final Boolean DEFAULT_USE_XS_AUDIT_LOGGING = true;
    private static final Integer DEFAULT_XS_CLIENT_CORE_THREADS = 2;
    private static final Integer DEFAULT_XS_CLIENT_MAX_THREADS = 8;
    private static final Integer DEFAULT_XS_CLIENT_QUEUE_CAPACITY = 8;
    private static final Integer DEFAULT_XS_CLIENT_KEEP_ALIVE = 60;
    private static final Boolean DEFAULT_STATISTICS_USAGE = false;

    /*
     * In async local operations there are usually two threads. One does the actual work, while the other waits for a specific amount of
     * time and then terminates the first if it is still alive (thus introducing a time-out period for the entire operation).
     * 
     * There should be at least 2 core threads in the thread pool, used by the executor that starts these threads, in order to make sure
     * that the 'worker' thread and the 'killer' thread can be executed simultaneously. Otherwise, the time-out behaviour introduced by the
     * 'killer' thread would not work, as it would not be executed until after the 'worker' thread has already been executed.
     */
    private static final Integer DEFAULT_ASYNC_EXECUTOR_CORE_THREADS = 10;

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
    private static final Integer DEFAULT_CONTROLLER_POLLING_INTERVAL = 6; // 6 second(s)
    private static final Integer DEFAULT_UPLOAD_APP_TIMEOUT = 30 * 60; // 30 minute(s)
    private static final Integer DEFAULT_CHANGE_LOG_LOCK_WAIT_TIME = 1; // 1 minute(s)
    private static final Integer DEFAULT_CHANGE_LOG_LOCK_DURATION = 1; // 1 minute(s)
    private static final Integer DEFAULT_CHANGE_LOG_LOCK_ATTEMPTS = 5; // 5 minute(s)

    // Type names
    private static final Map<String, PlatformType> TYPE_NAMES = createTypeNames();

    private static Map<String, PlatformType> createTypeNames() {
        Map<String, PlatformType> result = new HashMap<>();
        result.put("XSA", PlatformType.XS2);
        return Collections.unmodifiableMap(result);
    }

    // Cached configuration settings
    private static PlatformType platformType;
    private static URL targetURL;
    private static DatabaseType databaseType;
    private static Long maxUploadSize;
    private static Long maxMtaDescriptorSize;
    private static Boolean scanUploads;
    private static Boolean useXSAuditLogging;
    private static String spaceGuid;
    private static String orgName;
    private static Integer routerPort;
    private static Boolean dummyTokensEnabled;
    private static Boolean basicAuthEnabled;
    private static String adminUsername;
    private static Integer xsClientCoreThreads;
    private static Integer xsClientMaxThreads;
    private static Integer xsClientQueueCapacity;
    private static Integer xsClientKeepAlive;
    private static Integer asyncExecutorCoreThreads;
    private static Integer controllerPollingInterval;
    private static Integer uploadAppTimeout;
    private static Boolean skipSslValidation;
    private static Boolean xsPlaceholdersSupported;
    private static String version;
    private static String deployServiceUrl;
    private static Integer changeLogLockWaitTime;
    private static Integer changeLogLockDuration;
    private static Integer changeLogLockAttempts;
    private static String globalConfigSpace;
    private static Boolean gatherUsageStatistics;

    public static void load() {
        getPlatformType();
        getTargetURL();
        getDatabaseType();
        getPlatforms();
        getTargets();
        getMaxUploadSize();
        getMaxMtaDescriptorSize();
        shouldScanUploads();
        isUseXSAuditLogging();
        getSpaceGuid();
        getOrgName();
        getRouterPort();
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
        getDeployServiceUrl();
        getChangeLogLockWaitTime();
        getChangeLogLockDuration();
        getChangeLogLockAttempts();
        getGlobalConfigSpace();
        shouldGatherUsageStatistics();
    }

    public static void logFullConfig() {
        for (Map.Entry<String, String> envVariable : getFilteredEnv().entrySet()) {
            AuditLoggingProvider.getFacade().logConfig(envVariable.getKey(), envVariable.getValue());
        }
    }

    public static Map<String, String> getFilteredEnv() {
        Set<String> notSensitiveConfigVariables = getNotSensitiveConfigVariables();
        Map<String, String> env = System.getenv();
        return env.entrySet().stream().filter(envVariable -> notSensitiveConfigVariables.contains(envVariable.getKey())).collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Set<String> getNotSensitiveConfigVariables() {
        return new HashSet<>(Arrays.asList(CFG_TYPE, CFG_TARGET_URL, CFG_DB_TYPE, CFG_PLATFORMS, CFG_PLATFORMS_V2, CFG_PLATFORMS_V3,
            CFG_TARGETS, CFG_TARGETS_V2, CFG_TARGETS_V3, CFG_MAX_UPLOAD_SIZE, CFG_MAX_MTA_DESCRIPTOR_SIZE, CFG_SCAN_UPLOADS,
            CFG_USE_XS_AUDIT_LOGGING, CFG_DUMMY_TOKENS_ENABLED, CFG_BASIC_AUTH_ENABLED, CFG_ADMIN_USERNAME, CFG_XS_CLIENT_CORE_THREADS,
            CFG_XS_CLIENT_MAX_THREADS, CFG_XS_CLIENT_QUEUE_CAPACITY, CFG_XS_CLIENT_KEEP_ALIVE, CFG_ASYNC_EXECUTOR_CORE_THREADS,
            CFG_CONTROLLER_POLLING_INTERVAL, CFG_UPLOAD_APP_TIMEOUT, CFG_SKIP_SSL_VALIDATION, CFG_XS_PLACEHOLDERS_SUPPORTED, CFG_VERSION,
            CFG_CHANGE_LOG_LOCK_WAIT_TIME, CFG_CHANGE_LOG_LOCK_DURATION, CFG_CHANGE_LOG_LOCK_ATTEMPTS, CFG_GLOBAL_CONFIG_SPACE,
            CFG_GATHER_USAGE_STATISTICS));
    }

    public static Configuration getConfiguration() {
        return new DefaultConfiguration(getMaxUploadSize(), shouldScanUploads());
    }

    public static PlatformType getPlatformType() {
        if (platformType == null) {
            platformType = getPlatformType(System.getenv(CFG_TYPE));
        }
        return platformType;
    }

    public static URL getTargetURL() {
        if (targetURL == null) {
            targetURL = getTargetURL(getEnv(CFG_TARGET_URL));
        }
        return targetURL;
    }

    public static DatabaseType getDatabaseType() {
        if (databaseType == null) {
            databaseType = getDatabaseType(System.getenv(CFG_DB_TYPE));
        }
        return databaseType;
    }

    public static List<Platform> getPlatforms() {
        return getPlatforms(null, 1);
    }

    public static List<Platform> getPlatforms(ConfigurationParser parser, int majorVersion) {
        switch (majorVersion) {
            case 1:
                return getPlatforms(getEnv(CFG_PLATFORMS));
            case 2:
                return getPlatforms(getEnv(CFG_PLATFORMS_V2), parser);
            case 3:
                return getPlatforms(getEnv(CFG_PLATFORMS_V3), parser);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static List<? extends Target> getTargets() {
        return getTargets(null, 1);
    }

    public static List<? extends Target> getTargets(ConfigurationParser parser, int majorVersion) {
        switch (majorVersion) {
            case 1:
                return getTargets(System.getenv(CFG_TARGETS));
            case 2:
                return getTargets(getEnv(CFG_TARGETS_V2), parser);
            case 3:
                return getTargets(getEnv(CFG_TARGETS_V3), parser);
            default:
                throw new UnsupportedOperationException();
        }

    }

    public static Long getMaxUploadSize() {
        if (maxUploadSize == null) {
            maxUploadSize = getMaxUploadSize(System.getenv(CFG_MAX_UPLOAD_SIZE));
        }
        return maxUploadSize;
    }

    public static Long getMaxMtaDescriptorSize() {
        if (maxMtaDescriptorSize == null) {
            maxMtaDescriptorSize = getMaxMtaDescriptorSize(System.getenv(CFG_MAX_MTA_DESCRIPTOR_SIZE));
        }
        return maxMtaDescriptorSize;
    }

    public static Boolean shouldScanUploads() {
        if (scanUploads == null) {
            scanUploads = shouldScanUploads(System.getenv(CFG_SCAN_UPLOADS));
        }
        return scanUploads;
    }

    public static Boolean isUseXSAuditLogging() {
        if (useXSAuditLogging == null) {
            useXSAuditLogging = isUseXSAuditLogging(System.getenv(CFG_USE_XS_AUDIT_LOGGING));
        }
        return useXSAuditLogging;
    }

    public static String getSpaceGuid() {
        if (spaceGuid == null) {
            spaceGuid = getSpaceGuid(getEnv(CFG_VCAP_APPLICATION));
        }
        return spaceGuid;
    }

    public static String getOrgName() {
        if (orgName == null) {
            orgName = getOrgName(getEnv(CFG_VCAP_APPLICATION));
        }
        return orgName;
    }

    private static String getOrgName(String json) {
        try {
            Map<String, Object> vcapApplication = JsonUtil.convertJsonToMap(json);
            Object orgName = vcapApplication.get("organization_name");
            if (orgName != null) {
                LOGGER.info(format(Messages.ORG_NAME, orgName));
                return orgName.toString();
            }
            LOGGER.warn(format(Messages.SPACE_ID_NOT_SPECIFIED, DEFAULT_SPACE_ID));
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_VCAP_APPLICATION, json), e);
        }
        return null;
    }

    public static int getRouterPort() {
        if (routerPort == null) {
            routerPort = getRouterPort(getEnv(CFG_VCAP_APPLICATION));
        }
        return routerPort;
    }

    public static Boolean areDummyTokensEnabled() {
        if (dummyTokensEnabled == null) {
            dummyTokensEnabled = areDummyTokensEnabled(System.getenv(CFG_DUMMY_TOKENS_ENABLED));
        }
        return dummyTokensEnabled;
    }

    public static Boolean isBasicAuthEnabled() {
        if (basicAuthEnabled == null) {
            basicAuthEnabled = isBasicAuthEnabled(System.getenv(CFG_BASIC_AUTH_ENABLED));
        }
        return basicAuthEnabled;
    }

    public static String getAdminUsername() {
        if (adminUsername == null) {
            adminUsername = getAdminUsername(System.getenv(CFG_ADMIN_USERNAME));
        }
        return adminUsername;
    }

    public static int getXsClientCoreThreads() {
        if (xsClientCoreThreads == null) {
            xsClientCoreThreads = getXsClientCoreThreads(System.getenv(CFG_XS_CLIENT_CORE_THREADS));
        }
        return xsClientCoreThreads;
    }

    public static int getXsClientMaxThreads() {
        if (xsClientMaxThreads == null) {
            xsClientMaxThreads = getXsClientMaxThreads(System.getenv(CFG_XS_CLIENT_MAX_THREADS));
        }
        return xsClientMaxThreads;
    }

    public static int getXsClientQueueCapacity() {
        if (xsClientQueueCapacity == null) {
            xsClientQueueCapacity = getXsClientQueueCapacity(System.getenv(CFG_XS_CLIENT_QUEUE_CAPACITY));
        }
        return xsClientQueueCapacity;
    }

    public static int getXsClientKeepAlive() {
        if (xsClientKeepAlive == null) {
            xsClientKeepAlive = getXsClientKeepAlive(System.getenv(CFG_XS_CLIENT_KEEP_ALIVE));
        }
        return xsClientKeepAlive;
    }

    public static int getAsyncExecutorCoreThreads() {
        if (asyncExecutorCoreThreads == null) {
            asyncExecutorCoreThreads = getAsyncExecutorCoreThreads(System.getenv(CFG_ASYNC_EXECUTOR_CORE_THREADS));
        }
        return asyncExecutorCoreThreads;
    }

    public static int getControllerPollingInterval() {
        if (controllerPollingInterval == null) {
            controllerPollingInterval = getControllerPollingInterval(System.getenv(CFG_CONTROLLER_POLLING_INTERVAL));
        }
        return controllerPollingInterval;
    }

    public static Boolean shouldSkipSslValidation() {
        if (skipSslValidation == null) {
            skipSslValidation = shouldSkipSslValidation(System.getenv(CFG_SKIP_SSL_VALIDATION));
        }
        return skipSslValidation;
    }

    public static Boolean areXsPlaceholdersSupported() {
        if (xsPlaceholdersSupported == null) {
            xsPlaceholdersSupported = areXsPlaceholdersSupported(System.getenv(CFG_XS_PLACEHOLDERS_SUPPORTED));
        }
        return xsPlaceholdersSupported;
    }

    public static String getVersion() {
        if (version == null) {
            version = getVersion(System.getenv(CFG_VERSION));
        }
        return version;
    }

    public static String getDeployServiceUrl() {
        if (deployServiceUrl == null) {
            deployServiceUrl = getDeployServiceUrl(System.getenv(CFG_VCAP_APPLICATION));
        }
        return deployServiceUrl;
    }

    public static Integer getChangeLogLockWaitTime() {
        if (changeLogLockWaitTime == null) {
            changeLogLockWaitTime = getChangeLogLockWaitTime(System.getenv(CFG_CHANGE_LOG_LOCK_WAIT_TIME));
        }
        return changeLogLockWaitTime;
    }

    public static Integer getChangeLogLockDuration() {
        if (changeLogLockDuration == null) {
            changeLogLockDuration = getChangeLogLockDuration(System.getenv(CFG_CHANGE_LOG_LOCK_DURATION));
        }
        return changeLogLockDuration;
    }

    public static Integer getChangeLogLockAttempts() {
        if (changeLogLockAttempts == null) {
            changeLogLockAttempts = getChangeLogLockAttempts(System.getenv(CFG_CHANGE_LOG_LOCK_ATTEMPTS));
        }
        return changeLogLockAttempts;
    }

    static PlatformType getPlatformType(String type) {
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

    static URL getTargetURL(String targetURL) {
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

    static DatabaseType getDatabaseType(String type) {
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

    static List<Platform> getPlatforms(String json) {
        return getPlatforms(json, new ConfigurationParser());
    }

    static List<Platform> getPlatforms(String json, ConfigurationParser parser) {
        try {
            if (json != null) {
                List<Platform> result = parser.parsePlatformsJson(json);
                LOGGER.info(format(Messages.PLATFORM_TYPES, JsonUtil.toJson(result, true)));
                return result;
            }
            LOGGER.warn(format(Messages.PLATFORM_TYPES_NOT_SPECIFIED, DEFAULT_PLATFORM_TYPES));
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_PLATFORMS, json, DEFAULT_PLATFORM_TYPES), e);
        }
        return DEFAULT_PLATFORM_TYPES;
    }

    static List<Target> getTargets(String json) {
        return getTargets(json, new ConfigurationParser());
    }

    static List<Target> getTargets(String json, ConfigurationParser parser) {
        try {
            if (json != null) {
                List<Target> result = parser.parseTargetsJson(json);
                LOGGER.info(format(Messages.PLATFORMS, new SecureSerializationFacade().toJson(result)));
                return result;
            }
            LOGGER.info(format(Messages.PLATFORMS_NOT_SPECIFIED, DEFAULT_PLATFORMS));
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_TARGETS, json, DEFAULT_PLATFORMS), e);
        }
        return DEFAULT_PLATFORMS;
    }

    static Long getMaxUploadSize(String value) {
        return getLong(value, DEFAULT_MAX_UPLOAD_SIZE, Messages.MAX_UPLOAD_SIZE, Messages.MAX_UPLOAD_SIZE_NOT_SPECIFIED,
            Messages.INVALID_MAX_UPLOAD_SIZE);
    }

    static Long getMaxMtaDescriptorSize(String value) {
        return getLong(value, DEFAULT_MAX_MTA_DESCRIPTOR_SIZE, Messages.MAX_MTA_DESCRIPTOR_SIZE,
            Messages.MAX_MTA_DESCRIPTOR_SIZE_NOT_SPECIFIED, Messages.INVALID_MAX_MTA_DESCRIPTOR_SIZE);
    }

    static Boolean shouldScanUploads(String value) {
        return getBoolean(value, DEFAULT_SCAN_UPLOADS, Messages.SCAN_UPLOADS);
    }

    static Boolean isUseXSAuditLogging(String value) {
        return getBoolean(value, DEFAULT_USE_XS_AUDIT_LOGGING, Messages.USE_XS_AUDIT_LOGGING);
    }

    static String getSpaceGuid(String json) {
        try {
            Map<String, Object> vcapApplication = JsonUtil.convertJsonToMap(json);
            Object spaceId = vcapApplication.get("space_id");
            if (spaceId != null) {
                LOGGER.info(format(Messages.SPACE_ID, spaceId));
                return spaceId.toString();
            }
            LOGGER.warn(format(Messages.SPACE_ID_NOT_SPECIFIED, DEFAULT_SPACE_ID));
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_VCAP_APPLICATION_SPACE_ID, json, DEFAULT_SPACE_ID), e);
        }
        return DEFAULT_SPACE_ID;
    }

    static Integer getRouterPort(String json) {
        try {
            Map<String, Object> vcapApplication = JsonUtil.convertJsonToMap(json);
            List<String> uris = getApplicationUris(vcapApplication);
            if (!CommonUtil.isNullOrEmpty(uris)) {
                Pair<String, String> portAndDomain = UriUtil.getHostAndDomain(uris.get(0));
                int port = Integer.parseInt(portAndDomain._1);
                if (UriUtil.isValidPort(port)) {
                    return port;
                }
            }
            LOGGER.info(format(Messages.NO_APPLICATION_URIS_SPECIFIED, DEFAULT_ROUTER_PORT));
        } catch (ParsingException | NumberFormatException e) {
            LOGGER.warn(format(Messages.INVALID_VCAP_APPLICATION_ROUTER_PORT, json, DEFAULT_ROUTER_PORT), e);
        }
        return DEFAULT_ROUTER_PORT;
    }

    static String getDeployServiceUrl(String json) {
        try {
            Map<String, Object> vcapApplication = JsonUtil.convertJsonToMap(json);
            List<String> uris = getApplicationUris(vcapApplication);
            if (!CommonUtil.isNullOrEmpty(uris)) {
                return uris.get(0);
            }
        } catch (ParsingException e) {
            LOGGER.warn(format(Messages.INVALID_VCAP_APPLICATION_DEPLOY_SERVICE_URI, json), e);
        }
        return null;
    }

    private static List<String> getApplicationUris(Map<String, Object> vcapApplication) {
        for (String urisKey : VCAP_APPLICATION_URIS_KEYS) {
            List<String> uris = CommonUtil.cast(vcapApplication.get(urisKey));
            if (!CommonUtil.isNullOrEmpty(uris)) {
                return uris;
            }
        }
        return null;
    }

    static Boolean areDummyTokensEnabled(String value) {
        return getBoolean(value, false, Messages.DUMMY_TOKENS_ENABLED);
    }

    static Boolean isBasicAuthEnabled(String value) {
        return getBoolean(value, false, Messages.BASIC_AUTH_ENABLED);
    }

    static String getAdminUsername(String value) {
        return getString(value, "", Messages.ADMIN_USERNAME);
    }

    static Integer getXsClientCoreThreads(String value) {
        return getPositiveInt(value, DEFAULT_XS_CLIENT_CORE_THREADS, Messages.XS_CLIENT_CORE_THREADS);
    }

    static Integer getXsClientMaxThreads(String value) {
        return getPositiveInt(value, DEFAULT_XS_CLIENT_MAX_THREADS, Messages.XS_CLIENT_MAX_THREADS);
    }

    static Integer getXsClientQueueCapacity(String value) {
        return getPositiveInt(value, DEFAULT_XS_CLIENT_QUEUE_CAPACITY, Messages.XS_CLIENT_QUEUE_CAPACITY);
    }

    static int getXsClientKeepAlive(String value) {
        return getPositiveInt(value, DEFAULT_XS_CLIENT_KEEP_ALIVE, Messages.XS_CLIENT_KEEP_ALIVE);
    }

    static Integer getAsyncExecutorCoreThreads(String value) {
        return getPositiveInt(value, DEFAULT_ASYNC_EXECUTOR_CORE_THREADS, Messages.ASYNC_EXECUTOR_CORE_THREADS);
    }

    static int getControllerPollingInterval(String value) {
        return getPositiveInt(value, DEFAULT_CONTROLLER_POLLING_INTERVAL, Messages.CONTROLLER_POLLING_INTERVAL);
    }

    static int getUploadAppTimeout(String value) {
        return getPositiveInt(value, DEFAULT_UPLOAD_APP_TIMEOUT, Messages.UPLOAD_APP_TIMEOUT);
    }

    static Boolean shouldSkipSslValidation(String value) {
        return getBoolean(value, false, Messages.SKIP_SSL_VALIDATION);
    }

    static Boolean areXsPlaceholdersSupported(String value) {
        boolean result = (value != null) && (!value.equals(SupportedParameters.XSA_CONTROLLER_ENDPOINT_PLACEHOLDER));
        LOGGER.info(format(Messages.XS_PLACEHOLDERS_SUPPORTED, result));
        return result;
    }

    static String getVersion(String version) {
        return getString(version, "N/A", Messages.DS_VERSION);
    }

    static Integer getChangeLogLockWaitTime(String value) {
        return getPositiveInt(value, DEFAULT_CHANGE_LOG_LOCK_WAIT_TIME, Messages.CHANGE_LOG_LOCK_WAIT_TIME);
    }

    static Integer getChangeLogLockDuration(String value) {
        return getPositiveInt(value, DEFAULT_CHANGE_LOG_LOCK_DURATION, Messages.CHANGE_LOG_LOCK_DURATION);
    }

    static Integer getChangeLogLockAttempts(String value) {
        return getPositiveInt(value, DEFAULT_CHANGE_LOG_LOCK_ATTEMPTS, Messages.CHANGE_LOG_LOCK_ATTEMPTS);
    }

    private static String getString(String value, String defaultValue, String message) {
        String result = (value != null) ? value : defaultValue;
        LOGGER.info(format(message, result));
        return result;
    }

    private static Integer getPositiveInt(String value, Integer defaultValue, String message) {
        Integer result = (value != null) ? Integer.valueOf(value) : defaultValue;
        if (result <= 0)
            result = Integer.MAX_VALUE;
        LOGGER.info(format(message, result));
        return result;
    }

    private static Boolean getBoolean(String value, Boolean defaultValue, String message) {
        Boolean result = (value != null) ? Boolean.valueOf(value) : defaultValue;
        LOGGER.info(format(message, result));
        return result;
    }

    private static Long getLong(String value, Long defaultValue, String message, String notSpecifiedMessage, String invalidMessage) {
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

    private static String getEnv(String env) {
        String value = System.getenv(env);
        if (value == null)
            LOGGER.warn(format(Messages.ENVIRONMENT_VARIABLE_NOT_SET, env));
        return value;
    }

    private static URL url(String s) {
        try {
            return MiscUtil.getURL(s);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int getUploadAppTimeout() {
        if (uploadAppTimeout == null) {
            uploadAppTimeout = getUploadAppTimeout(System.getenv(CFG_UPLOAD_APP_TIMEOUT));
        }
        return uploadAppTimeout;
    }

    public static String getGlobalConfigSpace() {
        if (globalConfigSpace == null) {
            globalConfigSpace = System.getenv(CFG_GLOBAL_CONFIG_SPACE);
        }
        return globalConfigSpace;
    }

    static Boolean shouldGatherUsageStatistics(String value) {
        return getBoolean(value, DEFAULT_STATISTICS_USAGE, Messages.GATHER_STATISTICS);
    }

    public static Boolean shouldGatherUsageStatistics() {
        if (gatherUsageStatistics == null) {
            gatherUsageStatistics = shouldGatherUsageStatistics(System.getenv(CFG_GATHER_USAGE_STATISTICS));
        }
        return gatherUsageStatistics;
    }

    public static CloudTarget createImplicitCloudTarget(String targetSpace) {

        if (targetSpace == null) {
            return null;
        }
        Pattern whitespacePattern = Pattern.compile("\\S+\\s+\\S+");
        Matcher matcher = whitespacePattern.matcher(targetSpace);

        if (!matcher.find()) {
            throw new ParsingException("Target does not contain 'org' and 'space' parameters");
        }

        String[] orgAndSpace = targetSpace.split("\\s+");
        CloudTarget cloudTarget = new CloudTarget(orgAndSpace[0], orgAndSpace[1]);

        return cloudTarget;
    }

    public static CloudTarget splitTargetSpaceValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return new CloudTarget("", "");
        }

        Pattern whitespacePattern = Pattern.compile("\\s+");
        Matcher matcher = whitespacePattern.matcher(value);
        if (!matcher.find()) {
            return new CloudTarget("", value);
        }

        String[] orgSpace = value.split("\\s+", 2);
        return new CloudTarget(orgSpace[0], orgSpace[1]);
    }
}
