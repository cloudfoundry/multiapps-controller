package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudJob.Status;
import org.cloudfoundry.client.lib.domain.Upload;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.ScaleAppStepTest.SimpleApplication;

@RunWith(Parameterized.class)
public class PollUploadAppStatusExecutionTest extends AsyncStepOperationTest<UploadAppStep> {

    private static final CloudOperationException CLOUD_OPERATION_EXCEPTION = new CloudOperationException(HttpStatus.BAD_REQUEST);
    private static final String UPLOAD_TOKEN = "tokenString";
    private static final String APP_NAME = "test-app-1";

    private final Status uploadState;
    private final AsyncExecutionState expectedStatus;
    private final String expectedCfExceptionMessage;

    private SimpleApplication application = new SimpleApplication(APP_NAME, 2);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) The previous step used asynchronous upload but getting the upload progress fails with an exception:
            {
                null, null, CLOUD_OPERATION_EXCEPTION.getMessage(),
            },
            // (01) The previous step used asynchronous upload and it finished successfully:
            {
                Status.FINISHED, AsyncExecutionState.FINISHED, null,
            },
            // (02) The previous step used asynchronous upload but it is still not finished:
            {
                Status.RUNNING, AsyncExecutionState.RUNNING, null,
            },
            // (03) The previous step used asynchronous upload but it is still not finished:
            {
                Status.QUEUED, AsyncExecutionState.RUNNING, null,
            },
            // (04) The previous step used asynchronous upload but it failed:
            {
               Status.FAILED, AsyncExecutionState.ERROR, null,
            },
// @formatter:on
        });
    }

    public PollUploadAppStatusExecutionTest(Status uploadState, AsyncExecutionState expectedStatus, String expectedCfExceptionMessage) {
        this.uploadState = uploadState;
        this.expectedStatus = expectedStatus;
        this.expectedCfExceptionMessage = expectedCfExceptionMessage;
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        prepareClient();
        prepareExpectedException();
    }

    @Test
    public void testPollStatus() throws Exception {
        step.initializeStepLogger(context);
        testExecuteOperations();
    }

    private void prepareExpectedException() {
        if (expectedCfExceptionMessage != null) {
            expectedException.expectMessage(expectedCfExceptionMessage);
            expectedException.expect(CloudControllerException.class);
        }
    }

    private void prepareClient() {
        if (expectedCfExceptionMessage != null) {
            when(client.getUploadStatus(UPLOAD_TOKEN)).thenThrow(CLOUD_OPERATION_EXCEPTION);
        } else {
            when(client.getUploadStatus(UPLOAD_TOKEN)).thenReturn(new Upload(uploadState, null));
        }
    }

    private void prepareContext() {
        StepsTestUtil.mockApplicationsToDeploy(Arrays.asList(application.toCloudApplication()), context);
        context.setVariable(Constants.VAR_MODULES_INDEX, 0);
        context.setVariable(Constants.VAR_UPLOAD_TOKEN, UPLOAD_TOKEN);
    }

    @Override
    protected UploadAppStep createStep() {
        return new UploadAppStep();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ExecutionWrapper wrapper) {
        return step.getAsyncStepExecutions(wrapper);
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedStatus.toString(), result.toString());
    }

}
