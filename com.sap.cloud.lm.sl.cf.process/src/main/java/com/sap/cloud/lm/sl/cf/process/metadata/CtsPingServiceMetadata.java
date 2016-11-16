package com.sap.cloud.lm.sl.cf.process.metadata;

import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.slp.model.ParameterMetadata;
import com.sap.cloud.lm.sl.slp.model.RoadmapStepMetadata;
import com.sap.cloud.lm.sl.slp.model.ServiceMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

public class CtsPingServiceMetadata extends ServiceMetadata {

    private static final StepMetadata STEP_START = new StepMetadata("startEvent", "Start", "Start");
    private static final StepMetadata STEP_END = new StepMetadata("endEvent", "End", "End");

    private static final RoadmapStepMetadata ROADMAP_EXECUTE = new RoadmapStepMetadata("roadmap_execute", "Execute", "Execute",
        new StepMetadata[] { STEP_START, STEP_END });

    private final static Set<ParameterMetadata> PARAMS = new HashSet<>();

    static {
        PARAMS.add(new ParameterMetadata(Constants.PARAM_APPLICATION_TYPE, true));
        PARAMS.add(new ParameterMetadata(Constants.PARAM_DEPLOY_URI, false));
        PARAMS.add(new ParameterMetadata(Constants.PARAM_USERNAME, false));
        PARAMS.add(new ParameterMetadata(Constants.PARAM_PASSWORD, false));
    }

    public CtsPingServiceMetadata() {
        super(Constants.CTS_PING_SERVICE_ID, "CTS+ Ping", "CTS+ Ping", new RoadmapStepMetadata[] { ROADMAP_EXECUTE },
            Constants.SERVICE_VERSION_1_0, PARAMS, null);
    }

}
