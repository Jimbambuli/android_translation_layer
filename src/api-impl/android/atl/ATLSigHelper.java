package android.atl;

import android.content.pm.PackageParser;
import android.content.pm.Signature;
import android.util.Base64;

public final class ATLSigHelper {
	private static final Signature[] gmsSignatures = new Signature[] {
		new Signature(Base64.decode(
		    "MIIEQzCCAyugAwIBAgIJAMLgh0ZkSjCNMA0GCSqGSIb3DQEBBAUAMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9yb"
		    + "mlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1"
		    + "UEAxMHQW5kcm9pZDAeFw0wODA4MjEyMzEzMzRaFw0zNjAxMDcyMzEzMzRaMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWx"
		    + "pZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQ"
		    + "MA4GA1UEAxMHQW5kcm9pZDCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBAKtWLgDYO6IIrgqWbxJOKdoR8qtW0I9Y4"
		    + "sypEwPpt1TTcvZApxsdyxMJZ2JORland2qSGT2y5b+3JKkedxiLDmpHpDsz2WCbdxgxRczfey5YZnTJ4VZbH0xqWVW/8lGmPa"
		    + "v5xVwnIiJS6HXk+BVKZF+JcWjAsb/GEuq/eFdpuzSqeYTcfi6idkyugwfYwXFU1+5fZKUaRKYCwkkFQVfcAs1fXA5V+++FGfv"
		    + "jJ/CxURaSxaBvGdGDhfXE28LWuT9ozCl5xw4Yq5OGazvV24mZVSoOO0yZ31j7kYvtwYK6NeADwbSxDdJEqO4k//0zOHKrUiGY"
		    + "Xtqw/A0LFFtqoZKFjnkCAQOjgdkwgdYwHQYDVR0OBBYEFMd9jMIhF1Ylmn/Tgt9r45jk14alMIGmBgNVHSMEgZ4wgZuAFMd9j"
		    + "MIhF1Ylmn/Tgt9r45jk14aloXikdjB0MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bn"
		    + "RhaW4gVmlldzEUMBIGA1UEChMLR29vZ2xlIEluYy4xEDAOBgNVBAsTB0FuZHJvaWQxEDAOBgNVBAMTB0FuZHJvaWSCCQDC4Id"
		    + "GZEowjTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBAUAA4IBAQBt0lLO74UwLDYKqs6Tm8/yzKkEu116FmH4rkaymUIE0P9K"
		    + "aMftGlMexFlaYjzmB2OxZyl6euNXEsQH8gjwyxCUKRJNexBiGcCEyj6z+a1fuHHvkiaai+KL8W1EyNmgjmyy8AW7P+LLlkR+h"
		    + "o5zEHatRbM/YAnqGcFh5iZBqpknHf1SKMXFh4dd239FJ1jWYfbMDMy3NS5CTMQ2XFI1MvcyUTdZPErjQfTbQe3aDQsQcafEQP"
		    + "D+nqActifKZ0Np0IS9L9kR/wbNvyz6ENwPiTrjV2KRkEjH78ZMcUQXg0L3BYHJ3lc69Vs5Ddf9uUGGMYldX3WfMBEmh/9iFBD"
		    + "AaTCK",
		    Base64.DEFAULT))
	};

	public static void addGMSSignatures(PackageParser.Package pkg) {
		// Like signature spoofing on Android, report only the GMS signature. Apps verify that
		// Play Services has exactly one signature, so the package's own one must not be kept.
		pkg.mSignatures = gmsSignatures;
	}

	/* ATL's platform certificate: the signature of the platform packages ATL presents ("android", "atl"), like the
	 * vendor platform key of a device (AOSP: the package "android" is signed with it; apps and microG compare
	 * signatures against it). A self-signed X.509 certificate made once and kept fixed (the key was not kept, nothing
	 * is signed with it):
	 *   openssl req -x509 -newkey rsa:2048 -nodes -days 36500 -set_serial 1 -outform DER
	 *     -subj "/C=CH/O=Android Translation Layer/CN=Android Translation Layer platform"
	 * SHA-256 43:BE:A7:FA:2E:58:8E:49:9C:18:CB:EC:E4:51:39:E7:86:5C:3A:44:2A:C3:2E:DA:95:8E:D6:61:E6:A3:7A:27 */
	private static final Signature[] platformSignatures = new Signature[] {
		new Signature(Base64.decode(
		    "MIIDjDCCAnSgAwIBAgIBATANBgkqhkiG9w0BAQsFADBeMQswCQYDVQQGEwJDSDEiMCAGA1UECgwZQW5kcm9pZCBUcmFuc2xh"
		    + "dGlvbiBMYXllcjErMCkGA1UEAwwiQW5kcm9pZCBUcmFuc2xhdGlvbiBMYXllciBwbGF0Zm9ybTAgFw0yNjExMjIxMzE5MzBa"
		    + "GA8yMTI2MTAyOTEzMTkzMFowXjELMAkGA1UEBhMCQ0gxIjAgBgNVBAoMGUFuZHJvaWQgVHJhbnNsYXRpb24gTGF5ZXIxKzAp"
		    + "BgNVBAMMIkFuZHJvaWQgVHJhbnNsYXRpb24gTGF5ZXIgcGxhdGZvcm0wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB"
		    + "AQCHr3yIw+G3693hnZJ5xh+SM49PiFZYfWhYka7GiSqaoAyI8RlyDTPsCa7ndxoJnESHrou2w7kPcyb5M66Jgg+Ev5V6QQO2"
		    + "ZErsxSX8GrmTwHo7uID2Pz7e4G2/ueF1sFPffui6YdLEP/R6BJbp4BTA+KM2N0780NPLJ1MAY1QDaoXR8x8TmV948HweQi07"
		    + "YLUS8d8UTjJy02UNSMlJycjggx4XeSUbg9dyFivFgbrtC8fP2ROWxX0Yg7sNbRdRc2vpZasNOx3Ky2jEXQlK9y+IWgD7KVbH"
		    + "mYvF4DZbJSKNeDcbwi+OTY+Zs5HGH7BnJNNGKZKtq/sT/XA3cX/yt0wfAgMBAAGjUzBRMB0GA1UdDgQWBBTiyFNP8MfhYPdJ"
		    + "tk2Ycr8hZDmNCjAfBgNVHSMEGDAWgBTiyFNP8MfhYPdJtk2Ycr8hZDmNCjAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEB"
		    + "CwUAA4IBAQAkbLyPFs60NAJQo/310itamK8FNEYxTez4FND10Up4JMthFjvMhSyt6FU2iezLCKLtnmx+5LASxHqCzGJfRhJ9"
		    + "Y+E6xZan9quJqwnkCA8gFgw3Yxk6tBwi6A5lTmHGwkQCPBL3Ike0fg6G0JpWox9f2Zu9YfoyRkxO5wIAqIpgZDXSs4BlWarU"
		    + "A/Cx4H33nsqkM+nKv6zOKDFSvIy71rkYT+bhV3XVjQwd+EYohEHslANgsx9rbcbO85HIw2lX3zrvTQSfLHwft7KRQoWxGsFb"
		    + "F+X+5UOI12LvLl+llBaQJQYzu6S5A4pZ1aW1+twcdfIDJqCiGbDgMuJA3ojdbEmH", Base64.DEFAULT))
	};

	public static Signature[] getPlatformSignatures() {
		return platformSignatures.clone();
	}
}
