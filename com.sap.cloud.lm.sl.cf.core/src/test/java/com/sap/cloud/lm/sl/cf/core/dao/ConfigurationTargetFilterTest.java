package com.sap.cloud.lm.sl.cf.core.dao;

import static com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDtoDao.TARGET_WILDCARD_FILTER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConfigurationTargetFilterTest {

    @Test
    public void test() {
        assertTrue(TARGET_WILDCARD_FILTER.apply("a b", "a b"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("a b", "* b"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("a b", "a *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("a b", "* *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply(". b", ". b"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("a .", "* *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("ala bala", "* *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("ala bala", "* *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("al a b ala", "* *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("al a b ala", "* *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("al*a bala", "al*a *"));// TODO
        assertTrue(TARGET_WILDCARD_FILTER.apply("ala bal*a", "ala *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("ala bal*a", "* bal*a")); // TODO
        assertTrue(TARGET_WILDCARD_FILTER.apply("ala bal*a", "ala bal*a"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("* ala", "* *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("* ala", "* ala"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("ala *", "ala *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("al a *", "al a *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("al a b ala", "al a b *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("al a b ala", "al a *"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("al a b ala", "* b ala"));
        assertTrue(TARGET_WILDCARD_FILTER.apply("al a b ala", "* a b ala"));
        assertFalse(TARGET_WILDCARD_FILTER.apply("a b", "a c"));
        assertFalse(TARGET_WILDCARD_FILTER.apply("a b", "c b"));
        assertFalse(TARGET_WILDCARD_FILTER.apply("a b", "c c"));
        assertFalse(TARGET_WILDCARD_FILTER.apply("a b", "* c"));
        assertFalse(TARGET_WILDCARD_FILTER.apply("a b", "c *"));
        assertFalse(TARGET_WILDCARD_FILTER.apply("ala bala", "a b"));
        assertFalse(TARGET_WILDCARD_FILTER.apply("a b", "ala bala"));
    }

}
