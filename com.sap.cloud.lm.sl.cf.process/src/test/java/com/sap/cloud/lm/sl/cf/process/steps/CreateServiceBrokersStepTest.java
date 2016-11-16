package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.activiti.common.Constants;
import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelBuilder;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class CreateServiceBrokersStepTest extends AbstractStepTest<CreateServiceBrokersStep> {

    private final String expectedExceptionMessage;
    private final String inputLocation;
    private final String expectedOutputLocation;

    private StepInput input;
    private StepOutput expectedOutput;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private CloudFoundryOperations client = Mockito.mock(CloudFoundryOperations.class);

    @InjectMocks
    private CreateServiceBrokersStep step = new CreateServiceBrokersStep();
    private CloudFoundryException updateException;
    private CloudFoundryException createException;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) A service broker should be created and all necessary parameters are present:
            {
                "create-service-brokers-step-input-01.json", "create-service-brokers-step-output-01.json", null, null, null,
            },
            // (01) No service brokers should be created (implicit):
            {
                "create-service-brokers-step-input-02.json", "create-service-brokers-step-output-02.json", null, null, null,
            },
            // (02) No service brokers should be created (explicit):
            {
                "create-service-brokers-step-input-03.json", "create-service-brokers-step-output-03.json", null, null, null,
            },
            // (03) A service broker should be created but the username is missing:
            {
                "create-service-brokers-step-input-04.json", null, "Missing service broker username for application \"foo\"", null, null,
            },
            // (04) A service broker should be created and the password is missing:
            {
                "create-service-brokers-step-input-05.json", "create-service-brokers-step-output-05.json", null, null, null,
            },
            // (05) A service broker should be created but the url is missing:
            {
                "create-service-brokers-step-input-06.json", null, "Missing service broker url for application \"foo\"", null, null,
            },
            // (06) A service broker should be created and the name is missing:
            {
                "create-service-brokers-step-input-07.json", "create-service-brokers-step-output-07.json", null, null, null,
            },
            // (07) A service broker should be updated and all necessary parameters are present:
            {
                "create-service-brokers-step-input-08.json", "create-service-brokers-step-output-08.json", null, null, null,
            },
            // (08) Create/update calls for both brokers should be made, although update throws exception:
            {
                "create-service-brokers-step-input-09.json", "create-service-brokers-step-output-09.json", null, null, new CloudFoundryException(HttpStatus.NOT_IMPLEMENTED),
            },
            // (09) A random exception is thrown during create:
            {
                "create-service-brokers-step-input-09.json", null, "Controller operation failed: I_AM_A_TEAPOT", new CloudFoundryException(HttpStatus.I_AM_A_TEAPOT), null,
            },
            // (10) A random exception is thrown during update:
            {
                "create-service-brokers-step-input-09.json", null, "Controller operation failed: I_AM_A_TEAPOT", null, new CloudFoundryException(HttpStatus.I_AM_A_TEAPOT),
            },
            // (11) Create/update calls for both brokers should be made, although both create and update throw an exception: 
            {
                "create-service-brokers-step-input-09.json", "create-service-brokers-step-output-09.json", null, new CloudFoundryException(HttpStatus.FORBIDDEN), new CloudFoundryException(HttpStatus.FORBIDDEN),
            },
// @formatter:on
        });
    }

    public CreateServiceBrokersStepTest(String inputLocation, String expectedOutputLocation, String expectedExceptionMessage,
        CloudFoundryException createException, CloudFoundryException updateException) {
        this.expectedOutputLocation = expectedOutputLocation;
        this.expectedExceptionMessage = expectedExceptionMessage;
        this.inputLocation = inputLocation;
        this.updateException = updateException;
        this.createException = createException;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareClient();
        step.clientSupplier = (context) -> client;
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(), context.getVariable(Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        StepOutput actualOutput = captureStepOutput();

        assertEquals(JsonUtil.toJson(expectedOutput, true), JsonUtil.toJson(actualOutput, true));

        List<CloudServiceBroker> aServiceBrokersToCreate = StepsUtil.getServiceBrokersToCreate(context);
        Collections.sort(aServiceBrokersToCreate, (broker1, broker2) -> broker1.getName().compareTo(broker2.getName()));
        List<CloudServiceBroker> eServiceBrokersToCreate = new ArrayList<>(expectedOutput.createdServiceBrokers);
        eServiceBrokersToCreate.addAll(expectedOutput.updatedServiceBrokers);
        Collections.sort(eServiceBrokersToCreate, (broker1, broker2) -> broker1.getName().compareTo(broker2.getName()));

        assertEquals(JsonUtil.toJson(eServiceBrokersToCreate, true), JsonUtil.toJson(aServiceBrokersToCreate, true));
    }

    private void loadParameters() throws Exception {
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
            expectedException.expect(SLException.class);
        } else {
            expectedOutput = JsonUtil.fromJson(TestUtil.getResourceAsString(expectedOutputLocation, getClass()), StepOutput.class);
        }
        input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, getClass()), StepInput.class);
    }

    private void prepareContext() {
        StepsUtil.setAppsToDeploy(context, toCloudApplications(input.applications));
    }

    private List<CloudApplicationExtended> toCloudApplications(List<SimpleApplication> applications) {
        return applications.stream().map((application) -> application.toCloudApplication()).collect(Collectors.toList());
    }

    private void prepareClient() {
        Mockito.when(client.getServiceBrokers()).thenReturn(
            input.existingServiceBrokerNames.stream().map((name) -> new CloudServiceBroker(null, name, "", "", "")).collect(
                Collectors.toList()));
        if (updateException != null) {
            Mockito.doThrow(updateException).when(client).updateServiceBroker(Mockito.any());
        }
        if (createException != null) {
            Mockito.doThrow(createException).when(client).createServiceBroker(Mockito.any());
        }
    }

    private StepOutput captureStepOutput() {
        ArgumentCaptor<CloudServiceBroker> argumentCaptor;

        StepOutput actualOutput = new StepOutput();

        argumentCaptor = ArgumentCaptor.forClass(CloudServiceBroker.class);
        int expectedCreatedBrokersCnt = expectedOutput.createdServiceBrokers.size();
        Mockito.verify(client, Mockito.times(expectedCreatedBrokersCnt)).createServiceBroker(argumentCaptor.capture());
        actualOutput.createdServiceBrokers = argumentCaptor.getAllValues();

        argumentCaptor = ArgumentCaptor.forClass(CloudServiceBroker.class);
        int expectedUpdatedBrokersCnt = expectedOutput.updatedServiceBrokers.size();
        Mockito.verify(client, Mockito.times(expectedUpdatedBrokersCnt)).updateServiceBroker(argumentCaptor.capture());
        actualOutput.updatedServiceBrokers = argumentCaptor.getAllValues();

        return actualOutput;
    }

    private static class StepInput {
        List<String> existingServiceBrokerNames;
        List<SimpleApplication> applications;
    }

    private static class StepOutput {
        List<CloudServiceBroker> createdServiceBrokers;
        List<CloudServiceBroker> updatedServiceBrokers;
    }

    static class SimpleApplication {

        String name;
        Map<String, Object> attributes;

        CloudApplicationExtended toCloudApplication() {
            CloudApplicationExtended application = new CloudApplicationExtended(null, name);
            application.setEnv(MapUtil.asMap(CloudModelBuilder.ENV_DEPLOY_ATTRIBUTES, JsonUtil.toJson(attributes)));
            return application;
        }

    }

    @Override
    protected CreateServiceBrokersStep createStep() {
        return new CreateServiceBrokersStep();
    }

}
