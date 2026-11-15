package android.media.quality;

/* AOSP frameworks/base/media/java/android/media/quality/MediaQualityManager.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public final class MediaQualityManager {
    public void addActiveProcessingPictureListener(java.util.concurrent.Executor arg0, java.util.function.Consumer<java.util.List<android.media.quality.ActiveProcessingPicture>> arg1) {  }
    public void createPictureProfile(android.media.quality.PictureProfile arg0) {  }
    public void createSoundProfile(android.media.quality.SoundProfile arg0) {  }
    public java.util.List<android.media.quality.PictureProfile> getAvailablePictureProfiles(android.media.quality.MediaQualityManager.ProfileQueryParams arg0) { return null; }
    public java.util.List<android.media.quality.SoundProfile> getAvailableSoundProfiles(android.media.quality.MediaQualityManager.ProfileQueryParams arg0) { return null; }
    public java.util.List<android.media.quality.ParameterCapability> getParameterCapabilities(java.util.List<java.lang.String> arg0) { return null; }
    public android.media.quality.PictureProfile getPictureProfile(int arg0, String arg1, android.media.quality.MediaQualityManager.ProfileQueryParams arg2) { return null; }
    public android.media.quality.SoundProfile getSoundProfile(int arg0, String arg1, android.media.quality.MediaQualityManager.ProfileQueryParams arg2) { return null; }
    public boolean isAmbientBacklightEnabled() { return false; }
    public boolean isAutoPictureQualityEnabled() { return false; }
    public boolean isAutoSoundQualityEnabled() { return false; }
    public boolean isSuperResolutionEnabled() { return false; }
    public void registerAmbientBacklightCallback(java.util.concurrent.Executor arg0, android.media.quality.MediaQualityManager.AmbientBacklightCallback arg1) {  }
    public void registerPictureProfileCallback(java.util.concurrent.Executor arg0, android.media.quality.MediaQualityManager.PictureProfileCallback arg1) {  }
    public void registerSoundProfileCallback(java.util.concurrent.Executor arg0, android.media.quality.MediaQualityManager.SoundProfileCallback arg1) {  }
    public void removeActiveProcessingPictureListener(java.util.function.Consumer<java.util.List<android.media.quality.ActiveProcessingPicture>> arg0) {  }
    public void removePictureProfile(String arg0) {  }
    public void removeSoundProfile(String arg0) {  }
    public void setAmbientBacklightEnabled(boolean arg0) {  }
    public void setAmbientBacklightSettings(android.media.quality.AmbientBacklightSettings arg0) {  }
    public void unregisterAmbientBacklightCallback(android.media.quality.MediaQualityManager.AmbientBacklightCallback arg0) {  }
    public void unregisterPictureProfileCallback(android.media.quality.MediaQualityManager.PictureProfileCallback arg0) {  }
    public void unregisterSoundProfileCallback(android.media.quality.MediaQualityManager.SoundProfileCallback arg0) {  }
    public void updatePictureProfile(String arg0, android.media.quality.PictureProfile arg1) {  }
    public void updateSoundProfile(String arg0, android.media.quality.SoundProfile arg1) {  }

    public static interface AmbientBacklightCallback {
        void onAmbientBacklightEvent(android.media.quality.AmbientBacklightEvent arg0);
    }

    public abstract static class PictureProfileCallback {
        public PictureProfileCallback()  {}
        public void onError(String arg0, int arg1) {  }
        public void onParameterCapabilitiesChanged(String arg0, java.util.List<android.media.quality.ParameterCapability> arg1) {  }
        public void onPictureProfileAdded(String arg0, android.media.quality.PictureProfile arg1) {  }
        public void onPictureProfileRemoved(String arg0, android.media.quality.PictureProfile arg1) {  }
        public void onPictureProfileUpdated(String arg0, android.media.quality.PictureProfile arg1) {  }
    }

    public static final class ProfileQueryParams implements android.os.Parcelable {
        public boolean areParametersIncluded() { return false; }
        public int describeContents() { return 0; }
        public void writeToParcel(android.os.Parcel arg0, int arg1) {  }

        public static final class Builder {
            public Builder()  {}
            public android.media.quality.MediaQualityManager.ProfileQueryParams build() { return null; }
            public android.media.quality.MediaQualityManager.ProfileQueryParams.Builder setParametersIncluded(boolean arg0) { return this; }
        }
    }

    public abstract static class SoundProfileCallback {
        public SoundProfileCallback()  {}
        public void onError(String arg0, int arg1) {  }
        public void onParameterCapabilitiesChanged(String arg0, java.util.List<android.media.quality.ParameterCapability> arg1) {  }
        public void onSoundProfileAdded(String arg0, android.media.quality.SoundProfile arg1) {  }
        public void onSoundProfileRemoved(String arg0, android.media.quality.SoundProfile arg1) {  }
        public void onSoundProfileUpdated(String arg0, android.media.quality.SoundProfile arg1) {  }
    }
}
