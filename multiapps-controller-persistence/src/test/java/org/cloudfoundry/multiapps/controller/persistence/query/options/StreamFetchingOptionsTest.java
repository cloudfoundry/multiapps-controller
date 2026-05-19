package org.cloudfoundry.multiapps.controller.persistence.query.options;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StreamFetchingOptionsTest {

    @Test
    void testRecordExposesAccessors() {
        StreamFetchingOptions options = new StreamFetchingOptions(10L, 200L);

        Assertions.assertEquals(10L, options.startOffset());
        Assertions.assertEquals(200L, options.endOffset());
    }

    @Test
    void testRecordEqualsAndHashCodeFromComponents() {
        StreamFetchingOptions a = new StreamFetchingOptions(0L, 100L);
        StreamFetchingOptions b = new StreamFetchingOptions(0L, 100L);

        Assertions.assertEquals(a, b);
        Assertions.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void testRecordsWithDifferentOffsetsAreNotEqual() {
        StreamFetchingOptions a = new StreamFetchingOptions(0L, 100L);
        StreamFetchingOptions b = new StreamFetchingOptions(0L, 200L);

        Assertions.assertNotEquals(a, b);
    }
}
