package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudOrganization;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Organization;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3QueryParameters;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ResourceMappers;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Space;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class CloudSpaceClient {

    private final CloudControllerV3Client client;

    public CloudSpaceClient(CloudControllerV3Client client) {
        this.client = client;
    }

    public CloudSpace getSpace(UUID spaceGuid) {
        V3Space space = client.getOptional(V3Uri.path(CloudControllerV3Endpoints.SPACE_BY_GUID, spaceGuid)
                                                .build(), V3Space.class)
                              .orElseThrow(() -> new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                                                             MessageFormat.format(Messages.SPACE_WITH_GUID_0_NOT_FOUND,
                                                                                                  spaceGuid)));

        String orgGuid = space.organizationGuid();
        V3Organization org = client.getOptional(V3Uri.path(CloudControllerV3Endpoints.ORGANIZATION_BY_GUID, orgGuid)
                                                     .build(), V3Organization.class)
                                   .orElseThrow(
                                       () -> new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                                                         MessageFormat.format(
                                                                             Messages.ORGANIZATION_WITH_GUID_0_NOT_FOUND,
                                                                             orgGuid)));

        return mapToCloudSpace(space, org);
    }

    public CloudSpace getSpace(String organizationName, String spaceName) {
        V3Organization organization = findOrganizationByName(organizationName);
        V3Space space = findSpaceByName(spaceName, organization);

        return mapToCloudSpace(space, organization);
    }

    private V3Organization findOrganizationByName(String organizationName) {
        List<V3Organization> organizations = client.list(V3Uri.path(CloudControllerV3Endpoints.ORGANIZATIONS)
                                                              .queryEncoded(V3QueryParameters.NAMES, organizationName)
                                                              .build(),
                                                         new ParameterizedTypeReference<V3ListResponse<V3Organization>>() {
                                                         });

        if (organizations.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                              MessageFormat.format(Messages.ORGANISATION_0_NOT_FOUND, organizationName));
        }

        return organizations.getFirst();
    }

    private V3Space findSpaceByName(String spaceName, V3Organization org) {
        List<V3Space> spaces = client.list(
            V3Uri.path(CloudControllerV3Endpoints.SPACES)
                 .query(V3QueryParameters.ORGANIZATION_GUIDS, org.guid())
                 .queryEncoded(V3QueryParameters.NAMES, spaceName)
                 .build(),
            new ParameterizedTypeReference<V3ListResponse<V3Space>>() {
            });

        if (spaces.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                              MessageFormat.format(Messages.SPACE_0_NOT_FOUND_IN_ORGANIZATION_1, spaceName,
                                                                   org.name()));
        }

        return spaces.getFirst();
    }

    private CloudSpace mapToCloudSpace(V3Space space, V3Organization org) {
        return ImmutableCloudSpace.builder()
                                  .metadata(V3ResourceMappers.parseMetadata(space.guid(), space.createdAt(), space.updatedAt()))
                                  .name(space.name())
                                  .organization(ImmutableCloudOrganization.builder()
                                                                          .metadata(V3ResourceMappers.parseMetadata(org.guid(),
                                                                                                                    org.createdAt(),
                                                                                                                    org.updatedAt()))
                                                                          .name(org.name())
                                                                          .build())
                                  .build();
    }

}
