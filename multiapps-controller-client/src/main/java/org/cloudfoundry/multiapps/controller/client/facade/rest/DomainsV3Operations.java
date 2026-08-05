package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

/**
 * CF v3 <em>Domains</em> operations of the in-house {@code CloudControllerRestClient}. Reproduces the HTTP shape, filtering and domain
 * mapping of the OSS {@code CloudControllerRestClientImpl} domain methods on top of the shared {@link CloudControllerV3Client} machinery.
 *
 * <p>
 * Endpoint map (see {@code docs/cf-java-client-migration/02-endpoint-inventory.md}):
 * </p>
 * <ul>
 * <li>{@code addDomain}    &rarr; {@code POST /v3/domains} (org-scoped, non-async)</li>
 * <li>{@code deleteDomain} &rarr; {@code DELETE /v3/domains/{guid}} (async job)</li>
 * <li>{@code getDefaultDomain} &rarr; {@code GET /v3/organizations/{guid}/domains/default}</li>
 * <li>{@code getDomains} / {@code getSharedDomains} / {@code getPrivateDomains} &rarr; {@code GET /v3/domains} (paginated, filtered)</li>
 * <li>{@code getDomainsForOrganization} &rarr; {@code GET /v3/organizations/{guid}/domains} (paginated)</li>
 * </ul>
 */
public class DomainsV3Operations {

    private static final int PER_PAGE = 5000;
    // Mirrors the OSS DELETE_JOB_TIMEOUT (Duration.ofMinutes(5)) used for domain/route/service deletes.
    private static final Duration DELETE_JOB_TIMEOUT = Duration.ofMinutes(5);

    private static final ParameterizedTypeReference<V3ListResponse<V3Domain>> DOMAIN_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public DomainsV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    /**
     * Create a private domain in the target organization, but only if a domain with that name does not already exist — matching the OSS
     * {@code addDomain}, which no-ops when the domain is already present.
     */
    public void addDomain(String domainName) {
        assertSpaceProvided("add domain");
        CloudDomain domain = findDomainByName(domainName);
        if (domain == null) {
            doCreateDomain(domainName);
        }
    }

    /**
     * Delete the (required) domain by name. Reproduces the OSS {@code deleteDomain}: a missing domain throws
     * {@code CloudOperationException(NOT_FOUND)}, and the delete follows the async job to completion.
     */
    public void deleteDomain(String domainName) {
        assertSpaceProvided("delete domain");
        CloudDomain domain = findDomainByName(domainName, true);
        doDeleteDomain(domain.getGuid());
    }

    /**
     * {@code GET /v3/organizations/{guid}/domains/default} &rarr; {@link CloudDomain}. Non-paginated single fetch, mapping the OSS
     * {@code getDefaultDomain}.
     */
    public CloudDomain getDefaultDomain() {
        V3Domain domain = cc.get("/v3/organizations/" + getTargetOrganizationGuid() + "/domains/default", V3Domain.class);
        return V3DomainMapper.toCloudDomain(domain);
    }

    /**
     * All domains visible to the target ({@code GET /v3/domains}).
     */
    public List<CloudDomain> getDomains() {
        return getAllDomains().stream()
                              .map(V3DomainMapper::toCloudDomain)
                              .collect(Collectors.toList());
    }

    /**
     * Shared domains only — those whose {@code relationships.organization.data} is {@code null}, matching the OSS
     * {@code getSharedDomainResources} filter.
     */
    public List<CloudDomain> getSharedDomains() {
        return getAllDomains().stream()
                              .filter(domain -> !domain.isPrivate())
                              .map(V3DomainMapper::toCloudDomain)
                              .collect(Collectors.toList());
    }

    /**
     * Private domains only — those whose {@code relationships.organization.data} is present, matching the OSS
     * {@code getPrivateDomainResources} filter.
     */
    public List<CloudDomain> getPrivateDomains() {
        return getAllDomains().stream()
                              .filter(V3Domain::isPrivate)
                              .map(V3DomainMapper::toCloudDomain)
                              .collect(Collectors.toList());
    }

    /**
     * Domains available to the target organization ({@code GET /v3/organizations/{guid}/domains}), matching the OSS
     * {@code getDomainsForOrganization}.
     */
    public List<CloudDomain> getDomainsForOrganization() {
        assertSpaceProvided("access organization domains");
        String uri = "/v3/organizations/" + getTargetOrganizationGuid() + "/domains?per_page=" + PER_PAGE;
        return cc.list(uri, DOMAIN_LIST_TYPE)
                 .stream()
                 .map(V3DomainMapper::toCloudDomain)
                 .collect(Collectors.toList());
    }

    private List<V3Domain> getAllDomains() {
        return cc.list("/v3/domains?per_page=" + PER_PAGE, DOMAIN_LIST_TYPE);
    }

    private CloudDomain findDomainByName(String name, boolean required) {
        CloudDomain domain = findDomainByName(name);
        if (domain == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Domain " + name + " not found.");
        }
        return domain;
    }

    private CloudDomain findDomainByName(String name) {
        String uri = "/v3/domains?names=" + name + "&per_page=" + PER_PAGE;
        return cc.list(uri, DOMAIN_LIST_TYPE)
                 .stream()
                 .findFirst()
                 .map(V3DomainMapper::toCloudDomain)
                 .orElse(null);
    }

    private void doCreateDomain(String name) {
        cc.getRestClient()
          .post()
          .uri("/v3/domains")
          .body(Map.of("name", name, "relationships",
                       Map.of("organization", Map.of("data", Map.of("guid", getTargetOrganizationGuid().toString())))))
          .retrieve()
          .toBodilessEntity();
    }

    private void doDeleteDomain(UUID guid) {
        ResponseEntity<Void> response = cc.getRestClient()
                                          .delete()
                                          .uri("/v3/domains/{guid}", guid.toString())
                                          .retrieve()
                                          .toEntity(Void.class);
        cc.followAsyncJob(response, DELETE_JOB_TIMEOUT);
    }

    private UUID getTargetOrganizationGuid() {
        return getGuid(target.getOrganization());
    }

    private UUID getGuid(CloudEntity entity) {
        if (entity == null || entity.getMetadata() == null) {
            return null;
        }
        return entity.getMetadata()
                     .getGuid();
    }

    private void assertSpaceProvided(String operation) {
        Assert.notNull(target, "Unable to " + operation + " without specifying organization and space to use.");
    }

}
