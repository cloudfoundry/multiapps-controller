package com.sap.cloud.lm.sl.cf.process.metadata;

import com.sap.cloud.lm.sl.cf.core.model.ProcessType;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.ServiceMetadata;

public class ProcessTypeToServiceMetadataMapper {

    public static ServiceMetadata getServiceMetadata(ProcessType processType) {
        switch (processType) {
            case BLUE_GREEN_DEPLOY:
                return XS2BlueGreenDeployService.getMetadata();
            case UNDEPLOY:
                return XS2UndeployService.getMetadata();
            case DEPLOY:
                return XS2DeployService.getMetadata();
            default:
                throw new SLException(Messages.UNSUPPORTED_PROCESS_TYPE, processType.toString());
        }
    }

}
