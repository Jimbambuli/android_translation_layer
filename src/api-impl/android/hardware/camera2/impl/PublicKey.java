package android.hardware.camera2.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** @hide AOSP core/java/android/hardware/camera2/impl/PublicKey.java: marks the metadata keys of the public API;
 * CameraMetadata.getKeys() lists only those */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PublicKey {
}
