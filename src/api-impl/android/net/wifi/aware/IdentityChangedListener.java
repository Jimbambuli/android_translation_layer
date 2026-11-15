package android.net.wifi.aware;

import android.annotation.NonNull;
import android.net.MacAddress;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/aware/IdentityChangedListener.java */
public class IdentityChangedListener {
	public static final int CLUSTER_CHANGE_EVENT_STARTED = 0;
	public static final int CLUSTER_CHANGE_EVENT_JOINED = 1;

	public void onIdentityChanged(byte[] mac) {
	}

	public void onClusterIdChanged(int clusterEventType, @NonNull MacAddress clusterId) {
	}
}
