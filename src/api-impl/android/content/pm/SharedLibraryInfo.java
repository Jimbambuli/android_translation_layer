package android.content.pm;

import java.util.Collections;
import java.util.List;

/* AOSP core/java/android/content/pm/SharedLibraryInfo.java (android14-release), the getters apps use. ATL has
 * no shared library packages, so PackageManager.getSharedLibraries() lists none. */
public final class SharedLibraryInfo {
	public static final int TYPE_BUILTIN = 0;
	public static final int TYPE_DYNAMIC = 1;
	public static final int TYPE_STATIC = 2;
	public static final int TYPE_SDK_PACKAGE = 3;
	public static final int VERSION_UNDEFINED = -1;

	private final String mPath;
	private final String mPackageName;
	private final String mName;
	private final long mVersion;
	private final int mType;
	private final boolean mIsNative;

	public SharedLibraryInfo(String path, String packageName, String name, long version, int type, boolean isNative) {
		mPath = path;
		mPackageName = packageName;
		mName = name;
		mVersion = version;
		mType = type;
		mIsNative = isNative;
	}

	public int getType() {
		return mType;
	}

	public boolean isNative() {
		return mIsNative;
	}

	public String getName() {
		return mName;
	}

	public String getPath() {
		return mPath;
	}

	public String getPackageName() {
		return mPackageName;
	}

	public List<String> getAllCodePaths() {
		return mPath != null ? Collections.singletonList(mPath) : Collections.<String>emptyList();
	}

	@Deprecated
	public int getVersion() {
		return mVersion < 0 ? (int)mVersion : (int)(mVersion & 0x7fffffff);
	}

	public long getLongVersion() {
		return mVersion;
	}

	public boolean isBuiltin() {
		return mType == TYPE_BUILTIN;
	}

	public boolean isDynamic() {
		return mType == TYPE_DYNAMIC;
	}

	public boolean isStatic() {
		return mType == TYPE_STATIC;
	}

	public boolean isSdk() {
		return mType == TYPE_SDK_PACKAGE;
	}

	@Override
	public String toString() {
		return "SharedLibraryInfo{name:" + mName + ", type:" + mType + ", version:" + mLongVersionString() + "}";
	}

	private String mLongVersionString() {
		return Long.toString(mVersion);
	}
}
