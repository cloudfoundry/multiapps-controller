package com.sap.cloud.lm.sl.cf.core.filters;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.common.util.CommonUtil;

public class VisibilityFilter implements BiFunction<ConfigurationEntry, List<CloudTarget>, Boolean> {

    @Override
    public Boolean apply(ConfigurationEntry entry, List<CloudTarget> cloudTargets) {
        if (CommonUtil.isNullOrEmpty(cloudTargets)) {
            return true;
        }
        List<CloudTarget> visibilityTargets = getVisibilityTargets(entry);
        for (CloudTarget cloudTarget : cloudTargets) {
            if (isVisible(cloudTarget, visibilityTargets)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVisible(CloudTarget cloudTarget, List<CloudTarget> visibilityCloudTargets) {
        if (visibilityCloudTargets.contains(cloudTarget)) {
            return true;
        }
        for (CloudTarget visibleTarget : visibilityCloudTargets) {
            if (visibleTarget.getOrg().equals("*") && visibleTarget.getSpace().equals("*")) {
                return true;
            }
            if (visibleTarget.getOrg().equals("*") && visibleTarget.getSpace().equals(cloudTarget.getSpace())) {
                return true;
            }
            if (visibleTarget.getOrg().equals(cloudTarget.getOrg()) && visibleTarget.getSpace().equals("*")) {
                return true;
            }
        }
        return false;
    }

    private static List<CloudTarget> getVisibilityTargets(ConfigurationEntry entry) {
        List<CloudTarget> visibleTargets = entry.getVisibility();
        if (visibleTargets == null) {
            String org = entry.getTargetSpace().split(" ")[0];
            visibleTargets = Arrays.asList(new CloudTarget(org, "*"));
        }
        return visibleTargets;
    }
}
