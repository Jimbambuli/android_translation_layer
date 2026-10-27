/*
 * Copyright (C) 2012 The Android Open Source Project
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

/* C port of frameworks/native libs/input/VelocityTracker.cpp and include/input/VelocityTracker.h
 * (android-14): the velocity tracker with its strategies (impulse, lsq1-3, wlsq2-*, int1-2, legacy)
 * and the supported axes (X and Y with LSQ2, SCROLL with IMPULSE). The std::map/std::unique_ptr
 * members became fixed arrays; MotionEvent handling lives in views/android_view_VelocityTracker.c. */

#ifndef _VELOCITY_TRACKER_H_
#define _VELOCITY_TRACKER_H_

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

typedef int64_t nsecs_t;

#define VELOCITY_TRACKER_MAX_POINTER_ID 31
#define VELOCITY_TRACKER_HISTORY_SIZE 20
#define VELOCITY_TRACKER_MAX_DEGREE 4

/* MotionEvent.AXIS_X/Y/SCROLL (AMOTION_EVENT_AXIS_*) */
#define VELOCITY_AXIS_X 0
#define VELOCITY_AXIS_Y 1
#define VELOCITY_AXIS_SCROLL 26
#define VELOCITY_AXES 3 /* number of supported axes, see velocity_tracker_axis_slot() */

/* VelocityTracker::Strategy */
enum velocity_tracker_strategy {
	VELOCITY_TRACKER_STRATEGY_DEFAULT = -1,
	VELOCITY_TRACKER_STRATEGY_MIN = 0,
	VELOCITY_TRACKER_STRATEGY_IMPULSE = 0,
	VELOCITY_TRACKER_STRATEGY_LSQ1 = 1,
	VELOCITY_TRACKER_STRATEGY_LSQ2 = 2,
	VELOCITY_TRACKER_STRATEGY_LSQ3 = 3,
	VELOCITY_TRACKER_STRATEGY_WLSQ2_DELTA = 4,
	VELOCITY_TRACKER_STRATEGY_WLSQ2_CENTRAL = 5,
	VELOCITY_TRACKER_STRATEGY_WLSQ2_RECENT = 6,
	VELOCITY_TRACKER_STRATEGY_INT1 = 7,
	VELOCITY_TRACKER_STRATEGY_INT2 = 8,
	VELOCITY_TRACKER_STRATEGY_LEGACY = 9,
	VELOCITY_TRACKER_STRATEGY_MAX = 9,
};

/* VelocityTracker::Estimator */
struct velocity_estimator {
	nsecs_t time;
	float coeff[VELOCITY_TRACKER_MAX_DEGREE + 1];
	uint32_t degree;
	float confidence;
};

/* VelocityTracker::ComputedVelocity */
struct velocity_computed {
	bool has[VELOCITY_AXES][VELOCITY_TRACKER_MAX_POINTER_ID + 1];
	float velocity[VELOCITY_AXES][VELOCITY_TRACKER_MAX_POINTER_ID + 1];
};

struct velocity_strategy;

/* VelocityTracker */
struct velocity_tracker {
	nsecs_t last_event_time;
	uint32_t current_pointer_id_bits; /* bit n = pointer id n */
	int32_t active_pointer_id; /* -1: none (std::nullopt) */
	enum velocity_tracker_strategy override_strategy;
	struct velocity_strategy *configured_strategies[VELOCITY_AXES];
};

void velocity_tracker_init(struct velocity_tracker *tracker, enum velocity_tracker_strategy strategy);
void velocity_tracker_deinit(struct velocity_tracker *tracker);
bool velocity_tracker_is_axis_supported(int32_t axis);
void velocity_tracker_clear(struct velocity_tracker *tracker);
void velocity_tracker_clear_pointer(struct velocity_tracker *tracker, int32_t pointer_id);
void velocity_tracker_erase_strategy(struct velocity_tracker *tracker, int32_t axis);
void velocity_tracker_add_movement(struct velocity_tracker *tracker, nsecs_t event_time, int32_t pointer_id, int32_t axis, float position);
bool velocity_tracker_get_velocity(const struct velocity_tracker *tracker, int32_t axis, int32_t pointer_id, float *velocity);
void velocity_tracker_get_computed_velocity(struct velocity_tracker *tracker, int32_t units, float max_velocity, struct velocity_computed *computed);
bool velocity_tracker_get_estimator(const struct velocity_tracker *tracker, int32_t axis, int32_t pointer_id, struct velocity_estimator *estimator);
bool velocity_computed_get_velocity(const struct velocity_computed *computed, int32_t axis, int32_t id, float *velocity);

/* threshold for determining that a pointer has stopped moving (ASSUME_POINTER_STOPPED_TIME, 40 ms) */
#define VELOCITY_ASSUME_POINTER_STOPPED_TIME ((nsecs_t)40 * 1000000)

#endif
