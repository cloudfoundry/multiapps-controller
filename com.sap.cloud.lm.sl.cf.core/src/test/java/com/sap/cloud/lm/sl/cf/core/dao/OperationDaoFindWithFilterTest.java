package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class OperationDaoFindWithFilterTest extends AbstractOperationDaoParameterizedTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Filter based on end time upper bound:
            {
                "database-content-for-filter-test.json", "filter-with-end-time-upper-bound.json", "R:operations-filtered-based-on-end-time-upper-bound.json",
            },
            // (1) Filter based on end time lower bound:
            {
                "database-content-for-filter-test.json", "filter-with-end-time-lower-bound.json", "R:operations-filtered-based-on-end-time-lower-bound.json",
            },
            // (2) Filter based on end time upper and lower bounds:
            {
                "database-content-for-filter-test.json", "filter-with-end-time-upper-and-lower-bounds.json", "R:operations-filtered-based-on-end-time-upper-and-lower-bounds.json",
            },
            // (3) Filter based on space ID:
            {
                "database-content-for-filter-test.json", "filter-with-space-id.json", "R:operations-filtered-based-on-space-id.json",
            },
            // (4) Filter based on username:
            {
                "database-content-for-filter-test.json", "filter-with-user.json", "R:operations-filtered-based-on-user.json",
            },
            // (5) Filter based on MTA ID:
            {
                "database-content-for-filter-test.json", "filter-with-mta-id.json", "R:operations-filtered-based-on-mta-id.json",
            },
            // (6) Find final operations:
            {
                "database-content-for-filter-test.json", "filter-for-operations-in-final-state.json", "R:operations-in-final-state.json",
            },
            // (7) Find non final operations:
            {
                "database-content-for-filter-test.json", "filter-for-operations-in-non-final-state.json", "R:operations-in-non-final-state.json",
            },
// @formatter:on
        });
    }

    private final String filterJsonLocation;
    private final String expectedResult;

    private OperationFilter filter;

    public OperationDaoFindWithFilterTest(String databaseContentJsonLocation, String filterJsonLocation, String expectedResult) {
        super(databaseContentJsonLocation);
        this.filterJsonLocation = filterJsonLocation;
        this.expectedResult = expectedResult;
    }

    @Before
    public void loadFilter() throws Exception {
        String filterJson = TestUtil.getResourceAsString(filterJsonLocation, getClass());
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z['z']'").create();
        this.filter = gson.fromJson(filterJson, OperationFilter.class);
    }

    @Test
    public void testFind() {
        TestUtil.test(() -> dao.find(filter), expectedResult, getClass());
    }

}
