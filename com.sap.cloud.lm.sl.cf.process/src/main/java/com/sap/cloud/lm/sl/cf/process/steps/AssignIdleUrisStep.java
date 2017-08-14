package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("assignIdleUrisStep")
public class AssignIdleUrisStep extends SetAppsUrisStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("assignIdleUrisTask").displayName("Assign Idle URIs").description("Assign Idle URIs").build();
    }

    @Override
    protected String getStartProgressMessage() {
        return Messages.ASSIGNING_IDLE_URIS;
    }

    @Override
    protected String getEndProgressMessage() {
        return Messages.IDLE_URIS_ASSIGNED;
    }

    @Override
    protected List<String> getNewUris(CloudApplicationExtended app) {
        return app.getIdleUris();
    }
}
