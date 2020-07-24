package org.cloudfoundry.multiapps.controller.process.metadata;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.metadata.ProcessTypeToOperationMetadataMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProcessTypeToOperationMetadataMapperTest {

    private ProcessTypeToOperationMetadataMapper processTypeToOperationMetadataMapper;

    @BeforeEach
    public void setUp() {
        processTypeToOperationMetadataMapper = new ProcessTypeToOperationMetadataMapper();
    }

    @Test
    public void testGetDiagramDeployProcessType() {
        Assertions.assertEquals(Constants.DEPLOY_SERVICE_ID, processTypeToOperationMetadataMapper.getDiagramId(ProcessType.DEPLOY));
    }

    @Test
    public void testGetDiagramBlueGreenDeployProcessType() {
        Assertions.assertEquals(Constants.BLUE_GREEN_DEPLOY_SERVICE_ID,
                                processTypeToOperationMetadataMapper.getDiagramId(ProcessType.BLUE_GREEN_DEPLOY));
    }

    @Test
    public void testGetDiagramUndeployProcessType() {
        Assertions.assertEquals(Constants.UNDEPLOY_SERVICE_ID, processTypeToOperationMetadataMapper.getDiagramId(ProcessType.UNDEPLOY));
    }

    @Test
    public void testGetDiagramCtsDeployProcessType() {
        Assertions.assertEquals(Constants.CTS_DEPLOY_SERVICE_ID, processTypeToOperationMetadataMapper.getDiagramId(ProcessType.CTS_DEPLOY));
    }

}
