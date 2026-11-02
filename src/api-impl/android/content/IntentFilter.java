package android.content;

import android.net.Uri;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class IntentFilter {

	private List<String> actions = new ArrayList<>();
	private Set<String> categories = new HashSet<>();
	private List<String> dataSchemes = new ArrayList<>();
	private List<AuthorityEntry> dataAuthorities = new ArrayList<>();

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

	/* AOSP IntentFilter.addDataAuthority(String, String): the authorities are kept, matching does not use them yet */
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

	public void addDataPath(String path, int type) {}

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

	public int match(String action, String type, String scheme, Uri data, Set<String> categories, String logTag) {
		int ret = 0;
		if (!matchAction(action)) {
			ret = -3 /*NO_MATCH_ACTION*/;
		}
		if (scheme == null) {
			ret = 0x00100000 /*MATCH_CATEGORY_EMPTY*/ | 0x00008000 /*MATCH_ADJUSTMENT_NORMAL*/;
		} else if (hasDataScheme(scheme)) {
			ret = 0x00200000 /*MATCH_CATEGORY_SCHEME*/ | 0x00008000 /*MATCH_ADJUSTMENT_NORMAL*/;
		} else {
			ret = -2 /*NO_MATCH_DATA*/;
		}
		return ret;
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
