package com.littlehorizons.erp.config;

import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConfigLoader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single, centralized access point to MySQL.
 * It exists so URL and credentials are read once and no DAO builds its own connection.
 * It hands out a NEW Connection per call: each DAO closes it with try-with-resources,
 * and transactions need their own isolated connection.
 */
// SOLID: SRP – this class only opens connections; DAOs receive it by constructor (DIP)
public final class DatabaseConnection {

    // Design pattern: Singleton – one provider of connections for the whole app
    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection(ConfigLoader config) {
        this.url = config.get("db.url");
        this.user = config.get("db.user");
        this.password = config.get("db.password");
    }

    /** Lazy creation; synchronized to be safe if used from several threads. */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection(new ConfigLoader());
        }
        return instance;
    }

    /** Opens a new connection. The caller must close it (try-with-resources). */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            AppLogger.getInstance().error(DatabaseConnection.class, "Could not open database connection", e);
            throw new DatabaseException("Could not connect to the database. Check config.properties.", e);
        }
    }

    /** Used at startup to fail early with a clear message. */
    public boolean isReachable() {
        try (Connection connection = getConnection()) {
            return connection.isValid(3);
        } catch (SQLException | DatabaseException e) {
            return false;
        }
    }
}
