package org.cloudfoundry.multiapps.controller.web.configuration.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.jclouds.domain.Credentials;
import org.jclouds.googlecloud.GoogleCredentialsFromJson;

import com.google.common.base.Supplier;

import io.pivotal.cfenv.core.CfService;

public class ObjectStoreServiceInfoCreator {

    public List<ObjectStoreServiceInfo> getAllProvidersServiceInfo(CfService service) {
        Map<String, Object> credentials = service.getCredentials()
                                                 .getMap();
        return List.of(createServiceInfoForAws(credentials), createServiceInfoForAliCloud(credentials),
                       createServiceInfoForAzure(credentials), createServiceInfoForGcpCloud(credentials));
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

    private ObjectStoreServiceInfo createServiceInfoForAzure(Map<String, Object> credentials) {
        String accountName = (String) credentials.get(Constants.ACCOUNT_NAME);
        String sasToken = (String) credentials.get(Constants.SAS_TOKEN);
        String containerName = (String) credentials.get(Constants.CONTAINER_NAME);
        URL containerUrl = getContainerUriEndpoint(credentials);
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.AZUREBLOB)
                                              .identity(accountName)
                                              .credential(sasToken)
                                              .endpoint(containerUrl == null ? null : containerUrl.toString())
                                              .container(containerName)
                                              .build();
    }

    private URL getContainerUriEndpoint(Map<String, Object> credentials) {
        if (!credentials.containsKey(Constants.CONTAINER_URI)) {
            return null;
        }
        try {
            URL containerUri = new URL((String) credentials.get(Constants.CONTAINER_URI));
            return new URL(containerUri.getProtocol(), containerUri.getHost(), containerUri.getPort(), "");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(Messages.CANNOT_PARSE_CONTAINER_URI_OF_OBJECT_STORE, e);
        }
    }

    private ObjectStoreServiceInfo createServiceInfoForGcpCloud(Map<String, Object> credentials) {
        String bucket = (String) credentials.get(Constants.BUCKET);
        String region = (String) credentials.get(Constants.REGION);
        Supplier<Credentials> credentialsSupplier = getGcpCredentialsSupplier(credentials);
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.GOOGLE_CLOUD_STORAGE_CUSTOM)
                                              .credentialsSupplier(credentialsSupplier)
                                              .container(bucket)
                                              .region(region)
                                              .build();
    }

    protected Supplier<Credentials> getGcpCredentialsSupplier(Map<String, Object> credentials) {
        if (!credentials.containsKey(Constants.BASE_64_ENCODED_PRIVATE_KEY_DATA)) {
            return () -> null;
        }
        byte[] decodedKey = Base64.getDecoder()
                                  .decode((String) credentials.get(Constants.BASE_64_ENCODED_PRIVATE_KEY_DATA));
        String decodedCredential = new String(decodedKey, StandardCharsets.UTF_8);
        return new GoogleCredentialsFromJson(decodedCredential);
    }

}
