/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hardware;

/* AOSP SensorAdditionalInfo (an additional information frame of a sensor) */
public class SensorAdditionalInfo {

	public final Sensor sensor;
	public final int type;
	public final int serial;
	public final float[] floatValues;
	public final int[] intValues;

	public static final int TYPE_FRAME_BEGIN = 0;
	public static final int TYPE_FRAME_END = 1;
	public static final int TYPE_UNTRACKED_DELAY = 0x10000;
	public static final int TYPE_INTERNAL_TEMPERATURE = 0x10001;
	public static final int TYPE_VEC3_CALIBRATION = 0x10002;
	public static final int TYPE_SENSOR_PLACEMENT = 0x10003;
	public static final int TYPE_SAMPLING = 0x10004;
	/** @hide */
	public static final int TYPE_LOCAL_GEOMAGNETIC_FIELD = 0x30000;
	/** @hide */
	public static final int TYPE_LOCAL_GRAVITY = 0x30001;
	/** @hide */
	public static final int TYPE_DOCK_STATE = 0x30002;
	/** @hide */
	public static final int TYPE_HIGH_PERFORMANCE_MODE = 0x30003;
	/** @hide */
	public static final int TYPE_MAGNETIC_FIELD_CALIBRATION = 0x30004;
	/** @hide */
	public static final int TYPE_CUSTOM_INFO = 0x10000000;
	/** @hide */
	public static final int TYPE_DEBUG_INFO = 0x40000000;

	SensorAdditionalInfo(Sensor aSensor, int aType, int aSerial, int[] aIntValues, float[] aFloatValues) {
		sensor = aSensor;
		type = aType;
		serial = aSerial;
		intValues = aIntValues;
		floatValues = aFloatValues;
	}

	/** @hide */
	public static SensorAdditionalInfo createLocalGeomagneticField(float strength, float declination, float inclination) {
		if (strength < 10 || strength > 100 // much beyond extreme values on earth
		    || declination < -Math.PI / 2 || declination > Math.PI / 2
		    || inclination < -Math.PI / 2 || inclination > Math.PI / 2) {
			throw new IllegalArgumentException("Geomagnetic field info out of range");
		}
		return new SensorAdditionalInfo(null, TYPE_LOCAL_GEOMAGNETIC_FIELD, 0, null, new float[] {strength, declination, inclination});
	}

	/** @hide */
	public static SensorAdditionalInfo createCustomInfo(Sensor aSensor, int type, float[] data) {
		if (type < TYPE_CUSTOM_INFO || type >= TYPE_DEBUG_INFO || aSensor == null) {
			throw new IllegalArgumentException("invalid parameter(s): type: " + type + "; sensor: " + aSensor);
		}
		return new SensorAdditionalInfo(aSensor, type, 0, null, data);
	}
}
