package android.database;

import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.CursorWrapper;

/**
 * Cursor wrapper that implements {@link CrossProcessCursor}.
 * <p>
 * If the wrapped cursor implements {@link CrossProcessCursor}, then the wrapper
 * delegates {@link #fillWindow}, {@link #getWindow()} and {@link #onMove} to it.
 * Otherwise, the wrapper provides default implementations of these methods that
 * traverse the contents of the cursor similar to {@link AbstractCursor#fillWindow}.
 * </p><p>
 * This wrapper can be used to adapt an ordinary {@link Cursor} into a
 * {@link CrossProcessCursor}.
 * </p>
 */
/* AOSP core/java/android/database/CrossProcessCursorWrapper.java */
public class CrossProcessCursorWrapper extends CursorWrapper implements CrossProcessCursor {
    /**
     * Creates a cross process cursor wrapper.
     * @param cursor The underlying cursor to wrap.
     */
    public CrossProcessCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    @Override
    public void fillWindow(int position, CursorWindow window) {
        if (getWrappedCursor() instanceof CrossProcessCursor) {
            final CrossProcessCursor crossProcessCursor = (CrossProcessCursor)getWrappedCursor();
            crossProcessCursor.fillWindow(position, window);
            return;
        }

        DatabaseUtils.cursorFillWindow(getWrappedCursor(), position, window);
    }

    @Override
    public CursorWindow getWindow() {
        if (getWrappedCursor() instanceof CrossProcessCursor) {
            final CrossProcessCursor crossProcessCursor = (CrossProcessCursor)getWrappedCursor();
            return crossProcessCursor.getWindow();
        }

        return null;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        if (getWrappedCursor() instanceof CrossProcessCursor) {
            final CrossProcessCursor crossProcessCursor = (CrossProcessCursor)getWrappedCursor();
            return crossProcessCursor.onMove(oldPosition, newPosition);
        }

        return true;
    }
}
