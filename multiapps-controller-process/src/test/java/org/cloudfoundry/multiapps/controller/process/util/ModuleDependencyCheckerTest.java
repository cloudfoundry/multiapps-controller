package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.UUID;

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

class ModuleDependencyCheckerTest {

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
    void testV2ModuleIsAlwaysSatisfied() {
        Module v2Module = Module.createV2()
                                .setName("legacy-module");

        ModuleDependencyChecker checker = new ModuleDependencyChecker(client, userMessageLogger, moduleToDeployHelper, List.of(v2Module),
                                                                       List.of(v2Module), List.of());

        Assertions.assertTrue(checker.areAllDependenciesSatisfied(v2Module));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void testModuleWithoutDeployedAfterIsSatisfied() {
        Module module = v3Module("a").setDeployedAfter(List.of());
        ModuleDependencyChecker checker = new ModuleDependencyChecker(client, userMessageLogger, moduleToDeployHelper, List.of(module),
                                                                       List.of(module), List.of());

        Assertions.assertTrue(checker.areAllDependenciesSatisfied(module));
    }

    @Test
    void testDependencyAlreadyDeployedSatisfiesModule() {
        Module dependency = v3Module("dep");
        Module module = v3Module("app").setDeployedAfter(List.of("dep"));

        ModuleDependencyChecker checker = new ModuleDependencyChecker(client, userMessageLogger, moduleToDeployHelper,
                                                                       List.of(module, dependency), List.of(module, dependency),
                                                                       List.of(dependency));

        Assertions.assertTrue(checker.areAllDependenciesSatisfied(module));
    }

    @Test
    void testDependencyNotForDeploymentIsTreatedAsProcessed() {
        Module dependency = v3Module("not-deployed");
        Module module = v3Module("app").setDeployedAfter(List.of("not-deployed"));

        // dependency is in the descriptor but NOT in modulesForDeployment
        ModuleDependencyChecker checker = new ModuleDependencyChecker(client, userMessageLogger, moduleToDeployHelper,
                                                                       List.of(module, dependency), List.of(module), List.of());

        Assertions.assertTrue(checker.areAllDependenciesSatisfied(module));
    }

    @Test
    void testDependencyForDeploymentButNotYetCompletedIsNotSatisfied() {
        Module dependency = v3Module("dep");
        Module module = v3Module("app").setDeployedAfter(List.of("dep"));

        ModuleDependencyChecker checker = new ModuleDependencyChecker(client, userMessageLogger, moduleToDeployHelper,
                                                                       List.of(module, dependency), List.of(module, dependency),
                                                                       List.of());

        Assertions.assertFalse(checker.areAllDependenciesSatisfied(module));
    }

    @Test
    void testNonDeploymentDependencyExistingInCfSatisfiesModule() {
        // To reach the CF lookup branch, we need at least one for-deployment-not-yet-deployed dep
        // that forces fallthrough into areAllDependenciesAlreadyPresent.
        Module externalDep = v3Module("external");
        Module pendingDep = v3Module("pending");
        Module module = v3Module("app").setDeployedAfter(List.of("external", "pending"));
        Mockito.when(moduleToDeployHelper.isApplication(externalDep))
               .thenReturn(true);
        Mockito.when(client.getApplicationGuid("external"))
               .thenReturn(UUID.randomUUID());

        ModuleDependencyChecker checker = new ModuleDependencyChecker(client, userMessageLogger, moduleToDeployHelper,
                                                                       List.of(module, externalDep, pendingDep),
                                                                       List.of(module, pendingDep), List.of(pendingDep));

        // pending is in alreadyDeployed and modulesNotYetDeployed is empty; external resolves via CF.
        Assertions.assertTrue(checker.areAllDependenciesSatisfied(module));
    }

    @Test
    void testNonDeploymentDependencyMissingFromCfIsNotSatisfied() {
        Module externalDep = v3Module("missing");
        Module pendingDep = v3Module("pending");
        Module module = v3Module("app").setDeployedAfter(List.of("missing", "pending"));
        Mockito.when(moduleToDeployHelper.isApplication(externalDep))
               .thenReturn(true);
        Mockito.when(client.getApplicationGuid("missing"))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));

        // pending is for-deployment but not yet deployed -> forces fallthrough to areAllDependenciesAlreadyPresent.
        // external is not-for-deployment and CF says it doesn't exist -> not satisfied.
        ModuleDependencyChecker checker = new ModuleDependencyChecker(client, userMessageLogger, moduleToDeployHelper,
                                                                       List.of(module, externalDep, pendingDep),
                                                                       List.of(module, pendingDep), List.of());

        Assertions.assertFalse(checker.areAllDependenciesSatisfied(module));
    }

    @Test
    void testNonApplicationDependencyEmitsWarningAndIsSatisfied() {
        Module nonApp = v3Module("non-app");
        Module pendingDep = v3Module("pending");
        Module module = v3Module("app").setDeployedAfter(List.of("non-app", "pending"));
        Mockito.when(moduleToDeployHelper.isApplication(nonApp))
               .thenReturn(false);

        ModuleDependencyChecker checker = new ModuleDependencyChecker(client, userMessageLogger, moduleToDeployHelper,
                                                                       List.of(module, nonApp, pendingDep),
                                                                       List.of(module, pendingDep), List.of());

        // pending is for-deployment but not deployed -> forces fallthrough; non-app is not-for-deployment but not an
        // application, so isDependencyPresent returns true after warning. modulesNotYetDeployed = [pending] -> false.
        Assertions.assertFalse(checker.areAllDependenciesSatisfied(module));
        Mockito.verify(userMessageLogger)
               .warn(Mockito.contains("non-app"));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void testGettersExposeComputedSets() {
        Module deployed = v3Module("d");
        Module forDeployment = v3Module("f");
        Module notForDeployment = v3Module("n");

        ModuleDependencyChecker checker = new ModuleDependencyChecker(client, userMessageLogger, moduleToDeployHelper,
                                                                       List.of(deployed, forDeployment, notForDeployment),
                                                                       List.of(deployed, forDeployment), List.of(deployed));

        Assertions.assertEquals(java.util.Set.of("d", "f"), checker.getModulesForDeployment());
        Assertions.assertEquals(java.util.Set.of("n"), checker.getModulesNotForDeployment());
        Assertions.assertEquals(java.util.Set.of("d"), checker.getAlreadyDeployedModules());
    }

    private Module v3Module(String name) {
        Module module = Module.createV3()
                              .setName(name)
                              .setType("application");
        module.setDeployedAfter(List.of());
        return module;
    }

}
