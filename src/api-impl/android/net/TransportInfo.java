package android.net;

/* AOSP TransportInfo (packages/modules/Connectivity): both default methods as there; REDACT_NONE is 0 */
public interface TransportInfo {
	default TransportInfo makeCopy(long redactions) {
		return this;
	}

	default long getApplicableRedactions() {
		return 0L; // NetworkCapabilities.REDACT_NONE
	}
}
