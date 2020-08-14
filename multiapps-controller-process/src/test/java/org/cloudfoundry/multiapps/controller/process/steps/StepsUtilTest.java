package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudPackage;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudPackage;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.Phase;
import org.cloudfoundry.multiapps.controller.process.mock.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

        VariableHandling.set(execution, Variables.SERVICES_TO_CREATE, Collections.singletonList(service));
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
        Map<String, Map<String, Object>> bindingParameters = new HashMap<>();
        Map<String, Object> serviceBindingParameters = new HashMap<>();
        serviceBindingParameters.put("integer-value", 1);
        serviceBindingParameters.put("double-value", 1.4);
        serviceBindingParameters.put("string-value", "1");
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
                                     .isEmpty());
        assertEquals(Integer.class, actualAppToDeploy.getBindingParameters()
                                                     .get("service-1")
                                                     .get("integer-value")
                                                     .getClass());
        assertEquals(Double.class, actualAppToDeploy.getBindingParameters()
                                                    .get("service-1")
                                                    .get("double-value")
                                                    .getClass());
        assertEquals(String.class, actualAppToDeploy.getBindingParameters()
                                                    .get("service-1")
                                                    .get("string-value")
                                                    .getClass());
    }

    @Test
    void testSetAndGetCloudPackage() {
        CloudPackage expectedCloudPackage = ImmutableCloudPackage.builder()
                                                                 .metadata(ImmutableCloudMetadata.builder()
                                                                                                 .guid(UUID.fromString("ab0703c2-1a50-11e9-ab14-d663bd873d93"))
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

    @Test
    void testShouldVerifyArchiveSignatureSet() {
        VariableHandling.set(execution, Variables.VERIFY_ARCHIVE_SIGNATURE, true);
        Boolean result = VariableHandling.get(execution, Variables.VERIFY_ARCHIVE_SIGNATURE);

        assertTrue(result);
    }

}
