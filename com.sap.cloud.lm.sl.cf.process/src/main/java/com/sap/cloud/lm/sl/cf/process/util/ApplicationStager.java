package com.sap.cloud.lm.sl.cf.process.util;

import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.ExecutionWrapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;

public class ApplicationStager {
    
    public StagingState getStagingState(ExecutionWrapper execution, CloudControllerClient client) {
        UUID buildGuid = (UUID) execution.getContext().getVariable(Constants.VAR_BUILD_GUID);
        if (buildGuid == null) {
            return new StagingState(PackageState.STAGED, null);
        }
        CloudBuild build = client.getBuild(buildGuid);

        return getStagingState(build);
    }
    
    private StagingState getStagingState(CloudBuild build) {
        PackageState packageState = null;
        String stagingError = null;
        switch (build.getState()) {
            case FAILED:
                packageState = PackageState.FAILED;
                stagingError = build.getError();
                break;
            case STAGED:
                packageState = PackageState.STAGED;
                break;
            case STAGING:
                packageState = PackageState.PENDING;
                break;
        }

        return new StagingState(packageState, stagingError);
    }

    public void bindDropletToApp(ExecutionWrapper execution, UUID appId, CloudControllerClient client) {
        UUID buildGuid = (UUID) execution.getContext()
            .getVariable(Constants.VAR_BUILD_GUID);
        
        client.bindDropletToApp(client.getBuild(buildGuid)
            .getDroplet()
            .getGuid(), appId);
    }
    
    public StepPhase stageApp(DelegateExecution context, CloudControllerClient client, CloudApplication app, StepLogger stepLogger) {
        UploadToken uploadToken = StepsUtil.getUploadToken(context);
        if (uploadToken == null) {
            return StepPhase.DONE;
        }
        UUID packageGuid = uploadToken.getPackageGuid();

        stepLogger.info(Messages.STAGING_APP, app.getName());

        context
            .setVariable(Constants.VAR_BUILD_GUID, client
                .createBuild(packageGuid)
                .getMeta()
                .getGuid());

        return StepPhase.POLL;
    }
    
}
