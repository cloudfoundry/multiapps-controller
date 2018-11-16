package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceBrokerExtended;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceBrokerCreator;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceBrokersGetter;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class CreateOrUpdateServiceBrokersStepTest extends SyncFlowableStepTest<CreateOrUpdateServiceBrokersStep> {

    private final String expectedExceptionMessage;
    private final String inputLocation;
    private final String expectedOutputLocation;
    private final String expectedWarningMessage;

    private StepInput input;
    private StepOutput expectedOutput;

    @Mock
    private ServiceBrokerCreator serviceBrokerCreator;
    @Mock
    private ServiceBrokersGetter serviceBrokersGetter;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private CreateOrUpdateServiceBrokersStep step = new CreateOrUpdateServiceBrokersStep();
    private CloudOperationException updateException;
    private CloudOperationException createException;
    private Class<? extends Throwable> expectedExceptionClass;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) A service broker should be created, all necessary parameters are present and it isn't space scoped (explicit):
            {
                "create-service-brokers-step-input-00.json", "create-service-brokers-step-output-01.json", null, null, null, null, null,
            },
            // (01) A service broker should be created, all necessary parameters are present and it isn't space scoped (implicit):
            {
                "create-service-brokers-step-input-01.json", "create-service-brokers-step-output-01.json", null, null, null, null, null,
            },
            // (02) No service brokers should be created (implicit):
            {
                "create-service-brokers-step-input-02.json", "create-service-brokers-step-output-02.json", null, null, null, null, null,
            },
            // (03) No service brokers should be created (explicit):
            {
                "create-service-brokers-step-input-03.json", "create-service-brokers-step-output-03.json", null, null, null, null, null,
            },
            // (04) A service broker should be created but the username is missing:
            {
                "create-service-brokers-step-input-04.json", null, null, "Missing service broker username for application \"foo\"", null, null, null,
            },
            // (05) A service broker should be created and the password is missing:
            {
                "create-service-brokers-step-input-05.json", "create-service-brokers-step-output-05.json", null, "Missing service broker password for application \"foo\"", null, null, null,
            },
            // (06) A service broker should be created but the url is missing:
            {
                "create-service-brokers-step-input-06.json", null, null, "Missing service broker url for application \"foo\"", null, null, null,
            },
            // (07) A service broker should be created and the name is missing:
            {
                "create-service-brokers-step-input-07.json", "create-service-brokers-step-output-07.json", null, null, null, null, null,
            },
            // (08) A service broker should be updated and all necessary parameters are present:
            {
                "create-service-brokers-step-input-08.json", "create-service-brokers-step-output-08.json", null, null, null, null, null,
            },
            // (09) Create/update calls for both brokers should be made, although update throws exception:
            {
                "create-service-brokers-step-input-09.json", "create-service-brokers-step-output-09.json", "Could not update service broker \"foo-broker\". Operation not supported.", null, null, null, new CloudOperationException(HttpStatus.NOT_IMPLEMENTED),
            },
            // (10) A random exception is thrown during create:
            {
                "create-service-brokers-step-input-09.json", null, null, "Controller operation failed: 418 I'm a teapot", CloudControllerException.class, new CloudOperationException(HttpStatus.I_AM_A_TEAPOT), null,
            },
            // (11) A random exception is thrown during update:
            {
                "create-service-brokers-step-input-09.json", null, null, "Controller operation failed: 418 I'm a teapot", CloudControllerException.class, null, new CloudOperationException(HttpStatus.I_AM_A_TEAPOT),
            },
            // (12) Create/update calls for should fail, because both create and update throw an exception and failsafe option is not set: 
            {
                "create-service-brokers-step-input-09.json", "create-service-brokers-step-output-09.json", null, "Controller operation failed: 403 Forbidden", CloudControllerException.class, new CloudOperationException(HttpStatus.FORBIDDEN), new CloudOperationException(HttpStatus.FORBIDDEN),
            },
            // (13) Create/update calls for both brokers should be made, although both create and update throw an exception but failsafe option is set: 
            {
                "create-service-brokers-step-input-09.json", "create-service-brokers-step-output-09.json", "Could not create service broker \"bar-broker\". Operation forbidden. Only admin users can manage service brokers!", null, null, new CloudOperationException(HttpStatus.FORBIDDEN), new CloudOperationException(HttpStatus.FORBIDDEN),
            },
            // (14) A service broker should be created, all necessary parameters are present and it is space scoped:
            {
                "create-service-brokers-step-input-10.json", "create-service-brokers-step-output-10.json", null, null, null, null, null,
            },
            // (15) The visibility of a service broker should be changed from global to space-scoped:
            {
                "create-service-brokers-step-input-11.json", "create-service-brokers-step-output-11.json", "Visibility of service broker \"foo-broker\" will not be changed from global to space-scoped, as visibility changes are not yet supported!", null, null, null, null,
            },
            // (16) The visibility of a service broker should be changed from space-scoped to global:
            {
                "create-service-brokers-step-input-12.json", "create-service-brokers-step-output-12.json", "Visibility of service broker \"foo-broker\" will not be changed from space-scoped to global, as visibility changes are not yet supported!", null, null, null, null,
            },
            // (17) A space-scoped service broker should be created on XSA:
            {
                "create-service-brokers-step-input-13.json", "create-service-brokers-step-output-01.json", "Service broker \"foo-broker\" will be created as global, since space-scoped service brokers are not yet supported on this platform!", null, null, null, null,
            },
// @formatter:on
        });
    }

    public CreateOrUpdateServiceBrokersStepTest(String inputLocation, String expectedOutputLocation, String expectedWarningMessage,
        String expectedExceptionMessage, Class<? extends Throwable> expectedExceptionClass, CloudOperationException createException,
        CloudOperationException updateException) {
        this.expectedOutputLocation = expectedOutputLocation;
        this.expectedWarningMessage = expectedWarningMessage;
        this.expectedExceptionMessage = expectedExceptionMessage;
        this.expectedExceptionClass = (expectedExceptionClass != null) ? expectedExceptionClass : SLException.class;
        this.inputLocation = inputLocation;
        this.updateException = updateException;
        this.createException = createException;
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

        StepOutput actualOutput = captureStepOutput();

        assertEquals(JsonUtil.toJson(expectedOutput, true), JsonUtil.toJson(actualOutput, true));
        if (expectedWarningMessage != null) {
            Mockito.verify(stepLogger)
                .warn(expectedWarningMessage);
        }

        List<CloudServiceBrokerExtended> actuallyCreatedServiceBrokers = StepsUtil.getServiceBrokersToCreate(context);
        Collections.sort(actuallyCreatedServiceBrokers, (broker1, broker2) -> broker1.getName()
            .compareTo(broker2.getName()));
        List<CloudServiceBrokerExtended> expectedServiceBrokersToCreate = new ArrayList<>(expectedOutput.createdServiceBrokers);
        expectedServiceBrokersToCreate.addAll(expectedOutput.updatedServiceBrokers);
        Collections.sort(expectedServiceBrokersToCreate, (broker1, broker2) -> broker1.getName()
            .compareTo(broker2.getName()));

        assertEquals(JsonUtil.toJson(expectedServiceBrokersToCreate, true), JsonUtil.toJson(actuallyCreatedServiceBrokers, true));
    }

    private void loadParameters() throws Exception {
        boolean shouldSucceed = true;
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
            expectedException.expect(expectedExceptionClass);
            shouldSucceed = false;
        } else {
            expectedOutput = JsonUtil.fromJson(TestUtil.getResourceAsString(expectedOutputLocation, getClass()), StepOutput.class);
        }
        context.setVariable(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS, shouldSucceed);
        input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, getClass()), StepInput.class);
        Mockito.when(configuration.getPlatformType())
            .thenReturn(input.platformType);
    }

    private void prepareContext() {
        StepsUtil.setAppsToDeploy(context, toCloudApplications(input.applications));
        StepsUtil.setSpaceId(context, input.spaceGuid);
    }

    private List<CloudApplicationExtended> toCloudApplications(List<SimpleApplication> applications) {
        return applications.stream()
            .map((application) -> application.toCloudApplication())
            .collect(Collectors.toList());
    }

    private void prepareClient() {
        Mockito.when(serviceBrokersGetter.getServiceBrokers(client))
            .thenReturn(input.existingServiceBrokers);
        if (updateException != null) {
            Mockito.doThrow(updateException)
                .when(client)
                .updateServiceBroker(Mockito.any());
        }
        if (createException != null) {
            Mockito.doThrow(createException)
                .when(serviceBrokerCreator)
                .createServiceBroker(Mockito.any(), Mockito.any());
        }
        for (SimpleApplication application : input.applications) {
            Mockito.when(client.getApplication(application.name))
                .thenReturn(application.toCloudApplication());
        }
    }

    private StepOutput captureStepOutput() {

        StepOutput actualOutput = new StepOutput();

        ArgumentCaptor<CloudServiceBrokerExtended> createArgumentCaptor = ArgumentCaptor.forClass(CloudServiceBrokerExtended.class);
        int expectedCreatedBrokersCnt = expectedOutput.createdServiceBrokers.size();
        Mockito.verify(serviceBrokerCreator, Mockito.times(expectedCreatedBrokersCnt))
            .createServiceBroker(Mockito.eq(client), createArgumentCaptor.capture());
        actualOutput.createdServiceBrokers = createArgumentCaptor.getAllValues();

        ArgumentCaptor<CloudServiceBrokerExtended> updateArgumentCaptor = ArgumentCaptor.forClass(CloudServiceBrokerExtended.class);
        int expectedUpdatedBrokersCnt = expectedOutput.updatedServiceBrokers.size();
        Mockito.verify(client, Mockito.times(expectedUpdatedBrokersCnt))
            .updateServiceBroker(updateArgumentCaptor.capture());
        actualOutput.updatedServiceBrokers = updateArgumentCaptor.getAllValues();

        return actualOutput;
    }

    private static class StepInput {
        List<CloudServiceBrokerExtended> existingServiceBrokers;
        List<SimpleApplication> applications;
        String spaceGuid;
        PlatformType platformType = PlatformType.CF;
    }

    private static class StepOutput {
        List<CloudServiceBrokerExtended> createdServiceBrokers;
        List<CloudServiceBrokerExtended> updatedServiceBrokers;
    }

    static class SimpleApplication {

        String name;
        Map<String, Object> attributes;

        CloudApplicationExtended toCloudApplication() {
            CloudApplicationExtended application = new CloudApplicationExtended(null, name);
            application.setEnv(MapUtil.asMap(com.sap.cloud.lm.sl.cf.core.Constants.ENV_DEPLOY_ATTRIBUTES, JsonUtil.toJson(attributes)));
            return application;
        }

    }

    @Override
    protected CreateOrUpdateServiceBrokersStep createStep() {
        return new CreateOrUpdateServiceBrokersStep();
    }

}
