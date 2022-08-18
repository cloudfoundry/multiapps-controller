package org.cloudfoundry.multiapps.controller.client.lib.domain;

import java.util.Objects;

public class DockerDropletInfo implements DropletInfo {

    private final String image;
    private final String username;
    private final String password;

    DockerDropletInfo(String image) {
        this(image, null, null);
    }

    DockerDropletInfo(String image, String username, String password) {
        this.image = image;
        this.username = username;
        this.password = password;
    }

    public String getImage() {
        return image;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
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
        return Objects.equals(getImage(), otherDroplet.getImage())
            && Objects.equals(getUsername(), otherDroplet.getUsername())
            && Objects.equals(getPassword(), otherDroplet.getPassword());
    }
}
