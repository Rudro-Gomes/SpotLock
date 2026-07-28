package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Converts between LocalDateTime (used in Java code) and the text format stored
// in the SQLite database, plus a friendly format for showing on screen.
public final class TimeUtil {

    // Storage format, matches SQLite's datetime() output: "2026-07-28 14:30:00".
    private static final DateTimeFormatter DB_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Human-friendly display format: "28 Jul 2026 · 02:30 PM".
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  ·  hh:mm a");

    private TimeUtil() {
    }

    // LocalDateTime -> database text.
    public static String toDb(LocalDateTime time) {
        return time == null ? null : time.format(DB_FORMAT);
    }

    // Database text -> LocalDateTime (null-safe).
    public static LocalDateTime fromDb(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        // SQLite may return "yyyy-MM-ddTHH:mm:ss" or with a space between them.
        return LocalDateTime.parse(raw.trim().replace('T', ' '), DB_FORMAT);
    }

    // LocalDateTime -> friendly on-screen text.
    public static String display(LocalDateTime time) {
        return time == null ? "-" : time.format(DISPLAY_FORMAT);
    }
}
