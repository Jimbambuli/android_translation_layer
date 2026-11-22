package com.android.server.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The NFC controller of ATL, in the place of AOSP's NfcService with its NCI stack: neard over the system bus
 * (native part com_android_server_nfc_NeardAdapter.c). Without neard or an adapter the device has no NFC.
 *
 * Like AOSP's NfcService the adapter polls while an activity of the app is in the foreground (AOSP: while the
 * screen is on and unlocked) and dispatches a tag as NfcDispatcher.dispatchTag() does: reader mode of the
 * resumed activity, then its foreground dispatch (tryOverrides()), then the manifest (tryNdef(), tryTech(),
 * ACTION_TAG_DISCOVERED). neard reads the NDEF message itself and offers it as parsed records; the message is
 * built back from them (Text, URI, SmartPoster, MIME, AAR). neard gives no raw access to a tag, so a connected
 * technology can read the NDEF message but transceive() fails (AOSP: TransceiveResult null, "transceive failed").
 */
public final class NeardAdapter {
	private static final String TAG = "NeardAdapter";

	private static boolean sChecked;
	private static String sAdapter;
	private static NeardAdapter sPoller;

	/* the resumed activity and what it asked for (AOSP NfcActivityManager / NfcDispatcher.setForegroundDispatch) */
	private static Activity sResumed;
	private static Activity sReaderActivity;
	private static NfcAdapter.ReaderCallback sReaderCallback;
	private static int sReaderFlags;
	private static PendingIntent sDispatchIntent;
	private static IntentFilter[] sDispatchFilters;
	private static String[][] sDispatchTechLists;

	private long mHandle;
	private final Set<String> mReported = new HashSet<>();

	private static native String nativeGetAdapter();
	private static native boolean nativeIsPowered(String adapter);
	private static native boolean nativeIsTagPresent(String tag);
	private native long nativeStartPoll(long handle, String adapter);
	private native void nativeStopPoll(long handle);

	private NeardAdapter() {}

	/* the adapter, fixed when first asked like AOSP's feature list; null without NFC */
	private static synchronized String adapter() {
		if (!sChecked) {
			sChecked = true;
			sAdapter = nativeGetAdapter();
			Log.i(TAG, "neard adapter: " + sAdapter);
		}
		return sAdapter;
	}

	public static boolean isPresent() {
		return adapter() != null;
	}

	public static boolean isEnabled() {
		return isPresent() && nativeIsPowered(sAdapter);
	}

	public static boolean isTagPresent(String tag) {
		return nativeIsTagPresent(tag);
	}

	/* Activity.performResume()/performPause() */
	public static void onActivityResumed(Activity activity) {
		if (!isPresent())
			return;
		sResumed = activity;
		updatePolling();
	}

	public static void onActivityPaused(Activity activity) {
		if (sResumed != activity)
			return;
		sResumed = null;
		updatePolling();
	}

	public static void setReaderMode(Activity activity, NfcAdapter.ReaderCallback callback, int flags) {
		sReaderActivity = callback != null ? activity : null;
		sReaderCallback = callback;
		sReaderFlags = flags;
		updatePolling();
	}

	/* NfcService.setForegroundDispatch(): for the app until it is disabled */
	public static void setForegroundDispatch(PendingIntent intent, IntentFilter[] filters, String[][] techLists) {
		sDispatchIntent = intent;
		sDispatchFilters = filters;
		sDispatchTechLists = techLists;
	}

	private static synchronized void updatePolling() {
		boolean poll = sResumed != null && isEnabled();
		if (poll && sPoller == null) {
			sPoller = new NeardAdapter();
			sPoller.mHandle = sPoller.nativeStartPoll(0, sAdapter);
		} else if (!poll && sPoller != null) {
			if (sPoller.mHandle != 0)
				sPoller.nativeStopPoll(sPoller.mHandle);
			sPoller = null;
		}
	}

	/* native: a tag came into range (on the main looper) */
	private void onTagFound(String path, byte[] uid, String type, String protocol, boolean readOnly, String[] records, byte[][] payloads) {
		if (sPoller != this || !mReported.add(path))
			return;
		NdefMessage message = buildMessage(records, payloads);
		Log.i(TAG, "tag " + path + " " + type + "/" + protocol + " uid " + hex(uid) + " ndef " + message);
		Tag tag = buildTag(path, uid, type, protocol, readOnly, message);
		dispatchTag(tag, message);
	}

	/* native: the tag left; neard ended its poll loop when it found the tag, so poll again */
	private void onTagLost(String path) {
		if (sPoller != this)
			return;
		mReported.remove(path);
		mHandle = nativeStartPoll(mHandle, sAdapter);
	}

	private static String hex(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (byte b : data)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

	/* the NDEF message from neard's record properties (src/ndef.c record_properties) */
	private static NdefMessage buildMessage(String[] records, byte[][] payloads) {
		List<NdefRecord> list = new ArrayList<>();
		for (int i = 0; i < records.length; i++) {
			Bundle p = new Bundle();
			for (String entry : records[i].split("\u001f")) {
				int eq = entry.indexOf('=');
				if (eq > 0)
					p.putString(entry.substring(0, eq), entry.substring(eq + 1));
			}
			NdefRecord record = buildRecord(p, payloads[i]);
			if (record != null)
				list.add(record);
		}
		return list.isEmpty() ? null : new NdefMessage(list.toArray(new NdefRecord[0]));
	}

	private static NdefRecord buildRecord(Bundle p, byte[] payload) {
		String type = p.getString("Type", "");
		try {
			switch (type) {
				case "Text":
					return textRecord(p);
				case "URI":
					return NdefRecord.createUri(p.getString("URI"));
				case "MIME":
					return NdefRecord.createMime(p.getString("MIME"), payload != null ? payload : new byte[0]);
				case "AAR":
					return NdefRecord.createApplicationRecord(p.getString("AAR"));
				case "SmartPoster": {
					/* NFC Forum Smart Poster: a URI record, the title (a Text record), the action ("act") */
					List<NdefRecord> parts = new ArrayList<>();
					parts.add(NdefRecord.createUri(p.getString("URI")));
					if (p.getString("Representation") != null)
						parts.add(textRecord(p));
					String action = p.getString("Action");
					if (action != null) {
						byte act = (byte)("Do".equals(action) ? 0 : "Save".equals(action) ? 1 : 2);
						parts.add(new NdefRecord(NdefRecord.TNF_WELL_KNOWN, "act".getBytes(StandardCharsets.US_ASCII), null, new byte[] {act}));
					}
					return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_SMART_POSTER, null,
					                      new NdefMessage(parts.toArray(new NdefRecord[0])).toByteArray());
				}
				default:
					Log.w(TAG, "record type not rebuilt: " + type);
					return null;
			}
		} catch (RuntimeException e) {
			Log.w(TAG, "record not rebuilt: " + p, e);
			return null;
		}
	}

	/* RTD Text: status byte (bit 7 UTF-16, bits 0-5 language length), language, text */
	private static NdefRecord textRecord(Bundle p) {
		boolean utf16 = "UTF-16".equals(p.getString("Encoding"));
		byte[] lang = p.getString("Language", "en").getBytes(StandardCharsets.US_ASCII);
		byte[] text = p.getString("Representation", "").getBytes(utf16 ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_8);
		byte[] data = new byte[1 + lang.length + text.length];
		data[0] = (byte)((utf16 ? 0x80 : 0) | (lang.length & 0x3f));
		System.arraycopy(lang, 0, data, 1, lang.length);
		System.arraycopy(text, 0, data, 1 + lang.length, text.length);
		return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, null, data);
	}

	/* the technologies AOSP's NativeNfcTag reports for the NFC Forum tag types (neard's Type/Protocol) */
	private static Tag buildTag(String path, byte[] uid, String type, String protocol, boolean readOnly, NdefMessage message) {
		List<Integer> techs = new ArrayList<>();
		List<Bundle> extras = new ArrayList<>();
		int ndefType;
		switch (type) {
			case "Type 1":
				ndefType = Ndef.TYPE_1;
				techs.add(TagTechnology.NFC_A);
				break;
			case "Type 2":
				ndefType = Ndef.TYPE_2;
				techs.add(TagTechnology.NFC_A);
				break;
			case "Type 3":
				ndefType = Ndef.TYPE_3;
				techs.add(TagTechnology.NFC_F);
				break;
			case "Type 4":
				ndefType = Ndef.TYPE_4;
				techs.add(TagTechnology.NFC_A);
				techs.add(TagTechnology.ISO_DEP);
				break;
			case "Type 5":
				ndefType = Ndef.TYPE_OTHER;
				techs.add(TagTechnology.NFC_V);
				break;
			default:
				ndefType = Ndef.TYPE_OTHER;
				techs.add(TagTechnology.NFC_A);
				break;
		}
		for (int i = 0; i < techs.size(); i++) {
			Bundle b = new Bundle();
			if (techs.get(i) == TagTechnology.NFC_A) {
				/* neard reports neither SAK nor ATQA */
				b.putShort(NfcA.EXTRA_SAK, (short)0);
				b.putByteArray(NfcA.EXTRA_ATQA, new byte[2]);
			}
			extras.add(b);
		}
		if (message != null) {
			Bundle b = new Bundle();
			b.putParcelable(Ndef.EXTRA_NDEF_MSG, message);
			b.putInt(Ndef.EXTRA_NDEF_MAXLENGTH, message.getByteArrayLength());
			b.putInt(Ndef.EXTRA_NDEF_CARDSTATE, readOnly ? Ndef.NDEF_MODE_READ_ONLY : Ndef.NDEF_MODE_READ_WRITE);
			b.putInt(Ndef.EXTRA_NDEF_TYPE, ndefType);
			techs.add(TagTechnology.NDEF);
			extras.add(b);
		}
		int[] techList = new int[techs.size()];
		for (int i = 0; i < techList.length; i++)
			techList[i] = techs.get(i);
		return Tag.createNeardTag(uid, techList, extras.toArray(new Bundle[0]), path);
	}

	/* NfcDispatcher.dispatchTag() for the app of this process */
	private static void dispatchTag(final Tag tag, NdefMessage message) {
		final Activity resumed = sResumed;
		if (resumed == null)
			return;
		/* reader mode (NfcService.onRemoteEndpointDiscovered -> dispatchTagEndpoint, the callback on a binder thread) */
		if (sReaderActivity == resumed && sReaderCallback != null) {
			final NfcAdapter.ReaderCallback callback = sReaderCallback;
			final Tag readerTag = (sReaderFlags & NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK) != 0 ? withoutNdef(tag) : tag;
			new Thread("NfcReaderCallback") {
				@Override
				public void run() {
					callback.onTagDiscovered(readerTag);
				}
			}.start();
			return;
		}
		Context context = resumed.getApplicationContext();
		Intent intent = new Intent();
		intent.putExtra(NfcAdapter.EXTRA_TAG, tag);
		intent.putExtra(NfcAdapter.EXTRA_ID, tag.getId());
		Uri ndefUri = null;
		String ndefMimeType = null;
		if (message != null) {
			intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, new NdefMessage[] {message});
			ndefUri = message.getRecords()[0].toUri();
			ndefMimeType = message.getRecords()[0].toMimeType();
		}
		if (sDispatchIntent != null && tryOverrides(context, intent, tag, message, ndefUri, ndefMimeType))
			return;
		/* tryNdef() */
		if (message != null && setNdefIntent(intent, ndefUri, ndefMimeType) && tryStartActivity(context, intent))
			return;
		/* tryTech() */
		setIntent(intent, NfcAdapter.ACTION_TECH_DISCOVERED);
		if (techMatches(context, tag) && tryStartActivity(context, intent))
			return;
		setIntent(intent, NfcAdapter.ACTION_TAG_DISCOVERED);
		if (tryStartActivity(context, intent))
			return;
		Log.i(TAG, "tag not dispatched (no activity for it)");
	}

	private static Tag withoutNdef(Tag tag) {
		int[] techs = tag.getTechCodeList();
		int n = 0;
		for (int t : techs)
			if (t != TagTechnology.NDEF)
				n++;
		int[] list = new int[n];
		Bundle[] extras = new Bundle[n];
		for (int i = 0, j = 0; i < techs.length; i++) {
			if (techs[i] != TagTechnology.NDEF) {
				list[j] = techs[i];
				extras[j++] = tag.getTechExtras(techs[i]);
			}
		}
		return Tag.createNeardTag(tag.getId(), list, extras, tag.getNeardPath());
	}

	private static boolean setNdefIntent(Intent intent, Uri ndefUri, String ndefMimeType) {
		intent.setAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
		if (ndefUri != null) {
			intent.setData(ndefUri);
			return true;
		} else if (ndefMimeType != null) {
			intent.setType(ndefMimeType);
			return true;
		}
		return false;
	}

	private static void setIntent(Intent intent, String action) {
		intent.setData(null);
		intent.setType(null);
		intent.setAction(action);
	}

	/* NfcDispatcher.tryOverrides() */
	private static boolean tryOverrides(Context context, Intent intent, Tag tag, NdefMessage message, Uri ndefUri, String ndefMimeType) {
		boolean hasTechFilter = sDispatchTechLists != null;
		try {
			if (message != null && setNdefIntent(intent, ndefUri, ndefMimeType) && isFilterMatch(context, intent, hasTechFilter)) {
				sDispatchIntent.send(context, Activity.RESULT_OK, intent);
				return true;
			}
			setIntent(intent, NfcAdapter.ACTION_TECH_DISCOVERED);
			if (isTechMatch(tag, sDispatchTechLists)) {
				sDispatchIntent.send(context, Activity.RESULT_OK, intent);
				return true;
			}
			setIntent(intent, NfcAdapter.ACTION_TAG_DISCOVERED);
			if (isFilterMatch(context, intent, hasTechFilter)) {
				sDispatchIntent.send(context, Activity.RESULT_OK, intent);
				return true;
			}
		} catch (PendingIntent.CanceledException e) {
			Log.e(TAG, "tryOverrides: sendIntent failed", e);
		}
		return false;
	}

	private static boolean isFilterMatch(Context context, Intent intent, boolean hasTechFilter) {
		if (sDispatchFilters != null) {
			for (IntentFilter filter : sDispatchFilters) {
				if (filter.match(context.getContentResolver(), intent, false, TAG) >= 0)
					return true;
			}
			return false;
		}
		return !hasTechFilter;
	}

	private static boolean isTechMatch(Tag tag, String[][] techLists) {
		if (techLists == null)
			return false;
		String[] tagTechs = tag.getTechList();
		Arrays.sort(tagTechs);
		for (String[] filterTechs : techLists) {
			if (filterMatch(tagTechs, filterTechs))
				return true;
		}
		return false;
	}

	/* NfcDispatcher.filterMatch(): every tech of the filter is on the tag */
	private static boolean filterMatch(String[] tagTechs, String[] filterTechs) {
		if (filterTechs == null || filterTechs.length == 0)
			return false;
		for (String tech : filterTechs) {
			if (Arrays.binarySearch(tagTechs, tech) < 0)
				return false;
		}
		return true;
	}

	/* tryTech(): the activities with a TECH_DISCOVERED filter whose tech-list meta-data (RegisteredComponentCache)
	 * matches the tag */
	private static boolean techMatches(Context context, Tag tag) {
		String[] tagTechs = tag.getTechList();
		Arrays.sort(tagTechs);
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> infos = pm.queryIntentActivities(new Intent(NfcAdapter.ACTION_TECH_DISCOVERED), PackageManager.GET_META_DATA);
		for (ResolveInfo info : infos) {
			ActivityInfo ai = info.activityInfo;
			if (ai == null || ai.metaData == null)
				continue;
			int res = ai.metaData.getInt(NfcAdapter.ACTION_TECH_DISCOVERED);
			if (res == 0)
				continue;
			try (XmlResourceParser parser = context.getResources().getXml(res)) {
				List<String> techs = null;
				int event;
				while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
					if (event == XmlResourceParser.START_TAG && "tech-list".equals(parser.getName())) {
						techs = new ArrayList<>();
					} else if (event == XmlResourceParser.START_TAG && "tech".equals(parser.getName()) && techs != null) {
						techs.add(parser.nextText().trim());
					} else if (event == XmlResourceParser.END_TAG && "tech-list".equals(parser.getName()) && techs != null) {
						if (filterMatch(tagTechs, techs.toArray(new String[0])))
							return true;
						techs = null;
					}
				}
			} catch (Exception e) {
				Log.w(TAG, "tech-list of " + ai.name + " not read", e);
			}
		}
		return false;
	}

	/* DispatchInfo.tryStartActivity() */
	private static boolean tryStartActivity(Context context, Intent intent) {
		List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, 0);
		if (activities == null || activities.isEmpty())
			return false;
		Log.i(TAG, "dispatch " + intent);
		Intent launch = new Intent(intent);
		launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(launch);
		return true;
	}
}
