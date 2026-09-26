package com.org.llm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

/**
 * Runs LLM-generated SQL in a read-only transaction with a timeout, so the database itself rejects
 * writes and cancels runaway queries even if a statement slips past {@code SqlValidator}.
 */
@Component
public class ReadOnlyQueryExecutor {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate readOnlyTransaction;

    public ReadOnlyQueryExecutor(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager,
                                 @Value("${app.text2sql.query-timeout-seconds:10}") int timeoutSeconds) {
        this.jdbcTemplate = jdbcTemplate;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
        this.readOnlyTransaction.setTimeout(timeoutSeconds);
    }

    /** Runs {@code sql} and returns each row as a column-name-to-value map. */
    public List<Map<String, Object>> query(String sql) {
        return readOnlyTransaction.execute(status -> jdbcTemplate.queryForList(sql));
    }
}
