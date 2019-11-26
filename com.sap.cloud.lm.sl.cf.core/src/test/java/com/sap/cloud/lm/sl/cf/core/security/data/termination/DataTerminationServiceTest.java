package com.sap.cloud.lm.sl.cf.core.security.data.termination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
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

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.CFOptimizedEventGetter;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationEntryQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationSubscriptionQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.OperationQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.MockBuilder;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.Version;

public class DataTerminationServiceTest {

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
    private DataTerminationService dataTerminationService = createDataTerminationService();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
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

    public static Stream<Arguments> testDeleteData() {
        return Stream.of(
// @formatter:off                   
                        Arguments.of(2, true, true),
                        Arguments.of(3, false, true),
                        Arguments.of(5, true, false),
                        Arguments.of(0, false, false)
                        
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDeleteData(int countOfDeletedSpaces, boolean isExistSubscriptionData, boolean isExistConfigurationEntryData)
        throws FileStorageException {
        prepareGlobalAuditorCredentials();
        List<String> deletedSpaces = generateDeletedSpaces(countOfDeletedSpaces);
        prepareCfOptimizedEventsGetter(deletedSpaces);
        prepareServices(deletedSpaces, isExistSubscriptionData, isExistConfigurationEntryData);

        dataTerminationService.deleteOrphanUserData();

        verifyDeleteOrphanUserData(deletedSpaces, countOfDeletedSpaces, isExistSubscriptionData, isExistConfigurationEntryData);
    }

    private void prepareGlobalAuditorCredentials() {
        when(configuration.getGlobalAuditorUser()).thenReturn(GLOBAL_AUDITOR_USERNAME);
        when(configuration.getGlobalAuditorPassword()).thenReturn(GLOBAL_AUDITOR_PASSWORD);
    }

    private List<String> generateDeletedSpaces(int countOfDeletedSpaces) {
        return IntStream.range(0, countOfDeletedSpaces)
                        .mapToObj(counter -> MessageFormat.format("space-{0}", counter))
                        .collect(Collectors.toList());
    }

    private void prepareCfOptimizedEventsGetter(List<String> deletedSpaces) {
        when(cfOptimizedEventsGetter.findEvents(anyString(), anyString())).thenReturn(deletedSpaces);
    }

    private void prepareServices(List<String> deletedSpaces, boolean isExistSubscriptionData, boolean isExistConfigurationEntryData) {
        List<ConfigurationSubscription> subscriptions = generateSubscriptions(isExistSubscriptionData);
        List<ConfigurationEntry> configurationEntries = generatedConfigurationEntries(isExistConfigurationEntryData);
        deletedSpaces.forEach(deletedSpace -> initializeServiceMocks(subscriptions, configurationEntries, deletedSpace));
        when(operationService.createQuery()).thenReturn(operationQuery);
    }

    private List<ConfigurationSubscription> generateSubscriptions(boolean isExistSubscriptionData) {
        return isExistSubscriptionData ? Collections.singletonList(new ConfigurationSubscription()) : Collections.emptyList();
    }

    private List<ConfigurationEntry> generatedConfigurationEntries(boolean isExistConfigurationEntryData) {
        return isExistConfigurationEntryData
            ? Collections.singletonList(new ConfigurationEntry("",
                                                               "",
                                                               Version.parseVersion("1"),
                                                               "default",
                                                               new CloudTarget(),
                                                               "",
                                                               Collections.emptyList(),
                                                               ""))
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

    private void verifyDeleteOrphanUserData(List<String> deletedSpaces, int countOfDeletedSpaces, boolean isExistSubscriptionData,
                                            boolean isExistConfigurationEntryData)
        throws FileStorageException {
        verify(fileService, atLeast(countOfDeletedSpaces)).deleteBySpace(anyString());
        verifyExistSubscriptionData(deletedSpaces, isExistSubscriptionData);
        verifyExistConfigurationEntryData(deletedSpaces, countOfDeletedSpaces, isExistConfigurationEntryData);

    }

    private void verifyExistSubscriptionData(List<String> deletedSpaces, boolean isExistSubscriptionData) {
        if (isExistSubscriptionData) {
            verifySubscriptionsDeletedBySpace(deletedSpaces, times(1));
            return;
        }
        verifySubscriptionsDeletedBySpace(deletedSpaces, never());
    }

    private void verifySubscriptionsDeletedBySpace(List<String> deletedSpaces, VerificationMode verificationMode) {
        for (String deletedSpace : deletedSpaces) {
            verify(configurationSubscriptionQuery.spaceId(deletedSpace), verificationMode).delete();
        }
    }

    private void verifyExistConfigurationEntryData(List<String> deletedSpaces, int countOfDeletedSpaces,
                                                   boolean isExistConfigurationEntryData) {
        if (isExistConfigurationEntryData) {
            verifyEntriesDeletedBySpace(deletedSpaces, times(1));
            return;
        }
        verifyEntriesDeletedBySpace(deletedSpaces, never());
    }

    private void verifyEntriesDeletedBySpace(List<String> deletedSpaces, VerificationMode verificationMode) {
        for (String deletedSpace : deletedSpaces) {
            verify(configurationEntryQuery.spaceId(deletedSpace), verificationMode).delete();
        }
    }

    @Test
    public void testMissingAuditorCredentials() {
        Exception exception = assertThrows(IllegalStateException.class, () -> dataTerminationService.deleteOrphanUserData());
        assertEquals(Messages.MISSING_GLOBAL_AUDITOR_CREDENTIALS, exception.getMessage());
    }

    @Test
    public void testFailToDeleteSpace() throws FileStorageException {
        prepareGlobalAuditorCredentials();
        prepareCfOptimizedEventsGetter(generateDeletedSpaces(1));
        when(configurationEntryService.createQuery()).thenReturn(configurationEntryQuery);
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        when(operationService.createQuery()).thenReturn(operationQuery);
        when(fileService.deleteBySpace(anyString())).thenThrow(new FileStorageException(""));

        Exception exception = assertThrows(SLException.class, () -> dataTerminationService.deleteOrphanUserData());
        assertEquals(Messages.COULD_NOT_DELETE_SPACE_LEFTOVERS, exception.getMessage());
    }
}
