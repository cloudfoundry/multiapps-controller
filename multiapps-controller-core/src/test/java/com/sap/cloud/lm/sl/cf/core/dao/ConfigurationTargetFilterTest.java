package com.sap.cloud.lm.sl.cf.core.dao;

import static com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDtoDao.TARGET_WILDCARD_FILTER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;

public class ConfigurationTargetFilterTest {

    @Test
    public void test() {

        assertTrue(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space"), getCloudTarget("org", "space")));
        assertTrue(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org org", "space space s"), getCloudTarget("org org", "space space s")));
        assertTrue(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space"), getCloudTarget("*", "space")));
        assertTrue(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space"), getCloudTarget("org", "*")));
        assertTrue(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space"), getCloudTarget("*", "*")));
        assertTrue(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org org or", "space"), getCloudTarget("org org or", "*")));
        assertTrue(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space space"), getCloudTarget("*", "space space")));
        assertTrue(TARGET_WILDCARD_FILTER.apply(getCloudTarget("or*g *o", "*spac*e"), getCloudTarget("or*g *o", "*spac*e")));

        assertFalse(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space"), getCloudTarget("org", "diffspace")));
        assertFalse(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space"), getCloudTarget("difforg", "diffspace")));
        assertFalse(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space"), getCloudTarget("difforg", "space")));
        assertFalse(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space"), getCloudTarget("difforg", "space")));
        assertFalse(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space sp"), getCloudTarget("org", "space space")));
        assertFalse(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org org", "space"), getCloudTarget("org org ", "space")));
        assertFalse(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space"), getCloudTarget("difforg ", "*")));
        assertFalse(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space"), getCloudTarget("org o", "*")));
        assertFalse(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space"), getCloudTarget("*", "diffspace")));
        assertFalse(TARGET_WILDCARD_FILTER.apply(getCloudTarget("org", "space"), getCloudTarget("*", "space diff")));
    }

    private CloudTarget getCloudTarget(String org, String space) {
        return new CloudTarget(org, space);
    }
}
