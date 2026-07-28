package exception;

// Thrown when a vehicle number fails validation (blank or wrong format).
public class InvalidVehicleException extends Exception {
    public InvalidVehicleException(String message) {
        super(message);
    }
}
