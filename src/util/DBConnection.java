package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// Single place that opens JDBC connections to the SQLite parking database.
// Kept as plain DriverManager (no connection pool), matching the original
// project's approach.
public class DBConnection {

    private static final String DB_PROPERTY = "parkvault.db";

    // Returns a live connection, or throws. Never returns null.
    public static Connection getConnection() throws SQLException {
        Path dbPath = resolveDbPath();

        try {
            Path parent = dbPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new SQLException("Could not create database folder: " + dbPath.getParent(), e);
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

        try (Statement stmt = conn.createStatement()) {
            // SQLite ignores FOREIGN KEY clauses unless this is set per connection.
            stmt.execute("PRAGMA foreign_keys = ON");
            // When two connections write at once, wait up to 5s instead of
            // failing immediately with SQLITE_BUSY. Important for the concurrent
            // booking demo and the background expiry thread.
            stmt.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            conn.close();
            throw e;
        }

        return conn;
    }

    public static Path resolveDbPath() {
        String override = System.getProperty(DB_PROPERTY);
        if (override != null && !override.trim().isEmpty()) {
            return Paths.get(override).toAbsolutePath();
        }
        return projectRoot().resolve("database").resolve("parkvault.db");
    }

    // Anchors the database to the project folder instead of the current working
    // directory, so the app works no matter where it is launched from.
    private static Path projectRoot() {
        try {
            Path location = Paths.get(DBConnection.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());

            if (Files.isRegularFile(location)) {
                location = location.getParent();
            }

            for (Path dir = location; dir != null; dir = dir.getParent()) {
                if (Files.isDirectory(dir.resolve("sql")) || Files.isDirectory(dir.resolve("database"))) {
                    return dir;
                }
            }
        } catch (Exception ignored) {
            // Fall through to the working directory.
        }
        return Paths.get("").toAbsolutePath();
    }
}
