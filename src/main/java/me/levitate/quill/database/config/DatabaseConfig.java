package me.levitate.quill.database.config;

import lombok.Builder;
import lombok.Getter;
import me.levitate.quill.cache.config.RedisConfig;

@Builder
@Getter
public class DatabaseConfig {
    private final String host;
    private final Integer port;
    private final String database;
    private final String username;
    private final String password;
    private final Integer poolSize;
    private final Long connectionTimeout;
    private final String cacheType;
    private final RedisConfig redisConfig;
    private final String cacheName;

    public static DatabaseConfig defaultConfig() {
        return DatabaseConfig.builder().build();
    }

    public RedisConfig getRedisConfig() {
        return redisConfig != null ? redisConfig : RedisConfig.defaultConfig();
    }

    public String getCacheName() {
        return cacheName != null ? cacheName : "mysql_cache";
    }

    public String getHost() {
        return host != null ? host : "localhost";
    }

    public int getPort() {
        return port != null ? port : 3306;
    }

    public String getDatabase() {
        return database != null ? database : "minecraft";
    }

    public String getUsername() {
        return username != null ? username : "root";
    }

    public String getPassword() {
        return password != null ? password : "";
    }

    public int getPoolSize() {
        return poolSize != null ? poolSize : 10;
    }

    public long getConnectionTimeout() {
        return connectionTimeout != null ? connectionTimeout : 5000;
    }

    public String getCacheType() {
        return cacheType != null ? cacheType.toLowerCase() : "none";
    }
}