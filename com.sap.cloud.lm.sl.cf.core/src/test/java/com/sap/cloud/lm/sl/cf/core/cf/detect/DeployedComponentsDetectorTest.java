package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.ApplicationMetadataFieldExtractor;
import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.ServiceMetadataFieldExtractor;
import com.sap.cloud.lm.sl.cf.core.cf.detect.process.AppMetadataCollector;
import com.sap.cloud.lm.sl.cf.core.cf.detect.process.MtaMetadataExtractorFactoryImpl;
import com.sap.cloud.lm.sl.cf.core.cf.detect.process.ServiceMetadataCollector;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudService;
import org.cloudfoundry.client.v3.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.helpers.MapToEnvironmentConverter;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class DeployedComponentsDetectorTest {

    private final Tester tester = Tester.forClass(getClass());

    @Spy
    private ApplicationMetadataFieldExtractor applicationMetadataFieldExtractor = new ApplicationMetadataFieldExtractor();

    @Spy
    @InjectMocks
    private AppMetadataCollector appCollector = new AppMetadataCollector();

    @Spy
    private ServiceMetadataFieldExtractor serviceMetadataFieldExtractor = new ServiceMetadataFieldExtractor();

    @Spy
    @InjectMocks
    private ServiceMetadataCollector serviceCollector = new ServiceMetadataCollector();

    @Spy
    private List<MtaMetadataCollector> collectors = new ArrayList<>();

    @Spy
    private MtaMetadataExtractorFactory metadataExtractorFactory = new MtaMetadataExtractorFactoryImpl();

    @Spy
    @InjectMocks
    private MtaMetadataEntityAggregator mtaMetadataEntityAggregator = new MtaMetadataEntityAggregator();

    @InjectMocks
    private DeployedComponentsDetector detector = new DeployedComponentsDetector();

    @Mock
    private CloudControllerClient client;

    @BeforeEach
    private void initMocks() {
        MockitoAnnotations.initMocks(this);
        collectors.clear();
        collectors.add(appCollector);
        collectors.add(serviceCollector);
    }

    public static Stream<Arguments> testDetectAllApplications() {
        return Stream.of(
// @formatter:off
            // (1) No MTA applications:
            Arguments.of("metadata/apps-01.json", null, new Expectation(Expectation.Type.JSON, "metadata/deployed-components-01.json")),
            // (2) No metadata field for found apps:
            Arguments.of("metadata/apps-02.json", null, new Expectation(Expectation.Type.EXCEPTION, "Cannot parse MTA metadata for application \"mta-application-2\". This indicates that MTA reserved variables in the application's metadata were modified manually. Either revert the changes or delete the application.")),
            // (3) Applications with empty metadata:
            Arguments.of("metadata/apps-03.json", null, new Expectation(Expectation.Type.EXCEPTION, "Cannot parse MTA metadata for application \"mta-application-2\". This indicates that MTA reserved variables in the application's metadata were modified manually. Either revert the changes or delete the application.")),
            // (4) Applications without module in metadata:
            Arguments.of("metadata/apps-04.json", null, new Expectation(Expectation.Type.EXCEPTION, "Cannot parse MTA metadata for application \"mta-application-1\". This indicates that MTA reserved variables in the application's metadata were modified manually. Either revert the changes or delete the application.")),
            // (5) No metadata field for services:
            Arguments.of("metadata/apps-05.json", "metadata/services-05.json", new Expectation(Expectation.Type.EXCEPTION, "Cannot parse MTA metadata for service \"mta-service-1\". This indicates that MTA reserved variables in the services's metadata were modified manually. Either revert the changes or delete the service.")),
            // (6) Services without resource in metadata:
            Arguments.of("metadata/apps-06.json", "metadata/services-06.json", new Expectation(Expectation.Type.EXCEPTION, "Cannot parse MTA metadata for service \"mta-service-1\". This indicates that MTA reserved variables in the services's metadata were modified manually. Either revert the changes or delete the service.")),
            // (7) Two MTA applications:
            Arguments.of("metadata/apps-07.json", null, new Expectation(Expectation.Type.JSON, "metadata/deployed-components-07.json")),
            // (8) Applications from different versions of the same MTA:
            Arguments.of("metadata/apps-08.json", null, new Expectation(Expectation.Type.JSON, "metadata/deployed-components-08.json")),
            // (9) Applications from different versions of the same MTA (same modules):
            Arguments.of("metadata/apps-09.json", null, new Expectation(Expectation.Type.JSON, "metadata/deployed-components-09.json")),
            // (10) Two services with no applications
            Arguments.of(null, "metadata/services-10.json", new Expectation(Expectation.Type.JSON, "metadata/deployed-components-10.json")),
            // (10) Two services with same mta id and different version
            Arguments.of(null, "metadata/services-11.json", new Expectation(Expectation.Type.JSON, "metadata/deployed-components-11.json")),
            // (10) Two apps with one service each
            Arguments.of("metadata/apps-12.json", "metadata/services-12.json", new Expectation(Expectation.Type.JSON, "metadata/deployed-components-12.json")),
            // (10) Two apps with one service and one user provided service each
            Arguments.of("metadata/apps-13.json", "metadata/services-13.json", new Expectation(Expectation.Type.JSON, "metadata/deployed-components-13.json"))
            // @formatter:on
        );
    }


    @ParameterizedTest
    @MethodSource
    public void testDetectAllApplications(String appsResourceLocation, String servicesResourceLocation, Expectation expectation) throws IOException {
        List<CloudApplication> apps = parseApps(appsResourceLocation);
        List<CloudService> services = parseServices(servicesResourceLocation);
        prepareClient(apps, services);
        tester.test(() -> detector.getAllDeployedMta(client), expectation);
    }

    private List<CloudApplication> parseApps(String appsResourceLocation) throws IOException {
        if(appsResourceLocation == null) {
            return Collections.emptyList();
        }
        String appsJson = TestUtil.getResourceAsString(appsResourceLocation, getClass());
        List<TestCloudApplication> testApps = JsonUtil.fromJson(appsJson, new TypeReference<List<TestCloudApplication>>() {
        });
        return toCloudApplications(testApps);
    }

    private List<CloudService> parseServices(String servicesResourceLocation) throws IOException {
        if(servicesResourceLocation == null) {
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

    private List<CloudService> toCloudServices(List<TestCloudService> simpleApplications) {
        return simpleApplications.stream()
                .map(TestCloudService::toCloudService)
                .collect(Collectors.toList());
    }

    private void prepareClient(List<CloudApplication> apps, List<CloudService> services) {
        Mockito.doReturn(apps)
               .when(client)
               .getApplicationsByMetadata(Matchers.anyString());
        Mockito.doReturn(services)
                .when(client)
                .getServicesByMetadata(Matchers.anyString());
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
                                            .v3Metadata(Metadata.builder().annotations(annotations).labels(labels).build())
                                            .build();
        }
    }

    private static class TestCloudService {
        private String name;
        private Map<String, Map<String, String>> metadata;

        private CloudService toCloudService() {
            final Map<String, String> annotations = metadata == null ? null : metadata.get("annotations");
            final Map<String, String> labels = metadata == null ? null : metadata.get("labels");
            return ImmutableCloudService.builder()
                    .metadata(ImmutableCloudMetadata.builder()
                            .guid(NameUtil.getUUID(name))
                            .build())
                    .name(name)
                    .v3Metadata(Metadata.builder().annotations(annotations).labels(labels).build())
                    .build();
        }
    }
}
