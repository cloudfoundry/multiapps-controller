package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.StagingExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientFactory.PlatformType;
import com.sap.cloud.lm.sl.cf.core.dao.ContextExtensionDao;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationEntityUpdater;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.ArgumentMatcherProvider;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class CreateAppStepTest extends AbstractStepTest<CreateAppStep> {

    private final StepInput stepInput;
    private final String expectedExceptionMessage;

    private CloudApplicationExtended application;

    private CloudFoundryOperations client = Mockito.mock(CloudFoundryOperations.class);
    private ClientExtensions clientExtensions = Mockito.mock(ClientExtensions.class);
    private ApplicationEntityUpdater applicationUpdater = Mockito.mock(ApplicationEntityUpdater.class);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    ContextExtensionDao dao;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Disk quota is 0:
            {
                "create-app-step-input-00.json", null, PlatformType.XS2
            },
            // (1) Memory is 0:
            {
                "create-app-step-input-01.json", null, PlatformType.CF
            },
            // (2) Everything is specified properly:
            {
                "create-app-step-input-02.json", null, PlatformType.XS2
            },
            // (3) Binding parameters exist, and the services do too:
            {
                "create-app-step-input-03.json", null, PlatformType.CF
            },
            // (4) Binding parameters exist, but the services do not:
            {
                "create-app-step-input-04.json", "Cannot bind application \"application\" to non-existing service \"service-2\"!", null,
            },
// @formatter:on
        });
    }

    public CreateAppStepTest(String stepInput, String expectedExceptionMessage, PlatformType platform) throws Exception {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, CreateAppStepTest.class), StepInput.class);
        this.stepInput.platform = platform;
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

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        validateClient();
        validateApplicationUpdate();
    }

    private void validateApplicationUpdate() {
        if (stepInput.platform == PlatformType.CF) {
            Mockito.verify(applicationUpdater).updateApplicationStaging(eq(client), eq(application.getName()),
                (StagingExtended) eq(application.getStaging()));
        }
    }

    private void loadParameters() {
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
        }
        application = stepInput.applications.get(stepInput.applicationIndex);
    }

    private void prepareContext() {
        StepsUtil.setAppsToDeploy(context, stepInput.applications);
        StepsUtil.setServicesToCreate(context, Collections.emptyList());
        context.setVariable(Constants.VAR_APPS_INDEX, stepInput.applicationIndex);
    }

    private void prepareClient() {
        step.extensionsSupplier = (context) -> clientExtensions;
        step.clientSupplier = (context) -> client;
        step.platformTypeSupplier = () -> stepInput.platform;
        for (CloudService service : stepInput.services) {
            Mockito.when(client.getService(service.getName())).thenReturn(service);
        }
    }

    private void validateClient() {
        Integer diskQuota = (application.getDiskQuota() != 0) ? application.getDiskQuota() : null;
        Integer memory = (application.getMemory() != 0) ? application.getMemory() : null;

        Mockito.verify(client).createApplication(eq(application.getName()),
            argThat(ArgumentMatcherProvider.getStagingMatcher(application.getStaging())), eq(diskQuota), eq(memory),
            eq(application.getUris()), eq(Collections.emptyList()));
        for (String service : application.getServices()) {
            if (application.getBindingParameters() == null || application.getBindingParameters().get(service) == null) {
                Mockito.verify(client).bindService(application.getName(), service);
            } else {
                Mockito.verify(clientExtensions).bindService(application.getName(), service,
                    application.getBindingParameters().get(service));
            }
        }
        Mockito.verify(client).updateApplicationEnv(eq(application.getName()), eq(application.getEnvAsMap()));
    }

    private static class StepInput {
        List<CloudApplicationExtended> applications = Collections.emptyList();
        List<CloudService> services = Collections.emptyList();
        int applicationIndex;
        PlatformType platform;
    }

    @Override
    protected CreateAppStep createStep() {
        return new CreateAppStep();
    }

}
