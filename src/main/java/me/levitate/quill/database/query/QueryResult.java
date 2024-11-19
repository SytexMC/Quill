package me.levitate.quill.database.query;

import lombok.Getter;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class QueryResult implements AutoCloseable {
    private final ResultSet resultSet;
    @Getter
    private final int affectedRows;
    private final boolean isResultSet;
    private final List<Map<String, Object>> cachedRows;

    public QueryResult(ResultSet resultSet) throws SQLException {
        this.resultSet = resultSet;
        this.affectedRows = -1;
        this.isResultSet = true;
        this.cachedRows = new ArrayList<>();
        cacheResults();
    }

    public QueryResult(int affectedRows) {
        this.resultSet = null;
        this.affectedRows = affectedRows;
        this.isResultSet = false;
        this.cachedRows = new ArrayList<>();
    }

    private void cacheResults() throws SQLException {
        while (resultSet.next()) {
            cachedRows.add(getRowData());
        }
        resultSet.beforeFirst();
    }

    private Map<String, Object> getRowData() throws SQLException {
        Map<String, Object> row = new HashMap<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            row.put(metaData.getColumnName(i), resultSet.getObject(i));
        }
        return row;
    }

    public List<Map<String, Object>> getRows() {
        return cachedRows;
    }

    public Optional<Map<String, Object>> getFirst() {
        return cachedRows.isEmpty() ? Optional.empty() : Optional.of(cachedRows.get(0));
    }

    public int getRowCount() {
        return cachedRows.size();
    }

    public boolean isEmpty() {
        return cachedRows.isEmpty();
    }

    @Override
    public void close() throws SQLException {
        if (resultSet != null) {
            resultSet.close();
        }
    }
}