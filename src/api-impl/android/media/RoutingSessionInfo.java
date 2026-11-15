package android.media;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.android.internal.util.Preconditions;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/* AOSP media/java/android/media/RoutingSessionInfo.java */
public final class RoutingSessionInfo implements Parcelable {
	public static final Creator<RoutingSessionInfo> CREATOR =
			new Creator<RoutingSessionInfo>() {
				@Override
				public RoutingSessionInfo createFromParcel(Parcel in) {
					return new RoutingSessionInfo(in);
				}
				@Override
				public RoutingSessionInfo[] newArray(int size) {
					return new RoutingSessionInfo[size];
				}
			};

	private static final String TAG = "RoutingSessionInfo";

	/* android-16 RoutingSessionInfo transfer reason (the transfer initiator fields are left out) */
	public static final int TRANSFER_REASON_FALLBACK = 0;
	public static final int TRANSFER_REASON_SYSTEM_REQUEST = 1;
	public static final int TRANSFER_REASON_APP = 2;

	private static final String KEY_GROUP_ROUTE = "androidx.mediarouter.media.KEY_GROUP_ROUTE";
	private static final String KEY_VOLUME_HANDLING = "volumeHandling";

	final String mId;
	final CharSequence mName;
	final String mOwnerPackageName;
	final String mClientPackageName;
	final String mProviderId;
	final List<String> mSelectedRoutes;
	final List<String> mSelectableRoutes;
	final List<String> mDeselectableRoutes;
	final List<String> mTransferableRoutes;

	final int mVolumeHandling;
	final int mVolumeMax;
	final int mVolume;

	final Bundle mControlHints;
	final boolean mIsSystemSession;
	final int mTransferReason;

	RoutingSessionInfo(Builder builder) {
		Objects.requireNonNull(builder, "builder must not be null.");

		mId = builder.mId;
		mName = builder.mName;
		mOwnerPackageName = builder.mOwnerPackageName;
		mClientPackageName = builder.mClientPackageName;
		mProviderId = builder.mProviderId;

		mSelectedRoutes = Collections.unmodifiableList(
				convertToUniqueRouteIds(builder.mSelectedRoutes));
		mSelectableRoutes = Collections.unmodifiableList(
				convertToUniqueRouteIds(builder.mSelectableRoutes));
		mDeselectableRoutes = Collections.unmodifiableList(
				convertToUniqueRouteIds(builder.mDeselectableRoutes));
		mTransferableRoutes = Collections.unmodifiableList(
				convertToUniqueRouteIds(builder.mTransferableRoutes));

		mVolumeMax = builder.mVolumeMax;
		mVolume = builder.mVolume;

		mIsSystemSession = builder.mIsSystemSession;
		mTransferReason = builder.mTransferReason;

		boolean volumeAdjustmentForRemoteGroupSessions = Resources.getSystem().getBoolean(
				com.android.internal.R.bool.config_volumeAdjustmentForRemoteGroupSessions);
		mVolumeHandling = defineVolumeHandling(builder.mVolumeHandling, mSelectedRoutes,
				volumeAdjustmentForRemoteGroupSessions);

		mControlHints = updateVolumeHandlingInHints(builder.mControlHints, mVolumeHandling);
	}

	RoutingSessionInfo(Parcel src) {
		mId = src.readString();
		Preconditions.checkArgument(!TextUtils.isEmpty(mId));

		mName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(src);
		mOwnerPackageName = src.readString();
		mClientPackageName = ensureString(src.readString());
		mProviderId = src.readString();

		mSelectedRoutes = ensureList(src.createStringArrayList());
		Preconditions.checkArgument(!mSelectedRoutes.isEmpty());

		mSelectableRoutes = ensureList(src.createStringArrayList());
		mDeselectableRoutes = ensureList(src.createStringArrayList());
		mTransferableRoutes = ensureList(src.createStringArrayList());

		mVolumeHandling = src.readInt();
		mVolumeMax = src.readInt();
		mVolume = src.readInt();

		mControlHints = src.readBundle();
		mIsSystemSession = src.readBoolean();
		mTransferReason = src.readInt();
	}

	private static Bundle updateVolumeHandlingInHints(Bundle controlHints, int volumeHandling) {
		if (controlHints != null && controlHints.containsKey(KEY_GROUP_ROUTE)) {
			Bundle groupRoute = controlHints.getBundle(KEY_GROUP_ROUTE);

			if (groupRoute != null && groupRoute.containsKey(KEY_VOLUME_HANDLING)
					&& volumeHandling != groupRoute.getInt(KEY_VOLUME_HANDLING)) {
				Bundle newGroupRoute = new Bundle(groupRoute);
				newGroupRoute.putInt(KEY_VOLUME_HANDLING, volumeHandling);
				Bundle newControlHints = new Bundle(controlHints);
				newControlHints.putBundle(KEY_GROUP_ROUTE, newGroupRoute);
				return newControlHints;
			}
		}
		return controlHints;
	}

	private static int defineVolumeHandling(int volumeHandling, List<String> selectedRoutes,
			boolean volumeAdjustmentForRemoteGroupSessions) {
		if (!volumeAdjustmentForRemoteGroupSessions && selectedRoutes.size() > 1) {
			return MediaRoute2Info.PLAYBACK_VOLUME_FIXED;
		}
		return volumeHandling;
	}

	private static String ensureString(String str) {
		return str != null ? str : "";
	}

	private static <T> List<T> ensureList(List<? extends T> list) {
		if (list != null) {
			return Collections.unmodifiableList(list);
		}
		return Collections.emptyList();
	}

	public String getId() {
		if (!TextUtils.isEmpty(mProviderId)) {
			return MediaRouter2Utils.toUniqueId(mProviderId, mId);
		} else {
			return mId;
		}
	}

	public CharSequence getName() {
		return mName;
	}

	public String getOriginalId() {
		return mId;
	}

	public String getOwnerPackageName() {
		return mOwnerPackageName;
	}

	public String getClientPackageName() {
		return mClientPackageName;
	}

	public String getProviderId() {
		return mProviderId;
	}

	public List<String> getSelectedRoutes() {
		return mSelectedRoutes;
	}

	public List<String> getSelectableRoutes() {
		return mSelectableRoutes;
	}

	public List<String> getDeselectableRoutes() {
		return mDeselectableRoutes;
	}

	public List<String> getTransferableRoutes() {
		return mTransferableRoutes;
	}

	
	public int getVolumeHandling() {
		return mVolumeHandling;
	}

	public int getVolumeMax() {
		return mVolumeMax;
	}

	public int getVolume() {
		return mVolume;
	}

	public Bundle getControlHints() {
		return mControlHints;
	}

	public boolean isSystemSession() {
		return mIsSystemSession;
	}

	public int getTransferReason() {
		return mTransferReason;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mId);
		dest.writeCharSequence(mName);
		dest.writeString(mOwnerPackageName);
		dest.writeString(mClientPackageName);
		dest.writeString(mProviderId);
		dest.writeStringList(mSelectedRoutes);
		dest.writeStringList(mSelectableRoutes);
		dest.writeStringList(mDeselectableRoutes);
		dest.writeStringList(mTransferableRoutes);
		dest.writeInt(mVolumeHandling);
		dest.writeInt(mVolumeMax);
		dest.writeInt(mVolume);
		dest.writeBundle(mControlHints);
		dest.writeBoolean(mIsSystemSession);
		dest.writeInt(mTransferReason);
	}

	public void dump(PrintWriter pw, String prefix) {
		pw.println(prefix + "RoutingSessionInfo");

		String indent = prefix + "  ";

		pw.println(indent + "mId=" + mId);
		pw.println(indent + "mName=" + mName);
		pw.println(indent + "mOwnerPackageName=" + mOwnerPackageName);
		pw.println(indent + "mClientPackageName=" + mClientPackageName);
		pw.println(indent + "mProviderId=" + mProviderId);
		pw.println(indent + "mSelectedRoutes=" + mSelectedRoutes);
		pw.println(indent + "mSelectableRoutes=" + mSelectableRoutes);
		pw.println(indent + "mDeselectableRoutes=" + mDeselectableRoutes);
		pw.println(indent + "mTransferableRoutes=" + mTransferableRoutes);
		pw.println(indent + "mVolumeHandling=" + mVolumeHandling);
		pw.println(indent + "mVolumeMax=" + mVolumeMax);
		pw.println(indent + "mVolume=" + mVolume);
		pw.println(indent + "mControlHints=" + mControlHints);
		pw.println(indent + "mIsSystemSession=" + mIsSystemSession);
		pw.println(indent + "mTransferReason=" + mTransferReason);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RoutingSessionInfo)) {
			return false;
		}

		RoutingSessionInfo other = (RoutingSessionInfo) obj;
		return Objects.equals(mId, other.mId)
				&& Objects.equals(mName, other.mName)
				&& Objects.equals(mOwnerPackageName, other.mOwnerPackageName)
				&& Objects.equals(mClientPackageName, other.mClientPackageName)
				&& Objects.equals(mProviderId, other.mProviderId)
				&& Objects.equals(mSelectedRoutes, other.mSelectedRoutes)
				&& Objects.equals(mSelectableRoutes, other.mSelectableRoutes)
				&& Objects.equals(mDeselectableRoutes, other.mDeselectableRoutes)
				&& Objects.equals(mTransferableRoutes, other.mTransferableRoutes)
				&& (mVolumeHandling == other.mVolumeHandling)
				&& (mVolumeMax == other.mVolumeMax)
				&& (mVolume == other.mVolume)
				&& (mTransferReason == other.mTransferReason);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mId, mName, mOwnerPackageName, mClientPackageName, mProviderId,
				mSelectedRoutes, mSelectableRoutes, mDeselectableRoutes, mTransferableRoutes,
				mVolumeMax, mVolumeHandling, mVolume, mTransferReason);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder()
				.append("RoutingSessionInfo{ ")
				.append("sessionId=").append(getId())
				.append(", name=").append(getName())
				.append(", clientPackageName=").append(getClientPackageName())
				.append(", selectedRoutes={")
				.append(String.join(",", getSelectedRoutes()))
				.append("}")
				.append(", selectableRoutes={")
				.append(String.join(",", getSelectableRoutes()))
				.append("}")
				.append(", deselectableRoutes={")
				.append(String.join(",", getDeselectableRoutes()))
				.append("}")
				.append(", transferableRoutes={")
				.append(String.join(",", getTransferableRoutes()))
				.append("}")
				.append(", volumeHandling=").append(getVolumeHandling())
				.append(", volumeMax=").append(getVolumeMax())
				.append(", volume=").append(getVolume())
				.append(" }");
		return result.toString();
	}

	private List<String> convertToUniqueRouteIds(List<String> routeIds) {
		Objects.requireNonNull(routeIds, "RouteIds cannot be null.");
		if (TextUtils.isEmpty(mProviderId)) {
			return new ArrayList<>(routeIds);
		}

		List<String> result = new ArrayList<>();
		for (String routeId : routeIds) {
			result.add(MediaRouter2Utils.toUniqueId(mProviderId, routeId));
		}
		return result;
	}

	public static final class Builder {
		final String mId;
		CharSequence mName;
		String mOwnerPackageName;
		String mClientPackageName;
		String mProviderId;
		final List<String> mSelectedRoutes;
		final List<String> mSelectableRoutes;
		final List<String> mDeselectableRoutes;
		final List<String> mTransferableRoutes;
		int mVolumeHandling = MediaRoute2Info.PLAYBACK_VOLUME_FIXED;
		int mVolumeMax;
		int mVolume;
		Bundle mControlHints;
		boolean mIsSystemSession;
		int mTransferReason = TRANSFER_REASON_FALLBACK;

		public Builder(String id, String clientPackageName) {
			if (TextUtils.isEmpty(id)) {
				throw new IllegalArgumentException("id must not be empty");
			}

			mId = id;
			mClientPackageName =
					Objects.requireNonNull(clientPackageName, "clientPackageName must not be null");
			mSelectedRoutes = new ArrayList<>();
			mSelectableRoutes = new ArrayList<>();
			mDeselectableRoutes = new ArrayList<>();
			mTransferableRoutes = new ArrayList<>();
		}

		public Builder(RoutingSessionInfo sessionInfo) {
			Objects.requireNonNull(sessionInfo, "sessionInfo must not be null");

			mId = sessionInfo.mId;
			mName = sessionInfo.mName;
			mClientPackageName = sessionInfo.mClientPackageName;
			mProviderId = sessionInfo.mProviderId;

			mSelectedRoutes = new ArrayList<>(sessionInfo.mSelectedRoutes);
			mSelectableRoutes = new ArrayList<>(sessionInfo.mSelectableRoutes);
			mDeselectableRoutes = new ArrayList<>(sessionInfo.mDeselectableRoutes);
			mTransferableRoutes = new ArrayList<>(sessionInfo.mTransferableRoutes);

			if (mProviderId != null) {
				mSelectedRoutes.replaceAll(MediaRouter2Utils::getOriginalId);
				mSelectableRoutes.replaceAll(MediaRouter2Utils::getOriginalId);
				mDeselectableRoutes.replaceAll(MediaRouter2Utils::getOriginalId);
				mTransferableRoutes.replaceAll(MediaRouter2Utils::getOriginalId);
			}

			mVolumeHandling = sessionInfo.mVolumeHandling;
			mVolumeMax = sessionInfo.mVolumeMax;
			mVolume = sessionInfo.mVolume;

			mControlHints = sessionInfo.mControlHints;
			mIsSystemSession = sessionInfo.mIsSystemSession;
			mTransferReason = sessionInfo.mTransferReason;
		}

		public Builder setName(CharSequence name) {
			mName = name;
			return this;
		}

		public Builder setOwnerPackageName(String packageName) {
			mOwnerPackageName = packageName;
			return this;
		}

		public Builder setClientPackageName(String packageName) {
			mClientPackageName = packageName;
			return this;
		}

		public Builder setProviderId(String providerId) {
			if (TextUtils.isEmpty(providerId)) {
				throw new IllegalArgumentException("providerId must not be empty");
			}
			mProviderId = providerId;
			return this;
		}

		public Builder clearSelectedRoutes() {
			mSelectedRoutes.clear();
			return this;
		}

		public Builder addSelectedRoute(String routeId) {
			if (TextUtils.isEmpty(routeId)) {
				throw new IllegalArgumentException("routeId must not be empty");
			}
			mSelectedRoutes.add(routeId);
			return this;
		}

		public Builder removeSelectedRoute(String routeId) {
			if (TextUtils.isEmpty(routeId)) {
				throw new IllegalArgumentException("routeId must not be empty");
			}
			mSelectedRoutes.remove(routeId);
			return this;
		}

		public Builder clearSelectableRoutes() {
			mSelectableRoutes.clear();
			return this;
		}

		public Builder addSelectableRoute(String routeId) {
			if (TextUtils.isEmpty(routeId)) {
				throw new IllegalArgumentException("routeId must not be empty");
			}
			mSelectableRoutes.add(routeId);
			return this;
		}

		public Builder removeSelectableRoute(String routeId) {
			if (TextUtils.isEmpty(routeId)) {
				throw new IllegalArgumentException("routeId must not be empty");
			}
			mSelectableRoutes.remove(routeId);
			return this;
		}

		public Builder clearDeselectableRoutes() {
			mDeselectableRoutes.clear();
			return this;
		}

		public Builder addDeselectableRoute(String routeId) {
			if (TextUtils.isEmpty(routeId)) {
				throw new IllegalArgumentException("routeId must not be empty");
			}
			mDeselectableRoutes.add(routeId);
			return this;
		}

		public Builder removeDeselectableRoute(String routeId) {
			if (TextUtils.isEmpty(routeId)) {
				throw new IllegalArgumentException("routeId must not be empty");
			}
			mDeselectableRoutes.remove(routeId);
			return this;
		}

		public Builder clearTransferableRoutes() {
			mTransferableRoutes.clear();
			return this;
		}

		public Builder addTransferableRoute(String routeId) {
			if (TextUtils.isEmpty(routeId)) {
				throw new IllegalArgumentException("routeId must not be empty");
			}
			mTransferableRoutes.add(routeId);
			return this;
		}

		public Builder removeTransferableRoute(String routeId) {
			if (TextUtils.isEmpty(routeId)) {
				throw new IllegalArgumentException("routeId must not be empty");
			}
			mTransferableRoutes.remove(routeId);
			return this;
		}

		public RoutingSessionInfo.Builder setVolumeHandling(
				int volumeHandling) {
			mVolumeHandling = volumeHandling;
			return this;
		}

		public RoutingSessionInfo.Builder setVolumeMax(int volumeMax) {
			mVolumeMax = volumeMax;
			return this;
		}

		public RoutingSessionInfo.Builder setVolume(int volume) {
			mVolume = volume;
			return this;
		}

		public Builder setControlHints(Bundle controlHints) {
			mControlHints = controlHints;
			return this;
		}

		public Builder setSystemSession(boolean isSystemSession) {
			mIsSystemSession = isSystemSession;
			return this;
		}

		public Builder setTransferReason(int transferReason) {
			mTransferReason = transferReason;
			return this;
		}

		public RoutingSessionInfo build() {
			if (mSelectedRoutes.isEmpty()) {
				throw new IllegalArgumentException("selectedRoutes must not be empty");
			}
			return new RoutingSessionInfo(this);
		}
	}
}
