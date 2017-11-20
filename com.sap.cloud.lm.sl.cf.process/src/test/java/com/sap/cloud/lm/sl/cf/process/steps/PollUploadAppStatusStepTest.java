package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.http.HttpStatus;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadInfo;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadInfo.State;
import com.sap.cloud.lm.sl.cf.core.model.ContextExtension;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.ScaleAppStepTest.SimpleApplication;
import com.sap.cloud.lm.sl.common.SLException;

@RunWith(Parameterized.class)
public class PollUploadAppStatusStepTest extends AsyncStepOperationTest<UploadAppStep> {

    private static final CloudFoundryException CFEXCEPTION = new CloudFoundryException(HttpStatus.BAD_REQUEST);
    private static final String UPLOAD_TOKEN = "tokenString";
    private static final String APP_NAME = "test-app-1";

    private final boolean supportsExtensions;
    private final State uploadState;
    private final ExecutionStatus previousStatus;
    private final ExecutionStatus expectedStatus;
    private final String expectedCfExceptionMessage;
    private final String uploadToken;

    private SimpleApplication application = new SimpleApplication(APP_NAME, 2);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) The previous step used synchronous upload and finished successfully:
            {
                false, null, null, ExecutionStatus.SUCCESS, ExecutionStatus.SUCCESS, null,
            },
            // (01) For some reason the previous step was skipped and should be retried:
            {
                false, null, null, ExecutionStatus.SKIPPED, ExecutionStatus.FAILED, null,
            },
            // (02)
            {
                false, null, null, ExecutionStatus.FAILED , ExecutionStatus.FAILED, null,
            },
            // (03) The previous step used asynchronous upload but getting the upload progress fails with an exception:
            {
                true , State.RUNNING , UPLOAD_TOKEN, ExecutionStatus.RUNNING, null, StepsUtil.createException(CFEXCEPTION).getMessage(),
            },
            // (04) The previous step used asynchronous upload and it finished successfully:
            {
                true , State.FINISHED, UPLOAD_TOKEN, ExecutionStatus.RUNNING, ExecutionStatus.SUCCESS, null,
            },
            // (05) The previous step used asynchronous upload but it is still not finished:
            {
                true , State.RUNNING , UPLOAD_TOKEN, ExecutionStatus.RUNNING, ExecutionStatus.RUNNING, null,
            },
            // (06) The previous step used asynchronous upload but it is still not finished:
            {
                true , State.UNKNOWN , UPLOAD_TOKEN, ExecutionStatus.RUNNING, ExecutionStatus.RUNNING, null,
            },
            // (07) The previous step used asynchronous upload but it is still not finished:
            {
                true , State.QUEUED  , UPLOAD_TOKEN, ExecutionStatus.RUNNING, ExecutionStatus.RUNNING, null,
            },
            // (08) The previous step used asynchronous upload but it failed:
            {
                true , State.FAILED  , UPLOAD_TOKEN, ExecutionStatus.RUNNING, ExecutionStatus.FAILED, null,
            },
// @formatter:on
        });
    }

    public PollUploadAppStatusStepTest(boolean supportsExtenstions, State uploadState, String uploadToken, ExecutionStatus previousStatus,
        ExecutionStatus expectedStatus, String expectedCfExceptionMessage) {
        this.supportsExtensions = supportsExtenstions;
        this.uploadState = uploadState;
        this.uploadToken = uploadToken;
        this.previousStatus = previousStatus;
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
        Thread.sleep(1); // Simulate the time it takes to upload a file. Without this, some tests
                         // may fail on faster machines...
        step.createStepLogger(context);
        testExecuteOperations();
    }

    private void prepareExpectedException() {
        if (expectedCfExceptionMessage != null) {
            expectedException.expectMessage(expectedCfExceptionMessage);
            expectedException.expect(SLException.class);
        }
    }

    private void prepareClient() {
        if (!supportsExtensions) {
            when(clientProvider.getCloudFoundryClient(anyString(), anyString(), anyString(), anyString())).thenReturn(null);
            return;
        }
        if (expectedCfExceptionMessage != null) {
            when(clientExtensions.getUploadProgress(UPLOAD_TOKEN)).thenThrow(CFEXCEPTION);
        } else {
            UploadInfo uploadInfo = mock(UploadInfo.class);
            when(uploadInfo.getUploadJobState()).thenReturn(uploadState);
            when(clientExtensions.getUploadProgress(UPLOAD_TOKEN)).thenReturn(uploadInfo);
        }
    }

    private void prepareContext() {
        StepsTestUtil.mockApplicationsToDeploy(Arrays.asList(application.toCloudApplication()), context);
        context.setVariable(Constants.VAR_APPS_INDEX, 0);
        context.setVariable(Constants.VAR_UPLOAD_TOKEN, uploadToken);
        context.setVariable("StepExecution", previousStatus.toString());
        when(context.getProcessInstanceId()).thenReturn("test");
        ContextExtension extension = new ContextExtension(context.getProcessInstanceId(), "uploadState", previousStatus.toString(), null,
            null);
        when(contextExtensionDao.find(context.getProcessInstanceId(), "uploadState")).thenReturn(extension);
    }

    @Override
    protected UploadAppStep createStep() {
        return new UploadAppStep();
    }

    @Override
    protected List<AsyncStepOperation> getAsyncOperations() {
        return step.getAsyncStepOperations();
    }

    @Override
    protected void validateOperationExecutionResult(ExecutionStatus result) {
        assertEquals(expectedStatus.toString(), result.toString());
    }

}
