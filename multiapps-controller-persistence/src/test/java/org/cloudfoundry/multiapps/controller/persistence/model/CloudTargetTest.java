package org.cloudfoundry.multiapps.controller.persistence.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CloudTargetTest {

    private static final String ORG = "my-org";
    private static final String SPACE = "my-space";

    @Test
    void testNoArgConstructorLeavesFieldsNull() {
        CloudTarget target = new CloudTarget();

        Assertions.assertNull(target.getOrganizationName());
        Assertions.assertNull(target.getSpaceName());
    }

    @Test
    void testTwoArgConstructorSetsFields() {
        CloudTarget target = new CloudTarget(ORG, SPACE);

        Assertions.assertEquals(ORG, target.getOrganizationName());
        Assertions.assertEquals(SPACE, target.getSpaceName());
    }

    @Test
    void testSetters() {
        CloudTarget target = new CloudTarget();

        target.setOrganizationName(ORG);
        target.setSpaceName(SPACE);

        Assertions.assertEquals(ORG, target.getOrganizationName());
        Assertions.assertEquals(SPACE, target.getSpaceName());
    }

    @Test
    void testEqualsAndHashCodeForEquivalentValues() {
        CloudTarget a = new CloudTarget(ORG, SPACE);
        CloudTarget b = new CloudTarget(ORG, SPACE);

        Assertions.assertEquals(a, b);
        Assertions.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void testEqualsReturnsTrueForSameInstance() {
        CloudTarget target = new CloudTarget(ORG, SPACE);

        Assertions.assertEquals(target, target);
    }

    @Test
    void testEqualsReturnsFalseForDifferentOrg() {
        CloudTarget a = new CloudTarget(ORG, SPACE);
        CloudTarget b = new CloudTarget("other-org", SPACE);

        Assertions.assertNotEquals(a, b);
    }

    @Test
    void testEqualsReturnsFalseForDifferentSpace() {
        CloudTarget a = new CloudTarget(ORG, SPACE);
        CloudTarget b = new CloudTarget(ORG, "other-space");

        Assertions.assertNotEquals(a, b);
    }

    @Test
    void testEqualsReturnsFalseForNull() {
        CloudTarget target = new CloudTarget(ORG, SPACE);

        Assertions.assertNotEquals(target, null);
    }

    @Test
    void testEqualsReturnsFalseForOtherType() {
        CloudTarget target = new CloudTarget(ORG, SPACE);

        Assertions.assertNotEquals(target, "not-a-cloud-target");
    }

    @Test
    void testToStringIncludesBothFields() {
        CloudTarget target = new CloudTarget(ORG, SPACE);

        String result = target.toString();

        Assertions.assertTrue(result.contains(ORG));
        Assertions.assertTrue(result.contains(SPACE));
    }
}
