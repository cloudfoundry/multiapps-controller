package org.cloudfoundry.multiapps.controller.persistence.model.filters;

import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TargetWildcardFilterTest {

    private final TargetWildcardFilter filter = new TargetWildcardFilter();
    private final CloudTarget actual = new CloudTarget("my-org", "my-space");

    @Test
    void testNullRequestedTargetMatches() {
        Assertions.assertTrue(filter.test(actual, null));
    }

    @Test
    void testFullWildcardMatches() {
        Assertions.assertTrue(filter.test(actual, new CloudTarget("*", "*")));
    }

    @Test
    void testOrgWildcardMatchesWhenSpaceMatches() {
        Assertions.assertTrue(filter.test(actual, new CloudTarget("*", "my-space")));
    }

    @Test
    void testOrgWildcardFailsWhenSpaceDiffers() {
        Assertions.assertFalse(filter.test(actual, new CloudTarget("*", "other-space")));
    }

    @Test
    void testSpaceWildcardMatchesWhenOrgMatches() {
        Assertions.assertTrue(filter.test(actual, new CloudTarget("my-org", "*")));
    }

    @Test
    void testSpaceWildcardFailsWhenOrgDiffers() {
        Assertions.assertFalse(filter.test(actual, new CloudTarget("other-org", "*")));
    }

    @Test
    void testExactMatch() {
        Assertions.assertTrue(filter.test(actual, new CloudTarget("my-org", "my-space")));
    }

    @Test
    void testExactMismatch() {
        Assertions.assertFalse(filter.test(actual, new CloudTarget("other-org", "other-space")));
    }
}
