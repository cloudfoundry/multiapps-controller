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
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ResourceMappers;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Space;
import org.cloudfoundry.multiapps.controller.client.facade.util.UriUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class CloudSpaceClient {

    private static final List<String> CHARS_TO_ENCODE = List.of(",");

    private final CloudControllerV3Client cc;

    public CloudSpaceClient(CloudControllerV3Client cc) {
        this.cc = cc;
    }

    public CloudSpace getSpace(UUID spaceGuid) {
        V3Space space = cc.getOptional(CloudControllerV3Endpoints.SPACES + "/" + spaceGuid, V3Space.class)
                          .orElseThrow(() -> new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                                                         MessageFormat.format(Messages.SPACE_WITH_GUID_0_NOT_FOUND,
                                                                                              spaceGuid)));

        String orgGuid = space.organizationGuid();
        V3Organization org = cc.getOptional(CloudControllerV3Endpoints.ORGANIZATIONS + "/" + orgGuid, V3Organization.class)
                               .orElseThrow(() -> new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                                                              MessageFormat.format(
                                                                                  Messages.ORGANIZATION_WITH_GUID_0_NOT_FOUND, orgGuid)));

        return mapToCloudSpace(space, org);
    }

    public CloudSpace getSpace(String organizationName, String spaceName) {
        List<V3Organization> orgs = cc.list(CloudControllerV3Endpoints.ORGANIZATIONS + CloudControllerV3Endpoints.QUERY_NAMES
                                                + encodeAsQueryParam(organizationName),
                                            new ParameterizedTypeReference<V3ListResponse<V3Organization>>() {
                                            });

        if (orgs.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.ORGANISATION_0_NOT_FOUND, organizationName));
        }

        V3Organization org = orgs.get(0);

        List<V3Space> spaces = cc.list(CloudControllerV3Endpoints.SPACES + CloudControllerV3Endpoints.QUERY_ORGANIZATION_GUIDS + org.guid()
                                           + CloudControllerV3Endpoints.AMPERSAND_NAMES + encodeAsQueryParam(spaceName),
                                       new ParameterizedTypeReference<V3ListResponse<V3Space>>() {
                                       });

        if (spaces.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.SPACE_0_NOT_FOUND_IN_ORGANIZATION_1, spaceName,
                                                                   organizationName));
        }

        return mapToCloudSpace(spaces.get(0), org);
    }

    private String encodeAsQueryParam(String param) {
        return UriUtil.encodeChars(param, CHARS_TO_ENCODE);
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
