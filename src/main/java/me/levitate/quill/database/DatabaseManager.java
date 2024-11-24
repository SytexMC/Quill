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
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

@Module
public class DatabaseManager implements AutoCloseable {
    private static final String MYSQL_DRIVER = "me.levitate.mysql.cj.jdbc.Driver";

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

        try {
            // Load the MySQL driver explicitly
            Class.forName(MYSQL_DRIVER);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load MySQL driver", e);
            throw new RuntimeException("MySQL driver not found", e);
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
        try {
            // Get the configuration for Hikari.
            final HikariConfig hikariConfig = getHikariConfig();

            // Create connection pool
            dataSource = new HikariDataSource(hikariConfig);

            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(5)) { // 5 second timeout
                    isConnected = true;
                    plugin.getLogger().info("Successfully connected to MySQL database");
                } else {
                    throw new SQLException("Failed to validate connection");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL database: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database connection", e);
        }
    }

    @NotNull
    private HikariConfig getHikariConfig() {
        HikariConfig hikariConfig = new HikariConfig();

        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=utf8",
                config.getHost(),
                config.getPort(),
                config.getDatabase());

        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setMaximumPoolSize(config.getPoolSize());
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setIdleTimeout(300000); // 5 minutes
        hikariConfig.setMaxLifetime(600000); // 10 minutes
        hikariConfig.setKeepaliveTime(60000); // 1 minute

        // Set driver class name explicitly with relocated package
        hikariConfig.setDriverClassName(MYSQL_DRIVER);

        // Connection test query
        hikariConfig.setConnectionTestQuery("SELECT 1");

        // Optimized Settings
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        hikariConfig.addDataSourceProperty("autoReconnect", "true");

        // Add connection initialization query
        hikariConfig.setConnectionInitSql("SET NAMES utf8mb4");

        return hikariConfig;
    }

    public QueryBuilder createQuery() {
        return new QueryBuilder(this, cache);
    }

    public Connection getConnection() throws SQLException {
        if (!isConnected) {
            throw new IllegalStateException("Database is not connected");
        }

        try {
            Connection connection = dataSource.getConnection();
            if (!connection.isValid(2)) {
                throw new SQLException("Got invalid connection from pool");
            }
            return connection;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get database connection: " + e.getMessage(), e);
            throw e;
        }
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