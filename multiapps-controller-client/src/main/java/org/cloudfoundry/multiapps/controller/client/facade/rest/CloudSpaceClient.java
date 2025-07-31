package org.cloudfoundry.multiapps.controller.client.facade.rest;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.RawCloudEntity;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudOrganization;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.util.UriUtil;

import org.cloudfoundry.AbstractCloudFoundryException;
import org.cloudfoundry.client.v3.ClientV3Exception;
import org.cloudfoundry.client.v3.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v3.organizations.ListOrganizationsRequest;
import org.cloudfoundry.client.v3.organizations.Organization;
import org.cloudfoundry.client.v3.organizations.OrganizationsV3;
import org.cloudfoundry.client.v3.spaces.GetSpaceRequest;
import org.cloudfoundry.client.v3.spaces.ListSpacesRequest;
import org.cloudfoundry.client.v3.spaces.Space;
import org.cloudfoundry.client.v3.spaces.SpacesV3;
import org.springframework.http.HttpStatus;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class CloudSpaceClient {

    private static final List<String> CHARS_TO_ENCODE = List.of(",");
    private static final long RETRIES = 3;
    private static final Duration RETRY_INTERVAL = Duration.ofSeconds(3);

    private final SpacesV3 spacesClient;
    private final OrganizationsV3 orgsClient;

    public CloudSpaceClient(SpacesV3 spacesClient, OrganizationsV3 orgsClient) {
        this.spacesClient = spacesClient;
        this.orgsClient = orgsClient;
    }

    public CloudSpace getSpace(UUID spaceGuid) {
        Space space = spacesClient.get(GetSpaceRequest.builder()
                                                      .spaceId(spaceGuid.toString())
                                                      .build())
                                  .retryWhen(Retry.fixedDelay(RETRIES, RETRY_INTERVAL)
                                                  .onRetryExhaustedThrow(this::throwOriginalError))
                                  .onErrorMap(ClientV3Exception.class, this::convertV3ClientException)
                                  .block();
        if (space == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Space with GUID " + spaceGuid + " not found.");
        }

        String orgGuid = space.getRelationships()
                              .getOrganization()
                              .getData()
                              .getId();
        Organization org = orgsClient.get(GetOrganizationRequest.builder()
                                                                .organizationId(orgGuid)
                                                                .build())
                                     .retryWhen(Retry.fixedDelay(RETRIES, RETRY_INTERVAL)
                                                     .onRetryExhaustedThrow(this::throwOriginalError))
                                     .onErrorMap(ClientV3Exception.class, this::convertV3ClientException)
                                     .block();
        if (org == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Organization with GUID " + orgGuid + " not found.");
        }
        return mapToCloudSpace(space, org);
    }

    public CloudSpace getSpace(String organizationName, String spaceName) {
        var orgsResponse = orgsClient.list(ListOrganizationsRequest.builder()
                                                                   .name(encodeAsQueryParam(organizationName))
                                                                   .build())
                                     .retryWhen(Retry.fixedDelay(RETRIES, RETRY_INTERVAL)
                                                     .onRetryExhaustedThrow(this::throwOriginalError))
                                     .onErrorMap(ClientV3Exception.class, this::convertV3ClientException)
                                     .block();
        List<? extends Organization> orgs = orgsResponse.getResources();
        if (orgs.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Organization " + organizationName + " not found.");
        }

        Organization org = orgs.get(0);

        var spacesResponse = spacesClient.list(ListSpacesRequest.builder()
                                                                .organizationId(org.getId())
                                                                .name(encodeAsQueryParam(spaceName))
                                                                .build())
                                         .retryWhen(Retry.fixedDelay(RETRIES, RETRY_INTERVAL)
                                                         .onRetryExhaustedThrow(this::throwOriginalError))
                                         .onErrorMap(ClientV3Exception.class, this::convertV3ClientException)
                                         .block();
        List<? extends Space> spaces = spacesResponse.getResources();
        if (spaces.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Space " + spaceName + " not found in organization " + organizationName);
        }

        Space space = spaces.get(0);
        return mapToCloudSpace(space, org);
    }

    private String encodeAsQueryParam(String param) {
        return UriUtil.encodeChars(param, CHARS_TO_ENCODE);
    }

    private CloudSpace mapToCloudSpace(Space space, Organization org) {
        return ImmutableCloudSpace.builder()
                                  .metadata(RawCloudEntity.parseResourceMetadata(space))
                                  .name(space.getName())
                                  .organization(ImmutableCloudOrganization.builder()
                                                                          .metadata(RawCloudEntity.parseResourceMetadata(org))
                                                                          .name(org.getName())
                                                                          .build())
                                  .build();
    }

    private Throwable throwOriginalError(RetryBackoffSpec retrySpec, Retry.RetrySignal signal) {
        return signal.failure();
    }

    private CloudOperationException convertV3ClientException(AbstractCloudFoundryException e) {
        HttpStatus httpStatus = HttpStatus.valueOf(e.getStatusCode());
        return new CloudOperationException(httpStatus, httpStatus.getReasonPhrase(), e.getMessage(), e);
    }

}
