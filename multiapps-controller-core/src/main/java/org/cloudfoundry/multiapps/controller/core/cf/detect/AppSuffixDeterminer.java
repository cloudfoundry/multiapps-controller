package org.cloudfoundry.multiapps.controller.core.cf.detect;

public class AppSuffixDeterminer {

    private final boolean keepOriginalNamesAfterDeploy;
    private final boolean isAfterResumePhase;

    public AppSuffixDeterminer(boolean keepOriginalNamesAfterDeploy, boolean isAfterResumePhase) {
        this.keepOriginalNamesAfterDeploy = keepOriginalNamesAfterDeploy;
        this.isAfterResumePhase = isAfterResumePhase;
    }

    public boolean shouldAppendApplicationSuffix() {
        return keepOriginalNamesAfterDeploy && isAfterResumePhase;
    }
}
