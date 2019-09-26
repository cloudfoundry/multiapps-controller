package com.sap.cloud.lm.sl.cf.core.filters;

import java.util.function.BiPredicate;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;

public class TargetWildcardFilter implements BiPredicate<CloudTarget, CloudTarget> {

    public static final String ANY_TARGET_WILDCARD = "*";

    @Override
    public boolean test(CloudTarget actualEntryTarget, CloudTarget requestedTarget) {

        if (requestedTarget == null || ANY_TARGET_WILDCARD.equals(requestedTarget.getOrganizationName())
            && ANY_TARGET_WILDCARD.equals(requestedTarget.getSpaceName())) {
            return true;
        }

        if (ANY_TARGET_WILDCARD.equals(requestedTarget.getOrganizationName())) {
            return actualEntryTarget.getSpaceName()
                                    .equals(requestedTarget.getSpaceName());
        }

        if (ANY_TARGET_WILDCARD.equals(requestedTarget.getSpaceName())) {
            return actualEntryTarget.getOrganizationName()
                                    .equals(requestedTarget.getOrganizationName());
        }

        return actualEntryTarget.getOrganizationName()
                                .equals(requestedTarget.getOrganizationName())
            && actualEntryTarget.getSpaceName()
                                .equals(requestedTarget.getSpaceName());
    }
}
