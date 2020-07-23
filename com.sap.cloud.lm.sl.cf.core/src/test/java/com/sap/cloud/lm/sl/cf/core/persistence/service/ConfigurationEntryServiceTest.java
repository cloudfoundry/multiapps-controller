package com.sap.cloud.lm.sl.cf.core.persistence.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationEntryQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService.ConfigurationEntryMapper;

public class ConfigurationEntryServiceTest {

    private static final ConfigurationEntry CONFIGURATION_ENTRY_1 = createConfigurationEntry(1L, "providerNid1", "providerId1", "2.0", null,
                                                                                             "org1", "space1", "content1");
    private static final ConfigurationEntry CONFIGURATION_ENTRY_2 = createConfigurationEntry(2L, "providerNid2", "providerId2", "3.1", null,
                                                                                             "org2", "space2", "content2");
    private static final ConfigurationEntry CONFIGURATION_ENTRY_3 = createConfigurationEntry(3L, "providerNid3", "providerId3", "3.1",
                                                                                             "namespace", "org3", "space3", "content3");
    private static final List<ConfigurationEntry> ALL_ENTRIES = Arrays.asList(CONFIGURATION_ENTRY_1, CONFIGURATION_ENTRY_2,
                                                                              CONFIGURATION_ENTRY_3);
    private final ConfigurationEntryService configurationEntryService = createConfigurationEntryService();

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
                                                               CONFIGURATION_ENTRY_1.getProviderNamespace(),
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

    @Test
    public void testQueryByProviderNamespace() {
        addConfigurationEntries(ALL_ENTRIES);

        ConfigurationEntryQuery allEntries = configurationEntryService.createQuery()
                                                                      .providerNamespace(null, false);
        assertEquals(3, allEntries.list()
                                  .size());

        ConfigurationEntryQuery allEntriesNoNamespace = configurationEntryService.createQuery()
                                                                                 .providerNamespace(null, true);
        assertEquals(2, allEntriesNoNamespace.list()
                                             .size());

        ConfigurationEntryQuery allEntriesNoNamespaceUsingKeyword = configurationEntryService.createQuery()
                                                                                             .providerNamespace("default", false);
        assertEquals(2, allEntriesNoNamespaceUsingKeyword.list()
                                                         .size());

        ConfigurationEntryQuery specificEntryByNamespace = configurationEntryService.createQuery()
                                                                                    .providerNamespace("namespace", false);
        assertEquals(1, specificEntryByNamespace.list()
                                                .size());

    }

    private void testQueryByCriteria(ConfigurationEntryQueryBuilder configurationEntryQueryBuilder) {

        addConfigurationEntries(ALL_ENTRIES);

        assertThereAndDeleteEntries(ALL_ENTRIES, configurationEntryQueryBuilder);
    }

    private interface ConfigurationEntryQueryBuilder {

        ConfigurationEntryQuery build(ConfigurationEntryQuery entryQuery, ConfigurationEntry testedEntry);
    }

    private void addConfigurationEntries(List<ConfigurationEntry> entries) {
        entries.forEach(configurationEntryService::add);
    }

    private void assertThereAndDeleteEntries(List<ConfigurationEntry> entries,
                                             ConfigurationEntryQueryBuilder configurationEntryQueryBuilder) {
        for (ConfigurationEntry entry : entries) {
            assertConfigurationEntryExists(entry.getId());
            assertEquals(1, configurationEntryQueryBuilder.build(configurationEntryService.createQuery(), entry)
                                                          .list()
                                                          .size());
            assertEquals(1, configurationEntryQueryBuilder.build(configurationEntryService.createQuery(), entry)
                                                          .delete());
        }
    }

    private void assertConfigurationEntryExists(Long id) {
        // If does not exist, will throw NoResultException
        configurationEntryService.createQuery()
                                 .id(id)
                                 .singleResult();
    }

    private static ConfigurationEntry createConfigurationEntry(long id, String providerNid, String providerId, String version,
                                                               String providerNamespace, String org, String space, String content) {
        return new ConfigurationEntry(id,
                                      providerNid,
                                      providerId,
                                      Version.parseVersion(version),
                                      providerNamespace,
                                      new CloudTarget(org, space),
                                      content,
                                      Collections.emptyList(),
                                      space,
                                      null);
    }

    private ConfigurationEntryService createConfigurationEntryService() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("TestDefault");
        ConfigurationEntryService configurationEntryService = new ConfigurationEntryService(entityManagerFactory,
                                                                                            new ConfigurationEntryMapper());
        return configurationEntryService;
    }

}
