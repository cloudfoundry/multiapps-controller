package com.sap.cloud.lm.sl.cf.process.metadata;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.model.ProcessType;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.ServiceMetadata;

@Component
public class ProcessTypeToServiceMetadataMapper {

    public ServiceMetadata getServiceMetadata(ProcessType processType) {
        if (processType.equals(ProcessType.DEPLOY)) {
            return XS2DeployService.getMetadata();
        }
        if (processType.equals(ProcessType.BLUE_GREEN_DEPLOY)) {
            return XS2BlueGreenDeployService.getMetadata();
        }
        if (processType.equals(ProcessType.UNDEPLOY)) {
            return XS2UndeployService.getMetadata();
        }
        throw new SLException(Messages.UNSUPPORTED_PROCESS_TYPE, processType.toString());
    }

}
