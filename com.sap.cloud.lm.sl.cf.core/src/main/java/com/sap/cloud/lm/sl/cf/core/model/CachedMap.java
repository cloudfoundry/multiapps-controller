package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Collections;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;

public class CachedMap<K, V> {

    private final long expirationTimeInSeconds;
    private final LongSupplier currentTimeSupplier = System::currentTimeMillis;

    private final Map<K, CachedObject<V>> referenceMap = Collections.synchronizedMap(new ReferenceMap<>(ReferenceStrength.HARD,
                                                                                                  ReferenceStrength.SOFT));

    public CachedMap(long expirationTimeInSeconds) {
        this.expirationTimeInSeconds = expirationTimeInSeconds;
    }

    public synchronized V get(K key, Supplier<V> refreshFunction) {
        CachedObject<V> value = referenceMap.computeIfAbsent(key, k -> new CachedObject<>(expirationTimeInSeconds, currentTimeSupplier));
        return value.get(refreshFunction);
    }

    public synchronized V forceRefresh(K key, Supplier<V> refreshFunction) {
        CachedObject<V> value = referenceMap.computeIfAbsent(key, k -> new CachedObject<>(expirationTimeInSeconds, currentTimeSupplier));
        return value.forceRefresh(refreshFunction);
    }

}
