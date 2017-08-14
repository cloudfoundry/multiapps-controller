package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.HdiResourceTypeEnum;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("computeZdmServicesForDeletionStep")
public class ComputeZdmServicesForDeletionStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeZdmServicesForDeletionStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("computeZdmServicesForDeletionTask").displayName("Compute ZDM Services For Deletion").description(
            "Compute ZDM Services For Deletion").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws Exception {
        List<String> servicesToDelete = new ArrayList<>();
        List<String> undeployModelServicesToDelete = StepsUtil.getServicesToDelete(context);

        for (String serviceToDelete : undeployModelServicesToDelete) {
            if (serviceToDelete.contains(HdiResourceTypeEnum.ACCESS.toString())
                && (serviceToDelete.endsWith(ApplicationColor.BLUE.asSuffix())
                    || serviceToDelete.endsWith(ApplicationColor.GREEN.asSuffix()))) {
                servicesToDelete.add(serviceToDelete);
            }
        }

        CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);
        Map<String, Map<String, String>> moduleNameToZdmResourcesMap = StepsUtil.getAppNameToZdmHdiServiceNamesMap(context);
        for (Entry<String, Map<String, String>> moduleEntry : moduleNameToZdmResourcesMap.entrySet()) {
            String moduleName = moduleEntry.getKey();
            Map<String, String> moduleMap = moduleEntry.getValue();
            String tempHdiServiceName = moduleMap.get(HdiResourceTypeEnum.TEMP.toString());
            if (tempHdiServiceName == null) {
                continue;
            }
            client.unbindService(moduleName, tempHdiServiceName);
            servicesToDelete.add(tempHdiServiceName);
        }

        context.setVariable(Constants.SHOULD_DELETE_ZDM_SERVICES, (!servicesToDelete.isEmpty()));

        StepsUtil.setServicesToDelete(context, servicesToDelete);

        return ExecutionStatus.SUCCESS;
    }

}
