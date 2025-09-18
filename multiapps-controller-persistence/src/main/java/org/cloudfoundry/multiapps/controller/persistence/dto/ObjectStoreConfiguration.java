package org.cloudfoundry.multiapps.controller.persistence.dto;

import com.google.cloud.storage.Storage;

public record ObjectStoreConfiguration(String bucketName, Storage storage) {
}
