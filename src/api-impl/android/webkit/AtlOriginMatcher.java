package android.webkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/* The allowed-origin rules of androidx.webkit's addWebMessageListener()/addDocumentStartJavaScript(), as Chromium's
 * components/origin_matcher (OriginMatcher::AddRuleFromString(), SubdomainMatchingRule, MatchAllOriginsRule):
 * "*" matches every origin; otherwise "scheme://host[:port]", the host may start with "*." (subdomains only), http
 * and https need a host, other schemes must not have one; a missing port is the scheme's default. */
final class AtlOriginMatcher {
	private static final class Rule {
		final String scheme, host; // host: null for "*"; "" for schemes without a host
		final int port; // -1: any

		Rule(String scheme, String host, int port) {
			this.scheme = scheme;
			this.host = host;
			this.port = port;
		}
	}

	private final List<Rule> rules = new ArrayList<>();

	/* IllegalArgumentException for an invalid rule, as Chromium's addWebMessageListener() throws */
	AtlOriginMatcher(String[] allowedOriginRules) {
		for (String rule : allowedOriginRules) {
			if (!add(rule))
				throw new IllegalArgumentException("allowedOriginRules " + rule + " is invalid");
		}
	}

	private static int defaultPort(String scheme) {
		switch (scheme) {
			case "http":
			case "ws":
				return 80;
			case "https":
			case "wss":
				return 443;
			default:
				return -1;
		}
	}

	private boolean add(String raw) {
		if (raw == null)
			return false;
		raw = raw.trim();
		if (raw.equals("*")) {
			rules.add(new Rule(null, null, -1));
			return true;
		}
		int schemePos = raw.indexOf("://");
		if (schemePos < 0)
			return false;
		String scheme = raw.substring(0, schemePos);
		if (scheme.isEmpty() || scheme.indexOf('*') >= 0)
			return false;
		boolean canHaveHost = scheme.equals("http") || scheme.equals("https");
		String hostAndPort = raw.substring(schemePos + 3);
		if (hostAndPort.isEmpty()) {
			if (canHaveHost)
				return false;
			rules.add(new Rule(scheme, "", -1));
			return true;
		}
		if (!canHaveHost || hostAndPort.indexOf('/') >= 0)
			return false;
		String host = hostAndPort;
		int port = -1;
		int colon = hostAndPort.lastIndexOf(':');
		if (colon >= 0 && hostAndPort.indexOf(']', colon) < 0) {
			try {
				port = Integer.parseInt(hostAndPort.substring(colon + 1));
			} catch (NumberFormatException e) {
				return false;
			}
			if (port < 0 || port > 65535)
				return false;
			host = hostAndPort.substring(0, colon);
		}
		if (host.isEmpty())
			return false;
		int wildcards = host.length() - host.replace("*", "").length();
		if (wildcards > 1 || (wildcards == 1 && !host.startsWith("*.")))
			return false;
		rules.add(new Rule(scheme, host.toLowerCase(Locale.ROOT), port == -1 ? defaultPort(scheme) : port));
		return true;
	}

	/* origin as the frame's location.origin: "scheme://host[:port]", or "null" for an opaque one (then only "*") */
	boolean matches(String origin) {
		String scheme = null, host = "";
		int port = -1;
		int schemePos = origin == null ? -1 : origin.indexOf("://");
		if (schemePos > 0) {
			scheme = origin.substring(0, schemePos);
			String hostAndPort = origin.substring(schemePos + 3);
			int colon = hostAndPort.lastIndexOf(':');
			host = hostAndPort;
			if (colon >= 0 && hostAndPort.indexOf(']', colon) < 0) {
				try {
					port = Integer.parseInt(hostAndPort.substring(colon + 1));
					host = hostAndPort.substring(0, colon);
				} catch (NumberFormatException e) {
				}
			}
			if (port == -1)
				port = defaultPort(scheme);
			host = host.toLowerCase(Locale.ROOT);
		}
		for (Rule rule : rules) {
			if (rule.host == null)
				return true;
			if (scheme == null || !scheme.equals(rule.scheme) || (rule.port != -1 && rule.port != port))
				continue;
			if (rule.host.startsWith("*.") ? host.endsWith(rule.host.substring(1)) : host.equals(rule.host))
				return true;
		}
		return false;
	}
}
