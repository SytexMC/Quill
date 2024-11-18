package me.levitate.quill.cache.config;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RedisConfig {
    private final String host;
    private final int port;
    private final String password;
    private final int database;
    private final String keyPrefix;

    public static RedisConfig defaultConfig() {
        return RedisConfig.builder()
                .host("localhost")
                .port(6379)
                .password("")
                .database(0)
                .keyPrefix("quill:")
                .build();
    }
}