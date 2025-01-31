package org.cloudfoundry.multiapps.controller.process.metadata;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessTypeToOperationMetadataMapperTest {

    private ProcessTypeToOperationMetadataMapper processTypeToOperationMetadataMapper;

    @BeforeEach
    void setUp() {
        processTypeToOperationMetadataMapper = new ProcessTypeToOperationMetadataMapper();
    }

    @Test
    void testGetDiagramDeployProcessType() {
        Assertions.assertEquals(Constants.DEPLOY_SERVICE_ID, processTypeToOperationMetadataMapper.getDiagramId(ProcessType.DEPLOY));
    }

    @Test
    void testGetDiagramBlueGreenDeployProcessType() {
        Assertions.assertEquals(Constants.BLUE_GREEN_DEPLOY_SERVICE_ID,
                                processTypeToOperationMetadataMapper.getDiagramId(ProcessType.BLUE_GREEN_DEPLOY));
    }

    @Test
    void testGetDiagramUndeployProcessType() {
        Assertions.assertEquals(Constants.UNDEPLOY_SERVICE_ID, processTypeToOperationMetadataMapper.getDiagramId(ProcessType.UNDEPLOY));
    }

    @Test
    void testGetDiagramCtsDeployProcessType() {
        Assertions.assertEquals(Constants.CTS_DEPLOY_SERVICE_ID, processTypeToOperationMetadataMapper.getDiagramId(ProcessType.CTS_DEPLOY));
    }

    @Test
    void testGetDiagramRollbackMtaProcessType() {
        Assertions.assertEquals(Constants.ROLLBACK_MTA_SERVICE_ID,
                                processTypeToOperationMetadataMapper.getDiagramId(ProcessType.ROLLBACK_MTA));
    }

}
