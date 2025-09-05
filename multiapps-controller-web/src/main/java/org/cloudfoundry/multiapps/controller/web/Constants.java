package org.cloudfoundry.multiapps.controller.web;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import static org.cloudfoundry.multiapps.controller.persistence.Constants.VARIABLE_NAME_SERVICE_ID;

public class Constants {

    private Constants() {
    }

    public static final String RATE_LIMIT = "X-RateLimit-Limit";
    public static final String RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String RATE_LIMIT_RESET = "X-RateLimit-Reset";
    public static final String CSRF_TOKEN = "X-CSRF-TOKEN";
    public static final String CSRF_PARAM_NAME = "X-CSRF-PARAM";
    public static final String CSRF_HEADER_NAME = "X-CSRF-HEADER";
    public static final String CONTENT_LENGTH = "Content-Length";

    public static final long OAUTH_TOKEN_RETENTION_TIME_IN_SECONDS = TimeUnit.MINUTES.toSeconds(2);
    public static final long BASIC_TOKEN_RETENTION_TIME_IN_SECONDS = TimeUnit.MINUTES.toSeconds(6);

    // Object Store
    public static final String ACCESS_KEY_ID = "access_key_id";
    public static final String SECRET_ACCESS_KEY = "secret_access_key";
    public static final String BUCKET = "bucket";
    public static final String REGION = "region";
    public static final String ENDPOINT = "endpoint";
    public static final String ENDPOINT_URL = "endpoint-url";
    public static final String ACCOUNT_NAME = "account_name";
    public static final String SAS_TOKEN = "sas_token";
    public static final String CONTAINER_NAME = "container_name";
    public static final String CONTAINER_NAME_WITH_DASH = "container-name";
    public static final String CONTAINER_URI = "container_uri";
    public static final String BASE_64_ENCODED_PRIVATE_KEY_DATA = "base64EncodedPrivateKeyData";
    public static final String HOST = "host";

    public static final String AWS_S_3 = "aws-s3";
    public static final String AZUREBLOB = "azureblob";
    public static final String ALIYUN_OSS = "aliyun-oss";
    public static final String GOOGLE_CLOUD_STORAGE = "google-cloud-storage";

    public static final String AWS = "aws";
    public static final String AZURE = "azure";
    public static final String GCP = "gcp";
    public static final String ALIBABA = "ali";

    public static final String RETRY_LIMIT_PROPERTY = "jdk.httpclient.auth.retrylimit";

    public static class Resources {

        private Resources() {
        }

        public static final String APPLICATION_HEALTH = "/public/application-health";
        public static final String APPLICATION_SHUTDOWN = "/rest/admin/shutdown";
        public static final String CONFIGURATION_ENTRIES = "/rest/configuration-entries";
        public static final String CSRF_TOKEN = "/rest/csrf-token";
        public static final String HEALTH_CHECK = "/public/health";
        public static final String PING = "/public/ping";
    }

    public static class Endpoints {

        private Endpoints() {
        }

        public static final String PURGE = "/purge";
    }

    public static final Set<String> NAMES_OF_SERVICE_PARAMETERS = Set.of(
        VARIABLE_NAME_SERVICE_ID, Variables.USER.getName(),
        Variables.USER_GUID.getName(), Variables.SPACE_NAME.getName(), Variables.SPACE_GUID.getName(),
        Variables.ORGANIZATION_NAME.getName(), Variables.ORGANIZATION_GUID.getName(), Variables.TIMESTAMP.getName(),
        Variables.MTA_NAMESPACE.getName()
    );

    public static final Map<String, String> ENV_TO_OS_PROVIDER = Map.of(Constants.AWS, Constants.AWS_S_3, Constants.AZURE,
                                                                        Constants.AZUREBLOB, Constants.GCP, Constants.GOOGLE_CLOUD_STORAGE,
                                                                        Constants.ALIBABA, Constants.ALIYUN_OSS
    );

}
