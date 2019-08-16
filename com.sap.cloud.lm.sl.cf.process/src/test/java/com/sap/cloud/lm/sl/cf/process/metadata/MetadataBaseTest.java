package com.sap.cloud.lm.sl.cf.process.metadata;

import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;

public abstract class MetadataBaseTest {

    @Test
    public void testGetMetadata() {
        OperationMetadata operationMetadata = getMetadata();
        assertVersions(operationMetadata, getVersions());
        assertDiagramId(operationMetadata, getDiagramId());
        String[] parametersIds = getParametersIds();
        Assertions.assertEquals(parametersIds.length, operationMetadata.getParameters()
                                                                       .size());
        assertAllParametersExist(operationMetadata, parametersIds);
    }

    protected abstract OperationMetadata getMetadata();

    protected abstract String getDiagramId();

    protected abstract String[] getVersions();

    protected abstract String[] getParametersIds();

    private void assertVersions(OperationMetadata operationMetadata, String[] versions) {
        Assertions.assertArrayEquals(versions, operationMetadata.getVersions()
                                                                .toArray(new String[0]));
    }

    private void assertDiagramId(OperationMetadata operationMetadata, String diagramId) {
        Assertions.assertEquals(diagramId, operationMetadata.getDiagramId());
    }

    private void assertAllParametersExist(OperationMetadata operationMetadata, String[] parametersIds) {
        for (String parameterId : parametersIds) {
            Assertions.assertTrue(operationsMetadataContainsId(operationMetadata, parameterId));
        }
    }

    private boolean operationsMetadataContainsId(OperationMetadata operationMetadata, String parameterId) {
        return operationMetadata.getParameters()
                                .stream()
                                .map(ParameterMetadata::getId)
                                .anyMatch(currentParameterId -> Objects.equals(currentParameterId, parameterId));
    }
}
