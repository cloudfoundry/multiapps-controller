package com.sap.cloud.lm.sl.cf.core.filters;

import java.util.function.BiPredicate;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;

public class TargetWildcardFilter implements BiPredicate<CloudTarget, CloudTarget> {

    public static final String ANY_TARGET_WILDCARD = "*";

    @Override
    public boolean test(CloudTarget actualEntryTarget, CloudTarget requestedTarget) {

        if (requestedTarget == null
            || ANY_TARGET_WILDCARD.equals(requestedTarget.getOrg()) && ANY_TARGET_WILDCARD.equals(requestedTarget.getSpace())) {
            return true;
        }

        if (ANY_TARGET_WILDCARD.equals(requestedTarget.getOrg())) {
            return actualEntryTarget.getSpace()
                .equals(requestedTarget.getSpace());
        }

        if (ANY_TARGET_WILDCARD.equals(requestedTarget.getSpace())) {
            return actualEntryTarget.getOrg()
                .equals(requestedTarget.getOrg());
        }

        return actualEntryTarget.getOrg()
            .equals(requestedTarget.getOrg())
            && actualEntryTarget.getSpace()
                .equals(requestedTarget.getSpace());
    }
}
