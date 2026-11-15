/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.service.notification;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/**
 * The current condition of an {@link android.app.AutomaticZenRule}, provided by a
 * {@link ConditionProviderService}.
 * AOSP core/java/android/service/notification/Condition.java (android-16, modes API enabled). dumpDebug() (proto transport) is left out.
 */
public final class Condition implements Parcelable {

	public static final String SCHEME = "condition";

	public @interface State {}

	public static final int STATE_FALSE = 0;
	public static final int STATE_TRUE = 1;
	public static final int STATE_UNKNOWN = 2;
	public static final int STATE_ERROR = 3;

	public static final int FLAG_RELEVANT_NOW = 1 << 0;
	public static final int FLAG_RELEVANT_ALWAYS = 1 << 1;

	public final Uri id;
	public final String summary;
	public final String line1;
	public final String line2;
	@State
	public final int state;
	public final int flags;
	public final int icon;

	public @interface Source {}

	public static final int SOURCE_UNKNOWN = 0;
	public static final int SOURCE_USER_ACTION = 1;
	public static final int SOURCE_SCHEDULE = 2;
	public static final int SOURCE_CONTEXT = 3;

	public final @Source int source;

	/* AOSP Condition.MAX_STRING_LENGTH */
	public static final int MAX_STRING_LENGTH = 1000;

	public Condition(Uri id, String summary, int state) {
		this(id, summary, "", "", -1, state, SOURCE_UNKNOWN, FLAG_RELEVANT_ALWAYS);
	}

	public Condition(@Nullable Uri id, @Nullable String summary, @State int state,
	                 @Source int source) {
		this(id, summary, "", "", -1, state, source, FLAG_RELEVANT_ALWAYS);
	}

	public Condition(Uri id, String summary, String line1, String line2, int icon,
	                 int state, int flags) {
		this(id, summary, line1, line2, icon, state, SOURCE_UNKNOWN, flags);
	}

	public Condition(@Nullable Uri id, @Nullable String summary, @Nullable String line1,
	                 @Nullable String line2, int icon, @State int state, @Source int source,
	                 int flags) {
		if (id == null) throw new IllegalArgumentException("id is required");
		if (summary == null) throw new IllegalArgumentException("summary is required");
		if (!isValidState(state)) throw new IllegalArgumentException("state is invalid: " + state);
		this.id = getTrimmedUri(id);
		this.summary = getTrimmedString(summary);
		this.line1 = getTrimmedString(line1);
		this.line2 = getTrimmedString(line2);
		this.icon = icon;
		this.state = state;
		this.source = checkValidSource(source);
		this.flags = flags;
	}

	/* AOSP Condition(Parcel): the fields in constructor order, strings trimmed by the full constructor */
	public Condition(Parcel source) {
		this((Uri)source.readParcelable(Condition.class.getClassLoader(), android.net.Uri.class),
		     source.readString(), source.readString(), source.readString(), source.readInt(), source.readInt(),
		     source.readInt(), source.readInt());
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(id, 0);
		dest.writeString(summary);
		dest.writeString(line1);
		dest.writeString(line2);
		dest.writeInt(icon);
		dest.writeInt(state);
		dest.writeInt(this.source);
		dest.writeInt(this.flags);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public Condition copy() {
		final Parcel parcel = Parcel.obtain();
		try {
			writeToParcel(parcel, 0);
			parcel.setDataPosition(0);
			return new Condition(parcel);
		} finally {
			parcel.recycle();
		}
	}

	public static final @NonNull Parcelable.Creator<Condition> CREATOR = new Parcelable.Creator<Condition>() {
		@Override
		public Condition createFromParcel(Parcel source) {
			return new Condition(source);
		}

		@Override
		public Condition[] newArray(int size) {
			return new Condition[size];
		}
	};

	public void validate() {
		checkValidSource(source);
	}

	private static boolean isValidState(int state) {
		return state >= STATE_FALSE && state <= STATE_ERROR;
	}

	/* AOSP Condition.checkValidSource (Preconditions.checkArgument) */
	private static int checkValidSource(@Source int source) {
		if (!(source >= SOURCE_UNKNOWN && source <= SOURCE_CONTEXT)) {
			throw new IllegalArgumentException(
					"Condition source must be one of SOURCE_UNKNOWN, SOURCE_USER_ACTION, "
							+ "SOURCE_SCHEDULE, or SOURCE_CONTEXT");
		}
		return source;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(Condition.class.getSimpleName()).append('[')
				.append("state=").append(stateToString(state))
				.append(",id=").append(id)
				.append(",summary=").append(summary)
				.append(",line1=").append(line1)
				.append(",line2=").append(line2)
				.append(",icon=").append(icon)
				.append(",source=").append(sourceToString(source));
		return sb.append(",flags=").append(flags)
				.append(']').toString();
	}

	public static String stateToString(int state) {
		if (state == STATE_FALSE) return "STATE_FALSE";
		if (state == STATE_TRUE) return "STATE_TRUE";
		if (state == STATE_UNKNOWN) return "STATE_UNKNOWN";
		if (state == STATE_ERROR) return "STATE_ERROR";
		throw new IllegalArgumentException("state is invalid: " + state);
	}

	public static @NonNull String sourceToString(@Source int source) {
		if (source == SOURCE_UNKNOWN) return "SOURCE_UNKNOWN";
		if (source == SOURCE_USER_ACTION) return "SOURCE_USER_ACTION";
		if (source == SOURCE_SCHEDULE) return "SOURCE_SCHEDULE";
		if (source == SOURCE_CONTEXT) return "SOURCE_CONTEXT";
		throw new IllegalArgumentException("source is invalid: " + source);
	}

	public static String relevanceToString(int flags) {
		final boolean now = (flags & FLAG_RELEVANT_NOW) != 0;
		final boolean always = (flags & FLAG_RELEVANT_ALWAYS) != 0;
		if (!now && !always) return "NONE";
		if (now && always) return "NOW, ALWAYS";
		return now ? "NOW" : "ALWAYS";
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof Condition)) return false;
		if (o == this) return true;
		final Condition other = (Condition) o;
		return Objects.equals(other.id, id)
				&& Objects.equals(other.summary, summary)
				&& Objects.equals(other.line1, line1)
				&& Objects.equals(other.line2, line2)
				&& other.icon == icon
				&& other.state == state
				&& other.flags == flags
				&& other.source == source;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, summary, line1, line2, icon, state, source, flags);
	}

	public static Uri.Builder newId(Context context) {
		return new Uri.Builder()
				.scheme(Condition.SCHEME)
				.authority(context.getPackageName());
	}

	public static boolean isValidId(Uri id, String pkg) {
		return id != null && SCHEME.equals(id.getScheme()) && pkg.equals(id.getAuthority());
	}

	private static String getTrimmedString(String input) {
		if (input != null && input.length() > MAX_STRING_LENGTH) {
			return input.substring(0, MAX_STRING_LENGTH);
		}
		return input;
	}

	private static Uri getTrimmedUri(Uri input) {
		if (input != null && input.toString().length() > MAX_STRING_LENGTH) {
			return Uri.parse(getTrimmedString(input.toString()));
		}
		return input;
	}
}
