package org.cloudfoundry.multiapps.controller.process.flowable;

import org.flowable.engine.impl.cmd.ClearProcessInstanceLockTimesCmd;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;

public class MtaAsyncJobExecutor extends DefaultAsyncJobExecutor {

    private boolean unlockOwnedExecutions;

    public MtaAsyncJobExecutor() {
        super();
        // Unfortunately, no exposed setter for this flag
        super.shutdownTaskExecutor = true;
    }

    protected void shutdownAdditionalComponents() {
        super.shutdownAdditionalComponents();
        if (unlockOwnedExecutions) {
            unlockOwnedExecutions();
        }
    }

    protected void unlockOwnedExecutions() {
        jobServiceConfiguration.getCommandExecutor()
                               .execute(new ClearProcessInstanceLockTimesCmd(getLockOwner()));
    }

    public boolean isUnlockOwnedExecutions() {
        return unlockOwnedExecutions;
    }

    public void setUnlockOwnedExecutions(boolean unlockOwnedExecutions) {
        this.unlockOwnedExecutions = unlockOwnedExecutions;
    }
}
