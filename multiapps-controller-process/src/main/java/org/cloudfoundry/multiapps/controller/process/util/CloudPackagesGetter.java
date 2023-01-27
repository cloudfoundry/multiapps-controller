package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.DropletInfo;

@Named
public class CloudPackagesGetter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudPackagesGetter.class);

    public Optional<CloudPackage> getAppPackage(CloudControllerClient client, UUID applicationGuid) {
        Optional<DropletInfo> currentDropletForApplication = findOrReturnEmpty(() -> client.getCurrentDropletForApplication(applicationGuid));
        if (currentDropletForApplication.isEmpty()) {
            return getMostRecentAppPackage(client, applicationGuid);
        }
        Optional<CloudPackage> currentCloudPackage = findOrReturnEmpty(() -> client.getPackage(currentDropletForApplication.get()
                                                                                                                           .getPackageGuid()));
        if (currentCloudPackage.isEmpty()) {
            return Optional.empty();
        }
        LOGGER.info(MessageFormat.format(Messages.CURRENTLY_USED_PACKAGE_0, SecureSerialization.toJson(currentCloudPackage.get())));
        return currentCloudPackage;
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

    public Optional<CloudPackage> getMostRecentAppPackage(CloudControllerClient client, UUID applicationGuid) {
        List<CloudPackage> cloudPackages = client.getPackagesForApplication(applicationGuid);
        LOGGER.info(MessageFormat.format(Messages.PACKAGES_FOR_APPLICATION_0_ARE_1, applicationGuid,
                                         SecureSerialization.toJson(cloudPackages)));
        return cloudPackages.stream()
                            .max(Comparator.comparing(cloudPackage -> cloudPackage.getMetadata()
                                                                                  .getCreatedAt()));
    }

}
