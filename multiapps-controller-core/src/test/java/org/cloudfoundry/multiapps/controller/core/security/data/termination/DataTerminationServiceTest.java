package org.cloudfoundry.multiapps.controller.core.security.data.termination;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingFacade;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.cf.clients.CFOptimizedEventGetter;
import org.cloudfoundry.multiapps.controller.core.test.MockBuilder;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationEntryQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;

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
    private AuditLoggingFacade auditLoggingFacade;

    @InjectMocks
    private final DataTerminationService dataTerminationService = createDataTerminationService();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        AuditLoggingProvider.setFacade(auditLoggingFacade);
    }

    private DataTerminationService createDataTerminationService() {
        return new DataTerminationService() {

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

        ConfigurationSubscriptionQuery configurationSubscriptionQueryMock = new MockBuilder<>(configurationSubscriptionQuery).on(query -> query.spaceId(deletedSpace))
                                                                                                                             .build();
        doReturn(subscriptions).when(configurationSubscriptionQueryMock)
                               .list();

        ConfigurationEntryQuery configurationEntryQueryMock = new MockBuilder<>(configurationEntryQuery).on(query -> query.spaceId(deletedSpace))
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
        when(fileService.deleteBySpaceIds(anyList())).thenThrow(new FileStorageException(""));

        assertDoesNotThrow(() -> dataTerminationService.deleteOrphanUserData());
    }

}
