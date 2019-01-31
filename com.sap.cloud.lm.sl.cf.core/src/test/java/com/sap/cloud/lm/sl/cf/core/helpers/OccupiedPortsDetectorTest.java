package com.sap.cloud.lm.sl.cf.core.helpers;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class OccupiedPortsDetectorTest {

    private final String applicationJsonLocation;
    private final List<Integer> expectedPorts;

    private CloudApplication application;

    public OccupiedPortsDetectorTest(String applicationJsonLocation, List<Integer> expectedPorts) {
        this.applicationJsonLocation = applicationJsonLocation;
        this.expectedPorts = expectedPorts;
    }

    @Before
    public void setUp() throws Exception {
        String applicationJson = TestUtil.getResourceAsString(applicationJsonLocation, getClass());
        this.application = JsonUtil.fromJson(applicationJson, CloudApplication.class);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) All application URIs have ports:
            {
                "application-0.json", Arrays.asList(40001, 40002),
            },
            // (1) Some application URIs have ports:
            {
                "application-1.json", Collections.emptyList(),
            },
            // (2) The application contains a mix of port-based and host-based URIs:
            {
                "application-2.json", Arrays.asList(40001, 40002),
            },
            // (4) An application does not contain any URIs:
            {
                "application-3.json", Collections.emptyList(),
            },
            // (5) An application URI contains a backslash in the end:
            {
                "application-4.json", Arrays.asList(8080),
            },
            // (6) The application URIs are invalid:
            {
                "application-5.json", Collections.emptyList(),
            },
// @formatter:on
        });
    }

    @Test
    public void testGetOccupiedPorts() {
        Set<Integer> actualPorts = OccupiedPortsDetector.detectOccupiedPorts(application.getUris());
        assertEquals(expectedPorts.stream().collect(Collectors.toSet()), actualPorts);
    }

}
