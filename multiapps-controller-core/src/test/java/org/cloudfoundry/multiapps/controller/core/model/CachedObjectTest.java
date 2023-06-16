package org.cloudfoundry.multiapps.controller.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CachedObjectTest {

    @Test
    void testCaching() {
        @SuppressWarnings("unchecked")
        Supplier<String> refreshFunction = Mockito.mock(Supplier.class);
        Mockito.when(refreshFunction.get())
                .thenReturn("a", "b");

        CachedObject<String> cachedName = new CachedObject<>(Duration.ZERO);

        assertEquals("a", cachedName.getOrRefresh(refreshFunction));
        assertEquals("b", cachedName.getOrRefresh(refreshFunction));
        assertEquals("b", cachedName.get());
        assertTrue(cachedName.isExpired());
    }

    @Test
    void testExpiration() {
        CachedObject<String> cache = new CachedObject<>("a", 2);

        assertFalse(cache.isExpired(1));
        assertTrue(cache.isExpired(3));
    }
}
