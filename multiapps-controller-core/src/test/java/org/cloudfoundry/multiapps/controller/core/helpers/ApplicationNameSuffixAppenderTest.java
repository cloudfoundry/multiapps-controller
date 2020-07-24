package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.multiapps.controller.core.model.ApplicationColor;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationNameSuffixAppenderTest {

    @Test
    public void testGreenNameAppending() {
        DeploymentDescriptor descriptor = createDeploymentDescriptor("a", "b");
        descriptor.accept(getApplicationNameAppender(ApplicationColor.GREEN));

        Assertions.assertTrue(descriptor.getModules()
                                        .stream()
                                        .map(NameUtil::getApplicationName)
                                        .allMatch(appName -> appName.endsWith(ApplicationColor.GREEN.asSuffix())));
    }

    @Test
    public void testBlueNameAppending() {
        DeploymentDescriptor descriptor = createDeploymentDescriptor("a", "b");
        descriptor.accept(getApplicationNameAppender(ApplicationColor.BLUE));

        Assertions.assertTrue(descriptor.getModules()
                                        .stream()
                                        .map(NameUtil::getApplicationName)
                                        .allMatch(appName -> appName.endsWith(ApplicationColor.BLUE.asSuffix())));
    }

    private static DeploymentDescriptor createDeploymentDescriptor(String... moduleNames) {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        List<Module> modules = new ArrayList<>(5);
        for (String moduleName : moduleNames) {
            modules.add(Module.createV3()
                              .setName(moduleName));
        }
        descriptor.setModules(modules);
        return descriptor;
    }

    private static ApplicationNameSuffixAppender getApplicationNameAppender(ApplicationColor applicationColor) {
        return new ApplicationNameSuffixAppender(applicationColor.asSuffix());
    }

}
