package android.content.res.loader;

import android.content.res.Resources;
import java.util.ArrayList;
import java.util.List;

/* as in AOSP, a list of providers; a change reaches the Resources it was added to */
public class ResourcesLoader {
	private final ArrayList<ResourcesProvider> providers = new ArrayList<>();

	public List<ResourcesProvider> getProviders() {
		synchronized (providers) {
			return new ArrayList<>(providers);
		}
	}

	public void addProvider(ResourcesProvider provider) {
		synchronized (providers) {
			if (!providers.contains(provider))
				providers.add(provider);
		}
		Resources.atl_loadersChanged();
	}

	public void removeProvider(ResourcesProvider provider) {
		synchronized (providers) {
			providers.remove(provider);
		}
		Resources.atl_loadersChanged();
	}

	public void setProviders(List<ResourcesProvider> newProviders) {
		synchronized (providers) {
			providers.clear();
			providers.addAll(newProviders);
		}
		Resources.atl_loadersChanged();
	}

	public void clearProviders() {
		synchronized (providers) {
			providers.clear();
		}
		Resources.atl_loadersChanged();
	}
}
