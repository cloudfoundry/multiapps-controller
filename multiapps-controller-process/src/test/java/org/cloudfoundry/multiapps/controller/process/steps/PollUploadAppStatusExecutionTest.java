package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.process.steps.ScaleAppStepTest.SimpleApplication;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableErrorDetails;
import com.sap.cloudfoundry.client.facade.domain.ImmutableUpload;
import com.sap.cloudfoundry.client.facade.domain.Status;
import com.sap.cloudfoundry.client.facade.domain.Upload;

class PollUploadAppStatusExecutionTest extends AsyncStepOperationTest<UploadAppStep> {

    private static final CloudOperationException CLOUD_OPERATION_EXCEPTION_BAD_REQUEST = new CloudOperationException(HttpStatus.BAD_REQUEST);
    private static final CloudOperationException CLOUD_OPERATION_EXCEPTION_NOT_FOUND = new CloudOperationException(HttpStatus.NOT_FOUND);
    private static final UUID PACKAGE_GUID = UUID.fromString("20886182-1802-11e9-ab14-d663bd873d93");
    private static final String APP_NAME = "test-app-1";
    private final SimpleApplication application = new SimpleApplication(APP_NAME, 2);

    private AsyncExecutionState expectedStatus;

    public static Stream<Arguments> testPollStatus() {
        return Stream.of(
// @formatter:off
            // (00) The previous step used asynchronous upload but getting the upload progress fails with an exception:
            Arguments.of(null, null, CLOUD_OPERATION_EXCEPTION_BAD_REQUEST),
            // (01) The previous step used asynchronous upload and it finished successfully:
            Arguments.of(Status.READY, AsyncExecutionState.FINISHED, null),
            // (02) The previous step used asynchronous upload but it is still not finished:
            Arguments.of(Status.AWAITING_UPLOAD, AsyncExecutionState.RUNNING, null),
            // (03) The previous step used asynchronous upload but it is still not finished:
            Arguments.of(Status.PROCESSING_UPLOAD, AsyncExecutionState.RUNNING, null),
            // (04) The previous step used asynchronous upload but it failed with status EXPIRED:
            Arguments.of(Status.EXPIRED, AsyncExecutionState.ERROR, null),
            // (05) The previous step used asynchronous upload but it failed with status FAILED:
            Arguments.of(Status.FAILED, AsyncExecutionState.ERROR, null),
            // (06) The requested package is not found:
            Arguments.of(null, null, CLOUD_OPERATION_EXCEPTION_NOT_FOUND)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testPollStatus(Status uploadState, AsyncExecutionState expectedStatus, Exception expectedCfException) {
        this.expectedStatus = expectedStatus;
        initializeParameters(uploadState, expectedCfException);
        if (expectedCfException != null) {
            Exception exception = assertThrows(expectedCfException.getClass(), this::testExecuteOperations);
            assertTrue(exception.getMessage()
                                .contains(expectedCfException.getMessage()));
            return;
        }
        step.initializeStepLogger(execution);
        testExecuteOperations();
    }

    public void initializeParameters(Status uploadState, Exception expectedCfException) {
        prepareContext();
        prepareClient(uploadState, expectedCfException);
    }

    private void prepareClient(Status uploadState, Exception expectedCfException) {
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
        StepsTestUtil.mockApplicationsToDeploy(List.of(application.toCloudApplication()), execution);
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
