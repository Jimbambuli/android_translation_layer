package android.hardware.biometrics;

import android.os.Parcelable;

sealed interface PromptContentItemParcelable extends PromptContentItem, Parcelable
		permits PromptContentItemPlainText, PromptContentItemBulletedText {
}
