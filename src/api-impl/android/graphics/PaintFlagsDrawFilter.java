/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.graphics;

/**
 * Clears (clearBits) and sets (setBits) paint flags of what a canvas draws.
 *
 * ATL: the flags are kept (see DrawFilter), not applied.
 */
public class PaintFlagsDrawFilter extends DrawFilter {
	public final int clearBits;
	public final int setBits;

	public PaintFlagsDrawFilter(int clearBits, int setBits) {
		this.clearBits = clearBits;
		this.setBits = setBits;
	}
}
