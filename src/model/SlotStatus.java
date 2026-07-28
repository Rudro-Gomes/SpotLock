package model;

// The live state of a parking slot.
//   FREE     - available to book (green in the UI)
//   OCCUPIED - currently taken by an active booking (red in the UI)
//   DISABLED - out of service, cannot be booked (grey in the UI)
public enum SlotStatus {
    FREE,
    OCCUPIED,
    DISABLED;

    public static SlotStatus fromDb(String value) {
        if (value == null) {
            return DISABLED;
        }
        try {
            return SlotStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DISABLED;
        }
    }
}
