package com.sap.cloud.lm.sl.cf.core.security.data.termination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.Arrays;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.CFOptimizedEventGetter;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.Version;

public class DataTerminationServiceTest {

    private static final String GLOBAL_AUDITOR_USERNAME = "test";
    private static final String GLOBAL_AUDITOR_PASSWORD = "test";

    @Mock
    private ConfigurationEntryDao entryDao;
    @Mock
    private ConfigurationSubscriptionDao subscriptionDao;
    @Mock
    private OperationDao operationDao;
    @Mock
    private FileService fileService;
    @Mock
    private ApplicationConfiguration configuration;
    @Mock
    private CFOptimizedEventGetter cfOptmizedEventsGetter;
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
                return cfOptmizedEventsGetter;
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
        prepareDaos(deletedSpaces, isExistSubscriptionData, isExistConfigurationEntryData);

        dataTerminationService.deleteOrphanUserData();

        verifyDeleteOrphanUserData(countOfDeletedSpaces, isExistSubscriptionData, isExistConfigurationEntryData);
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
        when(cfOptmizedEventsGetter.findEvents(anyString(), anyString())).thenReturn(deletedSpaces);
    }

    private void prepareDaos(List<String> deletedSpaces, boolean isExistSubscriptionData, boolean isExistConfigurationEntryData) {
        List<ConfigurationSubscription> subscriptions = generateSubscriptions(isExistSubscriptionData);
        List<ConfigurationEntry> configurationEntries = generatedConfigurationEntries(isExistConfigurationEntryData);
        deletedSpaces.forEach(deletedSpace -> initializeDaoMocks(subscriptions, configurationEntries, deletedSpace));
    }

    private List<ConfigurationSubscription> generateSubscriptions(boolean isExistSubscriptionData) {
        return isExistSubscriptionData ? Arrays.asList(new ConfigurationSubscription()) : Collections.emptyList();
    }

    private List<ConfigurationEntry> generatedConfigurationEntries(boolean isExistConfigurationEntryData) {
        return isExistConfigurationEntryData
            ? Arrays.asList(new ConfigurationEntry("", "", Version.parseVersion("1"), new CloudTarget(), "", Collections.emptyList(), ""))
            : Collections.emptyList();
    }

    private void initializeDaoMocks(List<ConfigurationSubscription> subscriptions, List<ConfigurationEntry> configurationEntries,
                                    String deletedSpace) {
        when(subscriptionDao.findAll(null, null, deletedSpace, null)).thenReturn(subscriptions);
        when(entryDao.find(deletedSpace)).thenReturn(configurationEntries);
    }

    private void verifyDeleteOrphanUserData(int countOfDeletedSpaces, boolean isExistSubscriptionData,
                                            boolean isExistConfigurationEntryData)
        throws FileStorageException {
        verify(fileService, atLeast(countOfDeletedSpaces)).deleteBySpace(anyString());
        verifyExistSubscriptionData(countOfDeletedSpaces, isExistSubscriptionData);
        verifyExistConfigurationEntryData(countOfDeletedSpaces, isExistConfigurationEntryData);

    }

    private void verifyExistSubscriptionData(int countOfDeletedSpaces, boolean isExistSubscriptionData) {
        if (isExistSubscriptionData) {
            verify(subscriptionDao, atLeast(countOfDeletedSpaces)).removeAll(any());
            return;
        }
        verify(subscriptionDao, never()).removeAll(any());

    }

    private void verifyExistConfigurationEntryData(int countOfDeletedSpaces, boolean isExistConfigurationEntryData) {
        if (isExistConfigurationEntryData) {
            verify(entryDao, atLeast(countOfDeletedSpaces)).removeAll(any());
            return;
        }
        verify(entryDao, never()).removeAll(any());
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
        when(fileService.deleteBySpace(anyString())).thenThrow(new FileStorageException(""));

        Exception exception = assertThrows(SLException.class, () -> dataTerminationService.deleteOrphanUserData());
        assertEquals(Messages.COULD_NOT_DELETE_SPACE_LEFTOVERS, exception.getMessage());
    }
}
