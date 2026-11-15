/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.view.textclassifier;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;
import java.util.Objects;

/**
 * A representation of the context in which text classification would be performed.
 * TextClassificationManager#createTextClassificationSession(TextClassificationContext)
 */
public final class TextClassificationContext implements Parcelable {

    private String mPackageName;
    private final String mWidgetType;
    private final String mWidgetVersion;
    private SystemTextClassifierMetadata mSystemTcMetadata;

    private TextClassificationContext(
            String packageName,
            String widgetType,
            String widgetVersion) {
        mPackageName = Objects.requireNonNull(packageName);
        mWidgetType = Objects.requireNonNull(widgetType);
        mWidgetVersion = widgetVersion;
    }

    /**
     * Returns the package name of the app that this context originated in.
     */
        public String getPackageName() {
        return mPackageName;
    }

    /**
     * Sets the information about the {SystemTextClassifier} that sent this request.
     *
     *      */
    void setSystemTextClassifierMetadata(SystemTextClassifierMetadata systemTcMetadata) {
        mSystemTcMetadata = systemTcMetadata;
    }

    /**
     * Returns the information about the {SystemTextClassifier} that sent this request.
     *
     *      */
        public SystemTextClassifierMetadata getSystemTextClassifierMetadata() {
        return mSystemTcMetadata;
    }

    /**
     * Returns the widget type for this classification context.
     */
            public String getWidgetType() {
        return mWidgetType;
    }

    /**
     * Returns a custom version string for the widget type.
     *
     * #getWidgetType()
     */
        public String getWidgetVersion() {
        return mWidgetVersion;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "TextClassificationContext{"
                + "packageName=%s, widgetType=%s, widgetVersion=%s, systemTcMetadata=%s}",
                mPackageName, mWidgetType, mWidgetVersion, mSystemTcMetadata);
    }

    /**
     * A builder for building a TextClassification context.
     */
    public static final class Builder {

        private final String mPackageName;
        private final String mWidgetType;

        private String mWidgetVersion;

        /**
         * Initializes a new builder for text classification context objects.
         *
         * packageName the name of the calling package
         * widgetType the type of widget e.g. {TextClassifier#WIDGET_TYPE_TEXTVIEW}
         *
         * this builder
         */
        public Builder(String packageName, String widgetType) {
            mPackageName = Objects.requireNonNull(packageName);
            mWidgetType = Objects.requireNonNull(widgetType);
        }

        /**
         * Sets an optional custom version string for the widget type.
         *
         * this builder
         */
        public Builder setWidgetVersion(String widgetVersion) {
            mWidgetVersion = widgetVersion;
            return this;
        }

        /**
         * Builds the text classification context object.
         *
         * the built TextClassificationContext object
         */
                public TextClassificationContext build() {
            return new TextClassificationContext(mPackageName, mWidgetType, mWidgetVersion);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mPackageName);
        parcel.writeString(mWidgetType);
        parcel.writeString(mWidgetVersion);
        parcel.writeParcelable(mSystemTcMetadata, flags);
    }

    private TextClassificationContext(Parcel in) {
        mPackageName = in.readString();
        mWidgetType = in.readString();
        mWidgetVersion = in.readString();
        mSystemTcMetadata = in.readParcelable(null, android.view.textclassifier.SystemTextClassifierMetadata.class);
    }

    public static final Parcelable.Creator<TextClassificationContext> CREATOR =
            new Parcelable.Creator<TextClassificationContext>() {
                @Override
                public TextClassificationContext createFromParcel(Parcel parcel) {
                    return new TextClassificationContext(parcel);
                }

                @Override
                public TextClassificationContext[] newArray(int size) {
                    return new TextClassificationContext[size];
                }
            };
}
