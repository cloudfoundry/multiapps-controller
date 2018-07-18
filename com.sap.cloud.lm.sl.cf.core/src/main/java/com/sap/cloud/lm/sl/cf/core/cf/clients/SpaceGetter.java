package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.springframework.http.HttpStatus;

public class SpaceGetter extends CustomControllerClient {

    public SpaceGetter() {
        this(new RestTemplateFactory());
    }

    protected SpaceGetter(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    public CloudSpace findSpace(CloudControllerClient client, String orgName, String spaceName) {
        return filterSpaces(client, orgName, spaceName).findAny()
            .orElse(null);
    }

    public List<CloudSpace> findSpaces(CloudControllerClient client, String orgName) {
        return filterSpaces(client, orgName, null).collect(Collectors.toList());
    }

    private Stream<CloudSpace> filterSpaces(CloudControllerClient client, String orgName, String spaceName) {
        return client.getSpaces()
            .stream()
            .filter(space -> isSameSpace(space, orgName, spaceName));
    }

    private boolean isSameSpace(CloudSpace space, String orgName, String spaceName) {
        CloudOrganization org = space.getOrganization();
        if (orgName != null && !org.getName()
            .equals(orgName)) {
            return false;
        }
        return spaceName == null || space.getName()
            .equals(spaceName);
    }

    public CloudSpace getSpace(CloudControllerClient client, String spaceId) {
        return client.getSpaces()
            .stream()
            .filter(space -> isSameSpace(space, spaceId))
            .findAny()
            .orElseThrow(
                () -> new CloudOperationException(HttpStatus.NOT_FOUND, MessageFormat.format("Space with ID {0} does not exist", spaceId)));
    }

    private boolean isSameSpace(CloudSpace space, String spaceId) {
        return space.getMeta()
            .getGuid()
            .toString()
            .equals(spaceId);
    }

}
