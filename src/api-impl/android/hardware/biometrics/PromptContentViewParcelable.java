package android.hardware.biometrics;

import android.os.Parcelable;

sealed interface PromptContentViewParcelable extends PromptContentView, Parcelable
		permits PromptVerticalListContentView, PromptContentViewWithMoreOptionsButton {
}
