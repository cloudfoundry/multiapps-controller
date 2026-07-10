package org.cloudfoundry.multiapps.controller.core.security.data.termination;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.CloudLoggingServiceConfigurationAuditLog;
import org.cloudfoundry.multiapps.controller.core.auditlogging.MtaConfigurationPurgerAuditLog;
import org.cloudfoundry.multiapps.controller.core.cf.clients.CFOptimizedEventGetter;
import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.cloudfoundry.multiapps.controller.core.test.MockBuilder;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationEntryQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorBackupService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.persistence.services.cloudlogging.CloudLoggingServiceConfigurationService;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataTerminationServiceTest {

    private static final String GLOBAL_AUDITOR_USERNAME = "test";
    private static final String GLOBAL_AUDITOR_PASSWORD = "test";

    @Mock
    private ConfigurationEntryService configurationEntryService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationEntryQuery configurationEntryQuery;
    @Mock
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationSubscriptionQuery configurationSubscriptionQuery;
    @Mock
    private OperationService operationService;
    @Mock(answer = Answers.RETURNS_SELF)
    private OperationQuery operationQuery;
    @Mock
    private FileService fileService;
    @Mock
    private ApplicationConfiguration configuration;
    @Mock
    private CFOptimizedEventGetter cfOptimizedEventsGetter;
    @Mock
    private MtaConfigurationPurgerAuditLog mtaConfigurationPurgerAuditLog;
    @Mock
    private DescriptorBackupService descriptorBackupService;
    @Mock
    private WebClientFactory webClientFactory;
    @Mock
    private CloudLoggingServiceConfigurationService cloudLoggingServiceConfigurationService;
    @Mock
    private CloudLoggingServiceConfigurationAuditLog cloudLoggingServiceConfigurationAuditLog;
    private DataTerminationService dataTerminationService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        dataTerminationService = createDataTerminationService();
    }

    private DataTerminationService createDataTerminationService() {
        return new DataTerminationService(configurationEntryService, configurationSubscriptionService, operationService, fileService,
                                          configuration, webClientFactory, mtaConfigurationPurgerAuditLog, descriptorBackupService,
                                          cloudLoggingServiceConfigurationService, cloudLoggingServiceConfigurationAuditLog) {

            @Override
            protected CFOptimizedEventGetter getCfOptimizedEventGetter() {
                return cfOptimizedEventsGetter;
            }

        };
    }

    static Stream<Arguments> testDeleteData() {
        return Stream.of(Arguments.of(2, true, true), Arguments.of(3, false, true), Arguments.of(5, true, false),
                         Arguments.of(0, false, false));
    }

    @ParameterizedTest
    @MethodSource
    void testDeleteData(int countOfDeletedSpaceIds, boolean isExistSubscriptionData, boolean isExistConfigurationEntryData)
        throws FileStorageException {
        prepareGlobalAuditorCredentials();
        List<String> deletedSpaceIds = generateDeletedSpaceIds(countOfDeletedSpaceIds);
        prepareCfOptimizedEventsGetter(deletedSpaceIds);
        prepareServices(deletedSpaceIds, isExistSubscriptionData, isExistConfigurationEntryData);

        dataTerminationService.deleteOrphanUserData();

        verifyDeleteOrphanUserData(deletedSpaceIds, countOfDeletedSpaceIds, isExistSubscriptionData, isExistConfigurationEntryData);
    }

    private void prepareGlobalAuditorCredentials() {
        when(configuration.getGlobalAuditorUser()).thenReturn(GLOBAL_AUDITOR_USERNAME);
        when(configuration.getGlobalAuditorPassword()).thenReturn(GLOBAL_AUDITOR_PASSWORD);
    }

    private List<String> generateDeletedSpaceIds(int countOfDeletedSpaceIds) {
        return IntStream.range(0, countOfDeletedSpaceIds)
                        .mapToObj(counter -> MessageFormat.format("space-{0}", counter))
                        .collect(Collectors.toList());
    }

    private void prepareCfOptimizedEventsGetter(List<String> deletedSpaceIds) {
        when(cfOptimizedEventsGetter.findEvents(anyString(), anyString())).thenReturn(deletedSpaceIds);
    }

    private void prepareServices(List<String> deletedSpaceIds, boolean isExistSubscriptionData, boolean isExistConfigurationEntryData) {
        List<ConfigurationSubscription> subscriptions = generateSubscriptions(isExistSubscriptionData);
        List<ConfigurationEntry> configurationEntries = generatedConfigurationEntries(isExistConfigurationEntryData);
        deletedSpaceIds.forEach(deletedSpace -> initializeServiceMocks(subscriptions, configurationEntries, deletedSpace));
        when(operationService.createQuery()).thenReturn(operationQuery);
        when(cloudLoggingServiceConfigurationService.getLoggingConfigurationsBySpace(anyString())).thenReturn(List.of());
    }

    private List<ConfigurationSubscription> generateSubscriptions(boolean isExistSubscriptionData) {
        return isExistSubscriptionData ? List.of(new ConfigurationSubscription()) : Collections.emptyList();
    }

    private List<ConfigurationEntry> generatedConfigurationEntries(boolean isExistConfigurationEntryData) {
        return isExistConfigurationEntryData
            ? List.of(new ConfigurationEntry("test-providerNid",
                                             "providerId",
                                             Version.parseVersion("1"),
                                             "default",
                                             new CloudTarget(),
                                             "content",
                                             Collections.emptyList(),
                                             "space",
                                             null))
            : Collections.emptyList();
    }

    private void initializeServiceMocks(List<ConfigurationSubscription> subscriptions, List<ConfigurationEntry> configurationEntries,
                                        String deletedSpace) {
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        when(configurationEntryService.createQuery()).thenReturn(configurationEntryQuery);

        ConfigurationSubscriptionQuery configurationSubscriptionQueryMock = new MockBuilder<>(configurationSubscriptionQuery).on(
                                                                                                                                 query -> query.spaceId(deletedSpace))
                                                                                                                             .build();
        doReturn(subscriptions).when(configurationSubscriptionQueryMock)
                               .list();

        ConfigurationEntryQuery configurationEntryQueryMock = new MockBuilder<>(configurationEntryQuery).on(
                                                                                                            query -> query.spaceId(deletedSpace))
                                                                                                        .build();
        doReturn(configurationEntries).when(configurationEntryQueryMock)
                                      .list();
    }

    private void verifyDeleteOrphanUserData(List<String> deletedSpaceIds, int countOfDeletedSpaceIds, boolean isExistSubscriptionData,
                                            boolean isExistConfigurationEntryData)
        throws FileStorageException {
        verify(fileService, times(countOfDeletedSpaceIds > 0 ? 1 : 0)).deleteBySpaceIds(anyList());
        verifyExistSubscriptionData(deletedSpaceIds, isExistSubscriptionData);
        verifyExistConfigurationEntryData(deletedSpaceIds, isExistConfigurationEntryData);

    }

    private void verifyExistSubscriptionData(List<String> deletedSpaceIds, boolean isExistSubscriptionData) {
        if (isExistSubscriptionData) {
            verifySubscriptionsDeletedBySpace(deletedSpaceIds, times(1));
            return;
        }
        verifySubscriptionsDeletedBySpace(deletedSpaceIds, never());
    }

    private void verifySubscriptionsDeletedBySpace(List<String> deletedSpaceIds, VerificationMode verificationMode) {
        for (String deletedSpace : deletedSpaceIds) {
            verify(configurationSubscriptionQuery, verificationMode).deleteAll(deletedSpace);
        }
    }

    private void verifyExistConfigurationEntryData(List<String> deletedSpaceIds, boolean isExistConfigurationEntryData) {
        if (isExistConfigurationEntryData) {
            verifyEntriesDeletedBySpace(deletedSpaceIds, times(1));
            return;
        }
        verifyEntriesDeletedBySpace(deletedSpaceIds, never());
    }

    private void verifyEntriesDeletedBySpace(List<String> deletedSpaceIds, VerificationMode verificationMode) {
        for (String deletedSpaceId : deletedSpaceIds) {
            verify(configurationEntryQuery, verificationMode).deleteAll(deletedSpaceId);
        }
    }

    @Test
    void testMissingAuditorCredentials() {
        Exception exception = assertThrows(IllegalStateException.class, () -> dataTerminationService.deleteOrphanUserData());
        assertEquals(Messages.MISSING_GLOBAL_AUDITOR_CREDENTIALS, exception.getMessage());
    }

    @Test
    void testDoesNotThrowExceptionOnFailToDeleteSpace() throws FileStorageException {
        prepareGlobalAuditorCredentials();
        prepareCfOptimizedEventsGetter(generateDeletedSpaceIds(1));
        when(configurationEntryService.createQuery()).thenReturn(configurationEntryQuery);
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        when(operationService.createQuery()).thenReturn(operationQuery);
        when(cloudLoggingServiceConfigurationService.getLoggingConfigurationsBySpace(anyString())).thenReturn(List.of());
        when(fileService.deleteBySpaceIds(anyList())).thenThrow(new FileStorageException(""));

        assertDoesNotThrow(() -> dataTerminationService.deleteOrphanUserData());
    }

    @Test
    void testDeleteExistingCloudLoggingServiceConfiguration_deletesAllConfigurationsInSpace() {
        String spaceId = "space-1";
        LoggingConfiguration config1 = createLoggingConfiguration("id-1", spaceId, "mta-1");
        LoggingConfiguration config2 = createLoggingConfiguration("id-2", spaceId, "mta-2");
        stubLoggingConfigurationsForSpace(spaceId, List.of(config1, config2));
        prepareGlobalAuditorCredentials();
        prepareCfOptimizedEventsGetter(List.of(spaceId));
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        when(configurationEntryService.createQuery()).thenReturn(configurationEntryQuery);
        when(operationService.createQuery()).thenReturn(operationQuery);

        dataTerminationService.deleteOrphanUserData();

        verify(cloudLoggingServiceConfigurationService).deleteLoggingConfiguration("id-1");
        verify(cloudLoggingServiceConfigurationService).deleteLoggingConfiguration("id-2");
        verify(cloudLoggingServiceConfigurationAuditLog).logDeleteLoggingConfiguration("", spaceId, config1);
        verify(cloudLoggingServiceConfigurationAuditLog).logDeleteLoggingConfiguration("", spaceId, config2);
    }

    @Test
    void testDeleteExistingCloudLoggingServiceConfiguration_doesNothingWhenNoConfigurationsExist() {
        String spaceId = "space-2";
        stubLoggingConfigurationsForSpace(spaceId, List.of());
        prepareGlobalAuditorCredentials();
        prepareCfOptimizedEventsGetter(List.of(spaceId));
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        when(configurationEntryService.createQuery()).thenReturn(configurationEntryQuery);
        when(operationService.createQuery()).thenReturn(operationQuery);

        dataTerminationService.deleteOrphanUserData();

        verify(cloudLoggingServiceConfigurationService, never()).deleteLoggingConfiguration(anyString());
        verify(cloudLoggingServiceConfigurationAuditLog, never()).logDeleteLoggingConfiguration(anyString(), anyString(), any());
    }

    @Test
    void testDeleteExistingCloudLoggingServiceConfiguration_deletesSingleConfiguration() {
        String spaceId = "space-3";
        LoggingConfiguration config = createLoggingConfiguration("id-1", spaceId, "mta-1");
        stubLoggingConfigurationsForSpace(spaceId, List.of(config));
        prepareGlobalAuditorCredentials();
        prepareCfOptimizedEventsGetter(List.of(spaceId));
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        when(configurationEntryService.createQuery()).thenReturn(configurationEntryQuery);
        when(operationService.createQuery()).thenReturn(operationQuery);

        dataTerminationService.deleteOrphanUserData();

        verify(cloudLoggingServiceConfigurationService).deleteLoggingConfiguration("id-1");
        verify(cloudLoggingServiceConfigurationAuditLog).logDeleteLoggingConfiguration("", spaceId, config);
    }

    @Test
    void testDeleteExistingCloudLoggingServiceConfiguration_deletesAcrossMultipleSpaces() {
        String spaceId1 = "space-4";
        String spaceId2 = "space-5";
        LoggingConfiguration config1 = createLoggingConfiguration("id-1", spaceId1, "mta-1");
        LoggingConfiguration config2 = createLoggingConfiguration("id-2", spaceId2, "mta-2");
        stubLoggingConfigurationsForSpace(spaceId1, List.of(config1));
        stubLoggingConfigurationsForSpace(spaceId2, List.of(config2));
        prepareGlobalAuditorCredentials();
        prepareCfOptimizedEventsGetter(List.of(spaceId1, spaceId2));
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        when(configurationEntryService.createQuery()).thenReturn(configurationEntryQuery);
        when(operationService.createQuery()).thenReturn(operationQuery);

        dataTerminationService.deleteOrphanUserData();

        verify(cloudLoggingServiceConfigurationService).deleteLoggingConfiguration("id-1");
        verify(cloudLoggingServiceConfigurationService).deleteLoggingConfiguration("id-2");
        verify(cloudLoggingServiceConfigurationAuditLog).logDeleteLoggingConfiguration("", spaceId1, config1);
        verify(cloudLoggingServiceConfigurationAuditLog).logDeleteLoggingConfiguration("", spaceId2, config2);
    }

    private void stubLoggingConfigurationsForSpace(String spaceId, List<LoggingConfiguration> configurations) {
        when(cloudLoggingServiceConfigurationService.getLoggingConfigurationsBySpace(spaceId)).thenReturn(configurations);
    }

    private LoggingConfiguration createLoggingConfiguration(String id, String spaceId, String mtaId) {
        return ImmutableLoggingConfiguration.builder()
                                            .id(id)
                                            .mtaSpaceId(spaceId)
                                            .mtaId(mtaId)
                                            .build();
    }

}

