package com.sap.cloud.lm.sl.cf.process.metadata;

import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.BuildCloudUndeployModelStep;
import com.sap.cloud.lm.sl.cf.process.steps.DeletePublishedDependenciesStep;
import com.sap.cloud.lm.sl.cf.process.steps.DeleteServiceBrokersStep;
import com.sap.cloud.lm.sl.cf.process.steps.DeleteServicesStep;
import com.sap.cloud.lm.sl.cf.process.steps.DeleteSubscriptionsStep;
import com.sap.cloud.lm.sl.cf.process.steps.DetectDeployedMtaStep;
import com.sap.cloud.lm.sl.cf.process.steps.PrepareToUndeployStep;
import com.sap.cloud.lm.sl.cf.process.steps.UndeployAppsStep;
import com.sap.cloud.lm.sl.cf.process.steps.UnregisterServiceUrlsStep;
import com.sap.cloud.lm.sl.cf.process.steps.UpdateSubscribersStep;
import com.sap.cloud.lm.sl.slp.model.ParameterMetadata;
import com.sap.cloud.lm.sl.slp.model.ParameterMetadata.ParameterType;
import com.sap.cloud.lm.sl.slp.model.RoadmapStepMetadata;
import com.sap.cloud.lm.sl.slp.model.ServiceMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

public class XS2UndeployServiceMetadata extends ServiceMetadata {

    // Steps:
    private static final StepMetadata STEP_START = new StepMetadata("startEvent", "Start", "Start");
    private static final StepMetadata STEP_END = new StepMetadata("endEvent", "End", "End");

    // Roadmap steps:
    private static final RoadmapStepMetadata ROADMAP_PREPARE = new RoadmapStepMetadata("roadmap_prepare", "Prepare", "Prepare",
        new StepMetadata[] { STEP_START, DetectDeployedMtaStep.getMetadata(), PrepareToUndeployStep.getMetadata(),
            BuildCloudUndeployModelStep.getMetadata(), });
    private static final RoadmapStepMetadata ROADMAP_EXECUTE = new RoadmapStepMetadata("roadmap_execute", "Execute", "Execute",
        new StepMetadata[] { DeleteSubscriptionsStep.getMetadata(), DeletePublishedDependenciesStep.getMetadata(),
            UnregisterServiceUrlsStep.getMetadata(), DeleteServiceBrokersStep.getMetadata(), UndeployAppsStep.getMetadata(),
            DeleteServicesStep.getMetadata(), UpdateSubscribersStep.getMetadata(), STEP_END });

    // Startup parameters:
    private final static Set<ParameterMetadata> PARAMS = new HashSet<ParameterMetadata>();

    static {
        PARAMS.add(new ParameterMetadata(Constants.PARAM_DELETE_SERVICES, false, ParameterType.BOOLEAN, false));
        PARAMS.add(new ParameterMetadata(Constants.PARAM_DELETE_SERVICE_BROKERS, false, ParameterType.BOOLEAN, false));
        PARAMS.add(new ParameterMetadata(Constants.PARAM_MTA_ID, true));
        PARAMS.add(new ParameterMetadata(Constants.PARAM_NO_RESTART_SUBSCRIBED_APPS, false, ParameterType.BOOLEAN, false));
    }

    public XS2UndeployServiceMetadata() {
        super(Constants.UNDEPLOY_SERVICE_ID, "Undeploy", "Undeploy", new RoadmapStepMetadata[] { ROADMAP_PREPARE, ROADMAP_EXECUTE },
            Constants.SERVICE_VERSION_1_0, PARAMS, null);
    }

}
