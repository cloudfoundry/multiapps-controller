package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Domain;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3DomainMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Fields;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

public class DomainsV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Domain>> DOMAIN_PAGE = new ParameterizedTypeReference<>() {
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
            createDomain(domainName);
        }

    }

    public void deleteDomain(String domainName) {
        assertSpaceProvided("delete domain");
        CloudDomain domain = findDomainByName(domainName);

        if (domain == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.DOMAIN_0_NOT_FOUND, domainName));
        }

        deleteDomain(domain.getGuid());
    }

    public CloudDomain getDefaultDomain() {
        UUID organizationGuid = OperationsUtil.getGuid(targetSpace.getOrganization());
        V3Domain domain = client.get(CloudControllerV3Endpoints.ORGANIZATIONS + "/" + organizationGuid + "/domains/default",
                                     V3Domain.class);

        if (domain == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.DOMAIN_WITH_GUID_0_NOT_FOUND, organizationGuid));
        }

        return V3DomainMapper.toCloudDomain(domain);
    }

    public List<CloudDomain> getDomains() {
        return filterAndMapDomains(domain -> true);
    }

    public List<CloudDomain> getSharedDomains() {
        return filterAndMapDomains(domain -> !domain.isPrivate());
    }

    public List<CloudDomain> getPrivateDomains() {
        return filterAndMapDomains(domain -> domain.isPrivate());
    }

    private List<CloudDomain> filterAndMapDomains(Predicate<V3Domain> filterPredicate) {
        return getAllDomains().stream()
                              .filter(filterPredicate)
                              .map(V3DomainMapper::toCloudDomain)
                              .toList();
    }

    public List<CloudDomain> getDomainsForOrganization() {
        assertSpaceProvided("access organization domains");
        UUID targetOrganizationGuid = OperationsUtil.getGuid(targetSpace.getOrganization());

        String uri = CloudControllerV3Endpoints.ORGANIZATIONS + "/" + targetOrganizationGuid + "/domains"
            + CloudControllerV3Endpoints.QUERY_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, DOMAIN_PAGE)
                     .stream()
                     .map(V3DomainMapper::toCloudDomain)
                     .toList();
    }

    private List<V3Domain> getAllDomains() {
        return client.list(CloudControllerV3Endpoints.DOMAINS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                               + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE, DOMAIN_PAGE);
    }

    private CloudDomain findDomainByName(String name) {
        String uri = CloudControllerV3Endpoints.DOMAINS + CloudControllerV3Endpoints.QUERY_NAMES + name
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, DOMAIN_PAGE)
                     .stream()
                     .findFirst()
                     .map(V3DomainMapper::toCloudDomain)
                     .orElse(null);
    }

    private void createDomain(String name) {
        UUID organizationGuid = OperationsUtil.getGuid(targetSpace.getOrganization());
        if (organizationGuid == null) {
            throw new CloudOperationException(HttpStatus.BAD_REQUEST, Messages.BAD_REQUEST,
                                              MessageFormat.format(Messages.CANNOT_CREATE_DOMAIN_0_WITHOUT_ORGANIZATION, name));
        }

        Map<String, Object> organizationRelationship = OperationsUtil.organizationRelationship(organizationGuid);
        client.post(CloudControllerV3Endpoints.DOMAINS, Map.of(V3Fields.NAME, name, V3Fields.RELATIONSHIPS, organizationRelationship));
    }

    private void deleteDomain(UUID guid) {
        ResponseEntity<Void> response = client.delete(CloudControllerV3Endpoints.DOMAIN_BY_GUID, guid.toString());
        client.followAsyncJob(response, Constants.DELETE_JOB_TIMEOUT);
    }

    private void assertSpaceProvided(String operation) {
        Assert.notNull(targetSpace, "Unable to " + operation + " without specifying organization and space to use.");
    }

}
