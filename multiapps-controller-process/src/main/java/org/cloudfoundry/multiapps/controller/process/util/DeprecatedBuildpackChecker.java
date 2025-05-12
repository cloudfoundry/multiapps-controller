package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Map;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.builders.v2.ParametersChainBuilder;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

@Named
public class DeprecatedBuildpackChecker {

    private static final String DEPRECATION_MESSAGE = "SAP Java Buildpack 1 has been deprecated and is going to be removed from SAP BTP, Cloud Foundry environment after June, 2025!";
    private static final String DEFAULT_BUILDPACK_MESSAGE_1 = "If no buildpack is specified, sap_java_buildpack is currently applied by default for certain module types (java.tomee, java.tomcat, java).";
    private static final String DEFAULT_BUILDPACK_MESSAGE_2 = "This default will change after June - we strongly recommend that you manually migrate to the supported buildpack in advance to avoid deployment issues.";
    private static final String DEPRECATION_LINK = "https://help.sap.com/whats-new/cf0cb2cb149647329b5d02aa96303f56?Component=SAP+Java+Buildpack&Valid_as_Of=2025-04-01:2025-04-10&locale=en-US";

    private static final String DEPRECATED_BUILDPACK = "sap_java_buildpack";

    public void warnForDeprecatedBuildpacks(List<Module> modulesCalculatedForDeployment,
                                            DeploymentDescriptor deploymentDescriptor, StepLogger stepLogger,
                                            ModuleToDeployHelper moduleToDeployHelper) {
        List<String> appsWithDeprecatedBuildpacks = modulesCalculatedForDeployment.stream()
                                                                                  .filter(moduleToDeployHelper::isApplication)
                                                                                  .filter(module -> hasDeprecatedBuildpack(module,
                                                                                                                           deploymentDescriptor,
                                                                                                                           stepLogger))
                                                                                  .map(Module::getName)
                                                                                  .toList();

        if (!appsWithDeprecatedBuildpacks.isEmpty()) {
            logDeprecationNotice(stepLogger, appsWithDeprecatedBuildpacks);
        }
    }

    private boolean hasDeprecatedBuildpack(Module module, DeploymentDescriptor deploymentDescriptor, StepLogger stepLogger) {
        List<Map<String, Object>> parametersList = new ParametersChainBuilder(deploymentDescriptor)
            .buildModuleChain(module.getName());

        List<String> buildpacks = PropertiesUtil.getPluralOrSingular(
            parametersList, SupportedParameters.BUILDPACKS, SupportedParameters.BUILDPACK);

        return buildpacks.contains(DEPRECATED_BUILDPACK);
    }

    private void logDeprecationNotice(StepLogger stepLogger, List<String> appsWithDeprecatedBuildpacks) {
        String separator = "=".repeat(80);

        stepLogger.warn("==   ATTENTION:   ==");
        stepLogger.warn(separator);
        stepLogger.warn(DEPRECATION_MESSAGE);
        stepLogger.warn(DEFAULT_BUILDPACK_MESSAGE_1);
        stepLogger.warn(DEFAULT_BUILDPACK_MESSAGE_2);
        stepLogger.warn("Affected modules: " + appsWithDeprecatedBuildpacks);
        stepLogger.warn("For more information, see: " + DEPRECATION_LINK);
        stepLogger.warn(separator);
    }

}
