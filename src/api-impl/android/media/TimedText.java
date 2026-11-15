package android.media;

/* AOSP media/java/android/media/TimedText.java */
import android.graphics.Rect;
import android.os.Parcel;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public final class TimedText
{
	private static final int FIRST_PUBLIC_KEY				 = 1;

	// These keys must be in sync with the keys in TextDescription.h
	private static final int KEY_DISPLAY_FLAGS				 = 1; // int
	private static final int KEY_STYLE_FLAGS				   = 2; // int
	private static final int KEY_BACKGROUND_COLOR_RGBA		 = 3; // int
	private static final int KEY_HIGHLIGHT_COLOR_RGBA		  = 4; // int
	private static final int KEY_SCROLL_DELAY				  = 5; // int
	private static final int KEY_WRAP_TEXT					 = 6; // int
	private static final int KEY_START_TIME					= 7; // int
	private static final int KEY_STRUCT_BLINKING_TEXT_LIST	 = 8; // List<CharPos>
	private static final int KEY_STRUCT_FONT_LIST			  = 9; // List<Font>
	private static final int KEY_STRUCT_HIGHLIGHT_LIST		 = 10; // List<CharPos>
	private static final int KEY_STRUCT_HYPER_TEXT_LIST		= 11; // List<HyperText>
	private static final int KEY_STRUCT_KARAOKE_LIST		   = 12; // List<Karaoke>
	private static final int KEY_STRUCT_STYLE_LIST			 = 13; // List<Style>
	private static final int KEY_STRUCT_TEXT_POS			   = 14; // TextPos
	private static final int KEY_STRUCT_JUSTIFICATION		  = 15; // Justification
	private static final int KEY_STRUCT_TEXT				   = 16; // Text

	private static final int LAST_PUBLIC_KEY				  = 16;

	private static final int FIRST_PRIVATE_KEY				= 101;

	// The following keys are used between TimedText.java and
	// TextDescription.cpp in order to parce the Parcel.
	private static final int KEY_GLOBAL_SETTING			   = 101;
	private static final int KEY_LOCAL_SETTING				= 102;
	private static final int KEY_START_CHAR				   = 103;
	private static final int KEY_END_CHAR					 = 104;
	private static final int KEY_FONT_ID					  = 105;
	private static final int KEY_FONT_SIZE					= 106;
	private static final int KEY_TEXT_COLOR_RGBA			  = 107;

	private static final int LAST_PRIVATE_KEY				 = 107;

	private static final String TAG = "TimedText";

	private final HashMap<Integer, Object> mKeyObjectMap =
			new HashMap<Integer, Object>();

	private int mDisplayFlags = -1;
	private int mBackgroundColorRGBA = -1;
	private int mHighlightColorRGBA = -1;
	private int mScrollDelay = -1;
	private int mWrapText = -1;

	private List<CharPos> mBlinkingPosList = null;
	private List<CharPos> mHighlightPosList = null;
	private List<Karaoke> mKaraokeList = null;
	private List<Font> mFontList = null;
	private List<Style> mStyleList = null;
	private List<HyperText> mHyperTextList = null;

	private Rect mTextBounds = null;
	private String mTextChars = null;

	private Justification mJustification;

	public static final class CharPos {

		public final int startChar;

		public final int endChar;

		public CharPos(int startChar, int endChar) {
			this.startChar = startChar;
			this.endChar = endChar;
		}
	}

	public static final class Justification {

		public final int horizontalJustification;

		public final int verticalJustification;

		public Justification(int horizontal, int vertical) {
			this.horizontalJustification = horizontal;
			this.verticalJustification = vertical;
		}
	}

	public static final class Style {

		public final int startChar;

		public final int endChar;

		public final int fontID;

		public final boolean isBold;

		public final boolean isItalic;

		public final boolean isUnderlined;

		public final int fontSize;

		public final int colorRGBA;

		public Style(int startChar, int endChar, int fontId,
					 boolean isBold, boolean isItalic, boolean isUnderlined,
					 int fontSize, int colorRGBA) {
			this.startChar = startChar;
			this.endChar = endChar;
			this.fontID = fontId;
			this.isBold = isBold;
			this.isItalic = isItalic;
			this.isUnderlined = isUnderlined;
			this.fontSize = fontSize;
			this.colorRGBA = colorRGBA;
		}
	}

	public static final class Font {

		public final int ID;

		public final String name;

		public Font(int id, String name) {
			this.ID = id;
			this.name = name;
		}
	}

	public static final class Karaoke {

		public final int startTimeMs;

		public final int endTimeMs;

		public final int startChar;

		public final int endChar;

		public Karaoke(int startTimeMs, int endTimeMs, int startChar, int endChar) {
			this.startTimeMs = startTimeMs;
			this.endTimeMs = endTimeMs;
			this.startChar = startChar;
			this.endChar = endChar;
		}
	}

	public static final class HyperText {

		public final int startChar;

		public final int endChar;

		public final String URL;

		public final String altString;

		public HyperText(int startChar, int endChar, String url, String alt) {
			this.startChar = startChar;
			this.endChar = endChar;
			this.URL = url;
			this.altString = alt;
		}
	}

	public TimedText(Parcel parcel) {
		if (!parseParcel(parcel)) {
			mKeyObjectMap.clear();
			throw new IllegalArgumentException("parseParcel() fails");
		}
	}

	public TimedText(String text, Rect bounds) {
		mTextChars = text;
		mTextBounds = bounds;
	}

	public String getText() {
		return mTextChars;
	}

	public Rect getBounds() {
		return mTextBounds;
	}

	private boolean parseParcel(Parcel parcel) {
		parcel.setDataPosition(0);
		if (parcel.dataAvail() == 0) {
			return false;
		}

		int type = parcel.readInt();
		if (type == KEY_LOCAL_SETTING) {
			type = parcel.readInt();
			if (type != KEY_START_TIME) {
				return false;
			}
			int mStartTimeMs = parcel.readInt();
			mKeyObjectMap.put(type, mStartTimeMs);

			type = parcel.readInt();
			if (type != KEY_STRUCT_TEXT) {
				return false;
			}

			int textLen = parcel.readInt();
			byte[] text = parcel.createByteArray();
			if (text == null || text.length == 0) {
				mTextChars = null;
			} else {
				mTextChars = new String(text);
			}

		} else if (type != KEY_GLOBAL_SETTING) {
			Log.w(TAG, "Invalid timed text key found: " + type);
			return false;
		}

		while (parcel.dataAvail() > 0) {
			int key = parcel.readInt();
			if (!isValidKey(key)) {
				Log.w(TAG, "Invalid timed text key found: " + key);
				return false;
			}

			Object object = null;

			switch (key) {
				case KEY_STRUCT_STYLE_LIST: {
					readStyle(parcel);
					object = mStyleList;
					break;
				}
				case KEY_STRUCT_FONT_LIST: {
					readFont(parcel);
					object = mFontList;
					break;
				}
				case KEY_STRUCT_HIGHLIGHT_LIST: {
					readHighlight(parcel);
					object = mHighlightPosList;
					break;
				}
				case KEY_STRUCT_KARAOKE_LIST: {
					readKaraoke(parcel);
					object = mKaraokeList;
					break;
				}
				case KEY_STRUCT_HYPER_TEXT_LIST: {
					readHyperText(parcel);
					object = mHyperTextList;

					break;
				}
				case KEY_STRUCT_BLINKING_TEXT_LIST: {
					readBlinkingText(parcel);
					object = mBlinkingPosList;

					break;
				}
				case KEY_WRAP_TEXT: {
					mWrapText = parcel.readInt();
					object = mWrapText;
					break;
				}
				case KEY_HIGHLIGHT_COLOR_RGBA: {
					mHighlightColorRGBA = parcel.readInt();
					object = mHighlightColorRGBA;
					break;
				}
				case KEY_DISPLAY_FLAGS: {
					mDisplayFlags = parcel.readInt();
					object = mDisplayFlags;
					break;
				}
				case KEY_STRUCT_JUSTIFICATION: {

					int horizontal = parcel.readInt();
					int vertical = parcel.readInt();
					mJustification = new Justification(horizontal, vertical);

					object = mJustification;
					break;
				}
				case KEY_BACKGROUND_COLOR_RGBA: {
					mBackgroundColorRGBA = parcel.readInt();
					object = mBackgroundColorRGBA;
					break;
				}
				case KEY_STRUCT_TEXT_POS: {
					int top = parcel.readInt();
					int left = parcel.readInt();
					int bottom = parcel.readInt();
					int right = parcel.readInt();
					mTextBounds = new Rect(left, top, right, bottom);

					break;
				}
				case KEY_SCROLL_DELAY: {
					mScrollDelay = parcel.readInt();
					object = mScrollDelay;
					break;
				}
				default: {
					break;
				}
			}

			if (object != null) {
				if (mKeyObjectMap.containsKey(key)) {
					mKeyObjectMap.remove(key);
				}
				// Previous mapping will be replaced with the new object, if there was one.
				mKeyObjectMap.put(key, object);
			}
		}

		return true;
	}

	private void readStyle(Parcel parcel) {
		boolean endOfStyle = false;
		int startChar = -1;
		int endChar = -1;
		int fontId = -1;
		boolean isBold = false;
		boolean isItalic = false;
		boolean isUnderlined = false;
		int fontSize = -1;
		int colorRGBA = -1;
		while (!endOfStyle && (parcel.dataAvail() > 0)) {
			int key = parcel.readInt();
			switch (key) {
				case KEY_START_CHAR: {
					startChar = parcel.readInt();
					break;
				}
				case KEY_END_CHAR: {
					endChar = parcel.readInt();
					break;
				}
				case KEY_FONT_ID: {
					fontId = parcel.readInt();
					break;
				}
				case KEY_STYLE_FLAGS: {
					int flags = parcel.readInt();
					// In the absence of any bits set in flags, the text
					// is plain. Otherwise, 1: bold, 2: italic, 4: underline
					isBold = ((flags % 2) == 1);
					isItalic = ((flags % 4) >= 2);
					isUnderlined = ((flags / 4) == 1);
					break;
				}
				case KEY_FONT_SIZE: {
					fontSize = parcel.readInt();
					break;
				}
				case KEY_TEXT_COLOR_RGBA: {
					colorRGBA = parcel.readInt();
					break;
				}
				default: {
					// End of the Style parsing. Reset the data position back
					// to the position before the last parcel.readInt() call.
					parcel.setDataPosition(parcel.dataPosition() - 4);
					endOfStyle = true;
					break;
				}
			}
		}

		Style style = new Style(startChar, endChar, fontId, isBold,
								isItalic, isUnderlined, fontSize, colorRGBA);
		if (mStyleList == null) {
			mStyleList = new ArrayList<Style>();
		}
		mStyleList.add(style);
	}

	private void readFont(Parcel parcel) {
		int entryCount = parcel.readInt();

		for (int i = 0; i < entryCount; i++) {
			int id = parcel.readInt();
			int nameLen = parcel.readInt();

			byte[] text = parcel.createByteArray();
			final String name = new String(text, 0, nameLen);

			Font font = new Font(id, name);

			if (mFontList == null) {
				mFontList = new ArrayList<Font>();
			}
			mFontList.add(font);
		}
	}

	private void readHighlight(Parcel parcel) {
		int startChar = parcel.readInt();
		int endChar = parcel.readInt();
		CharPos pos = new CharPos(startChar, endChar);

		if (mHighlightPosList == null) {
			mHighlightPosList = new ArrayList<CharPos>();
		}
		mHighlightPosList.add(pos);
	}

	private void readKaraoke(Parcel parcel) {
		int entryCount = parcel.readInt();

		for (int i = 0; i < entryCount; i++) {
			int startTimeMs = parcel.readInt();
			int endTimeMs = parcel.readInt();
			int startChar = parcel.readInt();
			int endChar = parcel.readInt();
			Karaoke kara = new Karaoke(startTimeMs, endTimeMs,
									   startChar, endChar);

			if (mKaraokeList == null) {
				mKaraokeList = new ArrayList<Karaoke>();
			}
			mKaraokeList.add(kara);
		}
	}

	private void readHyperText(Parcel parcel) {
		int startChar = parcel.readInt();
		int endChar = parcel.readInt();

		int len = parcel.readInt();
		byte[] url = parcel.createByteArray();
		final String urlString = new String(url, 0, len);

		len = parcel.readInt();
		byte[] alt = parcel.createByteArray();
		final String altString = new String(alt, 0, len);
		HyperText hyperText = new HyperText(startChar, endChar, urlString, altString);

		if (mHyperTextList == null) {
			mHyperTextList = new ArrayList<HyperText>();
		}
		mHyperTextList.add(hyperText);
	}

	private void readBlinkingText(Parcel parcel) {
		int startChar = parcel.readInt();
		int endChar = parcel.readInt();
		CharPos blinkingPos = new CharPos(startChar, endChar);

		if (mBlinkingPosList == null) {
			mBlinkingPosList = new ArrayList<CharPos>();
		}
		mBlinkingPosList.add(blinkingPos);
	}

	private boolean isValidKey(final int key) {
		if (!((key >= FIRST_PUBLIC_KEY) && (key <= LAST_PUBLIC_KEY))
				&& !((key >= FIRST_PRIVATE_KEY) && (key <= LAST_PRIVATE_KEY))) {
			return false;
		}
		return true;
	}

	private boolean containsKey(final int key) {
		if (isValidKey(key) && mKeyObjectMap.containsKey(key)) {
			return true;
		}
		return false;
	}

	private Set keySet() {
		return mKeyObjectMap.keySet();
	}
	private Object getObject(final int key) {
		if (containsKey(key)) {
			return mKeyObjectMap.get(key);
		} else {
			throw new IllegalArgumentException("Invalid key: " + key);
		}
	}
}
