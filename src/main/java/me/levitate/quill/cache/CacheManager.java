package me.levitate.quill.cache;

import lombok.Getter;
import me.levitate.quill.cache.config.RedisConfig;
import me.levitate.quill.cache.local.LocalCache;
import me.levitate.quill.cache.redis.RedisCache;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Module
public class CacheManager {
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    private JedisPool jedisPool;

    @Inject
    @Getter
    private Plugin plugin;

    public <K, V> Cache<K, V> createLocalCache(String name) {
        return createAndRegisterCache(name, LocalCache::new);
    }

    public <K, V> Cache<K, V> createRedisCache(String name, RedisConfig redisConfig) {
        return createAndRegisterCache(name, () -> new RedisCache<>(redisConfig, this));
    }

    @SuppressWarnings("unchecked")
    private <K, V> Cache<K, V> createAndRegisterCache(String name, Supplier<Cache<K, V>> supplier) {
        return (Cache<K, V>) caches.computeIfAbsent(name, k -> supplier.get());
    }

    /**
     * Get existing cache by name
     */
    @SuppressWarnings("unchecked")
    public <K, V> Optional<Cache<K, V>> getCache(String name) {
        return Optional.ofNullable((Cache<K, V>) caches.get(name));
    }

    /**
     * Remove and close a cache
     */
    public void removeCache(String name) {
        Cache<?, ?> cache = caches.remove(name);
        if (cache != null) {
            cache.close();
        }
    }

    /**
     * Close all caches and connections
     */
    public void shutdown() {
        caches.values().forEach(Cache::close);
        caches.clear();

        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    public synchronized Jedis getJedisConnection(RedisConfig config) {
        if (jedisPool == null || jedisPool.isClosed()) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(8);
            poolConfig.setMaxIdle(8);
            poolConfig.setMinIdle(0);

            jedisPool = new JedisPool(poolConfig,
                    config.getHost(),
                    config.getPort(),
                    2000, // connectionTimeout
                    config.getPassword().isEmpty() ? null : config.getPassword(),
                    config.getDatabase());
        }
        return jedisPool.getResource();
    }
}