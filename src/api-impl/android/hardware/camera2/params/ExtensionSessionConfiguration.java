package android.hardware.camera2.params;


import android.hardware.camera2.CameraExtensionSession;

import java.util.List;
import java.util.concurrent.Executor;

/* AOSP core/java/android/hardware/camera2/params/ExtensionSessionConfiguration.java (comments and
 * annotations removed) */
public final class ExtensionSessionConfiguration {
	private static final String TAG = "ExtensionSessionConfiguration";

	private int mExtensionType;
	private List<OutputConfiguration> mOutputs;
	private OutputConfiguration mPostviewOutput = null;
	private Executor mExecutor = null;
	private CameraExtensionSession.StateCallback mCallback = null;

	public ExtensionSessionConfiguration(int extension,
			List<OutputConfiguration> outputs, Executor executor,
			CameraExtensionSession.StateCallback listener) {
		mExtensionType = extension;
		mOutputs = outputs;
		mExecutor = executor;
		mCallback = listener;
	}

	public int getExtension() {
		return mExtensionType;
	}

	public void setPostviewOutputConfiguration(OutputConfiguration postviewOutput) {
		mPostviewOutput = postviewOutput;
	}

	public OutputConfiguration getPostviewOutputConfiguration() {
		return mPostviewOutput;
	}

	public List<OutputConfiguration> getOutputConfigurations() {
		return mOutputs;
	}

	public CameraExtensionSession.StateCallback getStateCallback() {
		return mCallback;
	}

	public Executor getExecutor() {
		return mExecutor;
	}
}
