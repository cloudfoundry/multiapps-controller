package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication.ProductizationState;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorBackupService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

public class ExistingAppsToBackupCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingAppsToBackupCalculator.class);

    private final DeployedMta deployedMta;
    private final DeployedMta backupMta;
    private final DescriptorBackupService descriptorBackupService;

    public ExistingAppsToBackupCalculator(DeployedMta deployedMta, DeployedMta backupMta, DescriptorBackupService descriptorBackupService) {
        this.deployedMta = deployedMta;
        this.backupMta = backupMta;
        this.descriptorBackupService = descriptorBackupService;
    }

    public List<CloudApplication> calculateExistingAppsToBackup(List<CloudApplication> appsToUndeploy,
                                                                String mtaVersionOfCurrentDescriptor) {
        if (doesDeployedMtaVersionMatchToCurrentDeployment(deployedMta, mtaVersionOfCurrentDescriptor)) {
            return Collections.emptyList();
        }

        if (doesDeployedMtaVersionMatchToCurrentDeployment(backupMta, mtaVersionOfCurrentDescriptor)) {
            return Collections.emptyList();
        }

        return getAppsWithLiveProductizationState(appsToUndeploy);
    }

    private boolean doesDeployedMtaVersionMatchToCurrentDeployment(DeployedMta detectedMta, String mtaVersionOfCurrentDescriptor) {
        return detectedMta != null && detectedMta.getApplications()
                                                 .stream()
                                                 .allMatch(deployedApplication -> doesMtaVersionMatchToCurrentDeployment(deployedApplication,
                                                                                                                         mtaVersionOfCurrentDescriptor));
    }

    private boolean doesMtaVersionMatchToCurrentDeployment(DeployedMtaApplication deployedApplication,
                                                           String mtaVersionOfCurrentDescriptor) {
        String mtaVersionOfDeployedApplication = deployedApplication.getV3Metadata()
                                                                    .getAnnotations()
                                                                    .get(MtaMetadataAnnotations.MTA_VERSION);

        return mtaVersionOfDeployedApplication != null && mtaVersionOfDeployedApplication.equals(mtaVersionOfCurrentDescriptor);
    }

    private ProductizationState getProductizationStateOfApplication(CloudApplication appToUndeploy) {
        return deployedMta.getApplications()
                          .stream()
                          .filter(deployedMtaApp -> deployedMtaApp.getName()
                                                                  .equals(appToUndeploy.getName()))
                          .map(DeployedMtaApplication::getProductizationState)
                          .findFirst()
                          .get();
    }

    private List<CloudApplication> getAppsWithLiveProductizationState(List<CloudApplication> appsToUndeploy) {
        List<CloudApplication> appsToBackup = new ArrayList<>();
        for (CloudApplication appToUndeploy : appsToUndeploy) {
            ProductizationState productizationStateOfDeployedApplication = getProductizationStateOfApplication(appToUndeploy);
            if (productizationStateOfDeployedApplication == ProductizationState.LIVE) {
                appsToBackup.add(appToUndeploy);
            }
        }
        LOGGER.info(MessageFormat.format(Messages.EXISTING_APPS_TO_BACKUP, SecureSerialization.toJson(appsToBackup)));
        return appsToBackup;
    }

    public List<CloudApplication> calculateAppsToUndeploy(ProcessContext context, List<CloudApplication> existingAppsToBackup) {
        if (backupMta == null) {
            return Collections.emptyList();
        }

        List<CloudApplication> appsToUndeploy = new ArrayList<>();
        Optional<String> optionalBackupMtaVersion = backupMta.getApplications()
                                                             .stream()
                                                             .filter(backupApp -> backupApp.getV3Metadata()
                                                                                           .getAnnotations()
                                                                                           .containsKey(MtaMetadataAnnotations.MTA_VERSION))
                                                             .map(backupApp -> backupApp.getV3Metadata()
                                                                                        .getAnnotations()
                                                                                        .get(MtaMetadataAnnotations.MTA_VERSION))
                                                             .filter(Objects::nonNull)
                                                             .findFirst();

        if (optionalBackupMtaVersion.isPresent()) {
            List<BackupDescriptor> backupDescriptors = descriptorBackupService.createQuery()
                                                                              .mtaId(context.getVariable(Variables.MTA_ID))
                                                                              .spaceId(context.getVariable(Variables.SPACE_GUID))
                                                                              .namespace(context.getVariable(Variables.MTA_NAMESPACE))
                                                                              .mtaVersion(optionalBackupMtaVersion.get())
                                                                              .list();
            if (backupDescriptors.isEmpty()) {
                appsToUndeploy.addAll(backupMta.getApplications());
                return appsToUndeploy;
            }
        }

        if (existingAppsToBackup.isEmpty()) {
            return appsToUndeploy;
        }

        List<String> appsToBackupNames = existingAppsToBackup.stream()
                                                             .map(CloudApplication::getName)
                                                             .collect(Collectors.toList());

        List<DeployedMtaApplication> deployedAppsToUndeploy = backupMta.getApplications()
                                                                       .stream()
                                                                       .filter(application -> !appsToBackupNames.contains(application.getName()))
                                                                       .collect(Collectors.toList());
        appsToUndeploy.addAll(deployedAppsToUndeploy);
        return appsToUndeploy;
    }

}
