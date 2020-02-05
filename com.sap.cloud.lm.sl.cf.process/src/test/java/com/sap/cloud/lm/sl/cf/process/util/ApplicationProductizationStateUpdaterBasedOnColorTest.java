package com.sap.cloud.lm.sl.cf.process.util;

import static java.text.MessageFormat.format;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaApplication;

public class ApplicationProductizationStateUpdaterBasedOnColorTest {

    @Mock
    private StepLogger stepLogger;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private static Stream<Arguments> testUpdateApplicationsProductizationState() {
        return Stream.of(
        // @formatter:off
               // (1) Get module-1 from deployed modules [module-1, module-2]
               Arguments.of(Arrays.asList(new DeployedApplication("module-1", "foo-blue"), new DeployedApplication("module-2", "bar-green")), ApplicationColor.BLUE, new DeployedApplication("module-1", "foo-blue")),
               // (2) Get live application in case where both blue and green are deployed
               Arguments.of(Arrays.asList(new DeployedApplication("module-1", "foo-blue"), new DeployedApplication("module-1", "foo-green")), ApplicationColor.GREEN, new DeployedApplication("module-1", "foo-green")),
               // (3) Test without deployed mta
               Arguments.of(Collections.emptyList(), ApplicationColor.BLUE, null),
               // (4) Get application without color suffix when previous deployed color is BLUE
               Arguments.of(Arrays.asList(new DeployedApplication("module-1", "foo"), new DeployedApplication("module-1", "foo-green")), ApplicationColor.BLUE, new DeployedApplication("module-1", "foo")),
               // (5) Get live application (foo-green) when foo and foo-blue exists
               Arguments.of(Arrays.asList(new DeployedApplication("module-1", "foo"), new DeployedApplication("module-1", "foo-blue"), new DeployedApplication("module-1", "foo-green")), ApplicationColor.GREEN, 
                            new DeployedApplication("module-1", "foo-green"))
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testUpdateApplicationsProductizationState(List<DeployedApplication> applications, ApplicationColor liveMtaColor,
                                                          DeployedApplication expectedDeployedApplication) {
        List<DeployedMtaApplication> deployedApplications = createDeployedMtaApplications(applications);

        List<DeployedMtaApplication> updatedDeployedApplications = new ApplicationProductizationStateUpdaterBasedOnColor(stepLogger,
                                                                                                                         liveMtaColor).updateApplicationsProductizationState(deployedApplications);

        if (expectedDeployedApplication != null) {
            assertTrue(updatedDeployedApplications.stream()
                                                  .anyMatch(updatedDeployedApplication -> doesItMatchToExpectedDeployedApplication(updatedDeployedApplication,
                                                                                                                                   expectedDeployedApplication)),
                       format("Expected Deployed Application with module name:{0} application name:{1} and live:true",
                              expectedDeployedApplication.moduleName, expectedDeployedApplication.appName));

            assertTrue(updatedDeployedApplications.stream()
                                                  .filter(updatedDeployedApplication -> doesNotContainExpectedApplication(expectedDeployedApplication,
                                                                                                                          updatedDeployedApplication))
                                                  .allMatch(updatedDeployedApplication -> updatedDeployedApplication.getProductizationState() == DeployedMtaApplication.ProductizationState.IDLE),
                       format("Only Deployed Application with module name:{0} application name:{1} should be live",
                              expectedDeployedApplication.moduleName, expectedDeployedApplication.appName));
            return;
        }
        assertTrue(updatedDeployedApplications.isEmpty(), "Updated Deployed Applications list should be empty but was not");
    }

    private List<DeployedMtaApplication> createDeployedMtaApplications(List<DeployedApplication> applications) {
        return applications.stream()
                           .map(this::createDeployedMtaApplication)
                           .collect(Collectors.toList());
    }

    private DeployedMtaApplication createDeployedMtaApplication(DeployedApplication application) {
        return ImmutableDeployedMtaApplication.builder()
                                              .name(application.appName)
                                              .moduleName(application.moduleName)
                                              .build();
    }

    private boolean doesItMatchToExpectedDeployedApplication(DeployedMtaApplication deployedMtaApplication,
                                                             DeployedApplication expectedApplication) {
        String moduleName = deployedMtaApplication.getModuleName();
        String appName = deployedMtaApplication.getName();
        return expectedApplication.moduleName.equals(moduleName) && expectedApplication.appName.equals(appName)
            && deployedMtaApplication.getProductizationState() == DeployedMtaApplication.ProductizationState.LIVE;
    }

    private boolean doesNotContainExpectedApplication(DeployedApplication expectedApplication,
                                                      DeployedMtaApplication deployedMtaApplication) {
        return !deployedMtaApplication.getName()
                                      .equals(expectedApplication.appName);
    }

    private static class DeployedApplication {
        String moduleName;
        String appName;

        public DeployedApplication(String moduleName, String appName) {
            this.moduleName = moduleName;
            this.appName = appName;
        }

    }
}
