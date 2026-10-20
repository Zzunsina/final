package com.employment.global.common;

import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class Database {

    private final JdbcTemplate jdbc;

    public Database(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(String sql, Object... args) {
        return jdbc.query(
            sql,
            (rs, row) -> {
                Map<String, Object> result = new LinkedHashMap<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    String raw = rs.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT);
                    StringBuilder key = new StringBuilder();
                    boolean upper = false;
                    for (char c : raw.toCharArray()) {
                        if (c == '_') upper = true;
                        else {
                            key.append(upper ? Character.toUpperCase(c) : c);
                            upper = false;
                        }
                    }
                    Object value = rs.getObject(i);
                    if (value instanceof Timestamp time) value = time.toInstant().toString();
                    if (value instanceof java.sql.Date date) value = date.toLocalDate().toString();
                    result.put(key.toString(), value);
                }
                return result;
            },
            args
        );
    }

    public Map<String, Object> one(String sql, Object... args) {
        var rows = list(sql, args);
        if (rows.isEmpty()) throw new Problem(404, "정보를 찾을 수 없습니다.");
        return rows.get(0);
    }

    public int update(String sql, Object... args) {
        return jdbc.update(sql, args);
    }

    public long insert(String table, Map<String, Object> fields) {
        var keys = new ArrayList<>(fields.keySet());
        String sql =
            "INSERT INTO " +
            table +
            " (" +
            String.join(",", keys) +
            ") VALUES (" +
            String.join(",", Collections.nCopies(keys.size(), "?")) +
            ")";
        var holder = new GeneratedKeyHolder();
        jdbc.update(
            connection -> {
                var statement = connection.prepareStatement(
                    sql,
                    java.sql.Statement.RETURN_GENERATED_KEYS
                );
                for (int i = 0; i < keys.size(); i++) statement.setObject(
                    i + 1,
                    fields.get(keys.get(i))
                );
                return statement;
            },
            holder
        );
        var generated = Objects.requireNonNull(holder.getKeys());
        return generated
            .values()
            .stream()
            .filter(Number.class::isInstance)
            .map(Number.class::cast)
            .findFirst()
            .orElseThrow()
            .longValue();
    }

    public static long number(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
    }

    public static Map<String, Object> fields(Object... values) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) fields.put((String) values[i], values[i + 1]);
        return fields;
    }
}
