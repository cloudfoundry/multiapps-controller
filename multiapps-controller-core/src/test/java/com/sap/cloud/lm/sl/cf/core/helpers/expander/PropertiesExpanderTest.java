package com.sap.cloud.lm.sl.cf.core.helpers.expander;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.util.JsonUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;

@RunWith(Parameterized.class)
public class PropertiesExpanderTest {

    private String originalDependencyName;
    private String propertiesLocation;
    private List<String> newDependencyNames;
    private List<String> expandedProperties;
    private Expectation expectation;

    public PropertiesExpanderTest(String propertiesLocation, String originalDependencyName, List<String> newDependencyNames,
                                  Expectation expectation, List<String> expandedProperties) {
        this.newDependencyNames = newDependencyNames;
        this.propertiesLocation = propertiesLocation;
        this.originalDependencyName = originalDependencyName;
        this.expectation = expectation;
        this.expandedProperties = expandedProperties;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) 0 new dependency names:
            {
                "properties-00.json", "bar", generateNewDependencyNames("bar", 0),
                new Expectation(Expectation.Type.RESOURCE, "expanded-properties-00.json"),
                Arrays.asList("credentials_2", "credentials_4#user", "credentials_4#pass", "name_2", "url_2"),
            },
            // (2) 1 new dependency name:
            {
                "properties-00.json", "bar", generateNewDependencyNames("bar", 1),
                new Expectation(Expectation.Type.RESOURCE, "expanded-properties-01.json"),
                Arrays.asList("credentials_2", "credentials_4#user", "credentials_4#pass", "name_2", "url_2"),
            },
            // (1) 2 new dependency names:
            {
                "properties-00.json", "bar", generateNewDependencyNames("bar", 2),
                new Expectation(Expectation.Type.RESOURCE, "expanded-properties-02.json"),
                Arrays.asList("credentials_2", "credentials_4#user", "credentials_4#pass", "name_2", "url_2"),
            },
            // (1) 2 new dependency names (different than the original):
            {
                "properties-00.json", "bar", generateNewDependencyNames("qux", 2),
                new Expectation(Expectation.Type.RESOURCE, "expanded-properties-03.json"),
                Arrays.asList("credentials_2", "credentials_4#user", "credentials_4#pass", "name_2", "url_2"),
            },
// @formatter:on
        });
    }

    private static List<String> generateNewDependencyNames(String baseDependencyName, int newDependenciesCnt) {
        List<String> result = new ArrayList<>(newDependenciesCnt);
        for (int i = 0; i < newDependenciesCnt; i++) {
            result.add(String.format("%s.%s", baseDependencyName, i));
        }
        return result;
    }

    @Test
    public void testExpand() throws Exception {
        Map<String, Object> properties = JsonUtil.convertJsonToMap(TestUtil.getResourceAsString(propertiesLocation, getClass()));

        PropertiesExpander expander = new PropertiesExpander(originalDependencyName, newDependencyNames);

        TestUtil.test(() -> expander.expand(properties), expectation, getClass());
        assertEquals(expandedProperties, expander.getExpandedProperties());
    }

}
