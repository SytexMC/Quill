package me.levitate.quill.cache.local;

import lombok.Getter;
import me.levitate.quill.cache.Cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class LocalCache<K, V> implements Cache<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<V> get(K key) {
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public V getOrCompute(K key, Function<K, V> mappingFunction) {
        return get(key).orElseGet(() -> {
            V value = mappingFunction.apply(key);
            put(key, value);
            return value;
        });
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void putAll(Map<K, V> map) {
        cache.putAll(map);
    }

    @Override
    public boolean remove(K key) {
        return cache.remove(key) != null;
    }

    @Override
    public void removeAll(Collection<K> keys) {
        keys.forEach(cache::remove);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public Set<K> keys() {
        return new HashSet<>(cache.keySet());
    }

    @Override
    public Collection<V> values() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    @Override
    public void close() {
        clear();
    }

    @Override
    public Map<K, V> getMap() {
        return this.cache;
    }
}