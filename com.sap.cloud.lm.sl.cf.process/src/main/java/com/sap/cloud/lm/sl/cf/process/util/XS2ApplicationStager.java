package com.sap.cloud.lm.sl.cf.process.util;

import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.ExecutionWrapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.StagingState;
import com.sap.cloud.lm.sl.cf.process.util.StagingState.StagingLogs;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationStager;

public class XS2ApplicationStager extends ApplicationStager {

    @Override
    public StagingState getStagingState(ExecutionWrapper execution, CloudControllerClient client) {
        try {
            StartingInfo startingInfo = StepsUtil.getStartingInfo(execution.getContext());
            int offset = (Integer) execution.getContext()
                .getVariable(Constants.VAR_OFFSET);
            String stagingLogs = client.getStagingLogs(startingInfo, offset);

            return stagingLogs != null ? new StagingState(PackageState.PENDING, null, getStagingLogs(stagingLogs, offset))
                : new StagingState(PackageState.STAGED, null);

        } catch (CloudOperationException e) {
            return getStagingStateIfExceptionHasOccurred(e);
        }
    }

    private StagingLogs getStagingLogs(String stagingLogs, int offset) {
        // Staging logs successfully retrieved
        stagingLogs = stagingLogs.trim();
        StagingLogs logs = null;
        if (!stagingLogs.isEmpty()) {
            logs = new StagingLogs(stagingLogs, offset);
        }

        return logs;
    }

    private StagingState getStagingStateIfExceptionHasOccurred(CloudOperationException e) {
        // "400 Bad Request" might mean that staging had already finished
        if (e.getStatusCode()
            .equals(HttpStatus.BAD_REQUEST)) {
            return new StagingState(PackageState.STAGED, null);
        } else {
            return new StagingState(PackageState.FAILED, e.getMessage());
        }
    }

    @Override
    public void bindDropletToApp(ExecutionWrapper execution, UUID appId, CloudControllerClient client) {
        // NOT NEEDED
    }
}
