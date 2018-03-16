package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeleteServicesStepTest extends SyncActivitiStepTest<DeleteServicesStep> {

    private final StepInput stepInput;
    private final String expectedExceptionMessage;

    private List<String> servicesToDelete = new ArrayList<>();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
        // @formatter:off
            // (0) Services have bindings:
            {
                "delete-services-step-input-1.json", null,
            },
            // (1) Services do not have bindings:
            {
                "delete-services-step-input-2.json", null,
            },
            // (2) No services to delete:
            {
                "delete-services-step-input-3.json", null,
            },
            // (3) Some services have bindings, some do not:
            {
                "delete-services-step-input-4.json", null,
            },
            // (4) One of the services is missing (maybe the user deleted it manually):
            {
                "delete-services-step-input-5.json", null,
            },
            // (5) The user does not have the necessary rights to delete the service:
            {
                "delete-services-step-input-6.json", "Controller operation failed: 403 Forbidden",
            },
        // @formatter:on
        });
    }

    public DeleteServicesStepTest(String stepInput, String expectedExceptionMessage) throws Exception {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, DeleteServicesStepTest.class), StepInput.class);
        this.expectedExceptionMessage = expectedExceptionMessage;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareClient();
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        verifyClient();
    }

    private void loadParameters() {
        servicesToDelete = stepInput.servicesToDelete.stream()
            .map((service) -> service.name)
            .collect(Collectors.toList());
        if (expectedExceptionMessage != null) {
            expectedException.expect(SLException.class);
            expectedException.expectMessage(expectedExceptionMessage);
        }
    }

    private void prepareContext() {
        StepsUtil.setArrayVariableFromCollection(context, Constants.VAR_SERVICES_TO_DELETE, servicesToDelete);
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_DELETE_SERVICES, true);
    }

    private void prepareClient() {
        for (SimpleService service : stepInput.servicesToDelete) {
            Mockito.when(client.getServiceInstance(service.name))
                .thenReturn(createServiceInstance(service));
            if (service.httpErrorCodeToReturnOnDelete != null) {
                HttpStatus httpStatusToReturnOnDelete = HttpStatus.valueOf(service.httpErrorCodeToReturnOnDelete);
                Mockito.doThrow(new CloudFoundryException(httpStatusToReturnOnDelete))
                    .when(client)
                    .deleteService(service.name);
            }
        }
    }

    private CloudServiceInstance createServiceInstance(SimpleService service) {
        CloudServiceInstance instance = new CloudServiceInstance();
        if (service.hasBoundApplications) {
            instance.setBindings(Arrays.asList(new CloudServiceBinding()));
        }
        return instance;
    }

    private void verifyClient() {
        for (SimpleService service : stepInput.servicesToDelete) {
            if (!service.hasBoundApplications) {
                Mockito.verify(client, Mockito.times(1))
                    .deleteService(service.name);
            }
        }
    }

    private static class StepInput {
        List<SimpleService> servicesToDelete;
    }

    private static class SimpleService {
        String name;
        boolean hasBoundApplications;
        Integer httpErrorCodeToReturnOnDelete;
    }

    @Override
    protected DeleteServicesStep createStep() {
        return new DeleteServicesStep();
    }

}
