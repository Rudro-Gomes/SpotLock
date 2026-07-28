package exception;

// Unchecked wrapper for database failures raised by the DAO layer. Previously
// such errors were printed and swallowed, which let callers carry on as if the
// operation had succeeded.
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
