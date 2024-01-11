package org.cloudfoundry.multiapps.controller.web.configuration.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.jclouds.domain.Credentials;
import org.jclouds.googlecloud.GoogleCredentialsFromJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;

import io.pivotal.cfenv.core.CfService;

public class ObjectStoreServiceInfoCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectStoreServiceInfoCreator.class);

    public ObjectStoreServiceInfo createServiceInfo(CfService service) {
        Map<String, Object> credentials = service.getCredentials()
                                                 .getMap();
        if (isAliCloudIaas(credentials)) {
            LOGGER.info(Messages.CREATING_OBJECT_STORE_FOR_ALI_CLOUD);
            return createServiceInfoForAliCloud(credentials);
        }
        if (isAwsIaas(credentials)) {
            LOGGER.info(Messages.CREATING_OBJECT_STORE_FOR_AWS);
            return createServiceInfoForAws(credentials);
        }
        if (isAzureIaas(credentials)) {
            LOGGER.info(Messages.CREATING_OBJECT_STORE_FOR_AZURE);
            return createServiceInfoForAzure(credentials);
        }
        if (isGcpIaas(credentials)) {
            LOGGER.info(Messages.CREATING_OBJECT_STORE_FOR_GCP);
            return createServiceInfoForGcpCloud(credentials);
        }
        throw new IllegalStateException(Messages.ERROR_BUILDING_OBJECT_STORE_CONFIGURATION);
    }

    private boolean isAwsIaas(Map<String, Object> credentials) {
        // @formatter:off
        return credentials.containsKey(Constants.ACCESS_KEY_ID) &&
                credentials.containsKey(Constants.SECRET_ACCESS_KEY) &&
                credentials.containsKey(Constants.BUCKET);
        // @formatter:on
    }

    private ObjectStoreServiceInfo createServiceInfoForAws(Map<String, Object> credentials) {
        String accessKeyId = (String) credentials.get(Constants.ACCESS_KEY_ID);
        String secretAccessKey = (String) credentials.get(Constants.SECRET_ACCESS_KEY);
        String bucket = (String) credentials.get(Constants.BUCKET);
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.AWS_S_3)
                                              .identity(accessKeyId)
                                              .credential(secretAccessKey)
                                              .container(bucket)
                                              .build();
    }

    private boolean isAzureIaas(Map<String, Object> credentials) {
        // @formatter:off
        return credentials.containsKey(Constants.ACCOUNT_NAME) &&
                credentials.containsKey(Constants.SAS_TOKEN) &&
                credentials.containsKey(Constants.CONTAINER_NAME) &&
                credentials.containsKey(Constants.CONTAINER_URI);
        // @formatter:on
    }

    private ObjectStoreServiceInfo createServiceInfoForAzure(Map<String, Object> credentials) {
        String accountName = (String) credentials.get(Constants.ACCOUNT_NAME);
        String sasToken = (String) credentials.get(Constants.SAS_TOKEN);
        String containerName = (String) credentials.get(Constants.CONTAINER_NAME);
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.AZUREBLOB)
                                              .identity(accountName)
                                              .credential(sasToken)
                                              .endpoint(getContainerUriEndpoint(credentials).toString())
                                              .container(containerName)
                                              .build();
    }

    private URL getContainerUriEndpoint(Map<String, Object> credentials) {
        try {
            URL containerUri = new URL((String) credentials.get(Constants.CONTAINER_URI));
            return new URL(containerUri.getProtocol(), containerUri.getHost(), containerUri.getPort(), "");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(Messages.CANNOT_PARSE_CONTAINER_URI_OF_OBJECT_STORE, e);
        }
    }

    private boolean isAliCloudIaas(Map<String, Object> credentials) {
        // @formatter:off
        return credentials.containsKey(Constants.ACCESS_KEY_ID) &&
                credentials.containsKey(Constants.SECRET_ACCESS_KEY) &&
                credentials.containsKey(Constants.BUCKET) &&
                credentials.containsKey(Constants.REGION) &&
                credentials.containsKey(Constants.ENDPOINT);
        // @formatter:on
    }

    private ObjectStoreServiceInfo createServiceInfoForAliCloud(Map<String, Object> credentials) {
        String accessKeyId = (String) credentials.get(Constants.ACCESS_KEY_ID);
        String secretAccessKey = (String) credentials.get(Constants.SECRET_ACCESS_KEY);
        String bucket = (String) credentials.get(Constants.BUCKET);
        String region = (String) credentials.get(Constants.REGION);
        String endpoint = (String) credentials.get(Constants.ENDPOINT);
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.ALIYUN_OSS)
                                              .identity(accessKeyId)
                                              .credential(secretAccessKey)
                                              .container(bucket)
                                              .endpoint(endpoint)
                                              .region(region)
                                              .build();
    }

    private boolean isGcpIaas(Map<String, Object> credentials) {
        // @formatter:off
        return credentials.containsKey(Constants.BUCKET) &&
                credentials.containsKey(Constants.REGION) &&
                credentials.containsKey(Constants.BASE_64_ENCODED_PRIVATE_KEY_DATA);
        // @formatter:on
    }

    private ObjectStoreServiceInfo createServiceInfoForGcpCloud(Map<String, Object> credentials) {
        String bucket = (String) credentials.get(Constants.BUCKET);
        String region = (String) credentials.get(Constants.REGION);
        byte[] decodedKey = Base64.getDecoder()
                                  .decode((String) credentials.get(Constants.BASE_64_ENCODED_PRIVATE_KEY_DATA));
        String decodedCredential = new String(decodedKey, StandardCharsets.UTF_8);
        Supplier<Credentials> credentialsSupplier = new GoogleCredentialsFromJson(decodedCredential);
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.GOOGLE_CLOUD_STORAGE_CUSTOM)
                                              .credentialsSupplier(credentialsSupplier)
                                              .container(bucket)
                                              .region(region)
                                              .build();
    }

}
