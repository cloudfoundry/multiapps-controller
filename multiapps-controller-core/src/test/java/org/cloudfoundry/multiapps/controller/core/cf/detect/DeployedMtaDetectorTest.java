package org.cloudfoundry.multiapps.controller.core.cf.detect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor.MtaMetadataApplicationCollector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor.MtaMetadataEntityAggregator;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor.MtaMetadataEntityCollector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor.MtaMetadataServiceCollector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataValidator;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.fasterxml.jackson.core.type.TypeReference;

class DeployedMtaDetectorTest {

    private final Tester tester = Tester.forClass(getClass());

    private final MtaMetadataApplicationCollector appCollector = new MtaMetadataApplicationCollector();

    private final MtaMetadataValidator mtaMetadataValidator = new MtaMetadataValidator();

    @Spy
    private final MtaMetadataParser mtaMetadataParser = new MtaMetadataParser(mtaMetadataValidator);

    private final MtaMetadataServiceCollector serviceCollector = new MtaMetadataServiceCollector();

    @Spy
    private final List<MtaMetadataEntityCollector> collectors = new ArrayList<>();

    @Spy
    private final MtaMetadataEntityAggregator mtaMetadataEntityAggregator = new MtaMetadataEntityAggregator(mtaMetadataParser);

    @InjectMocks
    @Spy
    private DeployedMtaDetector deployedMtaDetector;

    @Mock
    private CloudControllerClient client;

    @BeforeEach
    void initMocks() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        collectors.clear();
        collectors.add(appCollector);
        collectors.add(serviceCollector);
    }

    static Stream<Arguments> testGetAllDeployedMtas() {
        return Stream.of(
                         // (1) No MTA applications:
                         Arguments.of("metadata/apps-01.json", null,
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-01.json")),
                         // (2) Applications without module in metadata:
                         Arguments.of("metadata/apps-02.json", null,
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "MTA metadata for entity \"mta-application-1\" is incomplete. This indicates that MTA reserved variables in the entitys metadata were modified manually. Either revert the changes or delete the entity.")),
                         // (3) Services without resource in metadata:
                         Arguments.of("metadata/apps-03.json", "metadata/services-03.json",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "MTA metadata for entity \"mta-service-1\" is incomplete. This indicates that MTA reserved variables in the entitys metadata were modified manually. Either revert the changes or delete the entity.")),
                         // (4) Two MTA applications:
                         Arguments.of("metadata/apps-04.json", null,
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-04.json")),
                         // (5) Applications from different versions of the same MTA:
                         Arguments.of("metadata/apps-05.json", null,
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-05.json")),
                         // (6) Applications from different versions of the same MTA (same modules):
                         Arguments.of("metadata/apps-06.json", null,
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-06.json")),
                         // (7) Two services with no applications
                         Arguments.of(null, "metadata/services-07.json",
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-07.json")),
                         // (8) Two services with same mta id and different version
                         Arguments.of(null, "metadata/services-08.json",
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-08.json")),
                         // (9) Two apps with one service each
                         Arguments.of("metadata/apps-09.json", "metadata/services-09.json",
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-09.json")),
                         // (10) Two apps with one service and one user provided service each
                         Arguments.of("metadata/apps-10.json", "metadata/services-10.json",
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-10.json")));
    }

    static Stream<Arguments> testGetAllDeployedMtasWithoutNamespace() {
        return testGetAllDeployedMtas();
    }

    static Stream<Arguments> testGetAllDeployedMtasByNamespace() {
        return Stream.of(
                         // (1) 3 applications, 2 in one mta and 1 in the other:
                         Arguments.of("namespace", "metadata/apps-11.json", "metadata/services-11.json",
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-11.json")),
                         // (2) Two apps with one service each, seraching by default namespace
                         Arguments.of(null, "metadata/apps-09.json", "metadata/services-09.json",
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-09.json")));
    }

    static Stream<Arguments> testGetAllDeployedMtasByName() {
        return Stream.of(
                         // (1) 3 applications, 2 in one mta and 1 in the other:
                         Arguments.of("quux", "metadata/apps-11.json", "metadata/services-11.json",
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-11.json")));
    }

    static Stream<Arguments> testGetAllDeployedMtaByNameAndNamespace() {
        return Stream.of(
                         // (1) 3 applications, 2 in one mta and 1 in the other:
                         Arguments.of("quux", "namespace", "metadata/apps-13.json", "metadata/services-13.json",
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-13.json")),
                         // (2) Two apps with one service each, seraching by default namespace
                         Arguments.of("quux", null, "metadata/apps-09.json", "metadata/services-09.json",
                                      new Expectation(Expectation.Type.JSON, "metadata/deployed-mtas-12.json")));
    }

    @ParameterizedTest
    @MethodSource
    void testGetAllDeployedMtas(String appsResourceLocation, String servicesResourceLocation, Expectation expectation) throws IOException {
        prepareMocks(appsResourceLocation, servicesResourceLocation);

        tester.test(() -> deployedMtaDetector.detectDeployedMtas(client), expectation);
    }

    @ParameterizedTest
    @MethodSource
    void testGetAllDeployedMtasWithoutNamespace(String appsResourceLocation, String servicesResourceLocation, Expectation expectation)
        throws IOException {
        prepareMocks(appsResourceLocation, servicesResourceLocation);

        tester.test(() -> deployedMtaDetector.detectDeployedMtasWithoutNamespace(client), expectation);
        verifyNamespaceWasChecked(null);
    }

    @ParameterizedTest
    @MethodSource
    void testGetAllDeployedMtasByNamespace(String namespace, String appsResourceLocation, String servicesResourceLocation,
                                           Expectation expectation)
        throws IOException {
        prepareMocks(appsResourceLocation, servicesResourceLocation);

        tester.test(() -> deployedMtaDetector.detectDeployedMtasByNamespace(namespace, client), expectation);
        verifyNamespaceWasChecked(namespace);
    }

    @ParameterizedTest
    @MethodSource
    void testGetAllDeployedMtasByName(String name, String appsResourceLocation, String servicesResourceLocation, Expectation expectation)
        throws IOException {
        prepareMocks(appsResourceLocation, servicesResourceLocation);

        tester.test(() -> deployedMtaDetector.detectDeployedMtasByName(name, client), expectation);
        verifyNameWasChecked(name);
    }

    @ParameterizedTest
    @MethodSource
    void testGetAllDeployedMtaByNameAndNamespace(String name, String namespace, String appsResourceLocation,
                                                 String servicesResourceLocation, Expectation expectation)
        throws IOException {
        prepareMocks(appsResourceLocation, servicesResourceLocation);

        tester.test(() -> deployedMtaDetector.detectDeployedMtaByNameAndNamespace(name, namespace, client)
                                             .get(),
                    expectation);
        verifyNameWasChecked(name);
        verifyNamespaceWasChecked(namespace);
    }

    private void verifyNamespaceWasChecked(String namespace) {
        if (namespace != null) {
            Mockito.verify(client)
                   .getApplicationsByMetadataLabelSelector(Mockito.contains("mta_namespace=" + MtaMetadataUtil.getHashedLabel(namespace)));
            Mockito.verify(client)
                   .getServiceInstancesByMetadataLabelSelector(Mockito.contains("mta_namespace="
                       + MtaMetadataUtil.getHashedLabel(namespace)));

        } else {
            Mockito.verify(client)
                   .getApplicationsByMetadataLabelSelector(Mockito.contains("!mta_namespace"));
            Mockito.verify(client)
                   .getServiceInstancesByMetadataLabelSelector(Mockito.contains("!mta_namespace"));
        }
    }

    private void verifyNameWasChecked(String name) {
        Mockito.verify(client)
               .getApplicationsByMetadataLabelSelector(Mockito.contains("mta_id=" + MtaMetadataUtil.getHashedLabel(name)));
        Mockito.verify(client)
               .getServiceInstancesByMetadataLabelSelector(Mockito.contains("mta_id=" + MtaMetadataUtil.getHashedLabel(name)));
    }

    private List<CloudApplication> parseApps(String appsResourceLocation) {
        if (appsResourceLocation == null) {
            return Collections.emptyList();
        }
        String appsJson = TestUtil.getResourceAsString(appsResourceLocation, getClass());
        List<TestCloudApplication> testApps = JsonUtil.fromJson(appsJson, new TypeReference<List<TestCloudApplication>>() {
        });
        return toCloudApplications(testApps);
    }

    private List<CloudServiceInstance> parseServices(String servicesResourceLocation) throws IOException {
        if (servicesResourceLocation == null) {
            return Collections.emptyList();
        }
        String appsJson = TestUtil.getResourceAsString(servicesResourceLocation, getClass());
        List<TestCloudService> testServices = JsonUtil.fromJson(appsJson, new TypeReference<List<TestCloudService>>() {
        });
        return toCloudServices(testServices);
    }

    private List<CloudApplication> toCloudApplications(List<TestCloudApplication> simpleApplications) {
        return simpleApplications.stream()
                                 .map(TestCloudApplication::toCloudApplication)
                                 .collect(Collectors.toList());
    }

    private List<CloudServiceInstance> toCloudServices(List<TestCloudService> simpleApplications) {
        return simpleApplications.stream()
                                 .map(TestCloudService::toCloudService)
                                 .collect(Collectors.toList());
    }

    private void prepareMocks(String appsResourceLocation, String servicesResourceLocation) throws IOException {
        List<CloudApplication> apps = parseApps(appsResourceLocation);
        List<CloudServiceInstance> services = parseServices(servicesResourceLocation);
        mockClientResults(apps, services);
    }

    private void mockClientResults(List<CloudApplication> apps, List<CloudServiceInstance> services) {
        Mockito.doReturn(apps)
               .when(client)
               .getApplicationsByMetadataLabelSelector(ArgumentMatchers.anyString());
        Mockito.doReturn(services)
               .when(client)
               .getServiceInstancesByMetadataLabelSelector(ArgumentMatchers.anyString());
    }

    private static class TestCloudApplication {
        private String name;
        private Map<String, Map<String, String>> metadata;

        private CloudApplication toCloudApplication() {
            final Map<String, String> annotations = metadata == null ? null : metadata.get("annotations");
            final Map<String, String> labels = metadata == null ? null : metadata.get("labels");
            return ImmutableCloudApplication.builder()
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .guid(NameUtil.getUUID(name))
                                                                            .build())
                                            .name(name)
                                            .v3Metadata(Metadata.builder()
                                                                .annotations(annotations)
                                                                .labels(labels)
                                                                .build())
                                            .build();
        }
    }

    private static class TestCloudService {
        private String name;
        private Map<String, Map<String, String>> metadata;

        private CloudServiceInstance toCloudService() {
            final Map<String, String> annotations = metadata == null ? null : metadata.get("annotations");
            final Map<String, String> labels = metadata == null ? null : metadata.get("labels");
            return ImmutableCloudServiceInstance.builder()
                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                .guid(NameUtil.getUUID(name))
                                                                                .build())
                                                .name(name)
                                                .v3Metadata(Metadata.builder()
                                                                    .annotations(annotations)
                                                                    .labels(labels)
                                                                    .build())
                                                .build();
        }
    }

}
