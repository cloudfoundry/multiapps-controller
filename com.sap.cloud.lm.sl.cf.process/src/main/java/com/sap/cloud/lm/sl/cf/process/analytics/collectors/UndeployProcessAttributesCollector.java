package com.sap.cloud.lm.sl.cf.process.analytics.collectors;

import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.process.analytics.model.UndeployProcessAttributes;

@Named("undeployProcessAttributesCollector")
public class UndeployProcessAttributesCollector extends AbstractCommonProcessAttributesCollector<UndeployProcessAttributes> {

    @Override
    protected UndeployProcessAttributes getProcessAttributes() {
        return new UndeployProcessAttributes();
    }

}
