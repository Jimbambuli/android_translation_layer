package android.atl;

import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.PackageParser;
import android.util.Slog;
import java.util.Objects;

public final class ATLProvider {
	private static final String TAG = "ATLProvider";
	private final ATLLoadedApp atlLoadedApp;
	private final PackageParser.Provider provider;
	private ContentProvider contentProvider;
	private boolean triedLoading;

	public ATLProvider(ATLLoadedApp atlLoadedApp, PackageParser.Provider provider) {
		Objects.requireNonNull(atlLoadedApp);
		Objects.requireNonNull(provider);
		this.atlLoadedApp = atlLoadedApp;
		this.provider = provider;
		this.triedLoading = false;
	}

	public ATLProvider(ContentProvider contentProvider) {
		Objects.requireNonNull(contentProvider);
		this.atlLoadedApp = null;
		this.provider = null;
		this.contentProvider = contentProvider;
		this.triedLoading = true;
	}

	public ContentProvider getContentProvider() {
		if (this.triedLoading) {
			return this.contentProvider;
		}
		this.triedLoading = true;
		ContentProvider contentProvider = null;
		Slog.i(TAG, "Loading content provider " + this.provider.info.authority + "(" + this.provider.className + ")");
		try {
			/* ActivityThread.installProvider(): the app's AppComponentFactory, with the class loader of the app's context
			 * or of its split's context */
			Context c = this.atlLoadedApp.getApplication();
			if (this.provider.info.splitName != null)
				c = c.createContextForSplit(this.provider.info.splitName);
			contentProvider = this.atlLoadedApp.app_component_factory.instantiateProvider(
			    c.getClassLoader(), this.provider.className);
			contentProvider.attachInfo(c, this.provider.info);
			contentProvider.onCreate();
			this.contentProvider = contentProvider;
		} catch (Throwable t) {
			// Catch all errors when loading a content provider to help the application boot up.
			Slog.e(TAG, "Failed to load content provider " + this.provider.info.authority, t);
		}
		return contentProvider;
	}
}
