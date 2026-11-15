package android.hardware.camera2.params;

import static com.android.internal.util.Preconditions.*;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.utils.HashCodeHelpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/* AOSP core/java/android/hardware/camera2/params/SessionConfiguration.java (comments and annotations removed) */
public final class SessionConfiguration implements android.os.Parcelable {
	private static final String TAG = "SessionConfiguration";

	public static final int SESSION_REGULAR = CameraDevice.SESSION_OPERATION_MODE_NORMAL;

	public static final int SESSION_HIGH_SPEED =
		CameraDevice.SESSION_OPERATION_MODE_CONSTRAINED_HIGH_SPEED;

	public static final int SESSION_VENDOR_START =
		CameraDevice.SESSION_OPERATION_MODE_VENDOR_START;

	private List<OutputConfiguration> mOutputConfigurations;
	private CameraCaptureSession.StateCallback mStateCallback;
	private int mSessionType;
	private Executor mExecutor = null;
	private InputConfiguration mInputConfig = null;
	private CaptureRequest mSessionParameters = null;

	public SessionConfiguration(int sessionType,
			List<OutputConfiguration> outputs,
			Executor executor,
			CameraCaptureSession.StateCallback cb) {
		mSessionType = sessionType;
		mOutputConfigurations = Collections.unmodifiableList(new ArrayList<>(outputs));
		mStateCallback = cb;
		mExecutor = executor;
	}

	public SessionConfiguration(int sessionType, List<OutputConfiguration> outputs) {
		mSessionType = sessionType;
		mOutputConfigurations = Collections.unmodifiableList(new ArrayList<>(outputs));
	}

	/* AOSP SessionConfiguration.setStateCallback() */
	public void setStateCallback(Executor executor, CameraCaptureSession.StateCallback cb) {
		if (executor == null) {
			throw new IllegalArgumentException("executor must not be null");
		}
		if (cb == null) {
			throw new IllegalArgumentException("cb must not be null");
		}
		mExecutor = executor;
		mStateCallback = cb;
	}

	public int describeContents() {
		return 0;
	}

	/* AOSP writeToParcel(): session type and output configurations; the input configuration and the session
	 * parameters are not Parcelable in ATL, executor and callback are not parceled in AOSP either */
	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeInt(mSessionType);
		dest.writeParcelableArray(mOutputConfigurations.toArray(new OutputConfiguration[0]), flags);
	}

	public static final android.os.Parcelable.Creator<SessionConfiguration> CREATOR =
	    new android.os.Parcelable.Creator<SessionConfiguration>() {
		    public SessionConfiguration createFromParcel(android.os.Parcel source) {
			    int sessionType = source.readInt();
			    ArrayList<OutputConfiguration> outputs = new ArrayList<>();
			    try {
				    for (android.os.Parcelable o : source.readParcelableArray(OutputConfiguration.class.getClassLoader()))
					    outputs.add((OutputConfiguration)o);
			    } catch (ReflectiveOperationException e) {
				    throw new RuntimeException(e);
			    }
			    return new SessionConfiguration(sessionType, outputs);
		    }

		    public SessionConfiguration[] newArray(int size) {
			    return new SessionConfiguration[size];
		    }
	    };

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (this == obj) {
			return true;
		} else if (obj instanceof SessionConfiguration) {
			final SessionConfiguration other = (SessionConfiguration) obj;
			if (mInputConfig != other.mInputConfig || mSessionType != other.mSessionType ||
					mOutputConfigurations.size() != other.mOutputConfigurations.size()) {
				return false;
			}

			for (int i = 0;  i < mOutputConfigurations.size(); i++) {
				if (!mOutputConfigurations.get(i).equals(other.mOutputConfigurations.get(i)))
					return false;
			}

			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return HashCodeHelpers.hashCode(mOutputConfigurations.hashCode(), mInputConfig.hashCode(),
				mSessionType);
	}

	/* android-16 SessionConfiguration color space; mColorSpace starts at 0 as there */
	private int mColorSpace;

	public void setColorSpace(android.graphics.ColorSpace.Named colorSpace) {
		mColorSpace = colorSpace.ordinal();
		for (OutputConfiguration outputConfiguration : mOutputConfigurations) {
			outputConfiguration.setColorSpace(colorSpace);
		}
	}

	public void clearColorSpace() {
		mColorSpace = ColorSpaceProfiles.UNSPECIFIED;
		for (OutputConfiguration outputConfiguration : mOutputConfigurations) {
			outputConfiguration.clearColorSpace();
		}
	}

	public android.graphics.ColorSpace getColorSpace() {
		if (mColorSpace != ColorSpaceProfiles.UNSPECIFIED) {
			return android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.values()[mColorSpace]);
		} else {
			return null;
		}
	}

	public int getSessionType() {
		return mSessionType;
	}

	public List<OutputConfiguration> getOutputConfigurations() {
		return mOutputConfigurations;
	}

	public CameraCaptureSession.StateCallback getStateCallback() {
		return mStateCallback;
	}

	public Executor getExecutor() {
		return mExecutor;
	}

	public void setInputConfiguration(InputConfiguration input) {
		if (mSessionType != SESSION_HIGH_SPEED) {
			mInputConfig = input;
		} else {
			throw new UnsupportedOperationException("Method not supported for high speed session" +
					" types");
		}
	}

	public InputConfiguration getInputConfiguration() {
		return mInputConfig;
	}

	public void setSessionParameters(CaptureRequest params) {
		mSessionParameters = params;
	}

	public CaptureRequest getSessionParameters() {
		return mSessionParameters;
	}
}
