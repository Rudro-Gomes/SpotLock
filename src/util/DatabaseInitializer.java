package util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Lets Main start the whole project on its own. Creates the schema only when
// the tables are missing, so existing bookings survive a restart. schema.sql
// begins with DROP TABLE, so it must never run blindly.
public class DatabaseInitializer {

    private static final String[] REQUIRED_TABLES = {"floor", "slot", "booking", "rate"};

    public static void ensureReady() throws SQLException {
        if (isReady()) {
            return;
        }
        initialize();
    }

    public static boolean isReady() {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            for (String table : REQUIRED_TABLES) {
                try (ResultSet rs = meta.getTables(null, null, table, null)) {
                    if (!rs.next()) {
                        return false;
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    // Runs schema.sql, dropping and recreating every table.
    public static void initialize() throws SQLException {
        Path schemaPath = resolveSchemaPath();

        if (!Files.isReadable(schemaPath)) {
            throw new SQLException("Cannot find the schema file at " + schemaPath);
        }

        String sql;
        try {
            sql = new String(Files.readAllBytes(schemaPath));
        } catch (Exception e) {
            throw new SQLException("Could not read " + schemaPath, e);
        }

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String statement : sql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement);
                }
            }
        }
    }

    public static Path resolveSchemaPath() {
        return DBConnection.resolveDbPath().getParent().getParent()
                .resolve("sql").resolve("schema.sql");
    }
}
