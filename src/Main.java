
import concurrent.SlotLockManager;
import dao.BookingDAO;
import dao.FloorDAO;
import dao.RateDAO;
import dao.SlotDAO;
import service.BillingService;
import service.ParkingService;
import ui.MainFrame;
import ui.UITheme;
import util.DatabaseInitializer;

import javax.swing.*;
import java.sql.SQLException;

// Entry point for ParkVault. Prepares the database, wires the object graph
// (DAOs -> services -> UI), and shows the main window. Running this one file is
// enough to start the whole project on a fresh copy.
public class Main {
    public static void main(String[] args) {

        // Create/seed the database before any window opens.
        try {
            DatabaseInitializer.ensureReady();
        } catch (SQLException e) {
            e.printStackTrace();
            UITheme.apply();
            JOptionPane.showMessageDialog(null,
                    "ParkVault could not prepare the database.\n\n" + e.getMessage(),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Wire the layers together once, here.
        FloorDAO floorDAO = new FloorDAO();
        SlotDAO slotDAO = new SlotDAO();
        BookingDAO bookingDAO = new BookingDAO();
        RateDAO rateDAO = new RateDAO();
        BillingService billingService = new BillingService(rateDAO);
        SlotLockManager slotLockManager = new SlotLockManager();
        ParkingService parkingService = new ParkingService(
                floorDAO, slotDAO, bookingDAO, billingService, slotLockManager);

        SwingUtilities.invokeLater(() -> {
            UITheme.apply();
            try {
                new MainFrame(parkingService).setVisible(true);
            } catch (RuntimeException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "ParkVault could not start.\n\n" + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
