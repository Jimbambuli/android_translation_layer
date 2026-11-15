package android.content.pm;

import android.annotation.NonNull;
import android.annotation.Nullable;

/**
 * Indicates an error when verifying the
 * <a href="https://source.android.com/docs/security/features/apksigning">app signing</a>
 * information.
 */
public class SigningInfoException extends Exception {
    private final int mCode;

    /** @hide */
    public SigningInfoException(int code, @NonNull String message, @Nullable Throwable cause) {
        super(message, cause);
        mCode = code;
    }

    /**
     * Returns a code representing the cause, in one of the installation parse return codes in
     * {@link PackageManager}.
     */
    public int getCode() {
        return mCode;
    }
}
