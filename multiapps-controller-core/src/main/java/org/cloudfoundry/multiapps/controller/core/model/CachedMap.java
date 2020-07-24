package org.cloudfoundry.multiapps.controller.core.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;

public class CachedMap<K, V> {

    private final long expirationTimeInSeconds;
    private final LongSupplier currentTimeSupplier = System::currentTimeMillis;

    private final Map<K, CachedObject<V>> referenceMap = new ConcurrentHashMap<>(new ReferenceMap<>(ReferenceStrength.HARD,
                                                                                                    ReferenceStrength.SOFT));

    public CachedMap(long expirationTimeInSeconds) {
        this.expirationTimeInSeconds = expirationTimeInSeconds;
    }

    public V get(K key, Supplier<V> refreshFunction) {
        CachedObject<V> value = referenceMap.computeIfAbsent(key, k -> new CachedObject<>(expirationTimeInSeconds, currentTimeSupplier));
        return value.get(refreshFunction);
    }

    public V forceRefresh(K key, Supplier<V> refreshFunction) {
        CachedObject<V> value = referenceMap.computeIfAbsent(key, k -> new CachedObject<>(expirationTimeInSeconds, currentTimeSupplier));
        return value.forceRefresh(refreshFunction);
    }

}
