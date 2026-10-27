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

/* C port of frameworks/native libs/input/VelocityTracker.cpp (android-14). Function and variable names
 * follow the original (snake_case); the order of the code and the arithmetic are kept as they are. */

#include <math.h>
#include <stdlib.h>
#include <string.h>

#include "VelocityTracker.h"

#define HISTORY_SIZE VELOCITY_TRACKER_HISTORY_SIZE
#define MAX_POINTER_ID VELOCITY_TRACKER_MAX_POINTER_ID
#define MAX_DEGREE VELOCITY_TRACKER_MAX_DEGREE

// Nanoseconds per milliseconds.
static const nsecs_t NANOS_PER_MS = 1000000;

// Threshold for determining that a pointer has stopped moving.
// Some input devices do not send ACTION_MOVE events in the case where a pointer has
// stopped.  We need to detect this case so that we can accurately predict the
// velocity after the pointer starts moving again.
#define ASSUME_POINTER_STOPPED_TIME VELOCITY_ASSUME_POINTER_STOPPED_TIME

enum strategy_kind {
	KIND_LEAST_SQUARES,
	KIND_INTEGRATING,
	KIND_LEGACY,
	KIND_IMPULSE,
};

/* LeastSquaresVelocityTrackerStrategy::Weighting */
enum weighting {
	WEIGHTING_NONE,
	WEIGHTING_DELTA,
	WEIGHTING_CENTRAL,
	WEIGHTING_RECENT,
};

/* Movement of the Least Squares, Legacy and Impulse strategies */
struct movement {
	nsecs_t event_time;
	float position;
};

/* mIndex and mMovements of one pointer; present = the std::map has an entry for the pointer */
struct movements {
	bool present;
	uint32_t index;
	struct movement movement[HISTORY_SIZE];
};

/* IntegratingVelocityTrackerStrategy::State */
struct integrating_state {
	nsecs_t update_time;
	uint32_t degree;
	float pos, vel, accel;
};

/* The four VelocityTrackerStrategy classes in one struct: a C port needs no virtual functions, kind selects. */
struct velocity_strategy {
	enum strategy_kind kind;
	uint32_t degree; /* least squares, integrating */
	enum weighting weighting; /* least squares */
	bool delta_values; /* impulse */
	uint32_t pointer_id_bits; /* integrating: mPointerIdBits */
	struct integrating_state pointer_state[MAX_POINTER_ID + 1]; /* integrating: mPointerState */
	struct movements pointers[MAX_POINTER_ID + 1]; /* mIndex + mMovements of the others */
};

// All axes supported for velocity tracking, mapped to their default strategies.
// Although other strategies are available for testing and comparison purposes,
// the default strategy is the one that applications will actually use.  Be very careful
// when adjusting the default strategy because it can dramatically affect
// (often in a bad way) the user experience.
static int32_t axis_slot(int32_t axis)
{
	switch (axis) {
		case VELOCITY_AXIS_X:
			return 0;
		case VELOCITY_AXIS_Y:
			return 1;
		case VELOCITY_AXIS_SCROLL:
			return 2;
	}
	return -1;
}

static enum velocity_tracker_strategy default_strategy_by_axis(int32_t axis)
{
	return axis == VELOCITY_AXIS_SCROLL ? VELOCITY_TRACKER_STRATEGY_IMPULSE : VELOCITY_TRACKER_STRATEGY_LSQ2;
}

// Axes whose motion values are differential values (i.e. deltas).
static bool is_differential_axis(int32_t axis)
{
	return axis == VELOCITY_AXIS_SCROLL;
}

// --- Helpers ---

static float vector_dot(const float *a, const float *b, uint32_t m)
{
	float r = 0;
	for (uint32_t i = 0; i < m; i++) {
		r += *(a++) * *(b++);
	}
	return r;
}

static float vector_norm(const float *a, uint32_t m)
{
	float r = 0;
	for (uint32_t i = 0; i < m; i++) {
		float t = *(a++);
		r += t * t;
	}
	return sqrtf(r);
}

// --- Strategies: the addMovement() and clearPointer() of all four classes ---

static void strategy_clear_pointer(struct velocity_strategy *strategy, int32_t pointer_id)
{
	if (strategy->kind == KIND_INTEGRATING)
		strategy->pointer_id_bits &= ~((uint32_t)1 << pointer_id);
	else
		strategy->pointers[pointer_id].present = false; /* mIndex.erase + mMovements.erase */
}

static void strategy_add_movement(struct velocity_strategy *strategy, nsecs_t event_time, int32_t pointer_id, float position)
{
	if (strategy->kind == KIND_INTEGRATING) {
		const nsecs_t MIN_TIME_DELTA = 2 * NANOS_PER_MS;
		const float FILTER_TIME_CONSTANT = 0.010f; // 10 milliseconds
		struct integrating_state *state = &strategy->pointer_state[pointer_id];
		if (strategy->pointer_id_bits & ((uint32_t)1 << pointer_id)) {
			// IntegratingVelocityTrackerStrategy::updateState
			if (event_time <= state->update_time + MIN_TIME_DELTA) {
				return;
			}

			float dt = (event_time - state->update_time) * 0.000000001f;
			state->update_time = event_time;

			float vel = (position - state->pos) / dt;
			if (state->degree == 0) {
				state->vel = vel;
				state->degree = 1;
			} else {
				float alpha = dt / (FILTER_TIME_CONSTANT + dt);
				if (strategy->degree == 1) {
					state->vel += (vel - state->vel) * alpha;
				} else {
					float accel = (vel - state->vel) / dt;
					if (state->degree == 1) {
						state->accel = accel;
						state->degree = 2;
					} else {
						state->accel += (accel - state->accel) * alpha;
					}
					state->vel += (state->accel * dt) * alpha;
				}
			}
			state->pos = position;
		} else {
			// IntegratingVelocityTrackerStrategy::initState
			state->update_time = event_time;
			state->degree = 0;
			state->pos = position;
			state->accel = 0;
			state->vel = 0;
		}
		strategy->pointer_id_bits |= (uint32_t)1 << pointer_id;
		return;
	}

	// Least Squares, Legacy and Impulse: identical circular buffer
	struct movements *movements = &strategy->pointers[pointer_id];
	bool inserted = !movements->present;
	if (inserted) {
		memset(movements, 0, sizeof(*movements));
		movements->present = true;
	}
	uint32_t index = movements->index;
	if (!inserted && movements->movement[index].event_time != event_time) {
		index++;
	}
	if (index == HISTORY_SIZE) {
		index = 0;
	}
	movements->index = index;

	struct movement *movement = &movements->movement[index];
	movement->event_time = event_time;
	movement->position = position;
}

// --- LeastSquaresVelocityTrackerStrategy ---

/**
 * Solves a linear least squares problem to obtain a N degree polynomial that fits
 * the specified input data as nearly as possible.
 *
 * Returns true if a solution is found, false otherwise.
 *
 * The input consists of two vectors of data points X and Y with indices 0..m-1
 * along with a weight vector W of the same size.
 *
 * The output is a vector B with indices 0..n that describes a polynomial
 * that fits the data, such the sum of W[i] * W[i] * abs(Y[i] - (B[0] + B[1] X[i]
 * + B[2] X[i]^2 ... B[n] X[i]^n)) for all i between 0 and m-1 is minimized.
 *
 * Accordingly, the weight vector W should be initialized by the caller with the
 * reciprocal square root of the variance of the error in each input data point.
 * In other words, an ideal choice for W would be W[i] = 1 / var(Y[i]) = 1 / stddev(Y[i]).
 * The weights express the relative importance of each data point.  If the weights are
 * all 1, then the data points are considered to be of equal importance when fitting
 * the polynomial.  It is a good idea to choose weights that diminish the importance
 * of data points that may have higher than usual error margins.
 *
 * Errors among data points are assumed to be independent.  W is represented here
 * as a vector although in the literature it is typically taken to be a diagonal matrix.
 *
 * That is to say, the function that generated the input data can be approximated
 * by y(x) ~= B[0] + B[1] x + B[2] x^2 + ... + B[n] x^n.
 *
 * The coefficient of determination (R^2) is also returned to describe the goodness
 * of fit of the model for the given data.  It is a value between 0 and 1, where 1
 * indicates perfect correspondence.
 *
 * This function first expands the X vector to a m by n matrix A such that
 * A[i][0] = 1, A[i][1] = X[i], A[i][2] = X[i]^2, ..., A[i][n] = X[i]^n, then
 * multiplies it by w[i]./
 *
 * Then it calculates the QR decomposition of A yielding an m by m orthonormal matrix Q
 * and an m by n upper triangular matrix R.  Because R is upper triangular (lower
 * part is all zeroes), we can simplify the decomposition into an m by n matrix
 * Q1 and a n by n matrix R1 such that A = Q1 R1.
 *
 * Finally we solve the system of linear equations given by R1 B = (Qtranspose W Y)
 * to find B.
 *
 * For efficiency, we lay out A and Q column-wise in memory because we frequently
 * operate on the column vectors.  Conversely, we lay out R row-wise.
 *
 * http://en.wikipedia.org/wiki/Numerical_methods_for_linear_least_squares
 * http://en.wikipedia.org/wiki/Gram-Schmidt
 */
static bool solve_least_squares(const float *x, const float *y, const float *w, uint32_t m, uint32_t n, float *out_b, float *out_det)
{
	// Expand the X vector to a matrix A, pre-multiplied by the weights.
	float a[MAX_DEGREE + 1][HISTORY_SIZE]; // column-major order
	for (uint32_t h = 0; h < m; h++) {
		a[0][h] = w[h];
		for (uint32_t i = 1; i < n; i++) {
			a[i][h] = a[i - 1][h] * x[h];
		}
	}

	// Apply the Gram-Schmidt process to A to obtain its QR decomposition.
	float q[MAX_DEGREE + 1][HISTORY_SIZE]; // orthonormal basis, column-major order
	float r[MAX_DEGREE + 1][MAX_DEGREE + 1]; // upper triangular matrix, row-major order
	for (uint32_t j = 0; j < n; j++) {
		for (uint32_t h = 0; h < m; h++) {
			q[j][h] = a[j][h];
		}
		for (uint32_t i = 0; i < j; i++) {
			float dot = vector_dot(&q[j][0], &q[i][0], m);
			for (uint32_t h = 0; h < m; h++) {
				q[j][h] -= dot * q[i][h];
			}
		}

		float norm = vector_norm(&q[j][0], m);
		if (norm < 0.000001f) {
			// vectors are linearly dependent or zero so no solution
			return false;
		}

		float inv_norm = 1.0f / norm;
		for (uint32_t h = 0; h < m; h++) {
			q[j][h] *= inv_norm;
		}
		for (uint32_t i = 0; i < n; i++) {
			r[j][i] = i < j ? 0 : vector_dot(&q[j][0], &a[i][0], m);
		}
	}

	// Solve R B = Qt W Y to find B.  This is easy because R is upper triangular.
	// We just work from bottom-right to top-left calculating B's coefficients.
	float wy[HISTORY_SIZE];
	for (uint32_t h = 0; h < m; h++) {
		wy[h] = y[h] * w[h];
	}
	for (uint32_t i = n; i != 0;) {
		i--;
		out_b[i] = vector_dot(&q[i][0], wy, m);
		for (uint32_t j = n - 1; j > i; j--) {
			out_b[i] -= r[i][j] * out_b[j];
		}
		out_b[i] /= r[i][i];
	}

	// Calculate the coefficient of determination as 1 - (SSerr / SStot) where
	// SSerr is the residual sum of squares (variance of the error),
	// and SStot is the total sum of squares (variance of the data) where each
	// has been weighted.
	float ymean = 0;
	for (uint32_t h = 0; h < m; h++) {
		ymean += y[h];
	}
	ymean /= m;

	float sserr = 0;
	float sstot = 0;
	for (uint32_t h = 0; h < m; h++) {
		float err = y[h] - out_b[0];
		float term = 1;
		for (uint32_t i = 1; i < n; i++) {
			term *= x[h];
			err -= term * out_b[i];
		}
		sserr += w[h] * w[h] * err * err;
		float var = y[h] - ymean;
		sstot += w[h] * w[h] * var * var;
	}
	*out_det = sstot > 0.000001f ? 1.0f - (sserr / sstot) : 1;
	return true;
}

/*
 * Optimized unweighted second-order least squares fit. About 2x speed improvement compared to
 * the default implementation
 */
static bool solve_unweighted_least_squares_deg2(const float *x, const float *y, uint32_t count, float *coeff)
{
	// Solving y = a*x^2 + b*x + c
	float sxi = 0, sxiyi = 0, syi = 0, sxi2 = 0, sxi3 = 0, sxi2yi = 0, sxi4 = 0;

	for (uint32_t i = 0; i < count; i++) {
		float xi = x[i];
		float yi = y[i];

		float xi2 = xi * xi;
		float xi3 = xi2 * xi;
		float xi4 = xi3 * xi;
		float xiyi = xi * yi;
		float xi2yi = xi2 * yi;

		sxi += xi;
		sxi2 += xi2;
		sxiyi += xiyi;
		sxi2yi += xi2yi;
		syi += yi;
		sxi3 += xi3;
		sxi4 += xi4;
	}

	float Sxx = sxi2 - sxi * sxi / count;
	float Sxy = sxiyi - sxi * syi / count;
	float Sxx2 = sxi3 - sxi * sxi2 / count;
	float Sx2y = sxi2yi - sxi2 * syi / count;
	float Sx2x2 = sxi4 - sxi2 * sxi2 / count;

	float denominator = Sxx * Sx2x2 - Sxx2 * Sxx2;
	if (denominator == 0) {
		return false;
	}
	// Compute a
	float numerator = Sx2y * Sxx - Sxy * Sxx2;
	float a = numerator / denominator;

	// Compute b
	numerator = Sxy * Sx2x2 - Sx2y * Sxx2;
	float b = numerator / denominator;

	// Compute c
	float c = syi / count - b * sxi / count - a * sxi2 / count;

	coeff[0] = c;
	coeff[1] = b;
	coeff[2] = a;
	return true;
}

static float choose_weight(const struct velocity_strategy *strategy, int32_t pointer_id, uint32_t index)
{
	const struct movements *m = &strategy->pointers[pointer_id];
	const struct movement *movements = m->movement;
	switch (strategy->weighting) {
		case WEIGHTING_DELTA: {
			// Weight points based on how much time passed between them and the next
			// point so that points that "cover" a longer time span are weighed more.
			if (index == m->index) {
				return 1.0f;
			}
			uint32_t next_index = (index + 1) % HISTORY_SIZE;
			float delta_millis = (movements[next_index].event_time - movements[index].event_time) * 0.000001f;
			if (delta_millis < 0) {
				return 0.5f;
			}
			if (delta_millis < 10) {
				return 0.5f + delta_millis * 0.05;
			}
			return 1.0f;
		}

		case WEIGHTING_CENTRAL: {
			// Weight points based on their age, weighing very recent and very old points less.
			float age_millis = (movements[m->index].event_time - movements[index].event_time) * 0.000001f;
			if (age_millis < 0) {
				return 0.5f;
			}
			if (age_millis < 10) {
				return 0.5f + age_millis * 0.05;
			}
			if (age_millis < 50) {
				return 1.0f;
			}
			if (age_millis < 60) {
				return 0.5f + (60 - age_millis) * 0.05;
			}
			return 0.5f;
		}

		case WEIGHTING_RECENT: {
			// Weight points based on their age, weighing older points less.
			float age_millis = (movements[m->index].event_time - movements[index].event_time) * 0.000001f;
			if (age_millis < 50) {
				return 1.0f;
			}
			if (age_millis < 100) {
				return 0.5f + (100 - age_millis) * 0.01f;
			}
			return 0.5f;
		}

		case WEIGHTING_NONE:
			return 1.0f;
	}
	return 1.0f;
}

static bool least_squares_get_estimator(const struct velocity_strategy *strategy, int32_t pointer_id, struct velocity_estimator *estimator_out)
{
	static const nsecs_t HORIZON = 100 * 1000000; // 100 ms
	const struct movements *movements = &strategy->pointers[pointer_id];
	if (!movements->present) {
		return false; // no data
	}

	// Iterate over movement samples in reverse time order and collect samples.
	float positions[HISTORY_SIZE];
	float w[HISTORY_SIZE];
	float time[HISTORY_SIZE];
	uint32_t m = 0;

	uint32_t index = movements->index;
	const struct movement *newest_movement = &movements->movement[index];
	do {
		const struct movement *movement = &movements->movement[index];

		nsecs_t age = newest_movement->event_time - movement->event_time;
		if (age > HORIZON) {
			break;
		}
		if (movement->event_time == 0 && index != 0) {
			break;
		}

		positions[m] = movement->position;
		w[m] = choose_weight(strategy, pointer_id, index);
		time[m] = -age * 0.000000001f;
		m++;
		index = (index == 0 ? HISTORY_SIZE : index) - 1;
	} while (m < HISTORY_SIZE);

	if (m == 0) {
		return false; // no data
	}

	// Calculate a least squares polynomial fit.
	uint32_t degree = strategy->degree;
	if (degree > m - 1) {
		degree = m - 1;
	}

	if (degree == 2 && strategy->weighting == WEIGHTING_NONE) {
		// Optimize unweighted, quadratic polynomial fit
		float coeff[3];
		if (solve_unweighted_least_squares_deg2(time, positions, m, coeff)) {
			memset(estimator_out, 0, sizeof(*estimator_out));
			estimator_out->time = newest_movement->event_time;
			estimator_out->degree = 2;
			estimator_out->confidence = 1;
			for (uint32_t i = 0; i <= estimator_out->degree; i++) {
				estimator_out->coeff[i] = coeff[i];
			}
			return true;
		}
	} else if (degree >= 1) {
		// General case for an Nth degree polynomial fit
		float det;
		uint32_t n = degree + 1;
		struct velocity_estimator estimator;
		memset(&estimator, 0, sizeof(estimator));
		if (solve_least_squares(time, positions, w, m, n, estimator.coeff, &det)) {
			estimator.time = newest_movement->event_time;
			estimator.degree = degree;
			estimator.confidence = det;
			*estimator_out = estimator;
			return true;
		}
	}

	// No velocity data available for this pointer, but we do have its current position.
	memset(estimator_out, 0, sizeof(*estimator_out));
	estimator_out->coeff[0] = positions[0];
	estimator_out->time = newest_movement->event_time;
	estimator_out->degree = 0;
	estimator_out->confidence = 1;
	return true;
}

// --- IntegratingVelocityTrackerStrategy ---

static bool integrating_get_estimator(const struct velocity_strategy *strategy, int32_t pointer_id, struct velocity_estimator *estimator)
{
	if (strategy->pointer_id_bits & ((uint32_t)1 << pointer_id)) {
		const struct integrating_state *state = &strategy->pointer_state[pointer_id];
		memset(estimator, 0, sizeof(*estimator));
		// populateEstimator
		estimator->time = state->update_time;
		estimator->confidence = 1.0f;
		estimator->degree = state->degree;
		estimator->coeff[0] = state->pos;
		estimator->coeff[1] = state->vel;
		estimator->coeff[2] = state->accel / 2;
		return true;
	}
	return false;
}

// --- LegacyVelocityTrackerStrategy ---

static bool legacy_get_estimator(const struct velocity_strategy *strategy, int32_t pointer_id, struct velocity_estimator *estimator)
{
	static const nsecs_t HORIZON = 200 * 1000000; // 100 ms
	static const nsecs_t MIN_DURATION = 10 * 1000000; // 10 ms
	const struct movements *movements = &strategy->pointers[pointer_id];
	if (!movements->present) {
		return false; // no data
	}

	const struct movement *newest_movement = &movements->movement[movements->index];

	// Find the oldest sample that contains the pointer and that is not older than HORIZON.
	nsecs_t min_time = newest_movement->event_time - HORIZON;
	uint32_t oldest_index = movements->index;
	uint32_t num_touches = 1;
	do {
		uint32_t next_oldest_index = (oldest_index == 0 ? HISTORY_SIZE : oldest_index) - 1;
		const struct movement *next_oldest_movement = &movements->movement[next_oldest_index];
		if (next_oldest_movement->event_time < min_time) {
			break;
		}
		oldest_index = next_oldest_index;
	} while (++num_touches < HISTORY_SIZE);

	// Calculate an exponentially weighted moving average of the velocity estimate
	// at different points in time measured relative to the oldest sample.
	// This is essentially an IIR filter.  Newer samples are weighted more heavily
	// than older samples.  Samples at equal time points are weighted more or less
	// equally.
	//
	// One tricky problem is that the sample data may be poorly conditioned.
	// Sometimes samples arrive very close together in time which can cause us to
	// overestimate the velocity at that time point.  Most samples might be measured
	// 16ms apart but some consecutive samples could be only 0.5sm apart because
	// the hardware or driver reports them irregularly or in bursts.
	float accum_v = 0;
	uint32_t index = oldest_index;
	uint32_t samples_used = 0;
	const struct movement *oldest_movement = &movements->movement[oldest_index];
	float oldest_position = oldest_movement->position;
	nsecs_t last_duration = 0;

	while (num_touches-- > 1) {
		if (++index == HISTORY_SIZE) {
			index = 0;
		}
		const struct movement *movement = &movements->movement[index];
		nsecs_t duration = movement->event_time - oldest_movement->event_time;

		// If the duration between samples is small, we may significantly overestimate
		// the velocity.  Consequently, we impose a minimum duration constraint on the
		// samples that we include in the calculation.
		if (duration >= MIN_DURATION) {
			float position = movement->position;
			float scale = 1000000000.0f / duration; // one over time delta in seconds
			float v = (position - oldest_position) * scale;
			accum_v = (accum_v * last_duration + v * duration) / (duration + last_duration);
			last_duration = duration;
			samples_used += 1;
		}
	}

	// Report the estimated velocity.
	float newest_position = newest_movement->position;
	memset(estimator, 0, sizeof(*estimator));
	estimator->time = newest_movement->event_time;
	estimator->confidence = 1;
	estimator->coeff[0] = newest_position;
	if (samples_used) {
		estimator->coeff[1] = accum_v;
		estimator->degree = 1;
	} else {
		estimator->degree = 0;
	}
	return true;
}

// --- ImpulseVelocityTrackerStrategy ---

static float kinetic_energy_to_velocity(float work)
{
	static const float sqrt2 = 1.41421356237;
	return (work < 0 ? -1.0 : 1.0) * sqrtf(fabsf(work)) * sqrt2;
}

/**
 * Use the intensity of the pointer's motion to estimate its velocity ("impulse" strategy).
 * The kinetic energy of a pointer is 0.5*m*v^2 and the work done on it is the integral of the
 * impulse; the estimate assumes unit mass and an initial energy of 0 at the oldest sample,
 * see the long explanation in frameworks/native libs/input/VelocityTracker.cpp.
 * The samples are passed newest first: t[0] is the newest time.
 */
static float calculate_impulse_velocity(const nsecs_t *t, const float *x, uint32_t count, bool delta_values)
{
	static const float SECONDS_PER_NANO = 1E-9;

	if (count < 2) {
		return 0; // if 0 or 1 points, velocity is zero
	}
	if (count == 2) { // if 2 points, basic linear calculation
		if (t[1] == t[0]) {
			return 0;
		}
		const float delta_x = delta_values ? -x[0] : x[1] - x[0];
		return delta_x / (SECONDS_PER_NANO * (t[1] - t[0]));
	}
	// Guaranteed to have at least 3 points here
	float work = 0;
	for (uint32_t i = count - 1; i > 0; i--) { // start with the oldest sample and go forward in time
		if (t[i] == t[i - 1]) {
			continue;
		}
		float vprev = kinetic_energy_to_velocity(work); // v[i-1]
		const float delta_x = delta_values ? -x[i - 1] : x[i] - x[i - 1];
		float vcurr = delta_x / (SECONDS_PER_NANO * (t[i] - t[i - 1])); // v[i]
		work += (vcurr - vprev) * fabsf(vcurr);
		if (i == count - 1) {
			work *= 0.5; // initial condition, case 2) above
		}
	}
	return kinetic_energy_to_velocity(work);
}

static bool impulse_get_estimator(const struct velocity_strategy *strategy, int32_t pointer_id, struct velocity_estimator *estimator)
{
	static const nsecs_t HORIZON = 100 * 1000000; // 100 ms
	const struct movements *movements = &strategy->pointers[pointer_id];
	if (!movements->present) {
		return false; // no data
	}

	// Iterate over movement samples in reverse time order and collect samples.
	float positions[HISTORY_SIZE];
	nsecs_t time[HISTORY_SIZE];
	uint32_t m = 0; // number of points that will be used for fitting
	uint32_t index = movements->index;
	const struct movement *newest_movement = &movements->movement[index];
	do {
		const struct movement *movement = &movements->movement[index];

		nsecs_t age = newest_movement->event_time - movement->event_time;
		if (age > HORIZON) {
			break;
		}
		if (movement->event_time == 0 && index != 0) {
			break;
		}

		positions[m] = movement->position;
		time[m] = movement->event_time;
		index = (index == 0 ? HISTORY_SIZE : index) - 1;
	} while (++m < HISTORY_SIZE);

	if (m == 0) {
		return false; // no data
	}
	memset(estimator, 0, sizeof(*estimator));
	estimator->coeff[0] = 0;
	estimator->coeff[1] = calculate_impulse_velocity(time, positions, m, strategy->delta_values);
	estimator->coeff[2] = 0;

	estimator->time = newest_movement->event_time;
	estimator->degree = 2; // similar results to 2nd degree fit
	estimator->confidence = 1;
	return true;
}

static bool strategy_get_estimator(const struct velocity_strategy *strategy, int32_t pointer_id, struct velocity_estimator *estimator)
{
	switch (strategy->kind) {
		case KIND_LEAST_SQUARES:
			return least_squares_get_estimator(strategy, pointer_id, estimator);
		case KIND_INTEGRATING:
			return integrating_get_estimator(strategy, pointer_id, estimator);
		case KIND_LEGACY:
			return legacy_get_estimator(strategy, pointer_id, estimator);
		case KIND_IMPULSE:
			return impulse_get_estimator(strategy, pointer_id, estimator);
	}
	return false;
}

// --- VelocityTracker ---

void velocity_tracker_init(struct velocity_tracker *tracker, enum velocity_tracker_strategy strategy)
{
	memset(tracker, 0, sizeof(*tracker));
	tracker->active_pointer_id = -1;
	tracker->override_strategy = strategy;
}

static void clear_configured_strategies(struct velocity_tracker *tracker)
{
	for (int32_t i = 0; i < VELOCITY_AXES; i++) {
		free(tracker->configured_strategies[i]);
		tracker->configured_strategies[i] = NULL;
	}
}

void velocity_tracker_deinit(struct velocity_tracker *tracker)
{
	clear_configured_strategies(tracker);
}

bool velocity_tracker_is_axis_supported(int32_t axis)
{
	return axis_slot(axis) >= 0;
}

void velocity_tracker_erase_strategy(struct velocity_tracker *tracker, int32_t axis)
{
	int32_t slot = axis_slot(axis);
	if (slot >= 0) {
		free(tracker->configured_strategies[slot]);
		tracker->configured_strategies[slot] = NULL;
	}
}

/* VelocityTracker::createStrategy: NULL for a strategy that does not exist */
static struct velocity_strategy *create_strategy(enum velocity_tracker_strategy strategy, bool delta_values)
{
	struct velocity_strategy *s = calloc(1, sizeof(*s));
	if (!s)
		return NULL;
	switch (strategy) {
		case VELOCITY_TRACKER_STRATEGY_IMPULSE:
			s->kind = KIND_IMPULSE;
			s->delta_values = delta_values;
			return s;

		case VELOCITY_TRACKER_STRATEGY_LSQ1:
			s->kind = KIND_LEAST_SQUARES;
			s->degree = 1;
			s->weighting = WEIGHTING_NONE;
			return s;

		case VELOCITY_TRACKER_STRATEGY_LSQ2:
			s->kind = KIND_LEAST_SQUARES;
			s->degree = 2;
			s->weighting = WEIGHTING_NONE;
			return s;

		case VELOCITY_TRACKER_STRATEGY_LSQ3:
			s->kind = KIND_LEAST_SQUARES;
			s->degree = 3;
			s->weighting = WEIGHTING_NONE;
			return s;

		case VELOCITY_TRACKER_STRATEGY_WLSQ2_DELTA:
			s->kind = KIND_LEAST_SQUARES;
			s->degree = 2;
			s->weighting = WEIGHTING_DELTA;
			return s;
		case VELOCITY_TRACKER_STRATEGY_WLSQ2_CENTRAL:
			s->kind = KIND_LEAST_SQUARES;
			s->degree = 2;
			s->weighting = WEIGHTING_CENTRAL;
			return s;
		case VELOCITY_TRACKER_STRATEGY_WLSQ2_RECENT:
			s->kind = KIND_LEAST_SQUARES;
			s->degree = 2;
			s->weighting = WEIGHTING_RECENT;
			return s;

		case VELOCITY_TRACKER_STRATEGY_INT1:
			s->kind = KIND_INTEGRATING;
			s->degree = 1;
			return s;

		case VELOCITY_TRACKER_STRATEGY_INT2:
			s->kind = KIND_INTEGRATING;
			s->degree = 2;
			return s;

		case VELOCITY_TRACKER_STRATEGY_LEGACY:
			s->kind = KIND_LEGACY;
			return s;

		default:
			break;
	}
	free(s);
	return NULL;
}

static struct velocity_strategy *configure_strategy(struct velocity_tracker *tracker, int32_t axis)
{
	const bool is_differential = is_differential_axis(axis);
	struct velocity_strategy *created_strategy;
	if (tracker->override_strategy != VELOCITY_TRACKER_STRATEGY_DEFAULT) {
		created_strategy = create_strategy(tracker->override_strategy, /*deltaValues=*/is_differential);
	} else {
		created_strategy = create_strategy(default_strategy_by_axis(axis), /*deltaValues=*/is_differential);
	}
	if (created_strategy == NULL) {
		abort(); // LOG_ALWAYS_FATAL_IF: could not create velocity tracker strategy for the axis
	}
	tracker->configured_strategies[axis_slot(axis)] = created_strategy;
	return created_strategy;
}

void velocity_tracker_clear(struct velocity_tracker *tracker)
{
	tracker->current_pointer_id_bits = 0;
	tracker->active_pointer_id = -1;
	clear_configured_strategies(tracker);
}

void velocity_tracker_clear_pointer(struct velocity_tracker *tracker, int32_t pointer_id)
{
	tracker->current_pointer_id_bits &= ~((uint32_t)1 << pointer_id);

	if (tracker->active_pointer_id != -1 && tracker->active_pointer_id == pointer_id) {
		// The active pointer id is being removed. Mark it invalid and try to find a new one
		// from the remaining pointers.
		tracker->active_pointer_id = -1;
		if (tracker->current_pointer_id_bits != 0) {
			tracker->active_pointer_id = __builtin_ctz(tracker->current_pointer_id_bits); // firstMarkedBit
		}
	}

	for (int32_t i = 0; i < VELOCITY_AXES; i++) {
		if (tracker->configured_strategies[i])
			strategy_clear_pointer(tracker->configured_strategies[i], pointer_id);
	}
}

void velocity_tracker_add_movement(struct velocity_tracker *tracker, nsecs_t event_time, int32_t pointer_id, int32_t axis, float position)
{
	if ((tracker->current_pointer_id_bits & ((uint32_t)1 << pointer_id)) &&
	    event_time - tracker->last_event_time > ASSUME_POINTER_STOPPED_TIME) {
		// We have not received any movements for too long.  Assume that all pointers
		// have stopped.
		clear_configured_strategies(tracker);
	}
	tracker->last_event_time = event_time;

	tracker->current_pointer_id_bits |= (uint32_t)1 << pointer_id;
	if (tracker->active_pointer_id == -1) {
		// Let this be the new active pointer if no active pointer is currently set
		tracker->active_pointer_id = pointer_id;
	}

	int32_t slot = axis_slot(axis);
	if (slot < 0)
		return;
	struct velocity_strategy *strategy = tracker->configured_strategies[slot];
	if (strategy == NULL) {
		strategy = configure_strategy(tracker, axis);
	}
	strategy_add_movement(strategy, event_time, pointer_id, position);
}

bool velocity_tracker_get_estimator(const struct velocity_tracker *tracker, int32_t axis, int32_t pointer_id, struct velocity_estimator *estimator)
{
	int32_t slot = axis_slot(axis);
	if (slot < 0 || tracker->configured_strategies[slot] == NULL || pointer_id < 0 || pointer_id > MAX_POINTER_ID) {
		return false;
	}
	return strategy_get_estimator(tracker->configured_strategies[slot], pointer_id, estimator);
}

bool velocity_tracker_get_velocity(const struct velocity_tracker *tracker, int32_t axis, int32_t pointer_id, float *velocity)
{
	struct velocity_estimator estimator;
	if (velocity_tracker_get_estimator(tracker, axis, pointer_id, &estimator) && estimator.degree >= 1) {
		*velocity = estimator.coeff[1];
		return true;
	}
	return false;
}

void velocity_tracker_get_computed_velocity(struct velocity_tracker *tracker, int32_t units, float max_velocity, struct velocity_computed *computed)
{
	memset(computed, 0, sizeof(*computed));
	static const int32_t axes[VELOCITY_AXES] = {VELOCITY_AXIS_X, VELOCITY_AXIS_Y, VELOCITY_AXIS_SCROLL};
	for (int32_t slot = 0; slot < VELOCITY_AXES; slot++) {
		if (tracker->configured_strategies[slot] == NULL)
			continue;
		uint32_t copy_id_bits = tracker->current_pointer_id_bits;
		while (copy_id_bits != 0) {
			uint32_t id = __builtin_ctz(copy_id_bits); // clearFirstMarkedBit
			copy_id_bits &= copy_id_bits - 1;
			float velocity;
			if (velocity_tracker_get_velocity(tracker, axes[slot], id, &velocity)) {
				float adjusted_velocity = velocity * units / 1000;
				// std::clamp(*velocity * units / 1000, -maxVelocity, maxVelocity)
				if (adjusted_velocity < -max_velocity)
					adjusted_velocity = -max_velocity;
				else if (adjusted_velocity > max_velocity)
					adjusted_velocity = max_velocity;
				computed->has[slot][id] = true;
				computed->velocity[slot][id] = adjusted_velocity;
			}
		}
	}
}

bool velocity_computed_get_velocity(const struct velocity_computed *computed, int32_t axis, int32_t id, float *velocity)
{
	int32_t slot = axis_slot(axis);
	if (slot < 0 || id < 0 || id > MAX_POINTER_ID || !computed->has[slot][id])
		return false;
	*velocity = computed->velocity[slot][id];
	return true;
}
