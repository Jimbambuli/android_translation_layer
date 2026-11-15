package android.net.sip;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.Serializable;
import java.text.ParseException;

/* android.net.sip.SipProfile (framework-current.txt). The AOSP class keeps a javax.sip Address; ATL keeps the same
 * information as plain strings. */
@Deprecated
public class SipProfile implements Parcelable, Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_PORT = 5060;
	private static final String UDP = "UDP";
	private static final String TCP = "TCP";

	private String mUserName;
	private String mDomain;
	private String mDisplayName;
	private String mProxyAddress;
	private String mPassword;
	private String mAuthUserName;
	private String mProtocol = UDP;
	private String mProfileName;
	private boolean mSendKeepAlive = false;
	private boolean mAutoRegistration = true;
	private int mCallingUid = 0;
	private int mPort = DEFAULT_PORT;

	public static final Parcelable.Creator<SipProfile> CREATOR = new Parcelable.Creator<SipProfile>() {
		@Override
		public SipProfile createFromParcel(Parcel in) {
			return new SipProfile(in);
		}

		@Override
		public SipProfile[] newArray(int size) {
			return new SipProfile[size];
		}
	};

	public static class Builder {
		private SipProfile mProfile = new SipProfile();

		public Builder(SipProfile profile) {
			if (profile == null)
				throw new NullPointerException();
			try {
				mProfile = (SipProfile)profile.clone();
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException("should not occur", e);
			}
			mProfile.mCallingUid = 0;
		}

		public Builder(String uriString) throws ParseException {
			if (uriString == null)
				throw new NullPointerException("uriString cannot be null");
			if (!uriString.startsWith("sip:"))
				throw new ParseException(uriString + " is not a SIP URI", 0);
			String rest = uriString.substring(4);
			int at = rest.indexOf('@');
			if (at <= 0 || at == rest.length() - 1)
				throw new ParseException("invalid SIP URI: " + uriString, 0);
			mProfile.mUserName = rest.substring(0, at);
			mProfile.mDomain = rest.substring(at + 1);
		}

		public Builder(String username, String serverDomain) throws ParseException {
			if (username == null || serverDomain == null) {
				throw new NullPointerException("username and serverDomain cannot be null");
			}
			if (username.isEmpty() || serverDomain.isEmpty())
				throw new ParseException("invalid SIP URI: sip:" + username + "@" + serverDomain, 0);
			mProfile.mUserName = username;
			mProfile.mDomain = serverDomain;
		}

		public Builder setProfileName(String name) {
			mProfile.mProfileName = name;
			return this;
		}

		public Builder setPassword(String password) {
			mProfile.mPassword = password;
			return this;
		}

		public Builder setPort(int port) throws IllegalArgumentException {
			if ((port > 65535) || (port < 1000)) {
				throw new IllegalArgumentException("incorrect port arguments: " + port);
			}
			mProfile.mPort = port;
			return this;
		}

		public Builder setProtocol(String protocol) throws IllegalArgumentException {
			if (protocol == null)
				throw new NullPointerException("protocol cannot be null");
			protocol = protocol.toUpperCase();
			if (!protocol.equals(UDP) && !protocol.equals(TCP)) {
				throw new IllegalArgumentException("unsupported protocol: " + protocol);
			}
			mProfile.mProtocol = protocol;
			return this;
		}

		public Builder setOutboundProxy(String outboundProxy) {
			mProfile.mProxyAddress = outboundProxy;
			return this;
		}

		public Builder setDisplayName(String displayName) {
			mProfile.mDisplayName = displayName;
			return this;
		}

		public Builder setSendKeepAlive(boolean flag) {
			mProfile.mSendKeepAlive = flag;
			return this;
		}

		public Builder setAutoRegistration(boolean flag) {
			mProfile.mAutoRegistration = flag;
			return this;
		}

		public Builder setAuthUserName(String name) {
			mProfile.mAuthUserName = name;
			return this;
		}

		public SipProfile build() {
			try {
				return (SipProfile)mProfile.clone();
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException("should not occur", e);
			}
		}
	}

	private SipProfile() {
	}

	private SipProfile(Parcel in) {
		mUserName = in.readString();
		mDomain = in.readString();
		mDisplayName = in.readString();
		mProxyAddress = in.readString();
		mPassword = in.readString();
		mProtocol = in.readString();
		mProfileName = in.readString();
		mSendKeepAlive = (in.readInt() == 0) ? false : true;
		mAutoRegistration = (in.readInt() == 0) ? false : true;
		mCallingUid = in.readInt();
		mPort = in.readInt();
		mAuthUserName = in.readString();
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(mUserName);
		out.writeString(mDomain);
		out.writeString(mDisplayName);
		out.writeString(mProxyAddress);
		out.writeString(mPassword);
		out.writeString(mProtocol);
		out.writeString(mProfileName);
		out.writeInt(mSendKeepAlive ? 1 : 0);
		out.writeInt(mAutoRegistration ? 1 : 0);
		out.writeInt(mCallingUid);
		out.writeInt(mPort);
		out.writeString(mAuthUserName);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public String getUriString() {
		/* with an outbound proxy the URI carries neither transport nor port */
		if (mProxyAddress != null && !mProxyAddress.isEmpty()) {
			return "sip:" + getUserName() + "@" + getSipDomain();
		}
		return "sip:" + mUserName + "@" + mDomain + (mPort != DEFAULT_PORT ? ":" + mPort : "");
	}

	public String getUserName() {
		return mUserName;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public String getSipDomain() {
		return mDomain;
	}

	public int getPort() {
		return mPort;
	}

	public String getProtocol() {
		return mProtocol;
	}

	public String getProxyAddress() {
		return mProxyAddress;
	}

	public String getPassword() {
		return mPassword;
	}

	public String getProfileName() {
		return mProfileName;
	}

	public String getAuthUserName() {
		return mAuthUserName;
	}

	public boolean getSendKeepAlive() {
		return mSendKeepAlive;
	}

	public boolean getAutoRegistration() {
		return mAutoRegistration;
	}

	public void setCallingUid(int uid) {
		mCallingUid = uid;
	}
}
