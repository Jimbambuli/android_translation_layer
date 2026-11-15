package android.hardware.biometrics;

import android.os.Parcelable;

/* AOSP declares it sealed (permits PromptContentItemPlainText, PromptContentItemBulletedText); api-impl is compiled with -source 1.8, which has no sealed types */
interface PromptContentItemParcelable extends PromptContentItem, Parcelable {
}
