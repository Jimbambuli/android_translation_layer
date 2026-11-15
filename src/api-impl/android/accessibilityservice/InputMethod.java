package android.accessibilityservice;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.SurroundingText;
import android.view.inputmethod.TextAttribute;

/* AOSP android.accessibilityservice.InputMethod (android-16.0.0_r4). AOSP gets startInput/finishInput/updateSelection
 * from the system's accessibility IME session and talks to the editor through RemoteAccessibilityInputConnection;
 * ATL has no accessibility IME session, so doStartInput()/doFinishInput() are only called by whoever binds one, and
 * the connection is the editor's InputConnection. Without a started input the connection is null as in AOSP. */
public class InputMethod {
    private final AccessibilityService mService;
    private boolean mInputStarted;
    private InputConnection mStartedInputConnection;
    private EditorInfo mInputEditorInfo;

    public InputMethod(@NonNull AccessibilityService service) {
        mService = service;
    }

    @Nullable
    public final AccessibilityInputConnection getCurrentInputConnection() {
        if (mStartedInputConnection != null) {
            return new AccessibilityInputConnection(mStartedInputConnection);
        }
        return null;
    }

    public final boolean getCurrentInputStarted() {
        return mInputStarted;
    }

    @Nullable
    public final EditorInfo getCurrentInputEditorInfo() {
        return mInputEditorInfo;
    }

    public void onStartInput(@NonNull EditorInfo attribute, boolean restarting) {
    }

    public void onFinishInput() {
    }

    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
    }

    final void startInput(@Nullable InputConnection ic, @NonNull EditorInfo attribute) {
        doStartInput(ic, attribute, false /* restarting */);
    }

    final void restartInput(@Nullable InputConnection ic, @NonNull EditorInfo attribute) {
        doStartInput(ic, attribute, true /* restarting */);
    }

    final void doStartInput(InputConnection ic, EditorInfo attribute, boolean restarting) {
        if ((ic == null || !restarting) && mInputStarted) {
            doFinishInput();
            if (ic == null) {
                return;
            }
        }
        mInputStarted = true;
        mStartedInputConnection = ic;
        mInputEditorInfo = attribute;
        onStartInput(attribute, restarting);
    }

    final void doFinishInput() {
        if (mInputStarted) {
            onFinishInput();
        }
        mInputStarted = false;
        mStartedInputConnection = null;
        mInputEditorInfo = null;
    }

    public final class AccessibilityInputConnection {
        private final InputConnection mIc;

        AccessibilityInputConnection(InputConnection ic) {
            this.mIc = ic;
        }

        public void commitText(@NonNull CharSequence text, int newCursorPosition,
                @Nullable TextAttribute textAttribute) {
            if (mIc != null) {
                mIc.commitText(text, newCursorPosition, textAttribute);
            }
        }

        public void setSelection(int start, int end) {
            if (mIc != null) {
                mIc.setSelection(start, end);
            }
        }

        @Nullable
        public SurroundingText getSurroundingText(@IntRange(from = 0) int beforeLength,
                @IntRange(from = 0) int afterLength, int flags) {
            if (mIc != null) {
                return mIc.getSurroundingText(beforeLength, afterLength, flags);
            }
            return null;
        }

        public void deleteSurroundingText(int beforeLength, int afterLength) {
            if (mIc != null) {
                mIc.deleteSurroundingText(beforeLength, afterLength);
            }
        }

        public void sendKeyEvent(@NonNull KeyEvent event) {
            if (mIc != null) {
                mIc.sendKeyEvent(event);
            }
        }

        public void performEditorAction(int editorAction) {
            if (mIc != null) {
                mIc.performEditorAction(editorAction);
            }
        }

        public void performContextMenuAction(int id) {
            if (mIc != null) {
                mIc.performContextMenuAction(id);
            }
        }

        public int getCursorCapsMode(int reqModes) {
            if (mIc != null) {
                return mIc.getCursorCapsMode(reqModes);
            }
            return 0;
        }

        public void clearMetaKeyStates(int states) {
            if (mIc != null) {
                mIc.clearMetaKeyStates(states);
            }
        }
    }
}
