package org.cloudfoundry.multiapps.controller.persistence.model.filters;

import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;

public class VisibilityFilter implements BiPredicate<ConfigurationEntry, List<CloudTarget>> {

    @Override
    public boolean test(ConfigurationEntry entry, List<CloudTarget> cloudTargets) {
        if (CollectionUtils.isEmpty(cloudTargets)) {
            return true;
        }
        List<CloudTarget> visibilityTargets = getVisibilityTargets(entry);
        return cloudTargets.stream()
                           .anyMatch(cloudTarget -> isVisible(cloudTarget, visibilityTargets));
    }

    private static boolean isVisible(CloudTarget cloudTarget, List<CloudTarget> visibilityCloudTargets) {
        if (visibilityCloudTargets.contains(cloudTarget)) {
            return true;
        }
        for (CloudTarget visibleTarget : visibilityCloudTargets) {
            if ("*".equals(visibleTarget.getOrganizationName()) && "*".equals(visibleTarget.getSpaceName())) {
                return true;
            }
            if ("*".equals(visibleTarget.getOrganizationName()) && visibleTarget.getSpaceName()
                                                                                .equals(cloudTarget.getSpaceName())) {
                return true;
            }
            if (visibleTarget.getOrganizationName()
                             .equals(cloudTarget.getOrganizationName())
                && "*".equals(visibleTarget.getSpaceName())) {
                return true;
            }
        }
        return false;
    }

    private static List<CloudTarget> getVisibilityTargets(ConfigurationEntry entry) {
        List<CloudTarget> visibleTargets = entry.getVisibility();
        if (visibleTargets == null) {
            String org = entry.getTargetSpace()
                              .getOrganizationName();
            visibleTargets = Collections.singletonList(new CloudTarget(org, "*"));
        }
        return visibleTargets;
    }
}
