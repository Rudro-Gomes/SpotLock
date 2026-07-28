package exception;

// Thrown when no active booking can be found for a given slot or vehicle -
// for example when checking out a slot that is already free, or searching for
// a vehicle that is not currently parked.
public class BookingNotFoundException extends Exception {
    public BookingNotFoundException(String message) {
        super(message);
    }
}
