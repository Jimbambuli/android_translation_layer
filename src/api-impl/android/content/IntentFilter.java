package android.content;

import android.net.Uri;
import android.os.PatternMatcher;
import android.util.AndroidException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class IntentFilter {

	public static final int MATCH_CATEGORY_MASK = 0xfff0000;
	public static final int MATCH_ADJUSTMENT_MASK = 0x000ffff;
	public static final int MATCH_ADJUSTMENT_NORMAL = 0x8000;
	public static final int MATCH_CATEGORY_EMPTY = 0x0100000;
	public static final int MATCH_CATEGORY_SCHEME = 0x0200000;
	public static final int MATCH_CATEGORY_HOST = 0x0300000;
	public static final int MATCH_CATEGORY_PORT = 0x0400000;
	public static final int MATCH_CATEGORY_PATH = 0x0500000;
	public static final int MATCH_CATEGORY_SCHEME_SPECIFIC_PART = 0x0580000;
	public static final int MATCH_CATEGORY_TYPE = 0x0600000;
	public static final int NO_MATCH_TYPE = -1;
	public static final int NO_MATCH_DATA = -2;
	public static final int NO_MATCH_ACTION = -3;
	public static final int NO_MATCH_CATEGORY = -4;

	public static class MalformedMimeTypeException extends AndroidException {
		public MalformedMimeTypeException() {}
		public MalformedMimeTypeException(String name) {
			super(name);
		}
	}

	private List<String> actions = new ArrayList<>();
	private Set<String> categories = new HashSet<>();
	private List<String> dataSchemes = new ArrayList<>();
	private List<AuthorityEntry> dataAuthorities = new ArrayList<>();
	/* AOSP: paths, MIME types and android:autoVerify of the filter */
	private List<PatternMatcher> dataPaths;
	private List<String> dataTypes;
	private boolean hasPartialTypes;
	private boolean autoVerify;

	public IntentFilter() {}
	public IntentFilter(String action) {
		addAction(action);
	}

	public void addAction(String action) {
		actions.add(action);
	}
	public int countActions() {
		return actions.size();
	}

	public final boolean matchAction(String action) {
		return actions.contains(action);
	}

	public void addCategory(String category) {
		categories.add(category);
	}
	public int countCategories() {
		return categories.size();
	}

	public final boolean hasCategory(String category) {
		return categories.contains(category);
	}

	public String getAction(int index) {
		return actions.get(index);
	}

	public void setPriority(int priority) {}

	public void addDataScheme(String dataScheme) {
		dataSchemes.add(dataScheme);
	}

	/* AOSP IntentFilter.addDataAuthority(String, String) */
	public void addDataAuthority(String host, String port) {
		if (port != null)
			port = port.intern();
		dataAuthorities.add(new AuthorityEntry(host.intern(), port));
	}

	public final int countDataAuthorities() {
		return dataAuthorities.size();
	}

	public final AuthorityEntry getDataAuthority(int index) {
		return dataAuthorities.get(index);
	}

	/* AOSP IntentFilter.handlesWebUris(boolean) */
	public final boolean handlesWebUris(boolean onlyWebSchemes) {
		// Require ACTION_VIEW, CATEGORY_BROWSEABLE, and at least one scheme
		if (!hasAction(Intent.ACTION_VIEW)
		    || !hasCategory(Intent.CATEGORY_BROWSABLE)
		    || dataSchemes.size() == 0) {
			return false;
		}

		// Now allow only the schemes "http" and "https"
		final int N = dataSchemes.size();
		for (int i = 0; i < N; i++) {
			final String scheme = dataSchemes.get(i);
			final boolean isWebScheme = "http".equals(scheme) || "https".equals(scheme);
			if (onlyWebSchemes) {
				// If we're specifically trying to ensure that there are no non-web schemes
				// declared in this filter, then if we ever see a non-http/https scheme then
				// we know it's a failure.
				if (!isWebScheme) {
					return false;
				}
			} else {
				// If we see any http/https scheme declaration in this case then the
				// filter matches what we're looking for.
				if (isWebScheme) {
					return true;
				}
			}
		}

		// We get here if:
		//   1) onlyWebSchemes and no non-web schemes were found, i.e success; or
		//   2) !onlyWebSchemes and no http/https schemes were found, i.e. failure.
		return onlyWebSchemes;
	}

	public final void addDataPath(String path, int type) {
		if (dataPaths == null)
			dataPaths = new ArrayList<>();
		dataPaths.add(new PatternMatcher(path.intern(), type));
	}

	public final int countDataPaths() {
		return dataPaths != null ? dataPaths.size() : 0;
	}

	public final PatternMatcher getDataPath(int index) {
		return dataPaths.get(index);
	}

	/* AOSP IntentFilter.hasDataPath(String) */
	public final boolean hasDataPath(String data) {
		if (dataPaths == null)
			return false;
		for (PatternMatcher pe : dataPaths) {
			if (pe.match(data))
				return true;
		}
		return false;
	}

	/* AOSP IntentFilter.addDataType(String), via processMimeType() */
	public final void addDataType(String type) throws MalformedMimeTypeException {
		final int slashpos = type.indexOf('/');
		final int typelen = type.length();
		if (slashpos <= 0 || typelen < slashpos + 2)
			throw new MalformedMimeTypeException(type);
		String internalType = type;
		if (typelen == slashpos + 2 && type.charAt(slashpos + 1) == '*') {
			internalType = type.substring(0, slashpos);
			hasPartialTypes = true;
		}
		if (dataTypes == null)
			dataTypes = new ArrayList<>();
		if (!dataTypes.contains(internalType))
			dataTypes.add(internalType.intern());
	}

	public final int countDataTypes() {
		return dataTypes != null ? dataTypes.size() : 0;
	}

	public final boolean hasDataType(String type) {
		return dataTypes != null && findMimeType(type);
	}

	/* AOSP IntentFilter.findMimeType(String) */
	private boolean findMimeType(String type) {
		final List<String> t = dataTypes;
		if (type == null)
			return false;
		if (t.contains(type))
			return true;
		// Deal with an Intent wanting to match every type in the IntentFilter.
		final int typeLength = type.length();
		if (typeLength == 3 && type.equals("*/*"))
			return !t.isEmpty();
		// Deal with this IntentFilter wanting to match every Intent type.
		if (hasPartialTypes && t.contains("*"))
			return true;
		final int slashpos = type.indexOf('/');
		if (slashpos > 0) {
			if (hasPartialTypes && t.contains(type.substring(0, slashpos)))
				return true;
			if (typeLength == slashpos + 2 && type.charAt(slashpos + 1) == '*') {
				// Need to look through all types for one that matches our base...
				for (String v : t) {
					if (type.regionMatches(0, v, 0, slashpos + 1))
						return true;
				}
			}
		}
		return false;
	}

	public final void setAutoVerify(boolean autoVerify) {
		this.autoVerify = autoVerify;
	}

	public final boolean getAutoVerify() {
		return autoVerify;
	}

	public final void addDataSchemeSpecificPart(String ssp, int type) {
		/* FIXME */
	}

	public boolean hasDataScheme(String dataScheme) {
		return dataSchemes.contains(dataScheme);
	}

	public int countDataSchemes() {
		return dataSchemes.size();
	}

	public String getDataScheme(int index) {
		return dataSchemes.get(index);
	}

	public final Iterator<String> actionsIterator() {
		return actions.iterator();
	}

	public boolean hasAction(String action) {
		return actions.contains(action);
	}

	/* AOSP IntentFilter.match(String, String, String, Uri, Set, String), without wildcards and extras */
	public final int match(String action, String type, String scheme, Uri data, Set<String> categories, String logTag) {
		if (action != null && !matchAction(action))
			return NO_MATCH_ACTION;
		int dataMatch = matchData(type, scheme, data);
		if (dataMatch < 0)
			return dataMatch;
		if (matchCategories(categories) != null)
			return NO_MATCH_CATEGORY;
		return dataMatch;
	}

	/* AOSP IntentFilter.matchCategories(Set) */
	public final String matchCategories(Set<String> categories) {
		if (categories == null)
			return null;
		for (String category : categories) {
			if (!this.categories.contains(category))
				return category;
		}
		return null;
	}

	/* AOSP IntentFilter.matchDataAuthority(Uri) */
	public final int matchDataAuthority(Uri data) {
		if (data == null)
			return NO_MATCH_DATA;
		for (AuthorityEntry ae : dataAuthorities) {
			int match = ae.match(data);
			if (match >= 0)
				return match;
		}
		return NO_MATCH_DATA;
	}

	/* AOSP IntentFilter.matchData(String, String, Uri), without scheme specific parts and wildcards */
	public final int matchData(String type, String scheme, Uri data) {
		final List<String> types = dataTypes;
		final List<String> schemes = dataSchemes;
		int match = MATCH_CATEGORY_EMPTY;
		if (types == null && schemes.isEmpty())
			return ((type == null && data == null) ? (MATCH_CATEGORY_EMPTY + MATCH_ADJUSTMENT_NORMAL) : NO_MATCH_DATA);
		if (!schemes.isEmpty()) {
			if (schemes.contains(scheme != null ? scheme : ""))
				match = MATCH_CATEGORY_SCHEME;
			else
				return NO_MATCH_DATA;
			if (!dataAuthorities.isEmpty()) {
				int authMatch = matchDataAuthority(data);
				if (authMatch >= 0) {
					if (dataPaths == null)
						match = authMatch;
					else if (hasDataPath(data.getPath()))
						match = MATCH_CATEGORY_PATH;
					else
						return NO_MATCH_DATA;
				} else {
					return NO_MATCH_DATA;
				}
			}
		} else {
			// Special case: match either an Intent with no data URI, or with a scheme: URI. This is to give a
			// convenience for the common case where you want to deal with data in a content provider, which is
			// done by type, and we don't want to force everyone to say they handle content: or file: URIs.
			if (scheme != null && !"".equals(scheme) && !"content".equals(scheme) && !"file".equals(scheme))
				return NO_MATCH_DATA;
		}
		if (types != null) {
			if (findMimeType(type))
				match = MATCH_CATEGORY_TYPE;
			else
				return NO_MATCH_TYPE;
		} else {
			// If no MIME types are specified, then we will only match against an Intent that does not have a MIME type.
			if (type != null)
				return NO_MATCH_TYPE;
		}
		return match + MATCH_ADJUSTMENT_NORMAL;
	}

	public final Iterator<String> schemesIterator() {
		return dataSchemes.iterator();
	}

	public final Iterator<AuthorityEntry> authoritiesIterator() {
		return dataAuthorities.iterator();
	}

	/* AOSP IntentFilter.AuthorityEntry, without parcelling and proto dumps */
	public final static class AuthorityEntry {
		private final String mOrigHost;
		private final String mHost;
		private final boolean mWild;
		private final int mPort;

		public AuthorityEntry(String host, String port) {
			mOrigHost = host;
			mWild = host.length() > 0 && host.charAt(0) == '*';
			mHost = mWild ? host.substring(1).intern() : host;
			mPort = port != null ? Integer.parseInt(port) : -1;
		}

		public String getHost() {
			return mOrigHost;
		}

		public int getPort() {
			return mPort;
		}

		/* AOSP AuthorityEntry.match(Uri, boolean), without wildcards */
		public int match(Uri data) {
			String host = data.getHost();
			if (host == null)
				return NO_MATCH_DATA;
			if (mWild) {
				if (host.length() < mHost.length())
					return NO_MATCH_DATA;
				host = host.substring(host.length() - mHost.length());
			}
			if (host.compareToIgnoreCase(mHost) != 0)
				return NO_MATCH_DATA;
			if (mPort >= 0) {
				if (mPort != data.getPort())
					return NO_MATCH_DATA;
				return MATCH_CATEGORY_PORT;
			}
			return MATCH_CATEGORY_HOST;
		}

		public boolean match(AuthorityEntry other) {
			if (mWild != other.mWild) {
				return false;
			}
			if (!mHost.equals(other.mHost)) {
				return false;
			}
			if (mPort != other.mPort) {
				return false;
			}
			return true;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof AuthorityEntry) {
				final AuthorityEntry other = (AuthorityEntry)obj;
				return match(other);
			}
			return false;
		}
	}
}
