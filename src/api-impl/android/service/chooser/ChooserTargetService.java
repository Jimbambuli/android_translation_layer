package android.service.chooser;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import java.util.List;

/* AOSP ChooserTargetService (deprecated since Q): the system's chooser binds it for direct share targets.
 * ATL has no chooser that asks, so onGetChooserTargets() is never called; AOSP's onBind() hands out the
 * IChooserTargetService binder only to the SERVICE_INTERFACE action, which nobody binds here */
@Deprecated
public abstract class ChooserTargetService extends Service {
	public static final String BIND_PERMISSION = "android.permission.BIND_CHOOSER_TARGET_SERVICE";
	public static final String SERVICE_INTERFACE = "android.service.chooser.ChooserTargetService";
	public static final String META_DATA_NAME = "android.service.chooser.chooser_target_service";

	public abstract List<ChooserTarget> onGetChooserTargets(ComponentName targetComponent, IntentFilter matchedFilter);

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
