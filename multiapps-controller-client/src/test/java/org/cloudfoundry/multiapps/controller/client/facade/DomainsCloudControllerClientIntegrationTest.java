package org.cloudfoundry.multiapps.controller.client.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;

class DomainsCloudControllerClientIntegrationTest extends CloudControllerClientIntegrationTest {

    @Test
    @DisplayName("Add missing domain")
    void addDomain() throws IOException {
        String domainName = ITVariable.DOMAIN_NAME.getValue();
        assertDomainExists(domainName, false);
        try {
            client.addDomain(domainName);
            assertDomainExists(domainName, true);
        } finally {
            client.deleteDomain(domainName);
        }
    }

    @Test
    @DisplayName("Add existing domain and verify addition does not fail")
    void addDomainAlreadyExists() throws IOException {
        String domainName = ITVariable.DOMAIN_NAME.getValue();
        try {
            client.addDomain(domainName);
            assertDomainExists(domainName, true);
            client.addDomain(domainName);
        } finally {
            client.deleteDomain(domainName);
        }
    }

    @Test
    @DisplayName("Delete existing domain")
    void deleteDomain() throws IOException {
        String domainName = "delete-domain" + ITVariable.DOMAIN_NAME.getValue();
        client.addDomain(domainName);
        assertDomainExists(domainName, true);
        client.deleteDomain(domainName);
        assertDomainExists(domainName, false);
    }

    @Test
    @DisplayName("Delete missing domain and verify deletion fails")
    void deleteDomainMissing() throws IOException {
        String domainName = "delete-domain-missing" + ITVariable.DOMAIN_NAME.getValue();
        assertDomainExists(domainName, false);
        try {
            client.deleteDomain(domainName);
            fail();
        } catch (CloudOperationException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    private static void assertDomainExists(String domainName, boolean domainExists) {
        boolean actualDomainExists = client.getDomainsForOrganization()
                                           .stream()
                                           .map(CloudDomain::getName)
                                           .anyMatch(domainName::equals);
        assertEquals(domainExists, actualDomainExists);
    }
}
