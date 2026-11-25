# Threads in ATL: Android's contract on top of GTK

Android lets apps call many graphics and media APIs from any thread; GTK allows almost nothing outside its main
thread. This file states which ATL code may run where, and how to check it.

## Rules of the layers below

- GTK 4 (docs/reference/gtk/section-threading.md): GTK, GDK and GSK objects belong to the main thread (the one
  iterating the default GMainContext). Exceptions: immutable objects (GdkTexture, GskRenderNode, GskPath,
  GskTransform, GdkColorState) on any thread; builders (GtkSnapshot, Gdk*TextureBuilder, GskPathBuilder) on any
  thread, one thread at a time; non-threadsafe objects in the thread that created them (e.g. a GskRenderer made by
  a worker thread). Downloading a GL texture off the main thread waits for the main thread (gdkgltexture.c).
- libwayland-client: requests may be sent from any thread, but the default queue of a wl_display is dispatched by
  one thread (GDK's main thread). Objects made in another thread go to a queue of their own
  (wl_proxy_create_wrapper() + wl_proxy_set_queue()), or ATL uses a connection of its own (android_view_Display.c).
- EGL: a context is current in one thread at a time; a surface is swapped in the thread where it is current; a
  wl_egl_window outlives its EGLSurface and is resized by the thread drawing to it.

## Android's contract and what ATL does underneath

| Android, called from | ATL underneath may | ATL must not |
|---|---|---|
| Bitmap, Canvas(Bitmap): any thread, caller serializes per bitmap | pixel memory of the bitmap; GdkTexture made for display | use one GtkSnapshot/GskRenderer from two threads; download GL textures |
| EGL/GLES with own context: any thread | ATL's EGL hull (libandroid/egl.c) with the app's context; frames handed to GTK with g_idle_add() | call gtk_*/gdk_* or make a GdkGLContext current in the app's thread |
| ANativeWindow_fromSurface/getWidth/getHeight, lock/unlockAndPost, Vulkan WSI, AHardwareBuffer: any thread | read SurfaceViewWidget.width/height/realized (atomics kept by the main thread); build memory/dmabuf textures; present with g_idle_add() | read the widget tree |
| SurfaceHolder.Callback, View: UI thread | GTK directly | – |
| View, PopupWindow built and set up on another thread before they are attached (AsyncLayoutInflater, prefetch) | keep the state in Java; View.gtk()/PopupWindow.gtk() queue the native calls, the main thread makes the widget when it first needs it (addView() at the latest) and runs them in order; getters answer from Java fields | make or change a widget in that thread |
| Drawable changed on another thread | change the paintable's data; atl_paintable_invalidate() signals from the main loop | gdk_paintable_invalidate_*() in that thread |
| MediaCodec / SurfaceTexture / camera frames: codec, binder, camera threads | the SurfaceViewWidget from surface_view_widget_of(); widget updates on the main thread | gtk_widget_get_first_child() and other tree reads |
| finalizers (FinalizerDaemon) | g_idle_add() with the pointer; everything else on the main thread | read or change widgets |
| Pango text in any thread | the thread's own PangoContext with atl_display_info's resolution and font options | the window's PangoContext |
| GTK's display in app threads (EGL display, Wayland handles) | atl_display_info, taken on the main thread at start | gtk_root_get_display() and friends |

A thread that needs GTK hands the work to the main thread (g_idle_add(), or g_main_context_invoke()); it waits for
the result only if the main thread can never be waiting for it (SurfaceHolder.Callback.surfaceDestroyed waits for
the render thread, see GLSurfaceView), so render threads read state the main thread keeps for them instead.

## Checking it

Debug builds on x86_64 build `libatl_threadcheck.so`. With `ATL_THREADCHECK=1` ATL starts itself again with the
library preloaded; it interposes every libgtk-4 function and the Wayland/EGL calls above and reports each call site
that breaks the rules once, with the native and the Java stack, on stderr:

    ATL-THREADCHECK: off-main gtk_widget_get_parent thread="FinalizerDaemon" tid=63 main=48 caller=libtranslation_layer_main.so+0x137b91 (...)
    ATL-THREADCHECK:   #0 ...
    ATL-THREADCHECK:   java: android.view.View.finalize(View.java:3434)

At exit, `ATL-THREADCHECK-SUMMARY:` lines count the calls per site. Kinds: `off-main`, `builder-concurrent`,
`foreign-owned`, `wl-default-queue`, `egl-context-two-threads`, `egl-swap-foreign-surface`,
`wl-egl-window-foreign-thread`, `wl-egl-window-outlived`. GTK's own calls are not seen (libgtk-4 is linked with
-Bsymbolic), nor are calls a library makes to itself.
