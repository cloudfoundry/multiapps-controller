package org.cloudfoundry.multiapps.controller.core.model;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CachedMap<K, V> {

    private final Duration expirationTime;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Map<K, CachedObject<V>> cache = new ConcurrentHashMap<>();

    public CachedMap(Duration expirationTime) {
        this(expirationTime, 10, TimeUnit.MINUTES);
    }

    public CachedMap(Duration expirationTime, long evictionCheckPeriod, TimeUnit evictionCheckTimeUnit) {
        this.expirationTime = expirationTime;
        executor.scheduleAtFixedRate(this::clearStaleEntries, evictionCheckPeriod, evictionCheckPeriod, evictionCheckTimeUnit);
    }

    public V get(K key) {
        var value = cache.get(key);
        if (value != null) {
            if (!value.isExpired()) {
                return value.get();
            }
            cache.remove(key);
        }
        return null;
    }

    public V computeIfAbsent(K key, Supplier<V> creator) {
        V value = get(key);
        if (value != null) {
            return value;
        }
        value = creator.get();
        put(key, value);
        return value;
    }

    public void put(K key, V value) {
        long expirationTimestamp = System.currentTimeMillis() + expirationTime.toMillis();
        cache.put(key, new CachedObject<>(value, expirationTimestamp));
    }

    public void remove(K key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
        executor.shutdown();
    }

    private void clearStaleEntries() {
        long currentTime = System.currentTimeMillis();
        cache.values().removeIf(obj -> obj.isExpired(currentTime));
    }

}
