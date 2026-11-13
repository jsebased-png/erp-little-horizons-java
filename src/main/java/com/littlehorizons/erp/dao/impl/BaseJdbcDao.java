package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.model.DbEnum;
import com.littlehorizons.erp.util.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Common JDBC helpers shared by every DAO implementation.
 * It exists so each DAO only writes its SQL and its row mapping, while opening
 * connections, binding parameters, closing resources and error handling live in one place.
 * Every statement is a PreparedStatement and every resource is closed with try-with-resources.
 */
public abstract class BaseJdbcDao {

    // SOLID: DIP – DAOs receive the connection provider through the constructor
    protected final DatabaseConnection database;

    protected BaseJdbcDao(DatabaseConnection database) {
        this.database = database;
    }

    /** Converts the current row of a ResultSet into an object. Used with lambdas / method references. */
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** A group of statements that must succeed or fail together. */
    @FunctionalInterface
    protected interface TransactionWork<T> {
        T run(Connection connection) throws SQLException;
    }

    // ------------------------------------------------------------------ queries

    /** Runs a SELECT on its own connection and maps every row. */
    protected <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection connection = database.getConnection()) {
            return queryList(connection, sql, mapper, params);
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    /** Runs a SELECT on an existing connection (used inside transactions). */
    protected <T> List<T> queryList(Connection connection, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    /** Returns the first row, or empty when nothing matches. */
    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        return queryList(sql, mapper, params).stream().findFirst();
    }

    /** Runs a query that returns one integer (COUNT, MAX...). Returns 0 when there is no row. */
    protected int scalarInt(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getInt(1), params).orElse(0);
    }

    // ------------------------------------------------------------------ writes

    protected int executeUpdate(String sql, Object... params) {
        try (Connection connection = database.getConnection()) {
            return executeUpdate(connection, sql, params);
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    protected int executeUpdate(Connection connection, String sql, Object... params) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    /** Runs an INSERT and returns the generated AUTO_INCREMENT id. */
    protected int executeInsert(String sql, Object... params) {
        try (Connection connection = database.getConnection()) {
            return executeInsert(connection, sql, params);
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    protected int executeInsert(Connection connection, String sql, Object... params) {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, params);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    // ------------------------------------------------------------------ transactions

    /**
     * Runs several statements in one transaction: commit if all succeed, rollback if any fails.
     * Used by compound operations (payment + invoice, loan + resource status).
     */
    protected <T> T inTransaction(TransactionWork<T> work) {
        try (Connection connection = database.getConnection()) {
            try {
                connection.setAutoCommit(false);
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(connection);
                throw e;
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
            AppLogger.getInstance().warning(getClass(), "Transaction rolled back");
        } catch (SQLException e) {
            AppLogger.getInstance().error(getClass(), "Rollback failed", e);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Sets every parameter. Enums are saved with their database label. */
    private void bind(PreparedStatement statement, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value instanceof DbEnum) {
                value = ((DbEnum) value).getLabel();
            }
            statement.setObject(i + 1, value);
        }
    }

    /** Logs the SQL error and converts it to our own exception. */
    private DatabaseException fail(SQLException e) {
        AppLogger.getInstance().error(getClass(), "SQL error (state " + e.getSQLState() + "): " + e.getMessage(), e);
        String state = e.getSQLState();
        if (state != null && state.startsWith("23")) {
            return new DatabaseException("The data breaks a database rule (duplicate value or missing reference).", e);
        }
        return new DatabaseException("Database error. See logs/error_log.txt for details.", e);
    }

    /** Returns a new array with one more value at the end (used for UPDATE ... WHERE id = ?). */
    protected static Object[] append(Object[] values, Object extra) {
        Object[] result = Arrays.copyOf(values, values.length + 1);
        result[values.length] = extra;
        return result;
    }

    /** Reads a nullable INT column. */
    protected static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /** Reads a nullable DATE column. */
    protected static LocalDate date(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }
}
