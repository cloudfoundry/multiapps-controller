package org.cloudfoundry.multiapps.controller.core.model;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class CachedMapTest {

    @Test
    void testEviction() throws InterruptedException {
        CachedMap<String, String> map = new CachedMap<>(Duration.ofMillis(3), 10, TimeUnit.MILLISECONDS);
        map.put("test", "test");
        assertNotNull(map.get("test"));
        TimeUnit.MILLISECONDS.sleep(15);
        assertNull(map.get("test"));
        map.clear();
    }

}
