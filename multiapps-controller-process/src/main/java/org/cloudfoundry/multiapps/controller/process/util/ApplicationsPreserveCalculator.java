package org.cloudfoundry.multiapps.controller.process.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication.ProductizationState;
import org.cloudfoundry.multiapps.controller.persistence.dto.PreservedDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorPreserverService;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

public class ApplicationsPreserveCalculator {

    private final DeployedMta deployedMta;
    private final DeployedMta preservedMta;
    private final DescriptorPreserverService descriptorPreserverService;

    public ApplicationsPreserveCalculator(DeployedMta deployedMta, DeployedMta preservedMta,
                                          DescriptorPreserverService descriptorPreserverService) {
        this.deployedMta = deployedMta;
        this.preservedMta = preservedMta;
        this.descriptorPreserverService = descriptorPreserverService;
    }

    public List<CloudApplication> calculateAppsToPreserve(List<CloudApplication> appsToUndeploy, String checksumOfCurrentDescriptor) {
        if (doesDeployedMtaChecksumMatchToCurrentDeployment(deployedMta, checksumOfCurrentDescriptor)) {
            return Collections.emptyList();
        }

        if (doesDeployedMtaChecksumMatchToCurrentDeployment(preservedMta, checksumOfCurrentDescriptor)) {
            return Collections.emptyList();
        }

        return getAppsWithLiveProductizationState(appsToUndeploy);
    }

    private boolean doesDeployedMtaChecksumMatchToCurrentDeployment(DeployedMta detectedMta, String checksumOfCurrentDescriptor) {
        return detectedMta != null && detectedMta.getApplications()
                                                 .stream()
                                                 .allMatch(deployedApplication -> doesApplicationChecksumMatchToCurrentDeployment(deployedApplication,
                                                                                                                                  checksumOfCurrentDescriptor));
    }

    private boolean doesApplicationChecksumMatchToCurrentDeployment(DeployedMtaApplication deployedApplication,
                                                                    String checksumOfCurrentDescriptor) {
        String checksumOfDeployedApplication = deployedApplication.getV3Metadata()
                                                                  .getLabels()
                                                                  .get(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM);

        return checksumOfDeployedApplication != null && checksumOfDeployedApplication.equals(checksumOfCurrentDescriptor);
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
        List<CloudApplication> appsToPreserve = new ArrayList<>();
        for (CloudApplication appToUndeploy : appsToUndeploy) {
            ProductizationState productizationStateOfDeployedApplication = getProductizationStateOfApplication(appToUndeploy);
            if (productizationStateOfDeployedApplication == ProductizationState.LIVE) {
                appsToPreserve.add(appToUndeploy);
            }
        }
        return appsToPreserve;
    }

    public List<CloudApplication> calculateAppsToUndeploy(ProcessContext context, List<CloudApplication> appsToPreserve) {
        if (preservedMta == null) {
            return Collections.emptyList();
        }

        List<CloudApplication> appsToUndeploy = new ArrayList<>();
        Optional<String> optionalPreservedMtaDescriptorChecksum = preservedMta.getApplications()
                                                                              .stream()
                                                                              .filter(preservedApp -> preservedApp.getV3Metadata()
                                                                                                                  .getLabels()
                                                                                                                  .containsKey(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM))
                                                                              .map(preservedApp -> preservedApp.getV3Metadata()
                                                                                                               .getLabels()
                                                                                                               .get(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM))
                                                                              .filter(Objects::nonNull)
                                                                              .findFirst();

        if (optionalPreservedMtaDescriptorChecksum.isPresent()) {
            List<PreservedDescriptor> preservedDescriptors = descriptorPreserverService.createQuery()
                                                                                       .mtaId(context.getVariable(Variables.MTA_ID))
                                                                                       .spaceId(context.getVariable(Variables.SPACE_GUID))
                                                                                       .namespace(context.getVariable(Variables.MTA_NAMESPACE))
                                                                                       .checksum(optionalPreservedMtaDescriptorChecksum.get())
                                                                                       .list();
            if (preservedDescriptors.isEmpty()) {
                appsToUndeploy.addAll(preservedMta.getApplications());
                return appsToUndeploy;
            }
        }

        if (appsToPreserve.isEmpty()) {
            return appsToUndeploy;
        }

        List<String> appsToPreserveNames = appsToPreserve.stream()
                                                         .map(CloudApplication::getName)
                                                         .collect(Collectors.toList());

        List<DeployedMtaApplication> deployedAppsToUndeploy = preservedMta.getApplications()
                                                                          .stream()
                                                                          .filter(application -> !appsToPreserveNames.contains(application.getName()))
                                                                          .collect(Collectors.toList());
        appsToUndeploy.addAll(deployedAppsToUndeploy);
        return appsToUndeploy;
    }

}
