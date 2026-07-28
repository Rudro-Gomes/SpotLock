package concurrent;

import service.ParkingService;
import util.AppLogger;

import javax.swing.SwingUtilities;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Background thread that every 30 seconds looks for bookings whose expected exit
// time has passed, frees those slots, and then refreshes the UI on the Swing
// event thread. Must be stopped cleanly when the window closes.
public class ExpiryScheduler {

    private static final long INTERVAL_SECONDS = 30;

    private final ParkingService parkingService;
    private final Runnable uiRefreshCallback;
    private final ScheduledExecutorService scheduler;

    public ExpiryScheduler(ParkingService parkingService, Runnable uiRefreshCallback) {
        this.parkingService = parkingService;
        this.uiRefreshCallback = uiRefreshCallback;
        // A single daemon thread, so it never blocks JVM shutdown.
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ParkVault-ExpiryScheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::releaseExpiredSlots,
                INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
        AppLogger.info("ExpiryScheduler started (every " + INTERVAL_SECONDS + "s).");
    }

    // Runs on the scheduler thread. Any exception is caught here so a single bad
    // tick cannot kill the scheduled task.
    private void releaseExpiredSlots() {
        try {
            int released = parkingService.releaseExpiredBookings();
            if (released > 0) {
                AppLogger.info("Auto-released " + released + " expired slot(s).");
                // UI work must happen on the Swing event thread.
                SwingUtilities.invokeLater(uiRefreshCallback);
            }
        } catch (Exception e) {
            AppLogger.error("ExpiryScheduler tick failed", e);
        }
    }

    // Stops the background thread cleanly. Called from the window-closing handler.
    public void stop() {
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        AppLogger.info("ExpiryScheduler stopped.");
    }
}
