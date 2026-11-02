package android.content.pm.verify.domain;

import android.os.UserHandle;
import java.util.Map;
import java.util.UUID;

/* AOSP core/java/android/content/pm/verify/domain/DomainVerificationUserState.java (android14-release),
 * the generated DataClass members without parcelling (ATL has no domain verification service to talk to) */
public final class DomainVerificationUserState {
	public static final int DOMAIN_STATE_NONE = 0;
	public static final int DOMAIN_STATE_SELECTED = 1;
	public static final int DOMAIN_STATE_VERIFIED = 2;

	private final UUID mIdentifier;
	private final String mPackageName;
	private final UserHandle mUser;
	private final boolean mLinkHandlingAllowed;
	private final Map<String, Integer> mHostToStateMap;

	public DomainVerificationUserState(UUID identifier, String packageName, UserHandle user,
	                                   boolean linkHandlingAllowed, Map<String, Integer> hostToStateMap) {
		mIdentifier = identifier;
		mPackageName = packageName;
		mUser = user;
		mLinkHandlingAllowed = linkHandlingAllowed;
		mHostToStateMap = hostToStateMap;
	}

	public UUID getIdentifier() {
		return mIdentifier;
	}

	public String getPackageName() {
		return mPackageName;
	}

	public UserHandle getUser() {
		return mUser;
	}

	public boolean isLinkHandlingAllowed() {
		return mLinkHandlingAllowed;
	}

	public Map<String, Integer> getHostToStateMap() {
		return mHostToStateMap;
	}

	public static String domainStateToString(int value) {
		switch (value) {
			case DOMAIN_STATE_NONE:
				return "DOMAIN_STATE_NONE";
			case DOMAIN_STATE_SELECTED:
				return "DOMAIN_STATE_SELECTED";
			case DOMAIN_STATE_VERIFIED:
				return "DOMAIN_STATE_VERIFIED";
			default:
				return Integer.toHexString(value);
		}
	}

	@Override
	public String toString() {
		return "DomainVerificationUserState { identifier = " + mIdentifier + ", packageName = " + mPackageName
		    + ", user = " + mUser + ", linkHandlingAllowed = " + mLinkHandlingAllowed
		    + ", hostToStateMap = " + mHostToStateMap + " }";
	}
}
