// Purpose: The error thrown when saving or loading data fails.
// It wraps the database error so the rest of the app does not need to catch SQL errors.

package gradeplanner.store;

public class StoreException extends RuntimeException {

    public StoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
