package android.media.effect;

/* AOSP frameworks/base/media/java/android/media/effect/Effect.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public abstract class Effect {
    public Effect()  {}
    public abstract void apply(int arg0, int arg1, int arg2, int arg3);
    public abstract String getName();
    public abstract void release();
    public abstract void setParameter(String arg0, Object arg1);
    public void setUpdateListener(android.media.effect.EffectUpdateListener arg0) {  }
}
