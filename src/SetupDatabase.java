import util.DBConnection;
import util.DatabaseInitializer;

// Optional helper: forces a clean rebuild of the parking database.
// Main sets the database up automatically, so this is only needed to wipe
// existing bookings and start over from the seed data in schema.sql.
public class SetupDatabase {
    public static void main(String[] args) {
        try {
            DatabaseInitializer.initialize();
            System.out.println("ParkVault database created successfully at "
                    + DBConnection.resolveDbPath());
        } catch (Exception e) {
            System.out.println("Setup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
