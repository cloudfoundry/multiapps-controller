package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public DomainsV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
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
        V3Domain domain = cc.get(CloudControllerV3Endpoints.ORGANIZATIONS + "/" + getTargetOrganizationGuid() + "/domains/default",
                                 V3Domain.class);
        return V3DomainMapper.toCloudDomain(domain);
    }

    public List<CloudDomain> getDomains() {
        return getAllDomains().stream()
                              .map(V3DomainMapper::toCloudDomain)
                              .collect(Collectors.toList());
    }

    public List<CloudDomain> getSharedDomains() {
        return getAllDomains().stream()
                              .filter(domain -> !domain.isPrivate())
                              .map(V3DomainMapper::toCloudDomain)
                              .collect(Collectors.toList());
    }

    public List<CloudDomain> getPrivateDomains() {
        return getAllDomains().stream()
                              .filter(V3Domain::isPrivate)
                              .map(V3DomainMapper::toCloudDomain)
                              .collect(Collectors.toList());
    }

    public List<CloudDomain> getDomainsForOrganization() {
        assertSpaceProvided("access organization domains");
        String uri = CloudControllerV3Endpoints.ORGANIZATIONS + "/" + getTargetOrganizationGuid() + "/domains"
            + CloudControllerV3Endpoints.QUERY_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return cc.list(uri, DOMAIN_LIST_TYPE)
                 .stream()
                 .map(V3DomainMapper::toCloudDomain)
                 .collect(Collectors.toList());
    }

    private List<V3Domain> getAllDomains() {
        return cc.list(CloudControllerV3Endpoints.DOMAINS + CloudControllerV3Endpoints.QUERY_PER_PAGE
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

        return cc.list(uri, DOMAIN_LIST_TYPE)
                 .stream()
                 .findFirst()
                 .map(V3DomainMapper::toCloudDomain)
                 .orElse(null);
    }

    private void doCreateDomain(String name) {
        cc.getRestClient()
          .post()
          .uri(CloudControllerV3Endpoints.DOMAINS)
          .body(Map.of("name", name, "relationships",
                       Map.of("organization", Map.of("data", Map.of("guid", getTargetOrganizationGuid().toString())))))
          .retrieve()
          .toBodilessEntity();
    }

    private void doDeleteDomain(UUID guid) {
        ResponseEntity<Void> response = cc.getRestClient()
                                          .delete()
                                          .uri(CloudControllerV3Endpoints.DOMAIN_BY_GUID, guid.toString())
                                          .retrieve()
                                          .toEntity(Void.class);
        cc.followAsyncJob(response, Constants.DELETE_JOB_TIMEOUT);
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
