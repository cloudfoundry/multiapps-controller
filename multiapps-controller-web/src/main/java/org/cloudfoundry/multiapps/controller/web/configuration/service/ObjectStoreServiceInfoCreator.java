package org.cloudfoundry.multiapps.controller.web.configuration.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.web.Messages;
import org.jclouds.domain.Credentials;
import org.jclouds.googlecloud.GoogleCredentialsFromJson;

import com.google.common.base.Supplier;

import io.pivotal.cfenv.core.CfService;

public class ObjectStoreServiceInfoCreator {

    private static final String OBJECT_STORE_AWS_PLAN = "s3-standard";
    private static final String OBJECT_STORE_AZURE_PLAN = "azure-standard";
    private static final String OBJECT_STORE_ALICLOUD_PLAN = "oss-standard";
    private static final String OBJECT_STORE_GCP_PLAN = "gcs-standard";

    public ObjectStoreServiceInfo createServiceInfo(CfService service) {
        String plan = service.getPlan();
        Map<String, Object> credentials = service.getCredentials()
                                                 .getMap();
        switch (plan) {
            case OBJECT_STORE_AWS_PLAN:
                return createServiceInfoForAws(credentials);
            case OBJECT_STORE_AZURE_PLAN:
                return createServiceInfoForAzure(credentials);
            case OBJECT_STORE_ALICLOUD_PLAN:
                return createServiceInfoForAliCloud(credentials);
            case OBJECT_STORE_GCP_PLAN:
                return createServiceInfoForGcpCloud(credentials);
            default:
                throw new IllegalStateException(Messages.UNSUPPORTED_SERVICE_PLAN_FOR_OBJECT_STORE);
        }
    }

    private ObjectStoreServiceInfo createServiceInfoForAws(Map<String, Object> credentials) {
        String accessKeyId = (String) credentials.get("access_key_id");
        String secretAccessKey = (String) credentials.get("secret_access_key");
        String bucket = (String) credentials.get("bucket");
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider("aws-s3")
                                              .identity(accessKeyId)
                                              .credential(secretAccessKey)
                                              .container(bucket)
                                              .build();
    }

    private ObjectStoreServiceInfo createServiceInfoForAzure(Map<String, Object> credentials) {
        String accountName = (String) credentials.get("account_name");
        String sasToken = (String) credentials.get("sas_token");
        String containerName = (String) credentials.get("container_name");
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider("azureblob")
                                              .identity(accountName)
                                              .credential(sasToken)
                                              .endpoint(getContainerUriEndpoint(credentials).toString())
                                              .container(containerName)
                                              .build();
    }

    private URL getContainerUriEndpoint(Map<String, Object> credentials) {
        try {
            URL containerUri = new URL((String) credentials.get("container_uri"));
            return new URL(containerUri.getProtocol(), containerUri.getHost(), containerUri.getPort(), "");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(Messages.CANNOT_PARSE_CONTAINER_URI_OF_OBJECT_STORE, e);
        }
    }

    private ObjectStoreServiceInfo createServiceInfoForAliCloud(Map<String, Object> credentials) {
        String accessKeyId = (String) credentials.get("access_key_id");
        String secretAccessKey = (String) credentials.get("secret_access_key");
        String bucket = (String) credentials.get("bucket");
        String region = (String) credentials.get("region");
        String endpoint = (String) credentials.get("endpoint");
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider("aliyun-oss")
                                              .identity(accessKeyId)
                                              .credential(secretAccessKey)
                                              .container(bucket)
                                              .endpoint(endpoint)
                                              .region(region)
                                              .build();
    }

    private ObjectStoreServiceInfo createServiceInfoForGcpCloud(Map<String, Object> credentials) {
        String bucket = (String) credentials.get("bucket");
        String region = (String) credentials.get("region");
        byte[] decodedKey = Base64.getDecoder()
                                  .decode((String) credentials.get("base64EncodedPrivateKeyData"));
        String decodedCredential = new String(decodedKey, StandardCharsets.UTF_8);
        Supplier<Credentials> credentialsSupplier = new GoogleCredentialsFromJson(decodedCredential);
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider("google-cloud-storage-custom")
                                              .credentialsSupplier(credentialsSupplier)
                                              .container(bucket)
                                              .region(region)
                                              .build();
    }

}
