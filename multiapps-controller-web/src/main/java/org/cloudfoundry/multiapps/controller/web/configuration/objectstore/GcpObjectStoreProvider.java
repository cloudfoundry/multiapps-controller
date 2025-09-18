package org.cloudfoundry.multiapps.controller.web.configuration.objectstore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.cloudfoundry.multiapps.controller.persistence.dto.ObjectStoreConfiguration;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcpObjectStoreProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpObjectStoreProvider.class);

    public static ObjectStoreConfiguration createObjectStoreStorage(Map<String, Object> credentials) {
        String bucketName = (String) credentials.get(Constants.BUCKET);
        Storage storage = StorageOptions.newBuilder()
                                        .setCredentials(getGcpCredentialsSupplier(credentials))
                                        .build()
                                        .getService();
        return new ObjectStoreConfiguration(bucketName, storage);
    }

    private static Credentials getGcpCredentialsSupplier(Map<String, Object> credentials) {
        if (!credentials.containsKey(Constants.BASE_64_ENCODED_PRIVATE_KEY_DATA)) {
            return null;
        }
        byte[] decodedKey = Base64.getDecoder()
                                  .decode((String) credentials.get(Constants.BASE_64_ENCODED_PRIVATE_KEY_DATA));
        try {
            return GoogleCredentials.fromStream(new ByteArrayInputStream(decodedKey));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
