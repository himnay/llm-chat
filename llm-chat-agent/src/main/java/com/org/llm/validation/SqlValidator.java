package com.org.llm.validation;

import com.org.llm.exception.SqlValidationException;
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
    private static final Pattern FROM_KEYWORD = Pattern.compile("(?i)\\bfrom\\b");
    private static final Pattern JOIN_TARGET = Pattern.compile("(?i)\\bjoin\\s+(\\S+)");
    /** Where a FROM list ends: the next clause keyword, a JOIN, or a parenthesis (a subquery has its own FROM). */
    private static final Pattern FROM_LIST_END = Pattern.compile(
            "(?i)\\b(?:where|group|order|having|limit|offset|union|intersect|except|window|fetch|for|join|inner|left"
                    + "|right|full|cross|natural|on|using)\\b|[()]");
    private static final Pattern TABLE_NAME = Pattern.compile("[a-zA-Z_][\\w.]*");
    // "into" closes the SELECT ... INTO new_table loophole, which creates a table
    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
            "(?i)\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|copy|call|do|vacuum|analyze|into)\\b"
    );
    /** Functions a SELECT can still call to read server files, run dynamic SQL, sleep, or change state. */
    private static final Pattern FORBIDDEN_FUNCTION_PATTERN = Pattern.compile(
            "(?i)\\b(pg_\\w+|lo_\\w+|dblink\\w*|set_config|current_setting|(?:query|table|cursor|schema|database)_to_xml\\w*"
                    + "|nextval|setval)\\s*\\("
    );
    private static final Pattern LIMIT_PATTERN = Pattern.compile("(?i)\\blimit\\s+\\d+");
    private static final Pattern COUNT_PATTERN = Pattern.compile("(?i)^\\s*select\\s+count\\s*\\(");

    /** Full guard pipeline: strip fences/semicolons, enforce read-only allow-listed SQL, cap rows. */
    public String prepare(String rawSql, int maxRows) {
        return enforceLimit(validateReadOnly(sanitize(rawSql)), maxRows);
    }

    /** Returns the sanitize. */
    public String sanitize(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new SqlValidationException("model did not return SQL");
        }
        String cleaned = sql
                .replace("```sql", "")
                .replace("```", "")
                .trim();
        return cleaned.endsWith(";") ? cleaned.substring(0, cleaned.length() - 1).trim() : cleaned;
    }

    /** Validates read only. */
    public String validateReadOnly(String sql) {
        String lower = sql.trim().toLowerCase();
        if (!(lower.startsWith("select") || lower.startsWith("with"))) {
            throw new SqlValidationException("only SELECT queries are allowed");
        }
        if (FORBIDDEN_PATTERN.matcher(sql).find()) {
            throw new SqlValidationException("unsafe SQL keyword found");
        }
        Matcher function = FORBIDDEN_FUNCTION_PATTERN.matcher(sql);
        if (function.find()) {
            throw new SqlValidationException("function not allowed: " + function.group(1));
        }
        if (sql.contains(";")) {
            throw new SqlValidationException("multiple statements are not allowed");
        }

        Set<String> referencedTables = extractTables(sql);
        if (referencedTables.isEmpty()) {
            throw new SqlValidationException("no table reference found");
        }
        for (String table : referencedTables) {
            if (!ALLOWED_TABLES.contains(table)) {
                throw new SqlValidationException("table not allowed: " + table);
            }
        }
        return sql;
    }

    /** Returns the enforce limit. */
    public String enforceLimit(String sql, int maxRows) {
        if (LIMIT_PATTERN.matcher(sql).find()) {
            return sql;
        }
        if (COUNT_PATTERN.matcher(sql).find()) {
            return sql;
        }
        return sql + " LIMIT " + maxRows;
    }

    /**
     * Every table in every FROM list (including comma-separated ones) and every JOIN target.
     * Subqueries are covered by their own FROM. Anything that is not a plain, optionally
     * schema-qualified name (for example a quoted identifier) is rejected rather than skipped.
     */
    private Set<String> extractTables(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        Matcher from = FROM_KEYWORD.matcher(sql);
        while (from.find()) {
            String rest = sql.substring(from.end());
            Matcher end = FROM_LIST_END.matcher(rest);
            String list = end.find() ? rest.substring(0, end.start()) : rest;
            for (String item : list.split(",")) {
                String trimmed = item.strip();
                if (!trimmed.isEmpty()) {
                    tables.add(tableName(trimmed.split("\\s+")[0]));
                }
            }
        }
        Matcher join = JOIN_TARGET.matcher(sql);
        while (join.find()) {
            if (!join.group(1).startsWith("(")) {
                tables.add(tableName(join.group(1)));
            }
        }
        return tables;
    }

    private String tableName(String token) {
        if (!TABLE_NAME.matcher(token).matches()) {
            throw new SqlValidationException("unsupported table reference: " + token);
        }
        String name = token.toLowerCase();
        return name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name;
    }
}
