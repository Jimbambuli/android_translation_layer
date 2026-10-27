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

/* C port of frameworks/native libs/input/VelocityControl.cpp (android-14) */

#include <limits.h>
#include <math.h>
#include <stdlib.h>

#include "VelocityControl.h"

// --- VelocityControl ---

// If no movements are received within this amount of time,
// we assume the movement has stopped and reset the movement counters.
static const nsecs_t STOP_TIME = 500 * 1000000; // 500 ms

void velocity_control_init(struct velocity_control *control)
{
	control->parameters.scale = 1.0f;
	control->parameters.low_threshold = 0.0f;
	control->parameters.high_threshold = 0.0f;
	control->parameters.acceleration = 1.0f;
	velocity_tracker_init(&control->velocity_tracker, VELOCITY_TRACKER_STRATEGY_DEFAULT);
	velocity_control_reset(control);
}

void velocity_control_deinit(struct velocity_control *control)
{
	velocity_tracker_deinit(&control->velocity_tracker);
}

void velocity_control_set_parameters(struct velocity_control *control, const struct velocity_control_parameters *parameters)
{
	control->parameters = *parameters;
	velocity_control_reset(control);
}

void velocity_control_reset(struct velocity_control *control)
{
	control->last_movement_time = LLONG_MIN;
	control->raw_position_x = 0;
	control->raw_position_y = 0;
	velocity_tracker_clear(&control->velocity_tracker);
}

void velocity_control_move(struct velocity_control *control, nsecs_t event_time, float *delta_x, float *delta_y)
{
	if ((delta_x && *delta_x) || (delta_y && *delta_y)) {
		if (event_time >= control->last_movement_time + STOP_TIME) {
			velocity_control_reset(control);
		}

		control->last_movement_time = event_time;
		if (delta_x) {
			control->raw_position_x += *delta_x;
		}
		if (delta_y) {
			control->raw_position_y += *delta_y;
		}
		velocity_tracker_add_movement(&control->velocity_tracker, event_time, /*pointerId=*/0, VELOCITY_AXIS_X, control->raw_position_x);
		velocity_tracker_add_movement(&control->velocity_tracker, event_time, /*pointerId=*/0, VELOCITY_AXIS_Y, control->raw_position_y);

		float vx, vy;
		bool has_vx = velocity_tracker_get_velocity(&control->velocity_tracker, VELOCITY_AXIS_X, 0, &vx);
		bool has_vy = velocity_tracker_get_velocity(&control->velocity_tracker, VELOCITY_AXIS_Y, 0, &vy);
		float scale = control->parameters.scale;
		if (has_vx && has_vy) {
			float speed = hypotf(vx, vy) * scale;
			if (speed >= control->parameters.high_threshold) {
				// Apply full acceleration above the high speed threshold.
				scale *= control->parameters.acceleration;
			} else if (speed > control->parameters.low_threshold) {
				// Linearly interpolate the acceleration to apply between the low and high
				// speed thresholds.
				scale *= 1 + (speed - control->parameters.low_threshold) / (control->parameters.high_threshold - control->parameters.low_threshold) * (control->parameters.acceleration - 1);
			}
		}

		if (delta_x) {
			*delta_x *= scale;
		}
		if (delta_y) {
			*delta_y *= scale;
		}
	}
}
