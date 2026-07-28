package model;

// The kind of vehicle a slot is meant for. Also used to look up the hourly rate.
public enum VehicleType {
    CAR,
    BIKE;

    // Parses a database string safely; unknown values fall back to CAR.
    public static VehicleType fromDb(String value) {
        if (value == null) {
            return CAR;
        }
        try {
            return VehicleType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CAR;
        }
    }
}
