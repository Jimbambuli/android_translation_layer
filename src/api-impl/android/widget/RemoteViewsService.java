/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.widget;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * The service to be connected to for a remote adapter to request RemoteViews.  Users should
 * extend the RemoteViewsService to provide the appropriate RemoteViewsFactory's used to
 * populate the remote collection view (ListView, GridView, etc).
 *
 * ATL has no app widget host that could bind it: onBind() gives no binder (AOSP's binder is the
 * IRemoteViewsFactory of the widget host).
 */
public abstract class RemoteViewsService extends Service {

	/**
	 * An interface for an adapter between a remote collection view (ListView, GridView, etc) and
	 * the underlying data for that view.
	 */
	public interface RemoteViewsFactory {
		public void onCreate();

		public void onDataSetChanged();

		public void onDestroy();

		public int getCount();

		public RemoteViews getViewAt(int position);

		public RemoteViews getLoadingView();

		public int getViewTypeCount();

		public long getItemId(int position);

		public boolean hasStableIds();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * To be implemented by the derived service to generate appropriate factories for
	 * the data.
	 */
	public abstract RemoteViewsFactory onGetViewFactory(Intent intent);
}
