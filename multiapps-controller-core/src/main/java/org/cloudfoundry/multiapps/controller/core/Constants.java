package org.cloudfoundry.multiapps.controller.core;

public class Constants {

    public static final String DEPENDENCY_TYPE_SOFT = "soft";
    public static final String DEPENDENCY_TYPE_HARD = "hard";

    public static final String UNIX_PATH_SEPARATOR = "/";
    public static final String MTA_ELEMENT_SEPARATOR = "/";
    public static final String MANIFEST_MTA_ENTITY_SEPARATOR = ",";
    public static final String NAMESPACE_SEPARATOR = "-";

    // Metadata attributes:
    public static final String ATTR_ID = "id";
    public static final String ATTR_NAMESPACE = "namespace";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_VERSION = "version";
    public static final String ATTR_DESCRIPTION = "description";

    // Deploy attributes:
    public static final String ATTR_APP_CONTENT_DIGEST = "app-content-digest";

    // Metadata environment variables:
    public static final String ENV_DEPLOY_ATTRIBUTES = "DEPLOY_ATTRIBUTES";
    public static final String ENV_DEPLOY_ID = "DEPLOY_ID";

    // Variables
    public static final String ATTR_CORRELATION_ID = "correlation_id";

    public static final String B3_TRACE_ID_HEADER = "X-B3-TraceId";
    public static final String B3_SPAN_ID_HEADER = "X-B3-SpanId";
    public static final String CIPHER_TRANSFORMATION_NAME = "AES/GCM/NoPadding";
    public static final String ENCRYPTION_DECRYPTION_ALGORITHM_NAME = "AES";

    public static final int TOKEN_SERVICE_DELETION_CORE_POOL_SIZE = 1;
    public static final int TOKEN_SERVICE_DELETION_MAXIMUM_POOL_SIZE = 3;
    public static final int TOKEN_SERVICE_DELETION_KEEP_ALIVE_THREAD_IN_SECONDS = 30;
    //The Initialisation Vector (also called nonce) is 12-bytes (96 bits), because that is the standard and recommended length for AES-GCM primarily for performance and simplicity of the implementation -
    //this is the exact length that balances a sufficiently large uniqueness space with maximum computational efficiency according to the NIST specifications for the GCM variant of AES
    public static final int INITIALISATION_VECTOR_LENGTH = 12;
    public static final int INITIALISATION_VECTOR_POSITION = 12;
    //The authentication tag in AES-GCM is always 128 bits because every version of the AES algorithm (including AES-256) processes data in fixed 128-bit blocks,
    //which dictates the size of the final authentication result required by the GCM mode's internals
    public static final int GCM_AUTHENTICATION_TAG_LENGTH = 128;

    public static final String APP_FEATURE_SSH = "ssh";

    protected Constants() {
    }
}
