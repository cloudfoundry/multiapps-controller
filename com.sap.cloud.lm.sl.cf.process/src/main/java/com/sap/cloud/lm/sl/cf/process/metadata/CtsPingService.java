package com.sap.cloud.lm.sl.cf.process.metadata;

import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.slp.model.ParameterMetadata;
import com.sap.cloud.lm.sl.slp.model.RoadmapStepMetadata;
import com.sap.cloud.lm.sl.slp.model.ServiceMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

public class CtsPingService {

    private static final StepMetadata STEP_START = StepMetadata.builder().id("startEvent").displayName("Start").description(
        "Start").build();
    private static final StepMetadata STEP_END = StepMetadata.builder().id("endEvent").displayName("End").description("End").build();

    private static final RoadmapStepMetadata ROADMAP_EXECUTE = RoadmapStepMetadata.builder().id("roadmap_execute").displayName(
        "Execute").description("Execute").children(STEP_START, STEP_END).build();

    private final static Set<ParameterMetadata> PARAMS = new HashSet<>();

    static {
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_APPLICATION_TYPE).required(true).build());
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_DEPLOY_URI).build());
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_USERNAME).build());
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_PASSWORD).build());
    }

    public static ServiceMetadata getMetadata() {
        return ServiceMetadata.builder().id(Constants.CTS_PING_SERVICE_ID).displayName("CTS+ Ping").description("CTS+ Ping").children(
            ROADMAP_EXECUTE).versions(Constants.SERVICE_VERSION_1_0).parameters(PARAMS).build();
    }

}
