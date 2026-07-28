package exception;

// Thrown when a slot cannot be booked because another booking already took it.
// This is the business signal that a booking lost the race for a slot.
public class SlotOccupiedException extends Exception {
    public SlotOccupiedException(String message) {
        super(message);
    }
}
