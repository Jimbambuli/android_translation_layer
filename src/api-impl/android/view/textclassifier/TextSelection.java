/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.os.Bundle;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannedString;
import android.util.ArrayMap;
import android.view.textclassifier.TextClassifier.Utils;

import com.android.internal.util.Preconditions;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Information about where text selection should be.
 */
/* ATL (from AOSP android-16.0.0_r4) */
public final class TextSelection implements Parcelable {

    private final int mStartIndex;
    private final int mEndIndex;
    private final EntityConfidence mEntityConfidence;
    private final String mId;
        private final TextClassification mTextClassification;
    private final Bundle mExtras;

    private TextSelection(
            int startIndex, int endIndex, Map<String, Float> entityConfidence, String id,
            TextClassification textClassification,
            Bundle extras) {
        mStartIndex = startIndex;
        mEndIndex = endIndex;
        mEntityConfidence = new EntityConfidence(entityConfidence);
        mId = id;
        mTextClassification = textClassification;
        mExtras = extras;
    }

    /**
     * Returns the start index of the text selection.
     */
    public int getSelectionStartIndex() {
        return mStartIndex;
    }

    /**
     * Returns the end index of the text selection.
     */
    public int getSelectionEndIndex() {
        return mEndIndex;
    }

    /**
     * Returns the number of entities found in the classified text.
     */
        public int getEntityCount() {
        return mEntityConfidence.getEntities().size();
    }

    /**
     * Returns the entity at the specified index. Entities are ordered from high confidence
     * to low confidence.
     *
     * IndexOutOfBoundsException if the specified index is out of range.
     * #getEntityCount() for the number of entities available.
     */
            public String getEntity(int index) {
        return mEntityConfidence.getEntities().get(index);
    }

    /**
     * Returns the confidence score for the specified entity. The value ranges from
     * 0 (low confidence) to 1 (high confidence). 0 indicates that the entity was not found for the
     * classified text.
     */
        public float getConfidenceScore(String entity) {
        return mEntityConfidence.getConfidenceScore(entity);
    }

    /**
     * Returns the id, if one exists, for this object.
     */
        public String getId() {
        return mId;
    }

    /**
     * Returns the text classification result of the suggested selection span. Enables the text
     * classification by calling
     * {TextSelection.Request.Builder#setIncludeTextClassification(boolean)}. If the text
     * classifier does not support it, a {null} is returned.
     *
     * TextSelection.Request.Builder#setIncludeTextClassification(boolean)
     */
        public TextClassification getTextClassification() {
        return mTextClassification;
    }

    /**
     * Returns the extended data.
     *
     * <p><b>NOTE: </b>Do not modify this bundle.
     */
        public Bundle getExtras() {
        return mExtras;
    }

    /** */
    public TextSelection.Builder toBuilder() {
        return new TextSelection.Builder(mStartIndex, mEndIndex)
                .setId(mId)
                .setEntityConfidence(mEntityConfidence)
                .setTextClassification(mTextClassification)
                .setExtras(mExtras);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "TextSelection {id=%s, startIndex=%d, endIndex=%d, entities=%s}",
                mId, mStartIndex, mEndIndex, mEntityConfidence);
    }

    /**
     * Builder used to build {TextSelection} objects.
     */
    public static final class Builder {

        private final int mStartIndex;
        private final int mEndIndex;
        private final Map<String, Float> mEntityConfidence = new ArrayMap<>();
        private String mId;
                private TextClassification mTextClassification;
                private Bundle mExtras;

        /**
         * Creates a builder used to build {TextSelection} objects.
         *
         * startIndex the start index of the text selection.
         * endIndex the end index of the text selection. Must be greater than startIndex
         */
        public Builder(int startIndex, int endIndex) {
            Preconditions.checkArgument(startIndex >= 0);
            Preconditions.checkArgument(endIndex > startIndex);
            mStartIndex = startIndex;
            mEndIndex = endIndex;
        }

        /**
         * Sets an entity type for the classified text and assigns a confidence score.
         *
         * confidenceScore a value from 0 (low confidence) to 1 (high confidence).
         *      0 implies the entity does not exist for the classified text.
         *      Values greater than 1 are clamped to 1.
         */
                public Builder setEntityType(
                String type,
                float confidenceScore) {
            Objects.requireNonNull(type);
            mEntityConfidence.put(type, confidenceScore);
            return this;
        }

        Builder setEntityConfidence(EntityConfidence scores) {
            mEntityConfidence.clear();
            mEntityConfidence.putAll(scores.toMap());
            return this;
        }

        /**
         * Sets an id for the TextSelection object.
         */
                public Builder setId(String id) {
            mId = id;
            return this;
        }

        /**
         * Sets the text classification result of the suggested selection. If
         * {Request#shouldIncludeTextClassification()} is {true}, set this value.
         * Otherwise this value may be set to null. The intention of this method is to avoid
         * doing expensive work if the client is not interested in such result.
         *
         * this builder
         * Request#shouldIncludeTextClassification()
         */
                public Builder setTextClassification(TextClassification textClassification) {
            mTextClassification = textClassification;
            return this;
        }

        /**
         * Sets the extended data.
         *
         * this builder
         */
                public Builder setExtras(Bundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * Builds and returns {TextSelection} object.
         */
                public TextSelection build() {
            return new TextSelection(
                    mStartIndex, mEndIndex, mEntityConfidence, mId,
                    mTextClassification, mExtras == null ? Bundle.EMPTY : mExtras);
        }
    }

    /**
     * A request object for generating TextSelection.
     */
    public static final class Request implements Parcelable {

        private final CharSequence mText;
        private final int mStartIndex;
        private final int mEndIndex;
        private final LocaleList mDefaultLocales;
        private final boolean mDarkLaunchAllowed;
        private final boolean mIncludeTextClassification;
        private final Bundle mExtras;
        private SystemTextClassifierMetadata mSystemTcMetadata;

        private Request(
                CharSequence text,
                int startIndex,
                int endIndex,
                LocaleList defaultLocales,
                boolean darkLaunchAllowed,
                boolean includeTextClassification,
                Bundle extras) {
            mText = text;
            mStartIndex = startIndex;
            mEndIndex = endIndex;
            mDefaultLocales = defaultLocales;
            mDarkLaunchAllowed = darkLaunchAllowed;
            mIncludeTextClassification = includeTextClassification;
            mExtras = extras;
        }

        /**
         * Returns the text providing context for the selected text (which is specified by the
         * sub sequence starting at startIndex and ending at endIndex).
         */
                public CharSequence getText() {
            return mText;
        }

        /**
         * Returns start index of the selected part of text.
         */
                public int getStartIndex() {
            return mStartIndex;
        }

        /**
         * Returns end index of the selected part of text.
         */
                public int getEndIndex() {
            return mEndIndex;
        }

        /**
         * Returns true if the TextClassifier should return selection suggestions when "dark
         * launched". Otherwise, returns false.
         *
         *          */
        public boolean isDarkLaunchAllowed() {
            return mDarkLaunchAllowed;
        }

        /**
         * ordered list of locale preferences that can be used to disambiguate the
         * provided text.
         */
                public LocaleList getDefaultLocales() {
            return mDefaultLocales;
        }

        /**
         * Returns the name of the package that sent this request.
         * This returns {null} if no calling package name is set.
         */
                public String getCallingPackageName() {
            return mSystemTcMetadata != null ? mSystemTcMetadata.getCallingPackageName() : null;
        }

        /**
         * Sets the information about the {SystemTextClassifier} that sent this request.
         *
         *          */
                public void setSystemTextClassifierMetadata(
                SystemTextClassifierMetadata systemTcMetadata) {
            mSystemTcMetadata = systemTcMetadata;
        }

        /**
         * Returns the information about the {SystemTextClassifier} that sent this request.
         *
         *          */
                public SystemTextClassifierMetadata getSystemTextClassifierMetadata() {
            return mSystemTcMetadata;
        }

        /**
         * Returns true if the client wants the text classifier to classify the text as well and
         * include a {TextClassification} object in the result.
         */
        public boolean shouldIncludeTextClassification() {
            return mIncludeTextClassification;
        }

        /**
         * Returns the extended data.
         *
         * <p><b>NOTE: </b>Do not modify this bundle.
         */
                public Bundle getExtras() {
            return mExtras;
        }

        /**
         * A builder for building TextSelection requests.
         */
        public static final class Builder {

            private final CharSequence mText;
            private final int mStartIndex;
            private final int mEndIndex;
            private LocaleList mDefaultLocales;
            private boolean mDarkLaunchAllowed;
            private boolean mIncludeTextClassification;
            private Bundle mExtras;

            /**
             * text text providing context for the selected text (which is specified by the
             *      sub sequence starting at selectionStartIndex and ending at selectionEndIndex)
             * startIndex start index of the selected part of text
             * endIndex end index of the selected part of text
             */
            public Builder(
                    CharSequence text,
                    int startIndex,
                    int endIndex) {
                Utils.checkArgument(text, startIndex, endIndex);
                mText = text;
                mStartIndex = startIndex;
                mEndIndex = endIndex;
            }

            /**
             * defaultLocales ordered list of locale preferences that may be used to
             *      disambiguate the provided text. If no locale preferences exist, set this to null
             *      or an empty locale list.
             *
             * this builder.
             */
                        public Builder setDefaultLocales(LocaleList defaultLocales) {
                mDefaultLocales = defaultLocales;
                return this;
            }

            /**
             * allowed whether or not the TextClassifier should return selection suggestions
             *      when "dark launched". When a TextClassifier is dark launched, it can suggest
             *      selection changes that should not be used to actually change the user's
             *      selection. Instead, the suggested selection is logged, compared with the user's
             *      selection interaction, and used to generate quality metrics for the
             *      TextClassifier. Not parceled.
             *
             * this builder.
             *              */
                        public Builder setDarkLaunchAllowed(boolean allowed) {
                mDarkLaunchAllowed = allowed;
                return this;
            }

            /**
             * includeTextClassification If true, suggests the TextClassifier to classify the
             *     text in the suggested selection span and include a TextClassification object in
             *     the result. The TextClassifier may not support this and in which case,
             *     {TextSelection#getTextClassification()} returns {null}.
             *
             * this builder.
             * TextSelection#getTextClassification()
             */
                        public Builder setIncludeTextClassification(boolean includeTextClassification) {
                mIncludeTextClassification = includeTextClassification;
                return this;
            }

            /**
             * Sets the extended data.
             *
             * this builder
             */
                        public Builder setExtras(Bundle extras) {
                mExtras = extras;
                return this;
            }

            /**
             * Builds and returns the request object.
             */
                        public Request build() {
                return new Request(new SpannedString(mText), mStartIndex, mEndIndex,
                        mDefaultLocales, mDarkLaunchAllowed,
                        mIncludeTextClassification,
                        mExtras == null ? Bundle.EMPTY : mExtras);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeCharSequence(mText);
            dest.writeInt(mStartIndex);
            dest.writeInt(mEndIndex);
            dest.writeParcelable(mDefaultLocales, flags);
            dest.writeBundle(mExtras);
            dest.writeParcelable(mSystemTcMetadata, flags);
            dest.writeBoolean(mIncludeTextClassification);
        }

        private static Request readFromParcel(Parcel in) {
            final CharSequence text = in.readCharSequence();
            final int startIndex = in.readInt();
            final int endIndex = in.readInt();
            final LocaleList defaultLocales = in.readParcelable(null, android.os.LocaleList.class);
            final Bundle extras = in.readBundle();
            final SystemTextClassifierMetadata systemTcMetadata = in.readParcelable(null, android.view.textclassifier.SystemTextClassifierMetadata.class);
            final boolean includeTextClassification = in.readBoolean();

            final Request request = new Request(text, startIndex, endIndex, defaultLocales,
                    /* darkLaunchAllowed= */ false, includeTextClassification, extras);
            request.setSystemTextClassifierMetadata(systemTcMetadata);
            return request;
        }

        public static final Parcelable.Creator<Request> CREATOR =
                new Parcelable.Creator<Request>() {
                    @Override
                    public Request createFromParcel(Parcel in) {
                        return readFromParcel(in);
                    }

                    @Override
                    public Request[] newArray(int size) {
                        return new Request[size];
                    }
                };
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mStartIndex);
        dest.writeInt(mEndIndex);
        mEntityConfidence.writeToParcel(dest, flags);
        dest.writeString(mId);
        dest.writeBundle(mExtras);
        dest.writeParcelable(mTextClassification, flags);
    }

    public static final Parcelable.Creator<TextSelection> CREATOR =
            new Parcelable.Creator<TextSelection>() {
                @Override
                public TextSelection createFromParcel(Parcel in) {
                    return new TextSelection(in);
                }

                @Override
                public TextSelection[] newArray(int size) {
                    return new TextSelection[size];
                }
            };

    private TextSelection(Parcel in) {
        mStartIndex = in.readInt();
        mEndIndex = in.readInt();
        mEntityConfidence = EntityConfidence.CREATOR.createFromParcel(in);
        mId = in.readString();
        mExtras = in.readBundle();
        mTextClassification = in.readParcelable(TextClassification.class.getClassLoader(), TextClassification.class);
    }
}
