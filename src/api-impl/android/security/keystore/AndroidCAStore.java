package android.security.keystore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;

/**
 * KeyStore.getInstance("AndroidCAStore"): the certificates the system trusts (AOSP: the files of /system/etc/security/cacerts
 * and the user's). ATL's are the trust store the process was started with (-Djavax.net.ssl.trustStore, which
 * APK Starter sets) or the system's Java store. It is read-only, as AOSP's is for apps.
 */
public class AndroidCAStore extends KeyStoreSpi {
	private KeyStore store;

	private synchronized KeyStore store() {
		if (store == null) {
			try {
				String path = System.getProperty("javax.net.ssl.trustStore", "/etc/ssl/certs/java/cacerts");
				String password = System.getProperty("javax.net.ssl.trustStorePassword", "changeit");
				File file = new File(path);
				KeyStore ks = null;
				/* the Java store of Debian is PKCS12, older ones are JKS */
				for (String type : new String[] {"PKCS12", "JKS"}) {
					try (InputStream in = file.exists() ? new FileInputStream(file) : null) {
						KeyStore candidate = KeyStore.getInstance(type);
						candidate.load(in, in != null ? password.toCharArray() : null);
						ks = candidate;
						break;
					} catch (IOException | java.security.GeneralSecurityException e) {
						if (type.equals("JKS"))
							throw e;
					}
				}
				store = ks;
			} catch (Exception e) {
				throw new IllegalStateException("system trust store: " + e, e);
			}
		}
		return store;
	}

	@Override public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException { return null; }
	@Override public Certificate[] engineGetCertificateChain(String alias) { return null; }
	@Override public Certificate engineGetCertificate(String alias) {
		try { return store().getCertificate(alias); } catch (KeyStoreException e) { return null; }
	}
	@Override public Date engineGetCreationDate(String alias) {
		try { return store().getCreationDate(alias); } catch (KeyStoreException e) { return null; }
	}
	@Override public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
		throw new KeyStoreException("the system trust store is read-only");
	}
	@Override public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
		throw new KeyStoreException("the system trust store is read-only");
	}
	@Override public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
		throw new KeyStoreException("the system trust store is read-only");
	}
	@Override public void engineDeleteEntry(String alias) throws KeyStoreException {
		throw new KeyStoreException("the system trust store is read-only");
	}
	@Override public Enumeration<String> engineAliases() {
		try { return store().aliases(); } catch (KeyStoreException e) { return java.util.Collections.emptyEnumeration(); }
	}
	@Override public boolean engineContainsAlias(String alias) {
		try { return store().containsAlias(alias); } catch (KeyStoreException e) { return false; }
	}
	@Override public int engineSize() {
		try { return store().size(); } catch (KeyStoreException e) { return 0; }
	}
	@Override public boolean engineIsKeyEntry(String alias) { return false; }
	@Override public boolean engineIsCertificateEntry(String alias) { return engineContainsAlias(alias); }
	@Override public String engineGetCertificateAlias(Certificate cert) {
		try { return store().getCertificateAlias(cert); } catch (KeyStoreException e) { return null; }
	}
	@Override public void engineStore(OutputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
		throw new UnsupportedOperationException("the system trust store is read-only");
	}
	@Override public void engineLoad(InputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
		// AOSP's AndroidCAStore ignores the arguments: it always shows the system's certificates
	}
}
