package me.levitate.quill.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.levitate.quill.cache.Cache;
import me.levitate.quill.cache.CacheManager;
import me.levitate.quill.database.config.DatabaseConfig;
import me.levitate.quill.database.query.QueryBuilder;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

@Module
public class DatabaseManager implements AutoCloseable {
    private HikariDataSource dataSource;
    private Cache<String, Object> cache;
    private boolean isConnected;
    private DatabaseConfig config;

    @Inject
    private Plugin plugin;

    @Inject
    private CacheManager cacheManager;

    public void connect(DatabaseConfig config) {
        if (isConnected) {
            throw new IllegalStateException("Database is already connected");
        }

        this.config = config;
        setupCache();
        setupDatabase();
    }

    private void setupCache() {
        String cacheType = config.getCacheType();

        if (cacheType.equals("redis")) {
            cache = cacheManager.createRedisCache(config.getCacheName(), config.getRedisConfig());
        } else {
            cache = cacheManager.createLocalCache(config.getCacheName());
        }
    }

    private void setupDatabase() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s",
                config.getHost(), config.getPort(), config.getDatabase()));
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setMaximumPoolSize(config.getPoolSize());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());

        try {
            dataSource = new HikariDataSource(hikariConfig);
            isConnected = true;
            plugin.getLogger().info("Successfully connected to database");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database", e);
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    public QueryBuilder createQuery() {
        return new QueryBuilder(this, cache);
    }

    public Connection getConnection() throws SQLException {
        if (!isConnected) {
            throw new IllegalStateException("Database is not connected");
        }
        return dataSource.getConnection();
    }

    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        isConnected = false;
    }

    public void clearCache() {
        if (cache != null) {
            cache.clear();
        }
    }

    public void invalidateCache(String key) {
        if (cache != null) {
            cache.remove(key);
        }
    }
}