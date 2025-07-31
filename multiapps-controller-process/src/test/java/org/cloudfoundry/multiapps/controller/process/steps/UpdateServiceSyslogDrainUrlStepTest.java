package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UpdateServiceSyslogDrainUrlStepTest extends SyncFlowableStepTest<UpdateServiceSyslogDrainUrlStep> {

    private static final String SYSLOG_DRAIN_URL = "test-syslog-url";

    @Test
    void testUpdateServiceSyslogDrainUrl() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(FALSE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        step.execute(execution);
        verify(client).updateServiceSyslogDrainUrl(SERVICE_NAME, SYSLOG_DRAIN_URL);
    }

    @Test
    void testSkipParametersUpdate() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceWithSkipUpdate();
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        step.execute(execution);
        verify(client, never()).updateServiceSyslogDrainUrl(SERVICE_NAME, SYSLOG_DRAIN_URL);
    }

    @Test
    void testExceptionIsThrownOnMandatoryServiceSyslogDrainUrlUpdateBadGateway() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(FALSE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceSyslogDrainUrlUpdate(HttpStatus.BAD_GATEWAY);

        assertThrows(SLException.class, () -> step.execute(execution));
        assertExecutionStepStatus(RETRY_STEP_EXECUTION_STATUS);
    }

    @Test
    void testExceptionIsThrownOnMandatoryServiceSyslogDrainUrlUpdateInternalServerError() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(FALSE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceSyslogDrainUrlUpdate(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThrows(SLException.class, () -> step.execute(execution));
        assertExecutionStepStatus(RETRY_STEP_EXECUTION_STATUS);
    }

    @Test
    void testExceptionIsNotThrownOnOptionalServiceSyslogDrainUrlUpdateBadGateway() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(TRUE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceSyslogDrainUrlUpdate(HttpStatus.BAD_GATEWAY);

        step.execute(execution);

        assertExecutionStepStatus(DONE_STEP_EXECUTION_STATUS);
    }

    @Test
    void testExceptionIsNotThrownOnOptionalServiceSyslogDrainUrlUpdateInternalServerError() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(TRUE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceSyslogDrainUrlUpdate(HttpStatus.INTERNAL_SERVER_ERROR);

        step.execute(execution);

        assertExecutionStepStatus(DONE_STEP_EXECUTION_STATUS);
    }

    @Test
    void testOperationType() {
        assertEquals(ServiceOperation.Type.UPDATE, step.getOperationType());
    }

    private void throwExceptionOnServiceSyslogDrainUrlUpdate(HttpStatus httpStatus) {
        Mockito.doThrow(new CloudOperationException(httpStatus, "Error occurred"))
               .when(client)
               .updateServiceSyslogDrainUrl(any(), any());
    }

    private CloudServiceInstanceExtended buildServiceWithSkipUpdate() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(UUID.randomUUID())
                                                                                    .build())
                                                    .name(SERVICE_NAME)
                                                    .syslogDrainUrl(SYSLOG_DRAIN_URL)
                                                    .shouldSkipSyslogUrlUpdate(true)
                                                    .build();
    }

    @Override
    protected UpdateServiceSyslogDrainUrlStep createStep() {
        return new UpdateServiceSyslogDrainUrlStep();
    }

}
