package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class OccupiedPortsDetectorTest {

    private final String applicationsJsonLocation;
    private final String expected;

    private List<CloudApplication> applications;

    public OccupiedPortsDetectorTest(String applicationsJsonLocation, String expected) {
        this.applicationsJsonLocation = applicationsJsonLocation;
        this.expected = expected;
    }

    @Before
    public void setUp() throws Exception {
        applications = JsonUtil.convertJsonToList(getClass().getResourceAsStream(applicationsJsonLocation),
            new TypeToken<List<CloudApplication>>() {
            }.getType());
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) All application uris have ports:
            {
                "applications-01.json", "R:occupied-ports-01.json",
            },
            // (1) Some application uris have ports:
            {
                "applications-02.json", "R:occupied-ports-02.json",
            },
            // (2) No application uris have ports:
            {
                "applications-03.json", "R:occupied-ports-03.json",
            },
            // (3) An application contains a mix of port-based and host-based uris:
            {
                "applications-04.json", "R:occupied-ports-04.json",
            },
            // (4) An application does not contain any uris:
            {
                "applications-05.json", "R:occupied-ports-05.json",
            },
//            // (5) An application uri contains a backslash in the end:
//            {
//                "applications-06.json", "R:occupied-ports-06.json",
//            },
            // (6) An application uri is invalid:
            {
                "applications-07.json", "R:occupied-ports-07.json",
            },
// @formatter:on
        });
    }

    @Test
    public void testGetOccupiedPorts() {
        TestUtil.test(() -> {

            return new OccupiedPortsDetector().detectOccupiedPorts(applications);

        } , expected, getClass());
    }

}
