package org.cloudfoundry.multiapps.controller.core.persistence.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationFilter;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationSubscription.ModuleDto;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationSubscription.ResourceDto;
import org.cloudfoundry.multiapps.controller.core.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationSubscriptionService.ConfigurationSubscriptionMapper;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ConfigurationSubscriptionServiceTest {

    private static final ConfigurationSubscription CONFIGURATION_SUBSCRIPTION_1 = createConfigurationSubscription(1L, "mta", "space", "app",
                                                                                                                  createConfigurationFilter("3.1"),
                                                                                                                  createModuleDto("moduleName"),
                                                                                                                  createResourceDto("resourceName"));

    private static final ConfigurationSubscription CONFIGURATION_SUBSCRIPTION_2 = createConfigurationSubscription(2L, "mta1", "space1",
                                                                                                                  "app1",
                                                                                                                  createConfigurationFilter("2.0"),
                                                                                                                  createModuleDto("moduleName1"),
                                                                                                                  createResourceDto("resourceName1"));

    private final ConfigurationSubscriptionService configurationSubscriptionService = createConfigurationSubscriptionService();

    @AfterEach
    public void cleanUp() {
        configurationSubscriptionService.createQuery()
                                        .delete();
    }

    @Test
    public void testAdd() {
        configurationSubscriptionService.add(CONFIGURATION_SUBSCRIPTION_1);
        assertEquals(1, configurationSubscriptionService.createQuery()
                                                        .list()
                                                        .size());
        assertEquals(CONFIGURATION_SUBSCRIPTION_1.getId(), configurationSubscriptionService.createQuery()
                                                                                           .id(CONFIGURATION_SUBSCRIPTION_1.getId())
                                                                                           .singleResult()
                                                                                           .getId());
    }

    @Test
    public void testAddWithNonEmptyDatabase() {
        addConfigurationSubscriptions(Arrays.asList(CONFIGURATION_SUBSCRIPTION_1, CONFIGURATION_SUBSCRIPTION_2));

        assertConfigurationSubscriptionExists(CONFIGURATION_SUBSCRIPTION_1.getId());
        assertConfigurationSubscriptionExists(CONFIGURATION_SUBSCRIPTION_2.getId());

        assertEquals(2, configurationSubscriptionService.createQuery()
                                                        .list()
                                                        .size());
    }

    @Test
    public void testAddWithAlreadyExistingSubscription() {
        configurationSubscriptionService.add(CONFIGURATION_SUBSCRIPTION_1);
        Exception exception = assertThrows(ConflictException.class,
                                           () -> configurationSubscriptionService.add(CONFIGURATION_SUBSCRIPTION_1));
        String expectedExceptionMessage = MessageFormat.format(Messages.CONFIGURATION_SUBSCRIPTION_ALREADY_EXISTS,
                                                               CONFIGURATION_SUBSCRIPTION_1.getMtaId(),
                                                               CONFIGURATION_SUBSCRIPTION_1.getAppName(),
                                                               CONFIGURATION_SUBSCRIPTION_1.getResourceDto()
                                                                                           .getName(),
                                                               CONFIGURATION_SUBSCRIPTION_1.getSpaceId());
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void testQueryById() {
        testQueryByCriteria((query, subscription) -> query.id(subscription.getId()));
    }

    @Test
    public void testQueryByMtaId() {
        testQueryByCriteria((query, subscription) -> query.mtaId(subscription.getMtaId()));
    }

    @Test
    public void testQueryBySpaceId() {
        testQueryByCriteria((query, subscription) -> query.spaceId(subscription.getSpaceId()));
    }

    @Test
    public void testQueryByAppName() {
        testQueryByCriteria((query, subscription) -> query.appName(subscription.getAppName()));
    }

    @Test
    public void testQueryByResourceName() {
        testQueryByCriteria((query, subscription) -> query.resourceName(subscription.getResourceDto()
                                                                                    .getName()));
    }

    @Test
    public void testQueryByFilterMatching() {
        addConfigurationSubscriptions(Arrays.asList(CONFIGURATION_SUBSCRIPTION_1, CONFIGURATION_SUBSCRIPTION_2));

        int foundSubscriptions = configurationSubscriptionService.createQuery()
                                                                 .onSelectMatching(Collections.singletonList(new ConfigurationEntry(null,
                                                                                                                                    null,
                                                                                                                                    Version.parseVersion("3.1"),
                                                                                                                                    "default",
                                                                                                                                    null,
                                                                                                                                    null,
                                                                                                                                    null,
                                                                                                                                    null,
                                                                                                                                    null)))
                                                                 .list()
                                                                 .size();
        assertEquals(1, foundSubscriptions);
    }

    private void testQueryByCriteria(ConfigurationSubscriptionQueryBuilder configurationSubscriptionQueryBuilder) {
        addConfigurationSubscriptions(Arrays.asList(CONFIGURATION_SUBSCRIPTION_1, CONFIGURATION_SUBSCRIPTION_2));
        assertEquals(1,
                     configurationSubscriptionQueryBuilder.build(configurationSubscriptionService.createQuery(),
                                                                 CONFIGURATION_SUBSCRIPTION_1)
                                                          .list()
                                                          .size());
        assertEquals(1,
                     configurationSubscriptionQueryBuilder.build(configurationSubscriptionService.createQuery(),
                                                                 CONFIGURATION_SUBSCRIPTION_1)
                                                          .delete());
        assertConfigurationSubscriptionExists(CONFIGURATION_SUBSCRIPTION_2.getId());
    }

    private interface ConfigurationSubscriptionQueryBuilder {

        ConfigurationSubscriptionQuery build(ConfigurationSubscriptionQuery entryQuery, ConfigurationSubscription testedSubscription);
    }

    private void addConfigurationSubscriptions(List<ConfigurationSubscription> subscriptions) {
        subscriptions.forEach(configurationSubscriptionService::add);
    }

    private static ConfigurationFilter createConfigurationFilter(String providerVersion) {
        return new ConfigurationFilter(null, null, providerVersion, null, null, null);
    }

    private static ModuleDto createModuleDto(String name) {
        return new ModuleDto(name, null, null, null);
    }

    private static ResourceDto createResourceDto(String name) {
        return new ResourceDto(name, null);
    }

    private void assertConfigurationSubscriptionExists(Long id) {
        // If does not exist, will throw NoResultException
        configurationSubscriptionService.createQuery()
                                        .id(id)
                                        .singleResult();
    }

    private static ConfigurationSubscription createConfigurationSubscription(long id, String mtaId, String spaceId, String appName,
                                                                             ConfigurationFilter filter, ModuleDto moduleDto,
                                                                             ResourceDto resourceDto) {
        return new ConfigurationSubscription(id, mtaId, spaceId, appName, filter, moduleDto, resourceDto, null, null);
    }

    private ConfigurationSubscriptionService createConfigurationSubscriptionService() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("TestDefault");
        ConfigurationSubscriptionService configurationSubscriptionService = new ConfigurationSubscriptionService(entityManagerFactory,
                                                                                                                 new ConfigurationSubscriptionMapper());
        return configurationSubscriptionService;
    }

}
