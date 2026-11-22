package android.security.keystore;

import android.atl.ATLLoadedApp;
import android.atl.ATLSecrets;
import android.util.Slog;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

public class AndroidKeyStore extends KeyStoreSpi {

	private final static String TAG = "AndroidKeyStore";

	private static native byte[] nativeGetKey(String package_name, String alias);
	private static native void nativeDeleteKey(String package_name, String alias);

	/* Like keystore2, which keeps an app's keys until the app's data is cleared, the keys live in the app's data
	 * dir, one file per alias. Keys that earlier versions kept in the desktop keyring (libsecret) are moved there
	 * on first use, so data encrypted with them stays readable. */
	private static String packageName() {
		return ATLLoadedApp.getPrimaryApplication().getApplication().getPackageName();
	}

	private static File keyFile(String alias) {
		File dir = new File(ATLLoadedApp.getPrimaryApplication().getApplication().getDataDir(), "keystore");
		return new File(dir, Base64.getUrlEncoder().withoutPadding().encodeToString(alias.getBytes(StandardCharsets.UTF_8)));
	}

	/* the entry's certificate, beside the key; base64url has no '.', so this cannot be another alias's key file */
	private static File certificateFile(String alias) {
		File key = keyFile(alias);
		return new File(key.getParentFile(), key.getName() + ".crt");
	}

	private static byte[] readKey(String alias) {
		File file = keyFile(alias);
		if (file.exists()) {
			try {
				return Files.readAllBytes(file.toPath());
			} catch (IOException e) {
				Slog.w(TAG, "cannot read key " + alias, e);
				return null;
			}
		}
		byte[] data = ATLSecrets.mayHoldSecrets() ? nativeGetKey(packageName(), alias) : null;
		if (data != null) {
			try {
				writeKey(alias, data);
			} catch (IOException e) {
				Slog.w(TAG, "cannot move key " + alias + " from the keyring", e);
			}
		}
		return data;
	}

	private static void writeKey(String alias, byte[] data) throws IOException {
		writeFile(keyFile(alias), data);
	}

	private static void writeFile(File file, byte[] data) throws IOException {
		File tmp = new File(file.getPath() + ".tmp");
		file.getParentFile().mkdirs();
		Files.write(tmp.toPath(), data);
		Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	@Override
	public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
		Slog.i(TAG, "engineGetKey alias=" + alias + " password=" + Arrays.toString(password));

		byte[] data = readKey(alias);
		if (data == null)
			return null;
		ByteBuffer buffer = ByteBuffer.wrap(data);
		byte[] algorithmBytes = new byte[buffer.getInt()];
		buffer.get(algorithmBytes);
		byte[] keyBytes = new byte[buffer.remaining()];
		buffer.get(keyBytes);
		String algorithm = new String(algorithmBytes, StandardCharsets.UTF_8);
		if ("RSA".equals(algorithm) || "EC".equals(algorithm)) {
			// a private key entry, kept in PKCS#8
			try {
				PrivateKey key = KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
				if (key instanceof RSAPrivateKey)
					return new AndroidKeyStoreRSAPrivateKey((RSAPrivateKey)key);
				if (key instanceof ECPrivateKey)
					return new AndroidKeyStoreECPrivateKey((ECPrivateKey)key);
				return new AndroidKeyStorePrivateKey(key);
			} catch (InvalidKeySpecException e) {
				throw (UnrecoverableKeyException)new UnrecoverableKeyException("cannot load key " + alias).initCause(e);
			}
		}
		return new AndroidKeyStoreSecretKey(keyBytes, algorithm);
	}

	@Override
	public Certificate[] engineGetCertificateChain(String alias) {
		// like AndroidKeyStoreSpi: the leaf certificate, or null for an entry without one
		Certificate leaf = engineGetCertificate(alias);
		return leaf != null ? new Certificate[] {leaf} : null;
	}

	@Override
	public Certificate engineGetCertificate(String alias) {
		File file = certificateFile(alias);
		if (!file.exists())
			return null;
		try {
			return CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(Files.readAllBytes(file.toPath())));
		} catch (IOException | CertificateException e) {
			Slog.w(TAG, "cannot read the certificate of " + alias, e);
			return null;
		}
	}

	@Override
	public Date engineGetCreationDate(String alias) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'engineGetCreationDate'");
	}

	@Override
	public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
		byte[] certificate = null;
		if (key instanceof PrivateKey) {
			// AndroidKeyStoreSpi.setPrivateKeyEntry(): the entry keeps the key's certificate
			if (chain == null || chain.length == 0)
				throw new KeyStoreException("Must supply at least one Certificate with PrivateKey");
			try {
				certificate = chain[0].getEncoded();
			} catch (CertificateEncodingException e) {
				throw new KeyStoreException("Failed to encode certificate #0", e);
			}
			if (key instanceof AndroidKeyStorePrivateKey)
				key = ((AndroidKeyStorePrivateKey)key).key;
		}
		byte[] algorithm = key.getAlgorithm().getBytes(StandardCharsets.UTF_8);
		byte[] encoded = key.getEncoded();
		ByteBuffer buffer = ByteBuffer.allocate(4 + algorithm.length + encoded.length);
		buffer.putInt(algorithm.length);
		buffer.put(algorithm);
		buffer.put(encoded);
		try {
			writeKey(alias, buffer.array());
			if (certificate != null)
				writeFile(certificateFile(alias), certificate);
			else
				Files.deleteIfExists(certificateFile(alias).toPath());
		} catch (IOException e) {
			throw new KeyStoreException("cannot store key " + alias, e);
		}
	}

	/* AOSP AndroidKeyStoreSpi.engineSetEntry(): KeyProtection (or none) is the protection for secret and private key
	 * entries. ATL stores the key as engineSetKeyEntry() does and does not evaluate the KeyProtection settings. */
	@Override
	public void engineSetEntry(String alias, java.security.KeyStore.Entry entry, java.security.KeyStore.ProtectionParameter param) throws KeyStoreException {
		if (entry == null) {
			throw new KeyStoreException("entry == null");
		}
		if (param != null && !(param instanceof KeyProtection)) {
			throw new KeyStoreException("Unsupported protection parameter class: " + param.getClass().getName() + ". Supported: " + KeyProtection.class.getName());
		}
		if (entry instanceof java.security.KeyStore.PrivateKeyEntry) {
			java.security.KeyStore.PrivateKeyEntry e = (java.security.KeyStore.PrivateKeyEntry) entry;
			engineSetKeyEntry(alias, e.getPrivateKey(), null, e.getCertificateChain());
		} else if (entry instanceof java.security.KeyStore.SecretKeyEntry) {
			engineSetKeyEntry(alias, ((java.security.KeyStore.SecretKeyEntry) entry).getSecretKey(), null, null);
		} else {
			throw new KeyStoreException("Entry must be a PrivateKeyEntry, SecretKeyEntry or TrustedCertificateEntry; was " + entry);
		}
	}

	@Override
	public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'engineSetKeyEntry'");
	}

	@Override
	public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'engineSetCertificateEntry'");
	}

	@Override
	public void engineDeleteEntry(String alias) throws KeyStoreException {
		// like keystore2: an unknown alias is not an error, any other failure is a KeyStoreException
		try {
			Files.deleteIfExists(keyFile(alias).toPath());
			Files.deleteIfExists(certificateFile(alias).toPath());
		} catch (IOException e) {
			throw new KeyStoreException("Failed to delete entry: " + alias, e);
		}
		// a key an earlier version left in the keyring would otherwise be moved back by readKey()
		if (ATLSecrets.mayHoldSecrets())
			nativeDeleteKey(packageName(), alias);
	}

	@Override
	public Enumeration<String> engineAliases() {
		Slog.i(TAG, "engineAliases() called");
		return Collections.emptyEnumeration();
	}

	@Override
	public boolean engineContainsAlias(String alias) {
		Slog.i(TAG, "engineContainsAlias(" + alias + ") called");
		return engineIsKeyEntry(alias);
	}

	@Override
	public int engineSize() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'engineSize'");
	}

	@Override
	public boolean engineIsKeyEntry(String alias) {
		return readKey(alias) != null;
	}

	@Override
	public boolean engineIsCertificateEntry(String alias) {
		return false;
	}

	@Override
	public String engineGetCertificateAlias(Certificate cert) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'engineGetCertificateAlias'");
	}

	@Override
	public void engineStore(OutputStream stream, char[] password)
	    throws IOException, NoSuchAlgorithmException, CertificateException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'engineStore'");
	}

	@Override
	public void engineLoad(InputStream stream, char[] password)
	    throws IOException, NoSuchAlgorithmException, CertificateException {
	}
}
