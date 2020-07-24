package com.sap.cloud.lm.sl.cf.core.security.serialization.masking;

import org.cloudfoundry.multiapps.mta.model.RequiredDependency;

public class RequiredDependencyMasker extends AbstractMasker<RequiredDependency> {

    @Override
    public void mask(RequiredDependency dependency) {
        maskProperties(dependency);
        maskParameters(dependency);
    }
}
