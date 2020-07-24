package org.cloudfoundry.multiapps.controller.process.metadata;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.OperationMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.Messages;

@Named
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

    public String getDiagramId(ProcessType processType) {
        return getOperationMetadata(processType).getDiagramId();
    }

}
