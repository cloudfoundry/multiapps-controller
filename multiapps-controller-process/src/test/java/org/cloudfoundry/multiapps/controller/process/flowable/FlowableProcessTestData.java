package org.cloudfoundry.multiapps.controller.process.flowable;

import java.time.Duration;
import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStateAction;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.immutables.value.Value;

@Value.Immutable
public interface FlowableProcessTestData {

    String getCorrelationId();

    String getSpaceGuid();

    String getOrgGuid();

    String getUsername();

    String getUserGuid();

    String getMtaId();

    String getMtaVersion();

    @Value.Default
    default boolean keepFiles() {
        return false;
    }

    @Value.Default
    default boolean deleteServices() {
        return false;
    }

    Duration getAppsStageTimeout();

    @Value.Default
    default int getModulesCount() {
        return 0;
    }

    DeploymentDescriptor getSmallDescriptor();

    DeploymentDescriptor getBigDescriptor();

    DeployedMta getDeployedMta();

    List<CloudRoute> getRoutes();

    StepPhase getStepPhase();

    List<ApplicationStateAction> getAppActions();

    List<CloudServiceKey> getServiceKeys();

    List<String> getModulesForDeployment();

    List<ExtensionDescriptor> getExtensionDescriptors();

}
