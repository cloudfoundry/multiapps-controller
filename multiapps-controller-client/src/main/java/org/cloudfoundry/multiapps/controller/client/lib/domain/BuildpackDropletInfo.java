package org.cloudfoundry.multiapps.controller.client.lib.domain;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

public class BuildpackDropletInfo implements DropletInfo {

    private final List<String> buildpacks;
    private final String stack;

    BuildpackDropletInfo(List<String> buildpacks, String stack) {
        this.buildpacks = buildpacks;
        this.stack = stack;
    }

    public List<String> getBuildpacks() {
        return buildpacks;
    }

    public String getStack() {
        return stack;
    }

    @Override
    public boolean equals(DropletInfo other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof BuildpackDropletInfo)) {
            return false;
        }
        var otherDroplet = (BuildpackDropletInfo) other;

        var shouldCompareStack = isStackSet(getStack()) && isStackSet(otherDroplet.getStack());
        var shouldCompareBuildpacks = areBuildpacksSet(getBuildpacks()) && areBuildpacksSet(otherDroplet.getBuildpacks());

        if (shouldCompareStack && !Objects.equals(getStack(), otherDroplet.getStack())) {
            return false;
        }
        return !shouldCompareBuildpacks || Objects.equals(getBuildpacks(), otherDroplet.getBuildpacks());
    }

    private static boolean isStackSet(String stack) {
        return StringUtils.hasLength(stack);
    }

    private static boolean areBuildpacksSet(List<String> buildpacks) {
        return !CollectionUtils.isEmpty(buildpacks);
    }
}
