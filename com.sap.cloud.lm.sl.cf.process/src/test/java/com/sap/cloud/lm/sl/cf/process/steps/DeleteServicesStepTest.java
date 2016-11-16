package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeleteServicesStepTest extends AbstractStepTest<DeleteServicesStep> {

    private final StepInput stepInput;

    private List<String> servicesToDelete = new ArrayList<>();

    private CloudFoundryOperations client = Mockito.mock(CloudFoundryOperations.class);

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
        // @formatter:off
            // (0) Services have bindings:
            {
                "delete-services-step-input-1.json",
            },
            // (1) Services do not have bindings:
            {
                "delete-services-step-input-2.json",
            },
            // (2) No services to delete:
            {
                "delete-services-step-input-3.json",
            },
            // (3) Some services have bindings, some do not:
            {
                "delete-services-step-input-4.json",
            },
        // @formatter:on
        });
    }

    public DeleteServicesStepTest(String stepInput) throws Exception {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, DeleteServicesStepTest.class), StepInput.class);
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

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        verifyClient();
    }

    private void loadParameters() {
        servicesToDelete = stepInput.servicesToDelete.stream().map((service) -> service.name).collect(Collectors.toList());
    }

    private void prepareContext() {
        ContextUtil.setArrayVariableFromCollection(context, Constants.VAR_SERVICES_TO_DELETE, servicesToDelete);
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_DELETE_SERVICES, true);
    }

    private void prepareClient() {
        for (SimpleService service : stepInput.servicesToDelete) {
            Mockito.when(client.getServiceInstance(service.name)).thenReturn(createServiceInstance(service));
        }
        step.clientSupplier = (context) -> client;
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
                Mockito.verify(client, Mockito.times(1)).deleteService(service.name);
            }
        }
    }

    private static class StepInput {
        List<SimpleService> servicesToDelete;
    }

    private static class SimpleService {
        String name;
        boolean hasBoundApplications;
    }

    @Override
    protected DeleteServicesStep createStep() {
        return new DeleteServicesStep();
    }

}
