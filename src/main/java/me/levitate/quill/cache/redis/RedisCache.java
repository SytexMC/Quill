package me.levitate.quill.cache.redis;

import io.lettuce.core.api.async.RedisAsyncCommands;
import me.levitate.quill.cache.Cache;
import me.levitate.quill.cache.CacheManager;
import me.levitate.quill.cache.config.RedisConfig;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;

public class RedisCache<K, V> implements Cache<K, V> {
    private final RedisConfig redisConfig;
    private final CacheManager cacheManager;
    private final Map<K, V> localCache;

    public RedisCache(RedisConfig redisConfig, CacheManager cacheManager) {
        this.redisConfig = redisConfig;
        this.cacheManager = cacheManager;
        this.localCache = new ConcurrentHashMap<>();
    }

    private RedisAsyncCommands<String, byte[]> getRedisCommands() {
        return cacheManager.getRedisConnection(redisConfig).async();
    }

    private String getRedisKey(K key) {
        return redisConfig.getKeyPrefix() + key.toString();
    }

    @Override
    public Optional<V> get(K key) {
        V localValue = localCache.get(key);
        if (localValue != null) {
            return Optional.of(localValue);
        }

        try {
            byte[] value = getRedisCommands().get(getRedisKey(key))
                    .get(5, TimeUnit.SECONDS);
            if (value != null) {
                V deserializedValue = CacheCodec.deserialize(value);
                localCache.put(key, deserializedValue);
                return Optional.of(deserializedValue);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[Quill] Failed to get value from Redis", e);
        }

        return Optional.empty();
    }

    @Override
    public void put(K key, V value) {
        localCache.put(key, value);
        try {
            byte[] serializedValue = CacheCodec.serialize(value);
            getRedisCommands().set(getRedisKey(key), serializedValue);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[Quill] Failed to put value in Redis", e);
        }
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
    public void putAll(Map<K, V> map) {
        map.forEach(this::put);
    }

    @Override
    public boolean remove(K key) {
        localCache.remove(key);
        try {
            return getRedisCommands().del(getRedisKey(key))
                    .get(5, TimeUnit.SECONDS) > 0;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[Quill] Failed to remove value from Redis", e);
            return false;
        }
    }

    @Override
    public void removeAll(Collection<K> keys) {
        keys.forEach(this::remove);
    }

    @Override
    public void clear() {
        localCache.clear();
        try {
            String pattern = redisConfig.getKeyPrefix() + "*";
            List<String> keys = getRedisCommands().keys(pattern)
                    .get(5, TimeUnit.SECONDS);
            if (keys != null && !keys.isEmpty()) {
                getRedisCommands().del(keys.toArray(new String[0]))
                        .get(5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[Quill] Failed to clear Redis cache", e);
        }
    }

    @Override
    public Set<K> keys() {
        return new HashSet<>(localCache.keySet());
    }

    @Override
    public Collection<V> values() {
        return new ArrayList<>(localCache.values());
    }

    @Override
    public int size() {
        return localCache.size();
    }

    @Override
    public boolean containsKey(K key) {
        return get(key).isPresent();
    }

    @Override
    public void close() {
        clear();
    }
}