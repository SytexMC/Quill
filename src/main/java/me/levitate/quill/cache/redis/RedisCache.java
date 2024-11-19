package me.levitate.quill.cache.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.levitate.quill.cache.Cache;
import me.levitate.quill.cache.CacheManager;
import me.levitate.quill.cache.config.RedisConfig;
import me.levitate.quill.cache.local.LocalCache;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;

public class RedisCache<K, V> implements Cache<K, V> {
    private final RedisConfig redisConfig;
    private final CacheManager cacheManager;
    private final Map<K, V> localCache;
    private final ObjectMapper objectMapper;

    public RedisCache(RedisConfig redisConfig, CacheManager cacheManager) {
        this.redisConfig = redisConfig;
        this.cacheManager = cacheManager;
        this.localCache = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
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

        try (Jedis jedis = cacheManager.getJedisConnection(redisConfig)) {
            String value = jedis.get(getRedisKey(key));
            if (value != null) {
                V deserializedValue = objectMapper.readValue(value, objectMapper.constructType(LocalCache.class));
                localCache.put(key, deserializedValue);
                return Optional.of(deserializedValue);
            }
        } catch (Exception e) {
            cacheManager.getPlugin().getLogger().log(Level.WARNING, "Failed to get value from Redis", e);
        }
        return Optional.empty();
    }

    @Override
    public void put(K key, V value) {
        localCache.put(key, value);
        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = cacheManager.getJedisConnection(redisConfig)) {
                String serializedValue = objectMapper.writeValueAsString(value);
                jedis.set(getRedisKey(key), serializedValue);
            } catch (Exception e) {
                cacheManager.getPlugin().getLogger().log(Level.WARNING, "Failed to put value in Redis", e);
            }
        });
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
        try (Jedis jedis = cacheManager.getJedisConnection(redisConfig)) {
            return jedis.del(getRedisKey(key)) > 0;
        } catch (Exception e) {
            cacheManager.getPlugin().getLogger().log(Level.WARNING, "Failed to remove value from Redis", e);
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
        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = cacheManager.getJedisConnection(redisConfig)) {
                String pattern = redisConfig.getKeyPrefix() + "*";
                for (String key : jedis.keys(pattern)) {
                    jedis.del(key);
                }
            } catch (Exception e) {
                cacheManager.getPlugin().getLogger().log(Level.WARNING, "Failed to clear Redis cache", e);
            }
        });
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