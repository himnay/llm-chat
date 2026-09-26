package com.org.llm.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Proves the database itself enforces what SqlValidator only checks by pattern. */
class ReadOnlyQueryExecutorTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    private static JdbcTemplate jdbcTemplate;
    private static ReadOnlyQueryExecutor executor;

    @BeforeAll
    static void startDatabase() {
        POSTGRES.start();
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE TABLE text2sql_orders (id int)");
        jdbcTemplate.execute("INSERT INTO text2sql_orders VALUES (1), (2)");
        executor = new ReadOnlyQueryExecutor(jdbcTemplate, new DataSourceTransactionManager(dataSource), 1);
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRES.stop();
    }

    @Test
    @DisplayName("Plain SELECTs run and return rows")
    void runsSelects() {
        assertThat(executor.query("SELECT id FROM text2sql_orders ORDER BY id")).hasSize(2);
    }

    @Test
    @DisplayName("The database rejects a write that got past the validator")
    void databaseRejectsWrites() {
        assertThatExceptionOfType(DataAccessException.class)
                .isThrownBy(() -> executor.query("SELECT * INTO order_copy FROM text2sql_orders"))
                .withMessageContaining("read-only transaction");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_tables WHERE tablename = 'order_copy'", Integer.class)).isZero();
    }

    @Test
    @DisplayName("A query running past the timeout is cancelled")
    void cancelsSlowQueries() {
        assertThatExceptionOfType(DataAccessException.class)
                .isThrownBy(() -> executor.query("SELECT pg_sleep(5)"));
    }
}
