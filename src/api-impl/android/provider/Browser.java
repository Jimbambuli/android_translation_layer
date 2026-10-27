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

package android.provider;

/**
 * Only the intent extras of AOSP's android.provider.Browser that android.text.style.URLSpan uses.
 * The bookmark and history contract (BOOKMARKS_URI, HISTORY_PROJECTION, ...) needs the
 * stock browser's content provider and has no counterpart here.
 */
public class Browser {

    /**
     * The name of the extra in the VIEW intent. The value is the package name of the application
     * that sends the intent (copied from android-14 Browser.java).
     */
    public static final String EXTRA_APPLICATION_ID = "com.android.browser.application_id";

    /**
     * The name of the extra in the VIEW intent. The value is a Bundle of HTTP headers.
     */
    public static final String EXTRA_HEADERS = "com.android.browser.headers";

    private Browser() {
    }
}
