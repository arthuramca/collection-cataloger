package com.arthas.cataloger.repository;

import java.io.IOException;
import java.nio.file.Files;
<parameter name="content">package com.arthas.cataloger.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_DIR = System.getProperty("user.home") + "/collection-cataloger";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + "/catalog.db";

    private static Connection connection;

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Files.createDirectories(Paths.get(DB_DIR));
            } catch (IOException e) {
                throw new SQLException("Não foi possível criar o diretório do banco de dados: " + DB_DIR, e);
            }
            connection = DriverManager.getConnection(DB_URL);
            initializeSchema(connection);
        }
        return connection;
    }

    private static void initializeSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS items (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT,
                        name             TEXT    NOT NULL,
                        category         TEXT    DEFAULT '',
                        description      TEXT    DEFAULT '',
                        condition        TEXT    DEFAULT '',
                        acquisition_date TEXT,
                        value            REAL    DEFAULT 0.0,
                        notes            TEXT    DEFAULT ''
                    )
                    """);
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }
}
