package com.sap.cloud.lm.sl.cf.process.steps;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("assignOriginalUrisStep")
public class AssignOriginalUrisStep extends SwapUrisStep {

    public static StepMetadata getMetadata() {
        return new StepMetadata("assignOriginalUrisTask", "Assign Original URIs", "Assign Original URIs");
    }

    @Override
    protected String getStartProgressMessage() {
        return Messages.ASSIGNING_ORIGINAL_URIS;
    }

    @Override
    protected String getEndProgressMessage() {
        return Messages.ORIGINAL_URIS_ASSIGNED;
    }

}
