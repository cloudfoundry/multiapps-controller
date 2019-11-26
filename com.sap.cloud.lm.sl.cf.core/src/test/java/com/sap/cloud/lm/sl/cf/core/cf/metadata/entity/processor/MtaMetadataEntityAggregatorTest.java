package com.sap.cloud.lm.sl.cf.core.cf.metadata.entity.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataValidator;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;

public class MtaMetadataEntityAggregatorTest {
    private final Tester tester = Tester.forClass(getClass());
    private List<CloudEntity> inputEntities;

    @Mock
    private MtaMetadataValidator mockMtaMetadataValidator;

    private MtaMetadataParser testMtaMetadataParser;
    private MtaMetadataEntityAggregator testMtaMetadataAggregator;

    public static Stream<Arguments> testAggregate() {
        return Stream.of(
        // @formatter:off
        // (1) 2 mtas in CloudEntities - first has 2 apps and 2 services (with older versions), second had only 1 app
        Arguments.of("input-apps-01.json", "input-services-01.json", new Expectation(Expectation.Type.JSON, "aggregated-deployed-mtas-01.json")),
        // (2) 3 versions of the same mta (2 apps and 1 service) - two with namespaces, the last one without
        Arguments.of("input-apps-02.json", "input-services-02.json", new Expectation(Expectation.Type.JSON, "aggregated-deployed-mtas-02.json"))
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testAggregate(String inputAppsLocation, String inputServicesLocation, Expectation expectation) {
        parseInput(inputAppsLocation, inputServicesLocation);
        initMocks();

        tester.test(() -> testMtaMetadataAggregator.aggregate(inputEntities), expectation);
    }

    private void initMocks() {
        MockitoAnnotations.initMocks(this);

        testMtaMetadataParser = new MtaMetadataParser(mockMtaMetadataValidator);
        testMtaMetadataAggregator = new MtaMetadataEntityAggregator(testMtaMetadataParser);
    }

    private void parseInput(String inputAppsLocation, String inputServicesLocation) {
        this.inputEntities = new ArrayList<CloudEntity>();
        String inputJson;

        if (inputAppsLocation != null) {
            inputJson = TestUtil.getResourceAsString(inputAppsLocation, getClass());
            this.inputEntities.addAll(JsonUtil.fromJson(inputJson, new TypeReference<List<CloudApplication>>() {
            }));
        }

        if (inputServicesLocation != null) {
            inputJson = TestUtil.getResourceAsString(inputServicesLocation, getClass());
            this.inputEntities.addAll(JsonUtil.fromJson(inputJson, new TypeReference<List<CloudServiceInstance>>() {
            }));
        }
    }
}
