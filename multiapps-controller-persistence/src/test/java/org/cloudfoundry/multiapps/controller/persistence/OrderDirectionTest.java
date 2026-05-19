package org.cloudfoundry.multiapps.controller.persistence;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OrderDirectionTest {

    @Test
    void testValuesContainsBothDirections() {
        OrderDirection[] values = OrderDirection.values();

        Assertions.assertEquals(2, values.length);
        Assertions.assertEquals(OrderDirection.ASCENDING, values[0]);
        Assertions.assertEquals(OrderDirection.DESCENDING, values[1]);
    }

    @Test
    void testValueOfRoundTrip() {
        Assertions.assertEquals(OrderDirection.ASCENDING, OrderDirection.valueOf("ASCENDING"));
        Assertions.assertEquals(OrderDirection.DESCENDING, OrderDirection.valueOf("DESCENDING"));
    }
}
