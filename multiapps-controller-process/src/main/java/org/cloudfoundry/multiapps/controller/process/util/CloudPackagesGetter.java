package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudPackage;
import org.cloudfoundry.client.lib.domain.DropletInfo;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

@Named
public class CloudPackagesGetter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudPackagesGetter.class);

    public Optional<CloudPackage> getLatestUnusedPackage(CloudControllerClient client, UUID applicationGuid) {
        Optional<DropletInfo> currentDropletForApplication = findOrReturnEmpty(() -> client.getCurrentDropletForApplication(applicationGuid));
        if (currentDropletForApplication.isEmpty()) {
            return getMostRecentCloudPackage(client, applicationGuid);
        }

        Optional<CloudPackage> currentCloudPackage = findOrReturnEmpty(() -> client.getPackage(currentDropletForApplication.get()
                                                                                                                           .getPackageGuid()));
        if (currentCloudPackage.isEmpty()) {
            return Optional.empty();
        }
        LOGGER.info(MessageFormat.format(Messages.CURRENTLY_USED_PACKAGE_0, SecureSerialization.toJson(currentCloudPackage.get())));
        Optional<CloudPackage> mostRecentApplicationPackage = getMostRecentCloudPackage(client, applicationGuid);
        if (mostRecentApplicationPackage.isEmpty()) {
            return Optional.empty();
        }

        if (!isCurrentCloudPackageUpToDate(currentCloudPackage.get(), mostRecentApplicationPackage.get())) {
            return mostRecentApplicationPackage;
        }
        return Optional.empty();
    }

    private <T> Optional<T> findOrReturnEmpty(Supplier<T> supplier) {
        try {
            return Optional.of(supplier.get());
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.warn(e.getMessage(), e);
                return Optional.empty();
            }
            throw e;
        }
    }

    private Optional<CloudPackage> getMostRecentCloudPackage(CloudControllerClient client, UUID applicationGuid) {
        List<CloudPackage> cloudPackages = client.getPackagesForApplication(applicationGuid);
        LOGGER.info(MessageFormat.format(Messages.PACKAGES_FOR_APPLICATION_0_ARE_1, applicationGuid,
                                         SecureSerialization.toJson(cloudPackages)));
        return cloudPackages.stream()
                            .max(Comparator.comparing(cloudPackage -> cloudPackage.getMetadata()
                                                                                  .getCreatedAt()));
    }

    private boolean isCurrentCloudPackageUpToDate(CloudPackage currentCloudPackage, CloudPackage mostRecentCloudPackage) {
        return Objects.equals(currentCloudPackage.getGuid(), mostRecentCloudPackage.getGuid());
    }
}
