package org.cloudfoundry.multiapps.controller.client.lib.domain;

import java.util.List;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.DockerData;
import com.sap.cloudfoundry.client.facade.domain.LifecycleType;
import com.sap.cloudfoundry.client.facade.domain.Staging;

public class DropletInfoFactory {

    public DropletInfo createDropletInfo(Staging staging) {
        if (staging.getDockerInfo() != null) {
            var dockerInfo = staging.getDockerInfo();
            return new DockerDropletInfo(dockerInfo.getImage());
        }
        return new BuildpackDropletInfo(staging.getBuildpacks(), staging.getStackName());
    }

    public DropletInfo createDropletInfo(CloudApplication app, CloudControllerClient client) {
        var lifecycle = app.getLifecycle();
        if (lifecycle.getType() == LifecycleType.BUILDPACK || lifecycle.getType() == LifecycleType.CNB) {
            var buildpacks = (List<String>) lifecycle.getData()
                                                     .get("buildpacks");
            var stack = (String) lifecycle.getData()
                                          .get("stack");
            return new BuildpackDropletInfo(buildpacks, stack);
        }

        var droplet = client.getCurrentDropletForApplication(app.getGuid());
        var cloudPackage = client.getPackage(droplet.getPackageGuid());
        var dockerData = (DockerData) cloudPackage.getData();
        return new DockerDropletInfo(dockerData.getImage());
    }

}
