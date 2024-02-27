package com.sap.cloud.lm.sl.cf.process.util;

import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;

public final class ApplicationStagerFactory {

    private ApplicationStagerFactory() {
    }

    public static ApplicationStager createApplicationStager(PlatformType platformType) {

        if (platformType == PlatformType.CF) {
            return new ApplicationStager();
        }
        if (platformType == PlatformType.XS2) {
            return new XS2ApplicationStager();
        }

        throw new IllegalStateException("Invalid platform type!");
    }
}
