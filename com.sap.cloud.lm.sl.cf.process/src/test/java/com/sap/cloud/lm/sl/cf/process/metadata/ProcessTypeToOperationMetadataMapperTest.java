package com.sap.cloud.lm.sl.cf.process.metadata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

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

    @Test
    public void testGetPreviousDiagramIdsDeployProcessType() {
        Assertions.assertEquals(0, processTypeToOperationMetadataMapper.getPreviousDiagramIds(ProcessType.DEPLOY)
                                                                       .size());
    }

    @Test
    public void testGetPreviousDiagramIdsBlueGreenDeployProcessType() {
        Assertions.assertEquals(0, processTypeToOperationMetadataMapper.getPreviousDiagramIds(ProcessType.BLUE_GREEN_DEPLOY)
                                                                       .size());
    }

    @Test
    public void testGetPreviousDiagramIdsUndeployProcessType() {
        Assertions.assertEquals(0, processTypeToOperationMetadataMapper.getPreviousDiagramIds(ProcessType.UNDEPLOY)
                                                                       .size());
    }

    @Test
    public void testGetPreviousDiagramIdsCtsDeployProcessType() {
        Assertions.assertEquals(0, processTypeToOperationMetadataMapper.getPreviousDiagramIds(ProcessType.CTS_DEPLOY)
                                                                       .size());
    }
}
