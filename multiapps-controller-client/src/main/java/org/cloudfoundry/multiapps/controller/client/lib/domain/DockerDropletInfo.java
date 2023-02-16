package org.cloudfoundry.multiapps.controller.client.lib.domain;

import org.springframework.util.StringUtils;

import java.util.Objects;

public class DockerDropletInfo implements DropletInfo {

    private final String image;

    DockerDropletInfo(String image) {
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    @Override
    public boolean equals(DropletInfo other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof DockerDropletInfo)) {
            return false;
        }
        var otherDroplet = (DockerDropletInfo) other;
        var shouldCompareImage = StringUtils.hasLength(getImage()) && StringUtils.hasLength(otherDroplet.getImage());
        return !shouldCompareImage || Objects.equals(getImage(), otherDroplet.getImage());
    }
}
