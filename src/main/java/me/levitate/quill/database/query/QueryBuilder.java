package me.levitate.quill.database.query;

import me.levitate.quill.cache.Cache;
import me.levitate.quill.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class QueryBuilder {
    private final DatabaseManager databaseManager;
    private final Cache<String, Object> cache;
    private final StringBuilder query;
    private final List<Object> parameters;
    private boolean isSelect;
    private String cacheKey;
    private boolean useCache = false;

    public QueryBuilder(DatabaseManager databaseManager, Cache<String, Object> cache) {
        this.databaseManager = databaseManager;
        this.cache = cache;
        this.query = new StringBuilder();
        this.parameters = new ArrayList<>();
    }

    public QueryBuilder select(String... columns) {
        query.append("SELECT ");
        if (columns.length == 0) {
            query.append("*");
        } else {
            query.append(String.join(", ", columns));
        }
        isSelect = true;
        return this;
    }

    public QueryBuilder from(String table) {
        query.append(" FROM ").append(table);
        return this;
    }

    public QueryBuilder where(String condition) {
        query.append(" WHERE ").append(condition);
        return this;
    }

    public QueryBuilder and(String condition) {
        query.append(" AND ").append(condition);
        return this;
    }

    public QueryBuilder orderBy(String column, boolean ascending) {
        query.append(" ORDER BY ").append(column)
             .append(ascending ? " ASC" : " DESC");
        return this;
    }

    public QueryBuilder limit(int limit) {
        query.append(" LIMIT ").append(limit);
        return this;
    }

    public QueryBuilder insertInto(String table) {
        query.append("INSERT INTO ").append(table);
        return this;
    }

    public QueryBuilder columns(String... columns) {
        query.append(" (").append(String.join(", ", columns)).append(")");
        return this;
    }

    public QueryBuilder values(Object... values) {
        query.append(" VALUES (")
             .append("?".repeat(values.length)
             .replace("", ", ").trim())
             .append(")");
        parameters.addAll(List.of(values));
        return this;
    }

    public QueryBuilder update(String table) {
        query.append("UPDATE ").append(table);
        return this;
    }

    public QueryBuilder set(String column, Object value) {
        if (!query.toString().contains(" SET ")) {
            query.append(" SET ");
        } else {
            query.append(", ");
        }
        query.append(column).append(" = ?");
        parameters.add(value);
        return this;
    }

    public QueryBuilder delete() {
        query.append("DELETE");
        return this;
    }

    public QueryBuilder cache(String key) {
        this.cacheKey = key;
        this.useCache = true;
        return this;
    }

    private void setParameters(PreparedStatement statement) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    public QueryResult execute() {
        if (isSelect && useCache && cacheKey != null) {
            Object cachedResult = cache.get(cacheKey).orElse(null);
            if (cachedResult instanceof QueryResult) {
                return (QueryResult) cachedResult;
            }
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString(), 
                 ResultSet.TYPE_SCROLL_INSENSITIVE, 
                 ResultSet.CONCUR_READ_ONLY)) {
            
            setParameters(stmt);
            
            QueryResult result;
            if (isSelect) {
                ResultSet rs = stmt.executeQuery();
                result = new QueryResult(rs);
                if (useCache && cacheKey != null) {
                    cache.put(cacheKey, result);
                }
            } else {
                int affectedRows = stmt.executeUpdate();
                result = new QueryResult(affectedRows);
                if (useCache && cacheKey != null) {
                    cache.remove(cacheKey);
                }
            }
            
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query: " + query, e);
        }
    }

    public CompletableFuture<QueryResult> executeAsync() {
        return CompletableFuture.supplyAsync(this::execute);
    }

    public void executeAsync(Consumer<QueryResult> callback) {
        executeAsync().thenAccept(callback);
    }

    @Override
    public String toString() {
        return query.toString();
    }
}