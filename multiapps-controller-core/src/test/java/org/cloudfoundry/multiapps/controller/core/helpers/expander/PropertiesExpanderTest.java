package org.cloudfoundry.multiapps.controller.core.helpers.expander;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PropertiesExpanderTest {

    private final Tester tester = Tester.forClass(getClass());

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) 0 new dependency names:
            {
                "properties-00.json", "bar", generateNewDependencyNames("bar", 0),
                new Expectation(Expectation.Type.JSON, "expanded-properties-00.json"),
                Arrays.asList("credentials_2", "credentials_4#user", "credentials_4#pass", "name_2", "url_2"),
            },
            // (2) 1 new dependency name:
            {
                "properties-00.json", "bar", generateNewDependencyNames("bar", 1),
                new Expectation(Expectation.Type.JSON, "expanded-properties-01.json"),
                Arrays.asList("credentials_2", "credentials_4#user", "credentials_4#pass", "name_2", "url_2"),
            },
            // (1) 2 new dependency names:
            {
                "properties-00.json", "bar", generateNewDependencyNames("bar", 2),
                new Expectation(Expectation.Type.JSON, "expanded-properties-02.json"),
                Arrays.asList("credentials_2", "credentials_4#user", "credentials_4#pass", "name_2", "url_2"),
            },
            // (1) 2 new dependency names (different than the original):
            {
                "properties-00.json", "bar", generateNewDependencyNames("qux", 2),
                new Expectation(Expectation.Type.JSON, "expanded-properties-03.json"),
                Arrays.asList("credentials_2", "credentials_4#user", "credentials_4#pass", "name_2", "url_2"),
            },
// @formatter:on
        });
    }

    private final String originalDependencyName;
    private final String propertiesLocation;
    private final List<String> newDependencyNames;
    private final List<String> expandedProperties;

    private final Expectation expectation;

    public PropertiesExpanderTest(String propertiesLocation, String originalDependencyName, List<String> newDependencyNames,
                                  Expectation expectation, List<String> expandedProperties) {
        this.newDependencyNames = newDependencyNames;
        this.propertiesLocation = propertiesLocation;
        this.originalDependencyName = originalDependencyName;
        this.expectation = expectation;
        this.expandedProperties = expandedProperties;
    }

    private static List<String> generateNewDependencyNames(String baseDependencyName, int newDependenciesCnt) {
        List<String> result = new ArrayList<>(newDependenciesCnt);
        for (int i = 0; i < newDependenciesCnt; i++) {
            result.add(String.format("%s.%s", baseDependencyName, i));
        }
        return result;
    }

    @Test
    public void testExpand() {
        Map<String, Object> properties = JsonUtil.convertJsonToMap(TestUtil.getResourceAsString(propertiesLocation, getClass()));

        PropertiesExpander expander = new PropertiesExpander(originalDependencyName, newDependencyNames);

        tester.test(() -> expander.expand(properties), expectation);
        assertEquals(expandedProperties, expander.getExpandedProperties());
    }

}
