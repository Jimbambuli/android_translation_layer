package android.media;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* AOSP modules/Media apex/framework/java/android/media/ApplicationMediaCapabilities.java (android-16) */
public final class ApplicationMediaCapabilities implements Parcelable {
	private static final String TAG = "ApplicationMediaCapabilities";

	private Set<String> mSupportedVideoMimeTypes = new HashSet<>();

	private Set<String> mUnsupportedVideoMimeTypes = new HashSet<>();

	private Set<String> mSupportedHdrTypes = new HashSet<>();

	private Set<String> mUnsupportedHdrTypes = new HashSet<>();

	private boolean mIsSlowMotionSupported = false;

	private ApplicationMediaCapabilities(Builder b) {
		mSupportedVideoMimeTypes.addAll(b.getSupportedVideoMimeTypes());
		mUnsupportedVideoMimeTypes.addAll(b.getUnsupportedVideoMimeTypes());
		mSupportedHdrTypes.addAll(b.getSupportedHdrTypes());
		mUnsupportedHdrTypes.addAll(b.getUnsupportedHdrTypes());
		mIsSlowMotionSupported = b.mIsSlowMotionSupported;
	}

	public boolean isVideoMimeTypeSupported(
			String videoMime) {
		if (mSupportedVideoMimeTypes.contains(videoMime.toLowerCase())) {
			return true;
		}
		return false;
	}

	public boolean isHdrTypeSupported(
			String hdrType) {
		if (mSupportedHdrTypes.contains(hdrType)) {
			return true;
		}
		return false;
	}

	public boolean isFormatSpecified(String format) {
		if (mSupportedVideoMimeTypes.contains(format) || mUnsupportedVideoMimeTypes.contains(format)
				|| mSupportedHdrTypes.contains(format) || mUnsupportedHdrTypes.contains(format)) {
			return true;

		}
		return false;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mSupportedVideoMimeTypes.size());
		for (String cap : mSupportedVideoMimeTypes) {
			dest.writeString(cap);
		}
		dest.writeInt(mUnsupportedVideoMimeTypes.size());
		for (String cap : mUnsupportedVideoMimeTypes) {
			dest.writeString(cap);
		}
		dest.writeInt(mSupportedHdrTypes.size());
		for (String cap : mSupportedHdrTypes) {
			dest.writeString(cap);
		}
		dest.writeInt(mUnsupportedHdrTypes.size());
		for (String cap : mUnsupportedHdrTypes) {
			dest.writeString(cap);
		}
		dest.writeBoolean(mIsSlowMotionSupported);
	}

	@Override
	public String toString() {
		String caps = new String(
				"Supported Video MimeTypes: " + mSupportedVideoMimeTypes.toString());
		caps += "Unsupported Video MimeTypes: " + mUnsupportedVideoMimeTypes.toString();
		caps += "Supported HDR types: " + mSupportedHdrTypes.toString();
		caps += "Unsupported HDR types: " + mUnsupportedHdrTypes.toString();
		caps += "Supported slow motion: " + mIsSlowMotionSupported;
		return caps;
	}

	public static final Creator<ApplicationMediaCapabilities> CREATOR =
			new Creator<ApplicationMediaCapabilities>() {
				public ApplicationMediaCapabilities createFromParcel(Parcel in) {
					ApplicationMediaCapabilities.Builder builder =
							new ApplicationMediaCapabilities.Builder();
					int count = in.readInt();
					for (int readCount = 0; readCount < count; ++readCount) {
						builder.addSupportedVideoMimeType(in.readString());
					}
					count = in.readInt();
					for (int readCount = 0; readCount < count; ++readCount) {
						builder.addUnsupportedVideoMimeType(in.readString());
					}
					count = in.readInt();
					for (int readCount = 0; readCount < count; ++readCount) {
						builder.addSupportedHdrType(in.readString());
					}
					count = in.readInt();
					for (int readCount = 0; readCount < count; ++readCount) {
						builder.addUnsupportedHdrType(in.readString());
					}

					boolean supported = in.readBoolean();
					builder.setSlowMotionSupported(supported);

					return builder.build();
				}

				public ApplicationMediaCapabilities[] newArray(int size) {
					return new ApplicationMediaCapabilities[size];
				}
			};

	public List<String> getSupportedVideoMimeTypes() {
		return new ArrayList<>(mSupportedVideoMimeTypes);
	}

	public List<String> getUnsupportedVideoMimeTypes() {
		return new ArrayList<>(mUnsupportedVideoMimeTypes);
	}

	public List<String> getSupportedHdrTypes() {
		return new ArrayList<>(mSupportedHdrTypes);
	}

	public List<String> getUnsupportedHdrTypes()  {
		return new ArrayList<>(mUnsupportedHdrTypes);
	}

	public boolean isSlowMotionSupported() {
		return mIsSlowMotionSupported;
	}

	public static ApplicationMediaCapabilities createFromXml(XmlPullParser xmlParser) {
		ApplicationMediaCapabilities.Builder builder = new ApplicationMediaCapabilities.Builder();
		builder.parseXml(xmlParser);
		return builder.build();
	}

	public final static class Builder {
		
		private Set<String> mSupportedVideoMimeTypes = new HashSet<>();

		private Set<String> mSupportedHdrTypes = new HashSet<>();

		private Set<String> mUnsupportedVideoMimeTypes = new HashSet<>();

		private Set<String> mUnsupportedHdrTypes = new HashSet<>();

		private boolean mIsSlowMotionSupported = false;

		private Map<String, Boolean> mFormatSupportedMap =  new HashMap<String, Boolean>();

		public Builder() {
		}

		private void parseXml(XmlPullParser xmlParser)
				throws UnsupportedOperationException {
			if (xmlParser == null) {
				throw new IllegalArgumentException("XmlParser must not be null");
			}

			try {
				while (xmlParser.next() != XmlPullParser.START_TAG) {
					continue;
				}
				if (!xmlParser.getName().equals("media-capabilities")) {
					throw new UnsupportedOperationException("Invalid tag");
				}

				xmlParser.next();
				while (xmlParser.getEventType() != XmlPullParser.END_TAG) {
					while (xmlParser.getEventType() != XmlPullParser.START_TAG) {
						if (xmlParser.getEventType() == XmlPullParser.END_DOCUMENT) {
							return;
						}
						xmlParser.next();
					}
					if (xmlParser.getName().equals("format")) {
						parseFormatTag(xmlParser);
					} else {
						throw new UnsupportedOperationException("Invalid tag");
					}
					while (xmlParser.getEventType() != XmlPullParser.END_TAG) {
						xmlParser.next();
					}
					xmlParser.next();
				}
			} catch (XmlPullParserException xppe) {
				throw new UnsupportedOperationException("Ill-formatted xml file");
			} catch (java.io.IOException ioe) {
				throw new UnsupportedOperationException("Unable to read xml file");
			}
		}

		private void parseFormatTag(XmlPullParser xmlParser) {
			String name = null;
			String supported = null;
			for (int i = 0; i < xmlParser.getAttributeCount(); i++) {
				String attrName = xmlParser.getAttributeName(i);
				if (attrName.equals("name")) {
					name = xmlParser.getAttributeValue(i);
				} else if (attrName.equals("supported")) {
					supported = xmlParser.getAttributeValue(i);
				} else {
					throw new UnsupportedOperationException("Invalid attribute name " + attrName);
				}
			}

			if (name != null && supported != null) {
				if (!supported.equals("true") && !supported.equals("false")) {
					throw new UnsupportedOperationException(
							("Supported value must be either true or false"));
				}
				boolean isSupported = Boolean.parseBoolean(supported);
				if (mFormatSupportedMap.get(name) != null && mFormatSupportedMap.get(name)
						!= isSupported) {
					throw new UnsupportedOperationException(
							"Format: " + name + " has conflict supported value");
				}

				switch (name) {
					case "HEVC":
						if (isSupported) {
							mSupportedVideoMimeTypes.add(MediaFormat.MIMETYPE_VIDEO_HEVC);
						} else {
							mUnsupportedVideoMimeTypes.add(MediaFormat.MIMETYPE_VIDEO_HEVC);
						}
						break;
					case "VP9":
						if (isSupported) {
							mSupportedVideoMimeTypes.add(MediaFormat.MIMETYPE_VIDEO_VP9);
						} else {
							mUnsupportedVideoMimeTypes.add(MediaFormat.MIMETYPE_VIDEO_VP9);
						}
						break;
					case "AV1":
						if (isSupported) {
							mSupportedVideoMimeTypes.add(MediaFormat.MIMETYPE_VIDEO_AV1);
						} else {
							mUnsupportedVideoMimeTypes.add(MediaFormat.MIMETYPE_VIDEO_AV1);
						}
						break;
					case "HDR10":
						if (isSupported) {
							mSupportedHdrTypes.add(MediaFeature.HdrType.HDR10);
						} else {
							mUnsupportedHdrTypes.add(MediaFeature.HdrType.HDR10);
						}
						break;
					case "HDR10Plus":
						if (isSupported) {
							mSupportedHdrTypes.add(MediaFeature.HdrType.HDR10_PLUS);
						} else {
							mUnsupportedHdrTypes.add(MediaFeature.HdrType.HDR10_PLUS);
						}
						break;
					case "Dolby-Vision":
						if (isSupported) {
							mSupportedHdrTypes.add(MediaFeature.HdrType.DOLBY_VISION);
						} else {
							mUnsupportedHdrTypes.add(MediaFeature.HdrType.DOLBY_VISION);
						}
						break;
					case "HLG":
						if (isSupported) {
							mSupportedHdrTypes.add(MediaFeature.HdrType.HLG);
						} else {
							mUnsupportedHdrTypes.add(MediaFeature.HdrType.HLG);
						}
						break;
					case "SlowMotion":
						mIsSlowMotionSupported = isSupported;
						break;
					default:
						Log.w(TAG, "Invalid format name " + name);
				}
				mFormatSupportedMap.put(name, isSupported);
			} else {
				throw new UnsupportedOperationException(
						"Format name and supported must both be specified");
			}
		}

		public ApplicationMediaCapabilities build() {
			Log.d(TAG,
					"Building ApplicationMediaCapabilities with: (Supported HDR: "
							+ mSupportedHdrTypes.toString() + " Unsupported HDR: "
							+ mUnsupportedHdrTypes.toString() + ") (Supported Codec: "
							+ " " + mSupportedVideoMimeTypes.toString() + " Unsupported Codec:"
							+ mUnsupportedVideoMimeTypes.toString() + ") "
							+ mIsSlowMotionSupported);
			if (!mSupportedHdrTypes.isEmpty() && !mSupportedVideoMimeTypes.contains(
					MediaFormat.MIMETYPE_VIDEO_HEVC)) {
				throw new UnsupportedOperationException("Only support HEVC mime type");
			}
			return new ApplicationMediaCapabilities(this);
		}

		public Builder addSupportedVideoMimeType(
				String codecMime) {
			mSupportedVideoMimeTypes.add(codecMime);
			return this;
		}

		private List<String> getSupportedVideoMimeTypes() {
			return new ArrayList<>(mSupportedVideoMimeTypes);
		}

		private boolean isValidVideoCodecMimeType(String codecMime) {
			if (!codecMime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_HEVC)
					&& !codecMime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_VP9)
					&& !codecMime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_AV1)) {
				return false;
			}
			return true;
		}

		public Builder addUnsupportedVideoMimeType(
				String codecMime) {
			if (!isValidVideoCodecMimeType(codecMime)) {
				throw new IllegalArgumentException("Invalid codec mime type: " + codecMime);
			}
			mUnsupportedVideoMimeTypes.add(codecMime);
			return this;
		}

		private List<String> getUnsupportedVideoMimeTypes() {
			return new ArrayList<>(mUnsupportedVideoMimeTypes);
		}

		public Builder addSupportedHdrType(
				String hdrType) {
			if (!isValidVideoCodecHdrType(hdrType)) {
				throw new IllegalArgumentException("Invalid hdr type: " + hdrType);
			}
			mSupportedHdrTypes.add(hdrType);
			return this;
		}

		private List<String> getSupportedHdrTypes() {
			return new ArrayList<>(mSupportedHdrTypes);
		}

		private boolean isValidVideoCodecHdrType(String hdrType) {
			if (!hdrType.equals(MediaFeature.HdrType.DOLBY_VISION)
					&& !hdrType.equals(MediaFeature.HdrType.HDR10)
					&& !hdrType.equals(MediaFeature.HdrType.HDR10_PLUS)
					&& !hdrType.equals(MediaFeature.HdrType.HLG)) {
				return false;
			}
			return true;
		}

		public Builder addUnsupportedHdrType(
				String hdrType) {
			if (!isValidVideoCodecHdrType(hdrType)) {
				throw new IllegalArgumentException("Invalid hdr type: " + hdrType);
			}
			mUnsupportedHdrTypes.add(hdrType);
			return this;
		}

		private List<String> getUnsupportedHdrTypes() {
			return new ArrayList<>(mUnsupportedHdrTypes);
		}

		public Builder setSlowMotionSupported(boolean slowMotionSupported) {
			mIsSlowMotionSupported = slowMotionSupported;
			return this;
		}
	}
}
