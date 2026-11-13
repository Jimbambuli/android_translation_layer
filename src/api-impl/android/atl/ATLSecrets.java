package android.atl;

import java.io.File;

/* Where AccountManager and AndroidKeyStore keep secrets: libsecret. In a Flatpak libsecret uses its file backend,
 * a keyring file in the app's data dir whose key it gets from the Secret portal; the first call shows the desktop's
 * password dialog (KWallet: «create a wallet», «unlock») and the app waits for it on its main thread, without a
 * window. Before that file exists nothing can be in it, so reading is skipped and the dialog only comes when the
 * app stores a secret. Outside a Flatpak libsecret talks to the secret service directly: always ask it. */
public final class ATLSecrets {
	private ATLSecrets() {}

	public static boolean mayHoldSecrets() {
		if (!new File("/.flatpak-info").exists())
			return true;
		String data = System.getenv("XDG_DATA_HOME");
		if (data == null || data.isEmpty())
			data = System.getenv("HOME") + "/.local/share";
		return new File(data, "keyrings/default.keyring").exists();
	}
}
