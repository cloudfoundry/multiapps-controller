package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.springframework.http.HttpStatus;

public class SpaceGetter extends CustomControllerClient {

    public SpaceGetter() {
        this(new RestTemplateFactory());
    }

    protected SpaceGetter(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    public CloudSpace findSpace(CloudFoundryOperations client, String orgName, String spaceName) {
        return filterSpaces(client, orgName, spaceName).findAny()
            .orElse(null);
    }

    public List<CloudSpace> findSpaces(CloudFoundryOperations client, String orgName) {
        return filterSpaces(client, orgName, null).collect(Collectors.toList());
    }

    private Stream<CloudSpace> filterSpaces(CloudFoundryOperations client, String orgName, String spaceName) {
        return client.getSpaces()
            .stream()
            .filter(s -> orgName == null || isSameOrganization(s, orgName))
            .filter(s -> spaceName == null || isSameSpace(s, orgName, spaceName));
    }

    private boolean isSameSpace(CloudSpace space, String orgName, String spaceName) {
        return space.getName()
            .equals(spaceName);
    }

    private boolean isSameOrganization(CloudSpace space, String orgName) {
        return space.getOrganization()
            .getName()
            .equals(orgName);
    }

    public CloudSpace getSpace(CloudFoundryOperations client, String spaceId) {
        return client.getSpaces()
            .stream()
            .filter((s) -> isSameSpace(s, spaceId))
            .findAny()
            .orElseThrow(
                () -> new CloudFoundryException(HttpStatus.NOT_FOUND, MessageFormat.format("Space with ID {0} does not exist", spaceId)));
    }

    private boolean isSameSpace(CloudSpace space, String spaceId) {
        return space.getMeta()
            .getGuid()
            .toString()
            .equals(spaceId);
    }

}
