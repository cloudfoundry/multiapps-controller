package org.cloudfoundry.multiapps.controller.client.lib.domain;

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
        return Objects.equals(getBuildpacks(), otherDroplet.getBuildpacks())
            && Objects.equals(getStack(), otherDroplet.getStack());
    }
}
