package android.security.keystore;

import android.atl.ATLLoadedApp;
import android.util.Slog;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

public class AndroidKeyStore extends KeyStoreSpi {

	private final static String TAG = "AndroidKeyStore";

	private static native byte[] nativeGetKey(String package_name, String alias);

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
		byte[] data = nativeGetKey(packageName(), alias);
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
		File file = keyFile(alias);
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
		return new AndroidKeyStoreSecretKey(keyBytes, new String(algorithmBytes, StandardCharsets.UTF_8));
	}

	@Override
	public Certificate[] engineGetCertificateChain(String alias) {
		Slog.i(TAG, "engineGetCertificateChain(" + alias + ") called");
		return new Certificate[0];
	}

	@Override
	public Certificate engineGetCertificate(String alias) {
		Slog.i(TAG, "engineGetCertificate(" + alias + ") called");
		return null;
	}

	@Override
	public Date engineGetCreationDate(String alias) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'engineGetCreationDate'");
	}

	@Override
	public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
		byte[] algorithm = key.getAlgorithm().getBytes(StandardCharsets.UTF_8);
		byte[] encoded = key.getEncoded();
		ByteBuffer buffer = ByteBuffer.allocate(4 + algorithm.length + encoded.length);
		buffer.putInt(algorithm.length);
		buffer.put(algorithm);
		buffer.put(encoded);
		try {
			writeKey(alias, buffer.array());
		} catch (IOException e) {
			throw new KeyStoreException("cannot store key " + alias, e);
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
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'engineDeleteEntry'");
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
