package com.sap.cloud.lm.sl.cf.core.persistence.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationEntryQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService.ConfigurationEntryMapper;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.mta.model.Version;

public class ConfigurationEntryServiceTest {

    private static final ConfigurationEntry CONFIGURATION_ENTRY_1 = createConfigurationEntry(1l, "providerNid", "providerId", "2.0", "org",
                                                                                             "space", "content");
    private static final ConfigurationEntry CONFIGURATION_ENTRY_2 = createConfigurationEntry(2l, "providerNid1", "providerId1", "3.1",
                                                                                             "org1", "space1", "content1");
    private ConfigurationEntryService configurationEntryService = createConfigurationEntryService();

    @AfterEach
    public void cleanUp() {
        configurationEntryService.createQuery()
                                 .delete();
    }

    @Test
    public void testAdd() {
        configurationEntryService.add(CONFIGURATION_ENTRY_1);
        assertEquals(1, configurationEntryService.createQuery()
                                                 .list()
                                                 .size());
        assertEquals(CONFIGURATION_ENTRY_1.getId(), configurationEntryService.createQuery()
                                                                             .id(CONFIGURATION_ENTRY_1.getId())
                                                                             .singleResult()
                                                                             .getId());
    }

    @Test
    public void testAddWithNonEmptyDatabase() {
        addConfigurationEntries(Arrays.asList(CONFIGURATION_ENTRY_1, CONFIGURATION_ENTRY_2));

        assertConfigurationEntryExists(CONFIGURATION_ENTRY_1.getId());
        assertConfigurationEntryExists(CONFIGURATION_ENTRY_2.getId());

        assertEquals(2, configurationEntryService.createQuery()
                                                 .list()
                                                 .size());
    }

    @Test
    public void testAddWithAlreadyExistingEntry() {
        configurationEntryService.add(CONFIGURATION_ENTRY_1);
        Exception exception = assertThrows(ConflictException.class, () -> configurationEntryService.add(CONFIGURATION_ENTRY_1));
        String expectedExceptionMessage = MessageFormat.format(Messages.CONFIGURATION_ENTRY_ALREADY_EXISTS,
                                                               CONFIGURATION_ENTRY_1.getProviderNid(),
                                                               CONFIGURATION_ENTRY_1.getProviderId(),
                                                               CONFIGURATION_ENTRY_1.getProviderVersion()
                                                                                    .toString(),
                                                               CONFIGURATION_ENTRY_1.getTargetSpace()
                                                                                    .getOrganizationName(),
                                                               CONFIGURATION_ENTRY_1.getTargetSpace()
                                                                                    .getSpaceName());
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void testQueryById() {
        testQueryByCriteria((query, entry) -> query.id(entry.getId()));
    }

    @Test
    public void testQueryByProviderNid() {
        testQueryByCriteria((query, entry) -> query.providerNid(entry.getProviderNid()));
    }

    @Test
    public void testQueryByProviderId() {
        testQueryByCriteria((query, entry) -> query.providerId(entry.getProviderId()));
    }

    @Test
    public void testQueryByTarget() {
        testQueryByCriteria((query, entry) -> query.target(entry.getTargetSpace()));
    }

    @Test
    public void testQueryBySpaceId() {
        testQueryByCriteria((query, entry) -> query.spaceId(entry.getSpaceId()));
    }

    @Test
    public void testQueryByVersion() {
        addConfigurationEntries(Arrays.asList(CONFIGURATION_ENTRY_1, CONFIGURATION_ENTRY_2));

        String version = ">3.0.0";
        assertEquals(1, configurationEntryService.createQuery()
                                                 .version(version)
                                                 .list()
                                                 .size());
    }

    private void testQueryByCriteria(ConfigurationEntryQueryBuilder configurationEntryQueryBuilder) {
        addConfigurationEntries(Arrays.asList(CONFIGURATION_ENTRY_1, CONFIGURATION_ENTRY_2));
        assertEquals(1, configurationEntryQueryBuilder.build(configurationEntryService.createQuery(), CONFIGURATION_ENTRY_1)
                                                      .list()
                                                      .size());
        assertEquals(1, configurationEntryQueryBuilder.build(configurationEntryService.createQuery(), CONFIGURATION_ENTRY_1)
                                                      .delete());
        assertConfigurationEntryExists(CONFIGURATION_ENTRY_2.getId());
    }

    private interface ConfigurationEntryQueryBuilder {

        ConfigurationEntryQuery build(ConfigurationEntryQuery entryQuery, ConfigurationEntry testedEntry);
    }

    private void addConfigurationEntries(List<ConfigurationEntry> entries) {
        entries.forEach(configurationEntryService::add);
    }

    private void assertConfigurationEntryExists(Long id) {
        // If does not exist, will throw NoResultException
        configurationEntryService.createQuery()
                                 .id(id)
                                 .singleResult();
    }

    private static ConfigurationEntry createConfigurationEntry(long id, String providerNid, String providerId, String version, String org,
                                                               String space, String content) {
        return new ConfigurationEntry(id,
                                      providerNid,
                                      providerId,
                                      Version.parseVersion(version),
                                      new CloudTarget(org, space),
                                      content,
                                      Collections.emptyList(),
                                      space);
    }

    private ConfigurationEntryService createConfigurationEntryService() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("TestDefault");
        ConfigurationEntryService configurationEntryService = new ConfigurationEntryService(entityManagerFactory);
        ConfigurationEntryMapper configurationEntryFactory = new ConfigurationEntryMapper();
        configurationEntryService.entryMapper = configurationEntryFactory;
        return configurationEntryService;
    }

}
