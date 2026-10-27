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

/* C port of frameworks/native libs/input/VelocityControl.cpp and include/input/VelocityControl.h (android-14):
 * the pointer/wheel acceleration of Android's InputReader, driven by a VelocityTracker. */

#ifndef _VELOCITY_CONTROL_H_
#define _VELOCITY_CONTROL_H_

#include "VelocityTracker.h"

struct velocity_control_parameters {
	// A scale factor that is multiplied with the raw velocity deltas
	// prior to applying any other velocity control factors.  The scale
	// factor should be used to adapt the input device resolution
	// (eg. counts per inch) to the output device resolution (eg. pixels per inch).
	//
	// Must be a positive value.
	// Default is 1.0 (no scaling).
	float scale;

	// The scaled speed at which acceleration begins to be applied.
	// This value establishes the upper bound of a low speed regime for
	// small precise motions that are performed without any acceleration.
	//
	// Must be a non-negative value.
	// Default is 0.0 (no low threshold).
	float low_threshold;

	// The scaled speed at which maximum acceleration is applied.
	// The difference between highThreshold and lowThreshold controls
	// the range of speeds over which the acceleration factor is interpolated.
	// The wider the range, the smoother the acceleration.
	//
	// Must be a non-negative value greater than or equal to lowThreshold.
	// Default is 0.0 (no high threshold).
	float high_threshold;

	// The acceleration factor.
	// When the speed is above the low speed threshold, the velocity will scaled
	// by an interpolated value between 1.0 and this amount.
	//
	// Must be a positive greater than or equal to 1.0.
	// Default is 1.0 (no acceleration).
	float acceleration;
};

struct velocity_control {
	struct velocity_control_parameters parameters;
	nsecs_t last_movement_time;
	float raw_position_x, raw_position_y;
	struct velocity_tracker velocity_tracker;
};

/* VelocityControl() with the default parameters of VelocityControlParameters() */
void velocity_control_init(struct velocity_control *control);
void velocity_control_deinit(struct velocity_control *control);
void velocity_control_set_parameters(struct velocity_control *control, const struct velocity_control_parameters *parameters);
void velocity_control_reset(struct velocity_control *control);
/* deltaX/deltaY may be NULL */
void velocity_control_move(struct velocity_control *control, nsecs_t event_time, float *delta_x, float *delta_y);

#endif
