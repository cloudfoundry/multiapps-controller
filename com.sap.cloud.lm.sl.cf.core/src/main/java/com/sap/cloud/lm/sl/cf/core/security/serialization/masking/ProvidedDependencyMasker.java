package com.sap.cloud.lm.sl.cf.core.security.serialization.masking;

import com.sap.cloud.lm.sl.mta.model.ProvidedDependency;

public class ProvidedDependencyMasker extends AbstractMasker<ProvidedDependency> {

    @Override
    public void mask(ProvidedDependency dependency) {
        maskProperties(dependency);
        maskParameters(dependency);
    }

}
