package com.sap.cloud.lm.sl.cf.process.analytics.collectors;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.analytics.model.UndeployProcessAttributes;

@Component("undeployProcessAttributesCollector")
public class UndeployProcessAttributesCollector extends AbstractCommonProcessAttributesCollector<UndeployProcessAttributes> {

    @Override
    protected UndeployProcessAttributes getProcessAttributes() {
        return new UndeployProcessAttributes();
    }

}
