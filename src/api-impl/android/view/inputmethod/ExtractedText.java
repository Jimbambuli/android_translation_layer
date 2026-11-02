/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.view.inputmethod;

import android.text.TextUtils;

/**
 * Information about text that has been extracted for use by an input method.
 *
 * This contains information about a portion of the currently edited text,
 * that the IME should display into its own interface while in extracted mode.
 */
public class ExtractedText {
	/**
	 * The text that has been extracted.
	 *
	 * @see android.widget.TextView#getText()
	 */
	public CharSequence text;

	/**
	 * The offset in the overall text at which the extracted text starts.
	 */
	public int startOffset;

	/**
	 * If the content is a report of a partial text change, this is the
	 * offset where the change starts and it runs until
	 * {@link #partialEndOffset}.  If the content is the full text, this
	 * field is -1.
	 */
	public int partialStartOffset;

	/**
	 * If the content is a report of a partial text change, this is the offset
	 * where the change ends.  Note that the actual text may be larger or
	 * smaller than the difference between this and {@link #partialStartOffset},
	 * meaning a reduction or increase, respectively, in the total text.
	 */
	public int partialEndOffset;

	/**
	 * The offset where the selection currently starts within the extracted
	 * text.  The real selection start position is at
	 * <var>startOffset</var>+<var>selectionStart</var>.
	 */
	public int selectionStart;

	/**
	 * The offset where the selection currently ends within the extracted
	 * text.  The real selection end position is at
	 * <var>startOffset</var>+<var>selectionEnd</var>.
	 */
	public int selectionEnd;

	/**
	 * Bit for {@link #flags}: set if the text being edited can only be on
	 * a single line.
	 */
	public static final int FLAG_SINGLE_LINE = 0x0001;

	/**
	 * Bit for {@link #flags}: set if the editor is currently in selection mode.
	 *
	 * This happens when a hardware keyboard with latched keys is attached and
	 * the shift key is currently latched.
	 */
	public static final int FLAG_SELECTING = 0x0002;

	/**
	 * Additional bit flags of information about the edited text.
	 */
	public int flags;

	/**
	 * The hint that has been extracted.
	 *
	 * @see android.widget.TextView#getHint()
	 */
	public CharSequence hint;

}
