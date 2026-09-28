package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEntity;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Domain;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3DomainMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

public class DomainsV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Domain>> DOMAIN_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;
    private final CloudSpace targetSpace;

    public DomainsV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this.client = client;
        this.targetSpace = targetSpace;
    }

    public void addDomain(String domainName) {
        assertSpaceProvided("add domain");
        CloudDomain domain = findDomainByName(domainName);

        if (domain == null) {
            doCreateDomain(domainName);
        }

    }

    public void deleteDomain(String domainName) {
        assertSpaceProvided("delete domain");
        CloudDomain domain = findDomainByName(domainName, true);
        doDeleteDomain(domain.getGuid());
    }

    public CloudDomain getDefaultDomain() {
        UUID organizationGuid = getTargetOrganizationGuid();
        V3Domain domain = client.get(CloudControllerV3Endpoints.ORGANIZATIONS + "/" + organizationGuid + "/domains/default",
                                     V3Domain.class);

        if (domain == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.DOMAIN_WITH_GUID_0_NOT_FOUND, organizationGuid));
        }

        return V3DomainMapper.toCloudDomain(domain);
    }

    public List<CloudDomain> getDomains() {
        return getAllDomains().stream()
                              .map(V3DomainMapper::toCloudDomain)
                              .toList();
    }

    public List<CloudDomain> getSharedDomains() {
        return getAllDomains().stream()
                              .filter(domain -> !domain.isPrivate())
                              .map(V3DomainMapper::toCloudDomain)
                              .toList();
    }

    public List<CloudDomain> getPrivateDomains() {
        return getAllDomains().stream()
                              .filter(V3Domain::isPrivate)
                              .map(V3DomainMapper::toCloudDomain)
                              .toList();
    }

    public List<CloudDomain> getDomainsForOrganization() {
        assertSpaceProvided("access organization domains");
        String uri = CloudControllerV3Endpoints.ORGANIZATIONS + "/" + getTargetOrganizationGuid() + "/domains"
            + CloudControllerV3Endpoints.QUERY_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, DOMAIN_LIST_TYPE)
                     .stream()
                     .map(V3DomainMapper::toCloudDomain)
                     .toList();
    }

    private List<V3Domain> getAllDomains() {
        return client.list(CloudControllerV3Endpoints.DOMAINS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                               + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE, DOMAIN_LIST_TYPE);
    }

    private CloudDomain findDomainByName(String name, boolean required) {
        CloudDomain domain = findDomainByName(name);

        if (domain == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.DOMAIN_0_NOT_FOUND, name));
        }

        return domain;
    }

    private CloudDomain findDomainByName(String name) {
        String uri = CloudControllerV3Endpoints.DOMAINS + CloudControllerV3Endpoints.QUERY_NAMES + name
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, DOMAIN_LIST_TYPE)
                     .stream()
                     .findFirst()
                     .map(V3DomainMapper::toCloudDomain)
                     .orElse(null);
    }

    private void doCreateDomain(String name) {
        UUID organizationGuid = getTargetOrganizationGuid();
        if (organizationGuid == null) {
            throw new CloudOperationException(HttpStatus.BAD_REQUEST, Messages.BAD_REQUEST,
                                              MessageFormat.format(Messages.CANNOT_CREATE_DOMAIN_0_WITHOUT_ORGANIZATION, name));
        }

        client.getRestClient()
              .post()
              .uri(CloudControllerV3Endpoints.DOMAINS)
              .body(Map.of("name", name, "relationships", organizationRelationship(organizationGuid)))
              .retrieve()
              .toBodilessEntity();
    }

    private Map<String, Object> organizationRelationship(UUID organizationGuid) {
        return Map.of("organization", Map.of("data", Map.of("guid", organizationGuid.toString())));
    }

    private void doDeleteDomain(UUID guid) {
        ResponseEntity<Void> response = client.getRestClient()
                                              .delete()
                                              .uri(CloudControllerV3Endpoints.DOMAIN_BY_GUID, guid.toString())
                                              .retrieve()
                                              .toEntity(Void.class);
        client.followAsyncJob(response, Constants.DELETE_JOB_TIMEOUT);
    }

    private UUID getTargetOrganizationGuid() {
        return getGuid(targetSpace.getOrganization());
    }

    private UUID getGuid(CloudEntity entity) {
        if (entity == null || entity.getMetadata() == null) {
            return null;
        }

        return entity.getMetadata()
                     .getGuid();
    }

    private void assertSpaceProvided(String operation) {
        Assert.notNull(targetSpace, "Unable to " + operation + " without specifying organization and space to use.");
    }

}
