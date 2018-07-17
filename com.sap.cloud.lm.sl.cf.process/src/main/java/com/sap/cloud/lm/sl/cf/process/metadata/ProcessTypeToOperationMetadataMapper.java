package com.sap.cloud.lm.sl.cf.process.metadata;

import java.util.List;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.SLException;

public class ProcessTypeToOperationMetadataMapper {

    public OperationMetadata getOperationMetadata(ProcessType processType) {
        if (processType.equals(ProcessType.DEPLOY)) {
            return DeployMetadata.getMetadata();
        }
        if (processType.equals(ProcessType.BLUE_GREEN_DEPLOY)) {
            return BlueGreenDeployMetadata.getMetadata();
        }
        if (processType.equals(ProcessType.UNDEPLOY)) {
            return UndeployMetadata.getMetadata();
        }
        if (processType.equals(ProcessType.CTS_DEPLOY)) {
            return CtsDeployMetadata.getMetadata();
        }
        throw new SLException(Messages.UNSUPPORTED_PROCESS_TYPE, processType.toString());
    }

    public String getActivitiDiagramId(ProcessType processType) {
        return getOperationMetadata(processType).getActivitiDiagramId();
    }
    
    public List<String> getPreviousActivitiDiagramIds(ProcessType processType) {
        return getOperationMetadata(processType).getPreviousActivitiDiagramIds();
    }

}
