package com.sap.cloud.lm.sl.cf.core.security.serialization.masking;

import com.sap.cloud.lm.sl.mta.model.v3.RequiredDependency;

public class RequiredDependencyMasker extends AbstractMasker<RequiredDependency> {

    @Override
    public void mask(RequiredDependency dependency) {
        maskProperties(dependency);
        maskParameters(dependency);
    }
}
