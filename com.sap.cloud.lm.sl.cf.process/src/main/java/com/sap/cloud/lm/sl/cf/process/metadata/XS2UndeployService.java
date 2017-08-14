package com.sap.cloud.lm.sl.cf.process.metadata;

import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.BuildCloudUndeployModelStep;
import com.sap.cloud.lm.sl.cf.process.steps.DeleteDiscontinuedConfigurationEntriesStep;
import com.sap.cloud.lm.sl.cf.process.steps.DeleteServiceBrokersStep;
import com.sap.cloud.lm.sl.cf.process.steps.DeleteServicesStep;
import com.sap.cloud.lm.sl.cf.process.steps.DeleteSubscriptionsStep;
import com.sap.cloud.lm.sl.cf.process.steps.DetectDeployedMtaStep;
import com.sap.cloud.lm.sl.cf.process.steps.PrepareToRestartServiceBrokersStep;
import com.sap.cloud.lm.sl.cf.process.steps.PrepareToUndeployAppsStep;
import com.sap.cloud.lm.sl.cf.process.steps.PrepareToUndeployStep;
import com.sap.cloud.lm.sl.cf.process.steps.RestartUpdatedSubscribersStep;
import com.sap.cloud.lm.sl.cf.process.steps.UnregisterServiceUrlsStep;
import com.sap.cloud.lm.sl.cf.process.steps.UpdateSubscribersStep;
import com.sap.cloud.lm.sl.slp.model.ParameterMetadata;
import com.sap.cloud.lm.sl.slp.model.ParameterMetadata.ParameterType;
import com.sap.cloud.lm.sl.slp.model.RoadmapStepMetadata;
import com.sap.cloud.lm.sl.slp.model.ServiceMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

public class XS2UndeployService {

    private static final StepMetadata STEP_START = StepMetadata.builder().id("startEvent").displayName("Start").description(
        "Start").build();
    private static final StepMetadata STEP_END = StepMetadata.builder().id("endEvent").displayName("End").description("End").build();

    private static final RoadmapStepMetadata ROADMAP_PREPARE = RoadmapStepMetadata.builder().id("roadmap_prepare").displayName(
        "Prepare").description("Prepare").children(STEP_START, DetectDeployedMtaStep.getMetadata(), PrepareToUndeployStep.getMetadata(),
            BuildCloudUndeployModelStep.getMetadata()).build();
    private static final RoadmapStepMetadata ROADMAP_EXECUTE = RoadmapStepMetadata.builder().id("roadmap_execute").displayName(
        "Execute").description("Execute").children(DeleteSubscriptionsStep.getMetadata(),
            DeleteDiscontinuedConfigurationEntriesStep.getMetadata(), UnregisterServiceUrlsStep.getMetadata(),
            DeleteServiceBrokersStep.getMetadata(), PrepareToUndeployAppsStep.getMetadata(), DeleteServicesStep.getMetadata(),
            UpdateSubscribersStep.getMetadata(), RestartUpdatedSubscribersStep.getMetadata(),
            PrepareToRestartServiceBrokersStep.getMetadata(), STEP_END).build();

    private final static Set<ParameterMetadata> PARAMS = new HashSet<ParameterMetadata>();

    static {
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_DELETE_SERVICES).type(ParameterType.BOOLEAN).defaultValue(false).build());
        PARAMS.add(
            ParameterMetadata.builder().id(Constants.PARAM_DELETE_SERVICE_BROKERS).type(ParameterType.BOOLEAN).defaultValue(false).build());
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_MTA_ID).required(true).build());
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_NO_RESTART_SUBSCRIBED_APPS).type(ParameterType.BOOLEAN).defaultValue(
            false).build());
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS).type(ParameterType.BOOLEAN).defaultValue(
            false).build());
    }

    public static ServiceMetadata getMetadata() {
        return ServiceMetadata.builder().id(Constants.UNDEPLOY_SERVICE_ID).displayName("Undeploy").description("Undeploy").children(
            ROADMAP_PREPARE, ROADMAP_EXECUTE).versions(Constants.SERVICE_VERSION_1_0).parameters(PARAMS).build();
    }

}
