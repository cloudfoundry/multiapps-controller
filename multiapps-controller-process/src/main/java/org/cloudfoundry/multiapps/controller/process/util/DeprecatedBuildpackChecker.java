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

    private static final String DEPRECATION_MESSAGE = "SAP Java Buildpack 1 has been deprecated and is going to be removed from SAP BTP, Cloud Foundry environment on June 30, 2025!";
    private static final String DEPRECATION_LINK = "For more information, see: https://help.sap.com/whats-new/cf0cb2cb149647329b5d02aa96303f56?Component=SAP+Java+Buildpack&Valid_as_Of=2025-04-01:2025-04-10&locale=en-US";

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
            logDeprecationNotice2(stepLogger, appsWithDeprecatedBuildpacks);
        }
    }

    private boolean hasDeprecatedBuildpack(Module module, DeploymentDescriptor deploymentDescriptor, StepLogger stepLogger) {
        List<Map<String, Object>> parametersList = new ParametersChainBuilder(deploymentDescriptor)
            .buildModuleChain(module.getName());

        List<String> buildpacks = PropertiesUtil.getPluralOrSingular(
            parametersList, SupportedParameters.BUILDPACKS, SupportedParameters.BUILDPACK);

        return buildpacks.contains(DEPRECATED_BUILDPACK);
    }

    private void logDeprecationNotice2(StepLogger stepLogger, List<String> appsWithDeprecatedBuildpacks) {
        String separator = "=".repeat(80);
        String message = String.join("\n",
                                     "==   ATTENTION:   ==",
                                     separator,
                                     DEPRECATION_MESSAGE,
                                     "Affected modules: " + appsWithDeprecatedBuildpacks,
                                     DEPRECATION_LINK,
                                     separator
        );
        stepLogger.warn(message);
    }

}
