package org.cloudfoundry.multiapps.controller.client.lib.domain;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.DockerData;
import com.sap.cloudfoundry.client.facade.domain.LifecycleType;
import com.sap.cloudfoundry.client.facade.domain.Staging;

import java.util.List;

public class DropletInfoFactory {

    public DropletInfo createDropletInfo(Staging staging) {
        if (staging.getDockerInfo() != null) {
            var dockerInfo = staging.getDockerInfo();
            var credentials = dockerInfo.getCredentials();
            if (credentials == null) {
                return new DockerDropletInfo(dockerInfo.getImage());
            }
            return new DockerDropletInfo(dockerInfo.getImage(), credentials.getUsername(), credentials.getPassword());
        }
        return new BuildpackDropletInfo(staging.getBuildpacks(), staging.getStackName());
    }

    public DropletInfo createDropletInfo(CloudApplication app, CloudControllerClient client) {
        var lifecycle = app.getLifecycle();
        if (lifecycle.getType() == LifecycleType.BUILDPACK) {
            var buildpacks = (List<String>) lifecycle.getData()
                                                     .get("buildpacks");
            var stack = (String) lifecycle.getData()
                                          .get("stack");
            return new BuildpackDropletInfo(buildpacks, stack);
        }

        var droplet = client.getCurrentDropletForApplication(app.getGuid());
        var cloudPackage = client.getPackage(droplet.getPackageGuid());
        var dockerData = (DockerData) cloudPackage.getData();
        return new DockerDropletInfo(dockerData.getImage(), dockerData.getUsername(), dockerData.getPassword());
    }

}
