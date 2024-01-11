package org.cloudfoundry.multiapps.controller.web;

import java.util.concurrent.TimeUnit;

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
    public static final String ACCOUNT_NAME = "account_name";
    public static final String SAS_TOKEN = "sas_token";
    public static final String CONTAINER_NAME = "container_name";
    public static final String CONTAINER_URI = "container_uri";
    public static final String BASE_64_ENCODED_PRIVATE_KEY_DATA = "base64EncodedPrivateKeyData";

    public static final String AWS_S_3 = "aws-s3";
    public static final String AZUREBLOB = "azureblob";
    public static final String ALIYUN_OSS = "aliyun-oss";
    public static final String GOOGLE_CLOUD_STORAGE_CUSTOM = "google-cloud-storage-custom";
}
