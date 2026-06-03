package com.org.llm.service;

import com.org.llm.model.TextToSqlRequest;
import com.org.llm.model.TextToSqlResponse;
import com.org.llm.validation.SqlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TextToSqlService {

    private final ChatClient chatClient;
    private final JdbcTemplate jdbcTemplate;
    private final SqlValidator sqlValidator;

    public TextToSqlResponse process(TextToSqlRequest request) {
        int maxRows = normalizeMaxRows(request.maxRows());
        String schemaContext = schemaContext();
        String generatedSql = generateSql(request.question(), schemaContext, maxRows);
        String executableSql = sqlValidator.enforceLimit(sqlValidator.validateReadOnly(sqlValidator.sanitize(generatedSql)), maxRows);

        boolean repaired = false;
        List<Map<String, Object>> rows = List.of();

        if (!Boolean.TRUE.equals(request.dryRun())) {
            try {
                rows = jdbcTemplate.queryForList(executableSql);
            } catch (BadSqlGrammarException ex) {
                repaired = true;
                String fixedSql = sqlValidator.sanitize(repairSql(request.question(), executableSql, ex.getMessage(), schemaContext, maxRows));
                executableSql = sqlValidator.enforceLimit(sqlValidator.validateReadOnly(fixedSql), maxRows);
                rows = jdbcTemplate.queryForList(executableSql);
            }
        }

        String explanation = null;
        if (Boolean.TRUE.equals(request.includeExplanation())) {
            explanation = explainSql(request.question(), executableSql);
        }

        return new TextToSqlResponse(
                request.question(),
                executableSql,
                repaired,
                Boolean.TRUE.equals(request.dryRun()),
                rows.size(),
                rows,
                explanation
        );
    }

    private String generateSql(String question, String schemaContext, int maxRows) {
        String prompt = """
                Convert user text into ONE PostgreSQL SELECT query.
                Rules:
                1) Return only SQL (no markdown, no comments, no prose, no semicolon).
                2) Use only tables/columns from the schema below.
                3) Prefer clear JOINs and aliases when needed.
                4) Keep it safe and read-only.
                5) Respect max rows %d by adding LIMIT when result can be multiple rows.

                Schema:
                %s

                User question:
                %s
                """.formatted(maxRows, schemaContext, question);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private String repairSql(String question, String sql, String error, String schemaContext, int maxRows) {
        String prompt = """
                Fix this PostgreSQL SELECT query using the schema and DB error.
                Return only corrected SQL (no markdown, no prose, no semicolon).
                Keep it read-only and use only allowed schema.
                Keep LIMIT <= %d for multi-row output.

                User question:
                %s

                Previous SQL:
                %s

                Database error:
                %s

                Schema:
                %s
                """.formatted(maxRows, question, sql, error, schemaContext);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private String explainSql(String question, String sql) {
        String prompt = """
                Explain this SQL in plain English in 3 short bullet points.
                Include what data it fetches and how it filters/groups.

                User question: %s
                SQL: %s
                """.formatted(question, sql);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private int normalizeMaxRows(Integer requestedMaxRows) {
        if (requestedMaxRows == null) {
            return 50;
        }
        return Math.max(1, Math.min(requestedMaxRows, 200));
    }

    private String schemaContext() {
        String sql = """
                SELECT table_name, column_name, data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name IN ('text2sql_customers', 'text2sql_products', 'text2sql_orders', 'text2sql_order_items')
                ORDER BY table_name, ordinal_position
                """;
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql);
        if (columns.isEmpty()) {
            return """
                    text2sql_customers(id, full_name, email, city, segment, created_at)
                    text2sql_products(id, name, category, unit_price, stock_quantity)
                    text2sql_orders(id, customer_id, order_date, status, total_amount)
                    text2sql_order_items(id, order_id, product_id, quantity, unit_price, discount_pct)
                    """;
        }

        Map<String, List<String>> grouped = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : columns) {
            String tableName = (String) row.get("table_name");
            String columnName = (String) row.get("column_name");
            String dataType = (String) row.get("data_type");
            grouped.computeIfAbsent(tableName, ignored -> new ArrayList<>())
                    .add(columnName + " " + dataType);
        }

        StringBuilder out = new StringBuilder();
        grouped.forEach((table, cols) -> out.append(table).append("(").append(String.join(", ", cols)).append(")\n"));
        return out.toString().trim();
    }
}
