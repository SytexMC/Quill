package me.levitate.quill.storage.config;

import lombok.Builder;
import lombok.Getter;
import me.levitate.quill.cache.config.RedisConfig;

@Builder
@Getter
public class StorageConfig {
    private final CacheType cacheType;
    private final RedisConfig redisConfig;
    private final boolean enableAsyncOperations;
    private final boolean autoSaveEnabled;
    private final long autoSaveInterval;

    public static StorageConfig defaultConfig() {
        return StorageConfig.builder()
                .cacheType(CacheType.LOCAL)
                .redisConfig(RedisConfig.defaultConfig())
                .enableAsyncOperations(true)
                .autoSaveEnabled(true)
                .autoSaveInterval(300) // 5 minutes
                .build();
    }

    public enum CacheType {
        LOCAL,
        REDIS
    }
}