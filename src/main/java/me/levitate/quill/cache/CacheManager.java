package me.levitate.quill.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import me.levitate.quill.cache.config.RedisConfig;
import me.levitate.quill.cache.local.LocalCache;
import me.levitate.quill.cache.redis.CacheCodec;
import me.levitate.quill.cache.redis.RedisCache;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Module
public class CacheManager {
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    private RedisClient redisClient;
    private StatefulRedisConnection<String, byte[]> redisConnection;

    @Inject
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

        if (redisConnection != null) {
            redisConnection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }

    // Internal methods for Redis connection management
    synchronized RedisClient getRedisClient(RedisConfig config) {
        if (redisClient == null) {
            RedisURI redisUri = RedisURI.builder()
                    .withHost(config.getHost())
                    .withPort(config.getPort())
                    .withPassword(config.getPassword().toCharArray())
                    .withDatabase(config.getDatabase())
                    .build();
            redisClient = RedisClient.create(redisUri);
        }
        return redisClient;
    }

    public synchronized StatefulRedisConnection<String, byte[]> getRedisConnection(RedisConfig config) {
        if (redisConnection == null || !redisConnection.isOpen()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                RedisURI redisUri = RedisURI.builder()
                        .withHost(config.getHost())
                        .withPort(config.getPort())
                        .withPassword(config.getPassword().toCharArray())
                        .withDatabase(config.getDatabase())
                        .build();
                redisClient = RedisClient.create(redisUri);
                redisConnection = redisClient.connect(CacheCodec.INSTANCE);
            });
        }
        return redisConnection;
    }
}