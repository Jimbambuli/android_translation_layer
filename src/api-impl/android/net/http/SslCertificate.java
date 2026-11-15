/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.http;

import android.annotation.Nullable;
import android.annotation.UnsupportedAppUsage;
import android.os.Bundle;


import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * SSL certificate info (certificate details) class
 */
public class SslCertificate {

    /**
     * SimpleDateFormat pattern for an ISO 8601 date
     */
    private static String ISO_8601_DATE_FORMAT = "yyyy-MM-dd HH:mm:ssZ";

    /**
     * Name of the entity this certificate is issued to
     */
    private final DName mIssuedTo;

    /**
     * Name of the entity this certificate is issued by
     */
    private final DName mIssuedBy;

    /**
     * Not-before date from the validity period
     */
    private final Date mValidNotBefore;

    /**
     * Not-after date from the validity period
     */
    private final Date mValidNotAfter;

    /**
     * The original source certificate, if available.
     *
     * TODO If deprecated constructors are removed, this should always
     * be available, and saveState and restoreState can be simplified
     * to be unconditional.
     */
    @UnsupportedAppUsage
    private final X509Certificate mX509Certificate;

    /**
     * Bundle key names
     */
    private static final String ISSUED_TO = "issued-to";
    private static final String ISSUED_BY = "issued-by";
    private static final String VALID_NOT_BEFORE = "valid-not-before";
    private static final String VALID_NOT_AFTER = "valid-not-after";
    private static final String X509_CERTIFICATE = "x509-certificate";

    /**
     * Saves the certificate state to a bundle
     * @param certificate The SSL certificate to store
     * @return A bundle with the certificate stored in it or null if fails
     */
    public static Bundle saveState(SslCertificate certificate) {
        if (certificate == null) {
            return null;
        }
        Bundle bundle = new Bundle();
        bundle.putString(ISSUED_TO, certificate.getIssuedTo().getDName());
        bundle.putString(ISSUED_BY, certificate.getIssuedBy().getDName());
        bundle.putString(VALID_NOT_BEFORE, certificate.getValidNotBefore());
        bundle.putString(VALID_NOT_AFTER, certificate.getValidNotAfter());
        X509Certificate x509Certificate = certificate.mX509Certificate;
        if (x509Certificate != null) {
            try {
                bundle.putByteArray(X509_CERTIFICATE, x509Certificate.getEncoded());
            } catch (CertificateEncodingException ignored) {
            }
        }
        return bundle;
    }

    /**
     * Restores the certificate stored in the bundle
     * @param bundle The bundle with the certificate state stored in it
     * @return The SSL certificate stored in the bundle or null if fails
     */
    public static SslCertificate restoreState(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        X509Certificate x509Certificate;
        byte[] bytes = bundle.getByteArray(X509_CERTIFICATE);
        if (bytes == null) {
            x509Certificate = null;
        } else {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(bytes));
                x509Certificate = (X509Certificate) cert;
            } catch (CertificateException e) {
                x509Certificate = null;
            }
        }
        return new SslCertificate(bundle.getString(ISSUED_TO),
                                  bundle.getString(ISSUED_BY),
                                  parseDate(bundle.getString(VALID_NOT_BEFORE)),
                                  parseDate(bundle.getString(VALID_NOT_AFTER)),
                                  x509Certificate);
    }

    /**
     * Creates a new SSL certificate object
     * @param issuedTo The entity this certificate is issued to
     * @param issuedBy The entity that issued this certificate
     * @param validNotBefore The not-before date from the certificate
     *     validity period in ISO 8601 format
     * @param validNotAfter The not-after date from the certificate
     *     validity period in ISO 8601 format
     * @deprecated Use {@link #SslCertificate(X509Certificate)}
     */
    @Deprecated
    public SslCertificate(
            String issuedTo, String issuedBy, String validNotBefore, String validNotAfter) {
        this(issuedTo, issuedBy, parseDate(validNotBefore), parseDate(validNotAfter), null);
    }

    /**
     * Creates a new SSL certificate object
     * @param issuedTo The entity this certificate is issued to
     * @param issuedBy The entity that issued this certificate
     * @param validNotBefore The not-before date from the certificate validity period
     * @param validNotAfter The not-after date from the certificate validity period
     * @deprecated Use {@link #SslCertificate(X509Certificate)}
     */
    @Deprecated
    public SslCertificate(
            String issuedTo, String issuedBy, Date validNotBefore, Date validNotAfter) {
        this(issuedTo, issuedBy, validNotBefore, validNotAfter, null);
    }

    /**
     * Creates a new SSL certificate object from an X509 certificate
     * @param certificate X509 certificate
     */
    public SslCertificate(X509Certificate certificate) {
        this(certificate.getSubjectDN().getName(),
             certificate.getIssuerDN().getName(),
             certificate.getNotBefore(),
             certificate.getNotAfter(),
             certificate);
    }

    private SslCertificate(
            String issuedTo, String issuedBy,
            Date validNotBefore, Date validNotAfter,
            X509Certificate x509Certificate) {
        mIssuedTo = new DName(issuedTo);
        mIssuedBy = new DName(issuedBy);
        mValidNotBefore = cloneDate(validNotBefore);
        mValidNotAfter  = cloneDate(validNotAfter);
        mX509Certificate = x509Certificate;
    }

    /**
     * @return Not-before date from the certificate validity period or
     * "" if none has been set
     */
    public Date getValidNotBeforeDate() {
        return cloneDate(mValidNotBefore);
    }

    /**
     * @return Not-before date from the certificate validity period in
     * ISO 8601 format or "" if none has been set
     *
     * @deprecated Use {@link #getValidNotBeforeDate()}
     */
    @Deprecated
    public String getValidNotBefore() {
        return formatDate(mValidNotBefore);
    }

    /**
     * @return Not-after date from the certificate validity period or
     * "" if none has been set
     */
    public Date getValidNotAfterDate() {
        return cloneDate(mValidNotAfter);
    }

    /**
     * @return Not-after date from the certificate validity period in
     * ISO 8601 format or "" if none has been set
     *
     * @deprecated Use {@link #getValidNotAfterDate()}
     */
    @Deprecated
    public String getValidNotAfter() {
        return formatDate(mValidNotAfter);
    }

    /**
     * @return Issued-to distinguished name or null if none has been set
     */
    public DName getIssuedTo() {
        return mIssuedTo;
    }

    /**
     * @return Issued-by distinguished name or null if none has been set
     */
    public DName getIssuedBy() {
        return mIssuedBy;
    }

    /**
     * @return The {@code X509Certificate} used to create this {@code SslCertificate} or
     * {@code null} if no certificate was provided.
     */
    public @Nullable X509Certificate getX509Certificate() {
        return mX509Certificate;
    }

    /**
     * Convenience for UI presentation, not intended as public API.
     */
    @UnsupportedAppUsage
    private static String getSerialNumber(X509Certificate x509Certificate) {
        if (x509Certificate == null) {
            return "";
        }
        BigInteger serialNumber = x509Certificate.getSerialNumber();
        if (serialNumber == null) {
            return "";
        }
        return fingerprint(serialNumber.toByteArray());
    }

    /**
     * Convenience for UI presentation, not intended as public API.
     */
    @UnsupportedAppUsage
    private static String getDigest(X509Certificate x509Certificate, String algorithm) {
        if (x509Certificate == null) {
            return "";
        }
        try {
            byte[] bytes = x509Certificate.getEncoded();
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(bytes);
            return fingerprint(digest);
        } catch (CertificateEncodingException ignored) {
            return "";
        } catch (NoSuchAlgorithmException ignored) {
            return "";
        }
    }

    private static final String fingerprint(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            sb.append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xf, 16))); // HexDump.appendByteAsHex(sb, b, true)
            sb.append(Character.toUpperCase(Character.forDigit(b & 0xf, 16)));
            if (i+1 != bytes.length) {
                sb.append(':');
            }
        }
        return sb.toString();
    }

    /**
     * @return A string representation of this certificate for debugging
     */
    public String toString() {
        return ("Issued to: " + mIssuedTo.getDName() + ";\n"
                + "Issued by: " + mIssuedBy.getDName() + ";\n");
    }

    /**
     * Parse an ISO 8601 date converting ParseExceptions to a null result;
     */
    private static Date parseDate(String string) {
        try {
            return new SimpleDateFormat(ISO_8601_DATE_FORMAT).parse(string);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Format a date as an ISO 8601 string, return "" for a null date
     */
    private static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat(ISO_8601_DATE_FORMAT).format(date);
    }

    /**
     * Clone a possibly null Date
     */
    private static Date cloneDate(Date date) {
        if (date == null) {
            return null;
        }
        return (Date) date.clone();
    }

    /**
     * A distinguished name helper class: a 3-tuple of:
     * <ul>
     *   <li>the most specific common name (CN)</li>
     *   <li>the most specific organization (O)</li>
     *   <li>the most specific organizational unit (OU)</li>
     * <ul>
     */
    public class DName {
        /**
         * Distinguished name (normally includes CN, O, and OU names)
         */
        private String mDName;

        /**
         * Common-name (CN) component of the name
         */
        private String mCName;

        /**
         * Organization (O) component of the name
         */
        private String mOName;

        /**
         * Organizational Unit (OU) component of the name
         */
        private String mUName;

        /**
         * Creates a new {@code DName} from a string. The attributes
         * are assumed to come in most significant to least
         * significant order which is true of human readable values
         * returned by methods such as {@code X500Principal.getName()}.
         * Be aware that the underlying sources of distinguished names
         * such as instances of {@code X509Certificate} are encoded in
         * least significant to most significant order, so make sure
         * the value passed here has the expected ordering of
         * attributes.
         */
        public DName(String dName) {
            if (dName != null) {
                mDName = dName;
                try {
                    /* AOSP parses with BouncyCastle's X509Name, which ATL does not have; X509NameTokenizer's rules:
                     * attributes split at unescaped ',' / ';' / '+' outside quotes, type names case-insensitive,
                     * OID forms accepted, quotes and backslash escapes removed from the value */
                    List<String[]> attrs = parseDName(dName);
                    for (String[] a : attrs) {
                        String type = a[0];
                        if (type.equalsIgnoreCase("CN") || type.equals("2.5.4.3") || type.equalsIgnoreCase("OID.2.5.4.3")) {
                            if (mCName == null) {
                                mCName = a[1];
                            }
                            continue;
                        }

                        if (type.equalsIgnoreCase("O") || type.equals("2.5.4.10") || type.equalsIgnoreCase("OID.2.5.4.10")) {
                            if (mOName == null) {
                                mOName = a[1];
                                continue;
                            }
                        }

                        if (type.equalsIgnoreCase("OU") || type.equals("2.5.4.11") || type.equalsIgnoreCase("OID.2.5.4.11")) {
                            if (mUName == null) {
                                mUName = a[1];
                                continue;
                            }
                        }
                    }
                } catch (IllegalArgumentException ex) {
                    // thrown if there is an error parsing the string
                }
            }
        }

        private List<String[]> parseDName(String dName) {
            List<String[]> out = new ArrayList<>();
            StringBuilder tok = new StringBuilder();
            boolean quoted = false, escaped = false;
            for (int i = 0; i <= dName.length(); i++) {
                char c = i < dName.length() ? dName.charAt(i) : ',';
                if (escaped) {
                    tok.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    quoted = !quoted;
                } else if (!quoted && (c == ',' || c == ';' || c == '+')) {
                    String t = tok.toString();
                    tok.setLength(0);
                    int eq = t.indexOf('=');
                    if (t.trim().isEmpty())
                        continue;
                    if (eq < 0)
                        throw new IllegalArgumentException("badly formated directory string");
                    out.add(new String[] {t.substring(0, eq).trim(), t.substring(eq + 1).trim()});
                } else {
                    tok.append(c);
                }
            }
            return out;
        }

        /**
         * @return The distinguished name (normally includes CN, O, and OU names)
         */
        public String getDName() {
            return mDName != null ? mDName : "";
        }

        /**
         * @return The most specific Common-name (CN) component of this name
         */
        public String getCName() {
            return mCName != null ? mCName : "";
        }

        /**
         * @return The most specific Organization (O) component of this name
         */
        public String getOName() {
            return mOName != null ? mOName : "";
        }

        /**
         * @return The most specific Organizational Unit (OU) component of this name
         */
        public String getUName() {
            return mUName != null ? mUName : "";
        }
    }
}
