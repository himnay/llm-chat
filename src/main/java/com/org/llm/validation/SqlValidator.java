package com.org.llm.validation;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SqlValidator {

    private static final Set<String> ALLOWED_TABLES = Set.of(
            "text2sql_customers",
            "text2sql_products",
            "text2sql_orders",
            "text2sql_order_items"
    );
    private static final Pattern TABLE_PATTERN = Pattern.compile("(?i)\\b(?:from|join)\\s+([a-zA-Z_][\\w\\.]*)");
    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
            "(?i)\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|copy|call|do|vacuum|analyze)\\b"
    );
    private static final Pattern LIMIT_PATTERN = Pattern.compile("(?i)\\blimit\\s+\\d+");
    private static final Pattern COUNT_PATTERN = Pattern.compile("(?i)^\\s*select\\s+count\\s*\\(");

    public String sanitize(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("model did not return SQL");
        }
        String cleaned = sql
                .replace("```sql", "")
                .replace("```", "")
                .trim();
        return cleaned.endsWith(";") ? cleaned.substring(0, cleaned.length() - 1).trim() : cleaned;
    }

    public String validateReadOnly(String sql) {
        String lower = sql.trim().toLowerCase();
        if (!(lower.startsWith("select") || lower.startsWith("with"))) {
            throw new IllegalArgumentException("only SELECT queries are allowed");
        }
        if (FORBIDDEN_PATTERN.matcher(sql).find()) {
            throw new IllegalArgumentException("unsafe SQL keyword found");
        }
        if (sql.contains(";")) {
            throw new IllegalArgumentException("multiple statements are not allowed");
        }

        Set<String> referencedTables = extractTables(sql);
        if (referencedTables.isEmpty()) {
            throw new IllegalArgumentException("no table reference found");
        }
        for (String table : referencedTables) {
            if (!ALLOWED_TABLES.contains(table)) {
                throw new IllegalArgumentException("table not allowed: " + table);
            }
        }
        return sql;
    }

    public String enforceLimit(String sql, int maxRows) {
        if (LIMIT_PATTERN.matcher(sql).find()) {
            return sql;
        }
        if (COUNT_PATTERN.matcher(sql).find()) {
            return sql;
        }
        return sql + " LIMIT " + maxRows;
    }

    private Set<String> extractTables(String sql) {
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        Set<String> tables = new LinkedHashSet<>();
        while (matcher.find()) {
            String token = matcher.group(1).toLowerCase();
            if (token.contains(".")) {
                token = token.substring(token.lastIndexOf('.') + 1);
            }
            tables.add(token);
        }
        return tables;
    }
}
