package org.cloudfoundry.multiapps.controller.client.facade;

public class IntegrationTestConstants {

    private IntegrationTestConstants() {
    }

    public static final String HEALTH_CHECK_ENDPOINT = "/public/ping";
    public static final String JAVA_BUILDPACK = "java_buildpack";
    public static final String NODEJS_BUILDPACK = "nodejs_buildpack";
    public static final String STATICFILE_BUILDPACK = "staticfile_buildpack";
    public static final int HEALTH_CHECK_TIMEMOUT = 100;
    public static final int DISK_IN_MB = 128;
    public static final int MEMORY_IN_MB = 128;
    public static final String DEFAULT_DOMAIN = "deploy-service.custom.domain.for.integration.tests";
    public static final String APPLICATION_HOST = "test-application-hostname-ztana-test";
    public static final String STATICFILE_APPLICATION_CONTENT = "staticfile.zip";
    public static final String JAVA_BUILDPACK_URL = "https://github.com/paketo-buildpacks/java";
    public static final String NODEJS_BUILDPACK_URL = "https://github.com/paketo-buildpacks/nodejs";

    // Service broker constants
    public static final int SERVICE_BROKER_DISK_IN_MB = 256;
    public static final int SERVICE_BROKER_MEMORY_IN_MB = 1024;
    public static final String SERVICE_BROKER_HOST = "test-service-foo-broker";
    public static final String SERVICE_BROKER_APP_NAME = "test-service-broker-app";
    public static final String SERVICE_BROKER_NAME = "test-service-broker";
    public static final String SERVICE_BROKER_ENV_CONTENT = "service-broker-env.json";
    public static final String SERVICE_BROKER_USERNAME = "test-user";
    public static final String SERVICE_BROKER_PASSWORD = "test-password";
    public static final String SERVICE_OFFERING = "finch";
    public static final String SERVICE_PLAN = "grey";
    public static final String SERVICE_PLAN_2 = "white";
}
