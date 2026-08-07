package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.UUID;

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

/**
 * Resolves a target {@link CloudSpace} (space + its organization) by GUID or by org-name/space-name, talking directly to the CF v3 REST
 * API through {@link CloudControllerV3Client}. Replaces the OSS cf-java-client-based implementation (which used {@code SpacesV3} /
 * {@code OrganizationsV3}); behaviour and 404 semantics are preserved.
 */
public class CloudSpaceClient {

    private static final List<String> CHARS_TO_ENCODE = List.of(",");

    private final CloudControllerV3Client cc;

    public CloudSpaceClient(CloudControllerV3Client cc) {
        this.cc = cc;
    }

    public CloudSpace getSpace(UUID spaceGuid) {
        V3Space space = cc.getOptional("/v3/spaces/" + spaceGuid, V3Space.class)
                          .orElseThrow(() -> new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                                                                         "Space with GUID " + spaceGuid + " not found."));
        String orgGuid = space.organizationGuid();
        V3Organization org = cc.getOptional("/v3/organizations/" + orgGuid, V3Organization.class)
                               .orElseThrow(() -> new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                                                                              "Organization with GUID " + orgGuid + " not found."));
        return mapToCloudSpace(space, org);
    }

    public CloudSpace getSpace(String organizationName, String spaceName) {
        List<V3Organization> orgs = cc.list("/v3/organizations?names=" + encodeAsQueryParam(organizationName),
                                            new ParameterizedTypeReference<V3ListResponse<V3Organization>>() {
                                            });
        if (orgs.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Organization " + organizationName + " not found.");
        }
        V3Organization org = orgs.get(0);

        List<V3Space> spaces = cc.list("/v3/spaces?organization_guids=" + org.guid() + "&names=" + encodeAsQueryParam(spaceName),
                                       new ParameterizedTypeReference<V3ListResponse<V3Space>>() {
                                       });
        if (spaces.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                                              "Space " + spaceName + " not found in organization " + organizationName);
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
