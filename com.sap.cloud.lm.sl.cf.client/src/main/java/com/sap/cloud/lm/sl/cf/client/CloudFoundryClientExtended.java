package com.sap.cloud.lm.sl.cf.client;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.rest.CloudControllerClient;

public class CloudFoundryClientExtended extends CloudFoundryClient implements CloudFoundryOperationsExtended {

    public CloudFoundryClientExtended(CloudControllerClient cc) {
        super(cc);
    }

    @Override
    public List<String> getSpaceManagers2(String spaceName) {
        return super.getSpaceManagers(spaceName).stream().map(uuid -> uuid.toString()).collect(Collectors.toList());
    }

    @Override
    public List<String> getSpaceDevelopers2(String spaceName) {
        return super.getSpaceDevelopers(spaceName).stream().map(uuid -> uuid.toString()).collect(Collectors.toList());
    }

    @Override
    public List<String> getSpaceAuditors2(String spaceName) {
        return super.getSpaceAuditors(spaceName).stream().map(uuid -> uuid.toString()).collect(Collectors.toList());
    }

    @Override
    public List<String> getSpaceManagers2(String orgName, String spaceName) {
        return super.getSpaceManagers(orgName, spaceName).stream().map(uuid -> uuid.toString()).collect(Collectors.toList());
    }

    @Override
    public List<String> getSpaceDevelopers2(String orgName, String spaceName) {
        return super.getSpaceDevelopers(orgName, spaceName).stream().map(uuid -> uuid.toString()).collect(Collectors.toList());
    }

    @Override
    public List<String> getSpaceAuditors2(String orgName, String spaceName) {
        return super.getSpaceAuditors(orgName, spaceName).stream().map(uuid -> uuid.toString()).collect(Collectors.toList());
    }

}
