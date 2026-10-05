package com.mycompany.solarpos.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Singleton: one shared database connection for the whole application. */
public class DBConnection {

    private static DBConnection instance;
    private final Properties props = new Properties();
    private Connection connection;

    private DBConnection() {
        try (InputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read config.properties. Is it in the project folder?", e);
        }
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /** Returns a live connection, reconnecting if it was closed or dropped. */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connection = DriverManager.getConnection(
                    props.getProperty("db.url"),
                    props.getProperty("db.user"),
                    props.getProperty("db.password"));
        }
        return connection;
    }
}