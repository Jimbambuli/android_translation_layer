#include "../defines.h"
#include "../util.h"

#include "mpris-dbus.h"

#include "../generated_headers/android_media_session_MediaSession.h"
#include "../generated_headers/android_os_SystemClock.h"

#define MPRIS_OBJECT_NAME "/org/mpris/MediaPlayer2"

MediaPlayer2Player *mpris_player = NULL;
static jobject session = NULL;
/* last PlaybackState, to extrapolate the position like Android's media controls do
 * (SystemUI SeekBarViewModel.computePosition) */
static gboolean is_playing = FALSE;
static jlong state_position = 0;    // ms
static jlong state_update_time = 0; // SystemClock.elapsedRealtime in ms
static jfloat state_speed = 1;
static jlong duration = 0; // ms

static jlong current_position(void)
{
	jlong position = state_position;
	if (is_playing && state_update_time > 0) {
		position += state_speed * (Java_android_os_SystemClock_elapsedRealtime(get_jni_env(), NULL) - state_update_time);
		if (duration > 0 && position > duration)
			position = duration;
	}
	return position < 0 ? 0 : position;
}

static gboolean on_media_player_handle_play(MediaPlayer2Player *mpris_player, GDBusMethodInvocation *invocation, gpointer user_data)
{
	if (session)
		J__MediaSession__dispatchPlay(get_jni_env(), session);
	media_player2_player_complete_play(mpris_player, invocation);
	return TRUE;
}

static gboolean on_media_player_handle_pause(MediaPlayer2Player *mpris_player, GDBusMethodInvocation *invocation, gpointer user_data)
{
	if (session)
		J__MediaSession__dispatchPause(get_jni_env(), session);
	media_player2_player_complete_pause(mpris_player, invocation);
	return TRUE;
}

static gboolean on_media_player_handle_play_pause(MediaPlayer2Player *mpris_player, GDBusMethodInvocation *invocation, gpointer user_data)
{
	if (session) {
		if (is_playing)
			J__MediaSession__dispatchPause(get_jni_env(), session);
		else
			J__MediaSession__dispatchPlay(get_jni_env(), session);
	}
	media_player2_player_complete_play_pause(mpris_player, invocation);
	return TRUE;
}

static gboolean on_media_player_handle_next(MediaPlayer2Player *mpris_player, GDBusMethodInvocation *invocation, gpointer user_data)
{
	if (session)
		J__MediaSession__dispatchNext(get_jni_env(), session);
	media_player2_player_complete_next(mpris_player, invocation);
	return TRUE;
}

static gboolean on_media_player_handle_previous(MediaPlayer2Player *mpris_player, GDBusMethodInvocation *invocation, gpointer user_data)
{
	if (session)
		J__MediaSession__dispatchPrevious(get_jni_env(), session);
	media_player2_player_complete_previous(mpris_player, invocation);
	return TRUE;
}

static gboolean on_media_player_handle_stop(MediaPlayer2Player *mpris_player, GDBusMethodInvocation *invocation, gpointer user_data)
{
	if (session)
		J__MediaSession__dispatchStop(get_jni_env(), session);
	media_player2_player_complete_stop(mpris_player, invocation);
	return TRUE;
}

static gboolean on_media_player_handle_seek(MediaPlayer2Player *mpris_player, GDBusMethodInvocation *invocation, gint64 offset_us, gpointer user_data)
{
	if (session) {
		jlong position = current_position() + offset_us / 1000;
		J__MediaSession__dispatchSeekTo(get_jni_env(), session, position < 0 ? 0 : position);
	}
	media_player2_player_complete_seek(mpris_player, invocation);
	return TRUE;
}

static gboolean on_media_player_handle_set_position(MediaPlayer2Player *mpris_player, GDBusMethodInvocation *invocation, const gchar *trackid, gint64 pos_us, gpointer user_data)
{
	if (session)
		J__MediaSession__dispatchSeekTo(get_jni_env(), session, pos_us / 1000);
	media_player2_player_complete_set_position(mpris_player, invocation);
	return TRUE;
}

/* MPRIS clients read Position on demand and expect the current value; the skeleton only
 * stores the last one set, so answer Position from the extrapolated playback state */
static GDBusInterfaceVTable player_vtable;
static GDBusInterfaceVTable *(*skeleton_get_vtable)(GDBusInterfaceSkeleton *skeleton);

static GVariant *player_get_property(GDBusConnection *connection, const gchar *sender, const gchar *object_path,
                                     const gchar *interface_name, const gchar *property_name, GError **error, gpointer user_data)
{
	if (!strcmp(property_name, "Position"))
		return g_variant_new_int64(current_position() * 1000);
	return skeleton_get_vtable(G_DBUS_INTERFACE_SKELETON(user_data))->get_property(connection, sender, object_path, interface_name, property_name, error, user_data);
}

static GDBusInterfaceVTable *player_get_vtable(GDBusInterfaceSkeleton *skeleton)
{
	player_vtable = *skeleton_get_vtable(skeleton);
	player_vtable.get_property = player_get_property;
	return &player_vtable;
}

#define ACTION_STOP             (1 << 0)
#define ACTION_PAUSE            (1 << 1)
#define ACTION_PLAY             (1 << 2)
#define ACTION_SKIP_TO_PREVIOUS (1 << 4)
#define ACTION_SKIP_TO_NEXT     (1 << 5)
#define ACTION_SEEK_TO          (1 << 8)
#define ACTION_PLAY_PAUSE       (1 << 9)

static void dict_insert_string(JNIEnv *env, GVariantDict *dict, const char *key, jstring value_str)
{
	if (!value_str)
		return;
	const char *value = (*env)->GetStringUTFChars(env, value_str, NULL);
	if (!strcmp(key, "xesam:artist")) // a list of strings in MPRIS
		g_variant_dict_insert_value(dict, key, g_variant_new_strv(&value, 1));
	else
		g_variant_dict_insert(dict, key, "s", value);
	(*env)->ReleaseStringUTFChars(env, value_str, value);
}

JNIEXPORT void JNICALL Java_android_media_session_MediaSession_nativeSetState(JNIEnv *env, jobject this, jint status, jlong actions, jlong position, jlong update_time, jfloat speed,
                                                                              jstring title_str, jstring artist_str, jstring album_str, jlong duration_ms, jstring art_url_str)
{
	const char *playback_states[] = {"Stopped", "Paused", "Playing"};
	if (!mpris_player) {
		mpris_player = media_player2_player_skeleton_new();
		GDBusInterfaceSkeletonClass *skeleton_class = G_DBUS_INTERFACE_SKELETON_GET_CLASS(mpris_player);
		skeleton_get_vtable = skeleton_class->get_vtable;
		skeleton_class->get_vtable = player_get_vtable;
		g_object_connect(mpris_player,
		                 "signal::handle-play", on_media_player_handle_play, NULL,
		                 "signal::handle-pause", on_media_player_handle_pause, NULL,
		                 "signal::handle-play-pause", on_media_player_handle_play_pause, NULL,
		                 "signal::handle-next", on_media_player_handle_next, NULL,
		                 "signal::handle-previous", on_media_player_handle_previous, NULL,
		                 "signal::handle-stop", on_media_player_handle_stop, NULL,
		                 "signal::handle-seek", on_media_player_handle_seek, NULL,
		                 "signal::handle-set-position", on_media_player_handle_set_position, NULL,
		                 NULL);
	}
	jlong expected = current_position();
	gboolean was_playing = is_playing;
	is_playing = status == 2;
	state_position = position;
	state_update_time = update_time;
	state_speed = speed;
	duration = duration_ms;

	media_player2_player_set_playback_status(mpris_player, playback_states[status]);
	media_player2_player_set_position(mpris_player, current_position() * 1000);
	media_player2_player_set_can_control(mpris_player, !!(actions));
	media_player2_player_set_can_play(mpris_player, !!(actions & (ACTION_PLAY | ACTION_PLAY_PAUSE)));
	media_player2_player_set_can_pause(mpris_player, !!(actions & (ACTION_PAUSE | ACTION_PLAY_PAUSE)));
	media_player2_player_set_can_seek(mpris_player, !!(actions & ACTION_SEEK_TO));
	media_player2_player_set_can_go_next(mpris_player, !!(actions & ACTION_SKIP_TO_NEXT));
	media_player2_player_set_can_go_previous(mpris_player, !!(actions & ACTION_SKIP_TO_PREVIOUS));

	GVariantDict dict;
	g_variant_dict_init(&dict, NULL);
	g_variant_dict_insert(&dict, "mpris:trackid", "o", MPRIS_OBJECT_NAME "/Track/0");
	if (duration > 0)
		g_variant_dict_insert(&dict, "mpris:length", "x", (gint64)duration * 1000);
	dict_insert_string(env, &dict, "mpris:artUrl", art_url_str);
	dict_insert_string(env, &dict, "xesam:title", title_str);
	dict_insert_string(env, &dict, "xesam:artist", artist_str);
	dict_insert_string(env, &dict, "xesam:album", album_str);
	media_player2_player_set_metadata(mpris_player, g_variant_dict_end(&dict));
	/* a jump against the extrapolated position is a seek, which MPRIS announces with the Seeked signal */
	if (was_playing == is_playing && ABS(current_position() - expected) > 1000)
		media_player2_player_emit_seeked(mpris_player, current_position() * 1000);
}

JNIEXPORT void JNICALL Java_android_media_session_MediaSession_nativeSetCallback(JNIEnv *env, jobject this, jobject new_session)
{
	if (session)
		(*env)->DeleteGlobalRef(env, session);
	session = new_session ? _REF(new_session) : NULL;
}
