package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.ScaleAppStepTest.SimpleApplication;

@RunWith(Parameterized.class)
public class StartAppStepTest extends AbstractStepTest<StartAppStep> {

    private final SimpleApplication application;
    private final boolean isAppStarted;
    private final boolean supportsExtensions;

    private CloudFoundryOperations client = Mockito.mock(CloudFoundryOperations.class);
    private ClientExtensions clientExtensions = Mockito.mock(ClientExtensions.class);

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            {
                new SimpleApplication("test-app-1", 2), true, true
            },
            {
                new SimpleApplication("test-app-1", 2), true, false
            },
            {
                new SimpleApplication("test-app-1", 2), false, true
            },
            {
                new SimpleApplication("test-app-1", 2), false, false
            }
            // @formatter:on
        });
    }

    public StartAppStepTest(SimpleApplication application, boolean isAppStarted, boolean supportsExtenstions) {
        this.application = application;
        this.isAppStarted = isAppStarted;
        this.supportsExtensions = supportsExtenstions;
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        prepareClient();
    }

    @Test
    public void testPollStatus() throws Exception {
        ExecutionStatus status = step.pollStatus(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(), status.toString());

        validateApplication();
    }

    private void prepareContext() {
        StepsUtil.setAppsToDeploy(context, Arrays.asList(application.toCloudApplication()));
        context.setVariable(Constants.VAR_APPS_INDEX, 0);
    }

    private void prepareClient() {
        if (supportsExtensions) {
            step.extensionsSupplier = (context) -> clientExtensions;
            Mockito.when(clientExtensions.stageApplication(application.name)).thenReturn(null);
        } else {
            step.extensionsSupplier = (context) -> null;
        }
        step.clientSupplier = (context) -> client;
        Mockito.when(client.getApplication(application.name)).thenReturn(new CloudApplication(null, application.name) {
            @Override
            public AppState getState() {
                return AppState.STARTED;
            }
        });
    }

    private void validateApplication() {
        if (isAppStarted) {
            Mockito.verify(client).stopApplication(application.name);
        }
    }

    @Override
    protected StartAppStep createStep() {
        return new StartAppStep();
    }

}
