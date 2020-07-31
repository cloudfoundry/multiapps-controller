package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudPackage;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudPackage;
import org.cloudfoundry.client.lib.domain.ImmutableErrorDetails;
import org.cloudfoundry.client.lib.domain.ImmutableUpload;
import org.cloudfoundry.client.lib.domain.Status;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.multiapps.controller.process.steps.ScaleAppStepTest.SimpleApplication;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.http.HttpStatus;

@RunWith(Parameterized.class)
public class PollUploadAppStatusExecutionTest extends AsyncStepOperationTest<UploadAppStep> {

    private static final CloudOperationException CLOUD_OPERATION_EXCEPTION_BAD_REQUEST = new CloudOperationException(HttpStatus.BAD_REQUEST);
    private static final CloudOperationException CLOUD_OPERATION_EXCEPTION_NOT_FOUND = new CloudOperationException(HttpStatus.NOT_FOUND);
    private static final UUID PACKAGE_GUID = UUID.fromString("20886182-1802-11e9-ab14-d663bd873d93");
    private static final String APP_NAME = "test-app-1";
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();
    private final Status uploadState;
    private final AsyncExecutionState expectedStatus;
    private final Exception expectedCfException;
    private final SimpleApplication application = new SimpleApplication(APP_NAME, 2);

    public PollUploadAppStatusExecutionTest(Status uploadState, AsyncExecutionState expectedStatus, Exception expectedCfException) {
        this.uploadState = uploadState;
        this.expectedStatus = expectedStatus;
        this.expectedCfException = expectedCfException;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) The previous step used asynchronous upload but getting the upload progress fails with an exception:
            {
                null, null, CLOUD_OPERATION_EXCEPTION_BAD_REQUEST,
            },
            // (01) The previous step used asynchronous upload and it finished successfully:
            {
                Status.READY, AsyncExecutionState.FINISHED, null,
            },
            // (02) The previous step used asynchronous upload but it is still not finished:
            {
                Status.AWAITING_UPLOAD, AsyncExecutionState.RUNNING, null,
            },
            // (03) The previous step used asynchronous upload but it is still not finished:
            {
                Status.PROCESSING_UPLOAD, AsyncExecutionState.RUNNING, null,
            },
            // (04) The previous step used asynchronous upload but it failed with status EXPIRED:
            {
                Status.EXPIRED, AsyncExecutionState.ERROR, null,
            },
            // (05) The previous step used asynchronous upload but it failed with status FAILED:
            {
                Status.FAILED, AsyncExecutionState.ERROR, null,
            },
            // (06) The requested package is not found:
            {
                null, null, CLOUD_OPERATION_EXCEPTION_NOT_FOUND,
            },
// @formatter:on
        });
    }

    @Before
    public void setUp() {
        prepareContext();
        prepareClient();
        prepareExpectedException();
    }

    @Test
    public void testPollStatus() throws Exception {
        step.initializeStepLogger(execution);
        testExecuteOperations();
    }

    private void prepareExpectedException() {
        if (expectedCfException != null) {
            expectedException.expectMessage(expectedCfException.getMessage());
            expectedException.expect(expectedCfException.getClass());
        }
    }

    private void prepareClient() {
        if (expectedCfException != null) {
            when(client.getUploadStatus(PACKAGE_GUID)).thenThrow(expectedCfException);
        } else {
            Upload upload = ImmutableUpload.builder()
                                           .status(uploadState)
                                           .errorDetails(ImmutableErrorDetails.builder()
                                                                              .description("Something happened!")
                                                                              .build())
                                           .build();
            when(client.getUploadStatus(PACKAGE_GUID)).thenReturn(upload);
        }
    }

    private void prepareContext() {
        StepsTestUtil.mockApplicationsToDeploy(Collections.singletonList(application.toCloudApplication()), execution);
        context.setVariable(Variables.MODULES_INDEX, 0);
        CloudPackage cloudPackage = ImmutableCloudPackage.builder()
                                                         .metadata(ImmutableCloudMetadata.builder()
                                                                                         .guid(PACKAGE_GUID)
                                                                                         .build())
                                                         .build();
        context.setVariable(Variables.CLOUD_PACKAGE, cloudPackage);
    }

    @Override
    protected UploadAppStep createStep() {
        return new UploadAppStep();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return step.getAsyncStepExecutions(wrapper);
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedStatus.toString(), result.toString());
    }

}
