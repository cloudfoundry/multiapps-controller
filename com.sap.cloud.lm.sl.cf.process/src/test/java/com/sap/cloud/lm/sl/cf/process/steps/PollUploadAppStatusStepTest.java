package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

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
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadInfo;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadInfo.State;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.ScaleAppStepTest.SimpleApplication;
import com.sap.cloud.lm.sl.common.SLException;

@RunWith(Parameterized.class)
public class PollUploadAppStatusStepTest extends AbstractStepTest<PollUploadAppStatusStep> {

    private static final CloudFoundryException CFEXCEPTION = new CloudFoundryException(HttpStatus.BAD_REQUEST);
    private static final String UPLOAD_TOKEN = "tokenString";
    private static final String APP_NAME = "test-app-1";

    private final boolean supportsExtensions;
    private final State uploadState;
    private final ExecutionStatus previousStatus;
    private final ExecutionStatus expectedStatus;
    private final String expectedCfExceptionMessage;
    private final String uploadToken;

    private ClientExtensions clientExtensions = mock(ClientExtensions.class);
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
                false, null, null, ExecutionStatus.SKIPPED, ExecutionStatus.LOGICAL_RETRY, null,
            },
            // (02)
            {
                false, null, null, ExecutionStatus.FAILED , ExecutionStatus.LOGICAL_RETRY, null,
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
                true , State.FAILED  , UPLOAD_TOKEN, ExecutionStatus.RUNNING, ExecutionStatus.LOGICAL_RETRY, null,
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
        ExecutionStatus status = step.pollStatus(context);

        assertEquals(expectedStatus.toString(), status.toString());
    }

    private void prepareExpectedException() {
        if (expectedCfExceptionMessage != null) {
            expectedException.expectMessage(expectedCfExceptionMessage);
            expectedException.expect(SLException.class);
        }
    }

    private void prepareClient() {
        if (supportsExtensions) {
            step.extensionsSupplier = (context) -> clientExtensions;
            if (expectedCfExceptionMessage != null) {
                when(clientExtensions.getUploadProgress(UPLOAD_TOKEN)).thenThrow(CFEXCEPTION);
            } else {
                UploadInfo uploadInfo = mock(UploadInfo.class);
                when(uploadInfo.getUploadJobState()).thenReturn(uploadState);
                when(clientExtensions.getUploadProgress(UPLOAD_TOKEN)).thenReturn(uploadInfo);
            }
        } else {
            step.extensionsSupplier = (context) -> null;
        }
    }

    private void prepareContext() {
        StepsUtil.setAppsToDeploy(context, Arrays.asList(application.toCloudApplication()));
        context.setVariable(Constants.VAR_APPS_INDEX, 0);
        context.setVariable(Constants.VAR_UPLOAD_TOKEN, uploadToken);
        context.setVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName(), previousStatus.toString());
    }

    @Override
    protected PollUploadAppStatusStep createStep() {
        return new PollUploadAppStatusStep();
    }

}
