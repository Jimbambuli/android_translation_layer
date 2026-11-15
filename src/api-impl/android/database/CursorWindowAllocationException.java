package android.database;


/**
 * This exception is thrown when a CursorWindow couldn't be allocated,
 * most probably due to memory not being available.
 */
/* AOSP core/java/android/database/CursorWindowAllocationException.java */
public class CursorWindowAllocationException extends RuntimeException {
    public CursorWindowAllocationException(String description) {
        super(description);
    }
}
