package com.sap.cloud.lm.sl.cf.process.steps;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("assignTemporaryUrisStep")
public class AssignTemporaryUrisStep extends SwapUrisStep {

    public static StepMetadata getMetadata() {
        return new StepMetadata("assignTemporaryUrisTask", "Assign Temporary URIs", "Assign Temporary URIs");
    }

    @Override
    protected String getStartProgressMessage() {
        return Messages.ASSIGNING_TEMPORARY_URIS;
    }

    @Override
    protected String getEndProgressMessage() {
        return Messages.TEMPORARY_URIS_ASSIGNED;
    }

}
