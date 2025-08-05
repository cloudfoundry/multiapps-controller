package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDockerData;
import org.cloudfoundry.multiapps.controller.client.lib.domain.BindingDetails;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableBindingDetails;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.Phase;
import org.cloudfoundry.multiapps.controller.process.util.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StepsUtilTest {

    private static final String EXAMPLE_USER = "exampleUser";

    protected final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    @Test
    void testDetermineCurrentUserWithSetUser() {
        VariableHandling.set(execution, Variables.USER, EXAMPLE_USER);
        String determinedUser = StepsUtil.determineCurrentUser(execution);
        assertEquals(EXAMPLE_USER, determinedUser);
    }

    @Test
    void testDetermineCurrentUserError() {
        Assertions.assertThrows(SLException.class, () -> StepsUtil.determineCurrentUser(execution));
    }

    @Test
    void testGetServicesToCreateWithCredentials() {
        CloudServiceInstanceExtended service = ImmutableCloudServiceInstanceExtended.builder()
                                                                                    .name("my-service")
                                                                                    .putCredential("integer-value", 1)
                                                                                    .putCredential("double-value", 1.4)
                                                                                    .putCredential("string-value", "1")
                                                                                    .build();

        VariableHandling.set(execution, Variables.SERVICES_TO_CREATE, List.of(service));
        List<CloudServiceInstanceExtended> actualServicesToCreate = VariableHandling.get(execution, Variables.SERVICES_TO_CREATE);

        assertEquals(1, actualServicesToCreate.size());
        assertFalse(actualServicesToCreate.get(0)
                                          .getCredentials()
                                          .isEmpty());
        assertEquals(Integer.class, actualServicesToCreate.get(0)
                                                          .getCredentials()
                                                          .get("integer-value")
                                                          .getClass());
        assertEquals(Double.class, actualServicesToCreate.get(0)
                                                         .getCredentials()
                                                         .get("double-value")
                                                         .getClass());
        assertEquals(String.class, actualServicesToCreate.get(0)
                                                         .getCredentials()
                                                         .get("string-value")
                                                         .getClass());
    }

    @Test
    void testGetAppsToDeployWithBindingParameters() {
        Map<String, Object> configParameters = new HashMap<>();
        configParameters.put("integer-value", 1);
        configParameters.put("double-value", 1.4);
        configParameters.put("string-value", "1");
        BindingDetails serviceBindingParameters = ImmutableBindingDetails.builder()
                                                                         .bindingName("service-1")
                                                                         .config(configParameters)
                                                                         .build();
        Map<String, BindingDetails> bindingParameters = new HashMap<>();
        bindingParameters.put("service-1", serviceBindingParameters);

        CloudApplicationExtended application = ImmutableCloudApplicationExtended.builder()
                                                                                .name("my-application")
                                                                                .bindingParameters(bindingParameters)
                                                                                .build();

        VariableHandling.set(execution, Variables.APP_TO_PROCESS, application);
        CloudApplicationExtended actualAppToDeploy = VariableHandling.get(execution, Variables.APP_TO_PROCESS);

        assertFalse(actualAppToDeploy.getBindingParameters()
                                     .isEmpty());
        assertFalse(actualAppToDeploy.getBindingParameters()
                                     .get("service-1")
                                     .getConfig()
                                     .isEmpty());
        assertEquals(Integer.class, actualAppToDeploy.getBindingParameters()
                                                     .get("service-1")
                                                     .getConfig()
                                                     .get("integer-value")
                                                     .getClass());
        assertEquals(Double.class, actualAppToDeploy.getBindingParameters()
                                                    .get("service-1")
                                                    .getConfig()
                                                    .get("double-value")
                                                    .getClass());
        assertEquals(String.class, actualAppToDeploy.getBindingParameters()
                                                    .get("service-1")
                                                    .getConfig()
                                                    .get("string-value")
                                                    .getClass());
    }

    @Test
    void testSetAndGetCloudPackage() {
        CloudPackage expectedCloudPackage = ImmutableCloudPackage.builder()
                                                                 .metadata(ImmutableCloudMetadata.of(
                                                                     UUID.fromString("ab0703c2-1a50-11e9-ab14-d663bd873d93")))
                                                                 .type(CloudPackage.Type.DOCKER)
                                                                 .data(ImmutableDockerData.builder()
                                                                                          .image("cloudfoundry/test")
                                                                                          .build())
                                                                 .build();
        VariableHandling.set(execution, Variables.CLOUD_PACKAGE, expectedCloudPackage);
        CloudPackage actualCloudPackage = VariableHandling.get(execution, Variables.CLOUD_PACKAGE);
        assertEquals(expectedCloudPackage.getGuid(), actualCloudPackage.getGuid());
    }

    @Test
    void testSetAndGetPhase() {
        Phase expectedPhase = Phase.UNDEPLOY;
        VariableHandling.set(execution, Variables.PHASE, expectedPhase);
        Phase actualPhase = VariableHandling.get(execution, Variables.PHASE);

        assertEquals(expectedPhase, actualPhase);
    }

}
