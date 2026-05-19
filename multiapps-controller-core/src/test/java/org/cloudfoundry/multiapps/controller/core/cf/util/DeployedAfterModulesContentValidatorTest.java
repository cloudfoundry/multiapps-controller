package org.cloudfoundry.multiapps.controller.core.cf.util;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

class DeployedAfterModulesContentValidatorTest {

    @Mock
    private CloudControllerClient client;
    @Mock
    private UserMessageLogger userMessageLogger;
    @Mock
    private ModuleToDeployHelper moduleToDeployHelper;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testV2ModuleWithUnknownDependencyPasses() {
        Module v2Module = Module.createV2()
                                .setName("legacy");
        DeployedAfterModulesContentValidator validator = new DeployedAfterModulesContentValidator(client, userMessageLogger,
                                                                                                   moduleToDeployHelper, List.of(v2Module));

        Assertions.assertDoesNotThrow(() -> validator.validate(List.of(v2Module)));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void testModuleWithoutDeployedAfterPasses() {
        Module module = v3Module("a", null);
        DeployedAfterModulesContentValidator validator = new DeployedAfterModulesContentValidator(client, userMessageLogger,
                                                                                                   moduleToDeployHelper, List.of(module));

        Assertions.assertDoesNotThrow(() -> validator.validate(List.of(module)));
    }

    @Test
    void testDependencyInDeploymentSetPasses() {
        Module dep = v3Module("dep", List.of());
        Module module = v3Module("app", List.of("dep"));
        DeployedAfterModulesContentValidator validator = new DeployedAfterModulesContentValidator(client, userMessageLogger,
                                                                                                   moduleToDeployHelper,
                                                                                                   List.of(module, dep));

        Assertions.assertDoesNotThrow(() -> validator.validate(List.of(module, dep)));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void testNonApplicationDependencyOutsideDeploymentEmitsWarningAndPasses() {
        Module nonApp = v3Module("non-app", List.of());
        Module module = v3Module("app", List.of("non-app"));
        Mockito.when(moduleToDeployHelper.isApplication(nonApp))
               .thenReturn(false);
        DeployedAfterModulesContentValidator validator = new DeployedAfterModulesContentValidator(client, userMessageLogger,
                                                                                                   moduleToDeployHelper,
                                                                                                   List.of(module, nonApp));

        Assertions.assertDoesNotThrow(() -> validator.validate(List.of(module)));
        Mockito.verify(userMessageLogger)
               .warn(Mockito.contains("non-app"));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void testApplicationDependencyExistingInCfPasses() {
        Module externalApp = v3Module("external", List.of());
        Module module = v3Module("app", List.of("external"));
        Mockito.when(moduleToDeployHelper.isApplication(externalApp))
               .thenReturn(true);
        Mockito.when(client.getApplicationGuid("external"))
               .thenReturn(UUID.randomUUID());
        DeployedAfterModulesContentValidator validator = new DeployedAfterModulesContentValidator(client, userMessageLogger,
                                                                                                   moduleToDeployHelper,
                                                                                                   List.of(module, externalApp));

        Assertions.assertDoesNotThrow(() -> validator.validate(List.of(module)));
    }

    @Test
    void testApplicationDependencyMissingFromCfThrowsContentException() {
        Module externalApp = v3Module("missing", List.of());
        Module module = v3Module("app", List.of("missing"));
        Mockito.when(moduleToDeployHelper.isApplication(externalApp))
               .thenReturn(true);
        Mockito.when(client.getApplicationGuid("missing"))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        DeployedAfterModulesContentValidator validator = new DeployedAfterModulesContentValidator(client, userMessageLogger,
                                                                                                   moduleToDeployHelper,
                                                                                                   List.of(module, externalApp));

        ContentException exception = Assertions.assertThrows(ContentException.class, () -> validator.validate(List.of(module)));
        Assertions.assertTrue(exception.getMessage()
                                       .contains("app"));
    }

    @Test
    void testDependencyAbsentFromArchiveTriggersCfLookup() {
        // Dependency name not in allMtaModules and not in deployment set -> doesAppExist is the only signal.
        Module module = v3Module("app", List.of("standalone"));
        Mockito.when(client.getApplicationGuid("standalone"))
               .thenReturn(UUID.randomUUID());
        DeployedAfterModulesContentValidator validator = new DeployedAfterModulesContentValidator(client, userMessageLogger,
                                                                                                   moduleToDeployHelper, List.of(module));

        Assertions.assertDoesNotThrow(() -> validator.validate(List.of(module)));
    }

    @Test
    void testMultipleUnresolvedDependenciesAreReportedTogether() {
        Module a = v3Module("a", List.of("missing-1"));
        Module b = v3Module("b", List.of("missing-2"));
        Mockito.when(client.getApplicationGuid(Mockito.anyString()))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        DeployedAfterModulesContentValidator validator = new DeployedAfterModulesContentValidator(client, userMessageLogger,
                                                                                                   moduleToDeployHelper, List.of(a, b));

        ContentException exception = Assertions.assertThrows(ContentException.class, () -> validator.validate(List.of(a, b)));
        Assertions.assertTrue(exception.getMessage()
                                       .contains("a"));
        Assertions.assertTrue(exception.getMessage()
                                       .contains("b"));
    }

    private Module v3Module(String name, List<String> deployedAfter) {
        Module module = Module.createV3()
                              .setName(name)
                              .setType("application");
        if (deployedAfter != null) {
            module.setDeployedAfter(deployedAfter);
        }
        return module;
    }

}
