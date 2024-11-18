package me.levitate.quill.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public interface Cache<K, V> {
    /**
     * Get a value from cache
     * @param key The key
     * @return Optional containing value if present
     */
    Optional<V> get(K key);

    /**
     * Get a value from cache, computing it if absent
     * @param key The key
     * @param mappingFunction Function to compute value if absent
     * @return The value
     */
    V getOrCompute(K key, Function<K, V> mappingFunction);

    /**
     * Put a value in cache
     * @param key The key
     * @param value The value
     */
    void put(K key, V value);

    /**
     * Put all entries from map into cache
     * @param map The map of entries
     */
    void putAll(Map<K, V> map);

    /**
     * Remove a value from cache
     * @param key The key
     * @return true if value was removed
     */
    boolean remove(K key);

    /**
     * Remove multiple values from cache
     * @param keys Collection of keys to remove
     */
    void removeAll(Collection<K> keys);

    /**
     * Clear all entries from cache
     */
    void clear();

    /**
     * Get all keys in cache
     * @return Set of keys
     */
    Set<K> keys();

    /**
     * Get all values in cache
     * @return Collection of values
     */
    Collection<V> values();

    /**
     * Get cache size
     * @return Number of entries in cache
     */
    int size();

    /**
     * Check if cache contains key
     * @param key The key
     * @return true if key exists
     */
    boolean containsKey(K key);

    /**
     * Close cache and free resources
     */
    void close();
}