package org.cloudfoundry.multiapps.controller.core.helpers.expander;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PropertiesExpanderTest {

    private final Tester tester = Tester.forClass(getClass());

    public static Stream<Arguments> testExpand() {
        return Stream.of(
// @formatter:off
            // (0) 0 new dependency names:
            Arguments.of("properties-00.json", "bar", generateNewDependencyNames("bar", 0),
                    new Expectation(Expectation.Type.JSON, "expanded-properties-00.json"),
                    List.of("credentials_2", "credentials_4#user", "credentials_4#pass", "name_2", "url_2")),
            // (2) 1 new dependency name:
            Arguments.of("properties-00.json", "bar", generateNewDependencyNames("bar", 1),
                    new Expectation(Expectation.Type.JSON, "expanded-properties-01.json"),
                    List.of("credentials_2", "credentials_4#user", "credentials_4#pass", "name_2", "url_2")),
            // (1) 2 new dependency names:
            Arguments.of("properties-00.json", "bar", generateNewDependencyNames("bar", 2),
                    new Expectation(Expectation.Type.JSON, "expanded-properties-02.json"),
                    List.of("credentials_2", "credentials_4#user", "credentials_4#pass", "name_2", "url_2")),
            // (1) 2 new dependency names (different than the original):
            Arguments.of("properties-00.json", "bar", generateNewDependencyNames("qux", 2),
                    new Expectation(Expectation.Type.JSON, "expanded-properties-03.json"),
                    List.of("credentials_2", "credentials_4#user", "credentials_4#pass", "name_2", "url_2"))
// @formatter:on
        );
    }

    private static List<String> generateNewDependencyNames(String baseDependencyName, int newDependenciesCnt) {
        List<String> result = new ArrayList<>(newDependenciesCnt);
        for (int i = 0; i < newDependenciesCnt; i++) {
            result.add(String.format("%s.%s", baseDependencyName, i));
        }
        return result;
    }

    @ParameterizedTest
    @MethodSource
    void testExpand(String propertiesLocation, String originalDependencyName, List<String> newDependencyNames, Expectation expectation,
                    List<String> expandedProperties) {
        Map<String, Object> properties = JsonUtil.convertJsonToMap(TestUtil.getResourceAsString(propertiesLocation, getClass()));

        PropertiesExpander expander = new PropertiesExpander(originalDependencyName, newDependencyNames);

        tester.test(() -> expander.expand(properties), expectation);
        assertEquals(expandedProperties, expander.getExpandedProperties());
    }

}
