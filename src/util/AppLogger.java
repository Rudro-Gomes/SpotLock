package util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

// Central logging for ParkVault. The UI layer calls these instead of
// printStackTrace(), so every handled error is recorded with its full stack
// trace in one consistent place.
public final class AppLogger {

    private static final Logger LOGGER = Logger.getLogger("ParkVault");

    static {
        LOGGER.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        LOGGER.addHandler(handler);
        LOGGER.setLevel(Level.ALL);
    }

    private AppLogger() {
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warning(message);
    }

    // Logs a handled error together with its full stack trace.
    public static void error(String message, Throwable cause) {
        LOGGER.log(Level.SEVERE, message, cause);
    }
}
