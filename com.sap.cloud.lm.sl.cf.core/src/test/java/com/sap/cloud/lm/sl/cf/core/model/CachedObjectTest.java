package com.sap.cloud.lm.sl.cf.core.model;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.Test;
import org.mockito.Mockito;

public class CachedObjectTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testGet() {
        Supplier<Long> currentTimeSupplier = Mockito.mock(Supplier.class);
        Mockito.when(currentTimeSupplier.get())
            .thenReturn(0L, toMillis(5), toMillis(15), toMillis(25), toMillis(30));

        Supplier<String> refreshFunction = Mockito.mock(Supplier.class);
        Mockito.when(refreshFunction.get())
            .thenReturn("a", "b");

        CachedObject<String> cachedName = new CachedObject<>(10, currentTimeSupplier);

        assertEquals("a", cachedName.get(refreshFunction));
        assertEquals("a", cachedName.get(refreshFunction));
        assertEquals("b", cachedName.get(refreshFunction));
        assertEquals("b", cachedName.get(refreshFunction));
        assertEquals("b", cachedName.get(refreshFunction));
    }

    private Long toMillis(int seconds) {
        return TimeUnit.SECONDS.toMillis(seconds);
    }

}
