/*
 * Copyright (C) 2014 The Android Open Source Project
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

/* C port of frameworks/base core/jni/android_view_VelocityTracker.cpp (android-14). The native MotionEvent
 * of Android (android_view_MotionEvent_getNativePtr) does not exist in ATL, whose MotionEvent is a Java class
 * without history or resampling: VelocityTracker::addMovement(const MotionEvent*) reads the current sample
 * through the exported MotionEvent getters instead of getHistoricalEventTime/getHistoricalAxisValue. */

#include <stdlib.h>
#include <string.h>

#include "../generated_headers/android_view_VelocityTracker.h"
#include "../input/VelocityTracker.h"
#include "../util.h"

// MotionEvent actions (AMOTION_EVENT_ACTION_*)
#define AMOTION_EVENT_ACTION_DOWN 0
#define AMOTION_EVENT_ACTION_UP 1
#define AMOTION_EVENT_ACTION_MOVE 2
#define AMOTION_EVENT_ACTION_POINTER_DOWN 5
#define AMOTION_EVENT_ACTION_POINTER_UP 6
#define AMOTION_EVENT_ACTION_HOVER_MOVE 7
#define AMOTION_EVENT_ACTION_SCROLL 8
#define AMOTION_EVENT_ACTION_HOVER_ENTER 9

// Special constant to request the velocity of the active pointer.
static const int32_t ACTIVE_POINTER_ID = -1;

static const nsecs_t NANOS_PER_MS = 1000000;

// --- VelocityTrackerState ---

struct velocity_tracker_state {
	struct velocity_tracker velocity_tracker;
	struct velocity_computed computed_velocity;
};

static void add_axes(const int32_t *axes, uint32_t count, int32_t *axes_to_process, uint32_t *axes_count)
{
	for (uint32_t i = 0; i < count; i++)
		axes_to_process[(*axes_count)++] = axes[i];
}

// VelocityTracker::addMovement(const MotionEvent* event)
static void velocity_tracker_state_add_movement(JNIEnv *env, struct velocity_tracker_state *state, jobject event)
{
	struct velocity_tracker *tracker = &state->velocity_tracker;
	// Axes specifying location on a 2D plane (i.e. X and Y).
	static const int32_t PLANAR_AXES[] = {VELOCITY_AXIS_X, VELOCITY_AXIS_Y};

	// Stores data about which axes to process based on the incoming motion event.
	int32_t axes_to_process[VELOCITY_AXES];
	uint32_t axes_count = 0;
	int32_t action_masked = J__MotionEvent__getActionMasked(env, event);

	switch (action_masked) {
		case AMOTION_EVENT_ACTION_DOWN:
		case AMOTION_EVENT_ACTION_HOVER_ENTER:
			// Clear all pointers on down before adding the new movement.
			velocity_tracker_clear(tracker);
			add_axes(PLANAR_AXES, 2, axes_to_process, &axes_count);
			break;
		case AMOTION_EVENT_ACTION_POINTER_DOWN: {
			// Start a new movement trace for a pointer that just went down.
			// We do this on down instead of on up because the client may want to query the
			// final velocity for a pointer that just went up.
			int32_t pointer_id = J__MotionEvent__getPointerId(env, event, J__MotionEvent__getActionIndex(env, event));
			if (pointer_id >= 0 && pointer_id <= VELOCITY_TRACKER_MAX_POINTER_ID)
				velocity_tracker_clear_pointer(tracker, pointer_id);
			add_axes(PLANAR_AXES, 2, axes_to_process, &axes_count);
			break;
		}
		case AMOTION_EVENT_ACTION_MOVE:
		case AMOTION_EVENT_ACTION_HOVER_MOVE:
			add_axes(PLANAR_AXES, 2, axes_to_process, &axes_count);
			break;
		case AMOTION_EVENT_ACTION_POINTER_UP:
		case AMOTION_EVENT_ACTION_UP: {
			nsecs_t delay_since_last_event = J__MotionEvent__getEventTime(env, event) * NANOS_PER_MS - tracker->last_event_time;
			if (delay_since_last_event > VELOCITY_ASSUME_POINTER_STOPPED_TIME) {
				// We have not received any movements for too long.  Assume that all pointers
				// have stopped.
				for (uint32_t i = 0; i < 2; i++) {
					velocity_tracker_erase_strategy(tracker, PLANAR_AXES[i]);
				}
			}
			// These actions because they do not convey any new information about
			// pointer movement.  We also want to preserve the last known velocity of the pointers.
			// Note that ACTION_UP and ACTION_POINTER_UP always report the last known position
			// of the pointers that went up.  ACTION_POINTER_UP does include the new position of
			// pointers that remained down but we will also receive an ACTION_MOVE with this
			// information if any of them actually moved.  Since we don't know how many pointers
			// will be going up at once it makes sense to just wait for the following ACTION_MOVE
			// before adding the movement.
			return;
		}
		case AMOTION_EVENT_ACTION_SCROLL:
			axes_to_process[axes_count++] = VELOCITY_AXIS_SCROLL;
			break;
		default:
			// Ignore all other actions.
			return;
	}

	const nsecs_t event_time = J__MotionEvent__getEventTime(env, event) * NANOS_PER_MS;
	const int32_t pointer_count = J__MotionEvent__getPointerCount(env, event);
	for (int32_t i = 0; i < pointer_count; i++) {
		const int32_t pointer_id = J__MotionEvent__getPointerId(env, event, i);
		if (pointer_id < 0 || pointer_id > VELOCITY_TRACKER_MAX_POINTER_ID)
			continue;
		for (uint32_t a = 0; a < axes_count; a++) {
			int32_t axis = axes_to_process[a];
			// ATL's MotionEvent.getAxisValue(axis, pointerIndex) is not backed by a native event: X and Y are the coordinates
			float position;
			if (axis == VELOCITY_AXIS_X)
				position = J__MotionEvent__getXAt(env, event, i);
			else if (axis == VELOCITY_AXIS_Y)
				position = J__MotionEvent__getYAt(env, event, i);
			else
				position = J__MotionEvent__getAxisValueAt(env, event, axis, i);
			velocity_tracker_add_movement(tracker, event_time, pointer_id, axis, position);
		}
	}
}

// --- JNI Methods ---

// Return a strategy enum from integer value.
static enum velocity_tracker_strategy get_strategy_from_int(const int32_t strategy)
{
	if (strategy < (int32_t)VELOCITY_TRACKER_STRATEGY_MIN || strategy > (int32_t)VELOCITY_TRACKER_STRATEGY_MAX) {
		return VELOCITY_TRACKER_STRATEGY_DEFAULT;
	}
	return (enum velocity_tracker_strategy)strategy;
}

JNIEXPORT jlong JNICALL Java_android_view_VelocityTracker_nativeInitialize(JNIEnv *env, jclass clazz, jint strategy)
{
	struct velocity_tracker_state *state = calloc(1, sizeof(*state));
	if (!state)
		return 0;
	velocity_tracker_init(&state->velocity_tracker, get_strategy_from_int(strategy));
	return (jlong)(intptr_t)state;
}

JNIEXPORT void JNICALL Java_android_view_VelocityTracker_nativeDispose(JNIEnv *env, jclass clazz, jlong ptr)
{
	struct velocity_tracker_state *state = (struct velocity_tracker_state *)(intptr_t)ptr;
	if (!state)
		return;
	velocity_tracker_deinit(&state->velocity_tracker);
	free(state);
}

JNIEXPORT void JNICALL Java_android_view_VelocityTracker_nativeClear(JNIEnv *env, jclass clazz, jlong ptr)
{
	struct velocity_tracker_state *state = (struct velocity_tracker_state *)(intptr_t)ptr;
	velocity_tracker_clear(&state->velocity_tracker);
}

JNIEXPORT void JNICALL Java_android_view_VelocityTracker_nativeAddMovement(JNIEnv *env, jclass clazz, jlong ptr, jobject eventObj)
{
	struct velocity_tracker_state *state = (struct velocity_tracker_state *)(intptr_t)ptr;
	velocity_tracker_state_add_movement(env, state, eventObj);
}

JNIEXPORT void JNICALL Java_android_view_VelocityTracker_nativeComputeCurrentVelocity(JNIEnv *env, jclass clazz, jlong ptr, jint units, jfloat maxVelocity)
{
	struct velocity_tracker_state *state = (struct velocity_tracker_state *)(intptr_t)ptr;
	velocity_tracker_get_computed_velocity(&state->velocity_tracker, units, maxVelocity, &state->computed_velocity);
}

JNIEXPORT jfloat JNICALL Java_android_view_VelocityTracker_nativeGetVelocity(JNIEnv *env, jclass clazz, jlong ptr, jint axis, jint id)
{
	struct velocity_tracker_state *state = (struct velocity_tracker_state *)(intptr_t)ptr;
	if (id == ACTIVE_POINTER_ID) {
		id = state->velocity_tracker.active_pointer_id;
	}

	float velocity;
	return velocity_computed_get_velocity(&state->computed_velocity, axis, id, &velocity) ? velocity : 0;
}

JNIEXPORT jboolean JNICALL Java_android_view_VelocityTracker_nativeIsAxisSupported(JNIEnv *env, jclass clazz, jint axis)
{
	return velocity_tracker_is_axis_supported(axis);
}
