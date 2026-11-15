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

import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannedString;
import android.util.Log;
import android.util.ArrayMap;
import android.view.View.OnClickListener;
import android.view.textclassifier.TextClassifier.Utils;

import com.android.internal.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Information for generating a widget to handle classified text.
 *
 * <p>A TextClassification object contains icons, labels, onClickListeners and intents that may
 * be used to build a widget that can be used to act on classified text. There is the concept of a
 * <i>primary action</i> and other <i>secondary actions</i>.
 *
 * <p>e.g. building a view that, when clicked, shares the classified text with the preferred app:
 *
 * <pre>{ *   // Called preferably outside the UiThread.
 *   TextClassification classification = textClassifier.classifyText(allText, 10, 25);
 *
 *   // Called on the UiThread.
 *   Button button = new Button(context);
 *   button.setCompoundDrawablesWithIntrinsicBounds(classification.getIcon(), null, null, null);
 *   button.setText(classification.getLabel());
 *   button.setOnClickListener(v -> classification.getActions().get(0).getActionIntent().send());
 * }</pre>
 *
 * <p>e.g. starting an action mode with menu items that can handle the classified text:
 *
 * <pre>{ *   // Called preferably outside the UiThread.
 *   final TextClassification classification = textClassifier.classifyText(allText, 10, 25);
 *
 *   // Called on the UiThread.
 *   view.startActionMode(new ActionMode.Callback() {
 *
 *       public boolean onCreateActionMode(ActionMode mode, Menu menu) {
 *           for (int i = 0; i < classification.getActions().size(); ++i) {
 *              RemoteAction action = classification.getActions().get(i);
 *              menu.add(Menu.NONE, i, 20, action.getTitle())
 *                 .setIcon(action.getIcon());
 *           }
 *           return true;
 *       }
 *
 *       public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
 *           classification.getActions().get(item.getItemId()).getActionIntent().send();
 *           return true;
 *       }
 *
 *       ...
 *   });
 * }</pre>
 */
/* ATL (from AOSP android-16.0.0_r4): not Parcelable, because ATL's RemoteAction is not; the Request still is */
public final class TextClassification {

    /**
     *      */
    public static final TextClassification EMPTY = new TextClassification.Builder().build();

    private static final String LOG_TAG = "TextClassification";
    // TODO(toki): investigate a way to derive this based on device properties.
    private static final int MAX_LEGACY_ICON_SIZE = 192;

            private @interface IntentType {
        int UNSUPPORTED = -1;
        int ACTIVITY = 0;
        int SERVICE = 1;
    }

    private final String mText;
    private final Drawable mLegacyIcon;
    private final String mLegacyLabel;
    private final Intent mLegacyIntent;
    private final OnClickListener mLegacyOnClickListener;
    private final List<RemoteAction> mActions;
    private final EntityConfidence mEntityConfidence;
    private final String mId;
    private final Bundle mExtras;

    private TextClassification(
            String text,
            Drawable legacyIcon,
            String legacyLabel,
            Intent legacyIntent,
            OnClickListener legacyOnClickListener,
            List<RemoteAction> actions,
            EntityConfidence entityConfidence,
            String id,
            Bundle extras) {
        mText = text;
        mLegacyIcon = legacyIcon;
        mLegacyLabel = legacyLabel;
        mLegacyIntent = legacyIntent;
        mLegacyOnClickListener = legacyOnClickListener;
        mActions = Collections.unmodifiableList(actions);
        mEntityConfidence = Objects.requireNonNull(entityConfidence);
        mId = id;
        mExtras = extras;
    }

    /**
     * Gets the classified text.
     */
        public String getText() {
        return mText;
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
     * Returns a list of actions that may be performed on the text. The list is ordered based on
     * the likelihood that a user will use the action, with the most likely action appearing first.
     */
    public List<RemoteAction> getActions() {
        return mActions;
    }

    /**
     * Returns an icon that may be rendered on a widget used to act on the classified text.
     *
     * <p><strong>NOTE: </strong>This field is not parcelable and only represents the icon of the
     * first {RemoteAction} (if one exists) when this object is read from a parcel.
     *
     * Use {#getActions()} instead.
     */
    @Deprecated
        public Drawable getIcon() {
        return mLegacyIcon;
    }

    /**
     * Returns a label that may be rendered on a widget used to act on the classified text.
     *
     * <p><strong>NOTE: </strong>This field is not parcelable and only represents the label of the
     * first {RemoteAction} (if one exists) when this object is read from a parcel.
     *
     * Use {#getActions()} instead.
     */
    @Deprecated
        public CharSequence getLabel() {
        return mLegacyLabel;
    }

    /**
     * Returns an intent that may be fired to act on the classified text.
     *
     * <p><strong>NOTE: </strong>This field is not parcelled and will always return null when this
     * object is read from a parcel.
     *
     * Use {#getActions()} instead.
     */
    @Deprecated
        public Intent getIntent() {
        return mLegacyIntent;
    }

    /**
     * Returns the OnClickListener that may be triggered to act on the classified text.
     *
     * <p><strong>NOTE: </strong>This field is not parcelable and only represents the first
     * {RemoteAction} (if one exists) when this object is read from a parcel.
     *
     * Use {#getActions()} instead.
     */
        public OnClickListener getOnClickListener() {
        return mLegacyOnClickListener;
    }

    /**
     * Returns the id, if one exists, for this object.
     */
        public String getId() {
        return mId;
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
    public Builder toBuilder() {
        return new Builder()
                .setId(mId)
                .setText(mText)
                .addActions(mActions)
                .setEntityConfidence(mEntityConfidence)
                .setIcon(mLegacyIcon)
                .setLabel(mLegacyLabel)
                .setIntent(mLegacyIntent)
                .setOnClickListener(mLegacyOnClickListener)
                .setExtras(mExtras);
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "TextClassification {text=%s, entities=%s, actions=%s, id=%s, extras=%s}",
                mText, mEntityConfidence, mActions, mId, mExtras);
    }

    /**
     * Creates an OnClickListener that triggers the specified PendingIntent.
     *
     *      */
    public static OnClickListener createIntentOnClickListener(final PendingIntent intent) {
        Objects.requireNonNull(intent);
        return v -> {
            try {
                /* AOSP sends with options allowing a background activity start; ATL does not restrict them */
                intent.send((android.os.Bundle) null);
            } catch (PendingIntent.CanceledException e) {
                Log.e(LOG_TAG, "Error sending PendingIntent", e);
            }
        };
    }

    /**
     * Creates a PendingIntent for the specified intent.
     * Returns null if the intent is not supported for the specified context.
     *
     * IllegalArgumentException if context or intent is null
     *      */
    public static PendingIntent createPendingIntent(
            final Context context, final Intent intent, int requestCode) {
        return PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Builder for building {TextClassification} objects.
     *
     * <p>e.g.
     *
     * <pre>{     *   TextClassification classification = new TextClassification.Builder()
     *          .setText(classifiedText)
     *          .setEntityType(TextClassifier.TYPE_EMAIL, 0.9)
     *          .setEntityType(TextClassifier.TYPE_OTHER, 0.1)
     *          .addAction(remoteAction1)
     *          .addAction(remoteAction2)
     *          .build();
     * }</pre>
     */
    public static final class Builder {

        private final List<RemoteAction> mActions = new ArrayList<>();
        private final Map<String, Float> mTypeScoreMap = new ArrayMap<>();
        private String mText;
        private Drawable mLegacyIcon;
        private String mLegacyLabel;
        private Intent mLegacyIntent;
        private OnClickListener mLegacyOnClickListener;
        private String mId;
        private Bundle mExtras;

        /**
         * Sets the classified text.
         */
                public Builder setText(String text) {
            mText = text;
            return this;
        }

        /**
         * Sets an entity type for the classification result and assigns a confidence score.
         * If a confidence score had already been set for the specified entity type, this will
         * override that score.
         *
         * confidenceScore a value from 0 (low confidence) to 1 (high confidence).
         *      0 implies the entity does not exist for the classified text.
         *      Values greater than 1 are clamped to 1.
         */
                public Builder setEntityType(
                String type,
                float confidenceScore) {
            mTypeScoreMap.put(type, confidenceScore);
            return this;
        }

        Builder setEntityConfidence(EntityConfidence scores) {
            mTypeScoreMap.clear();
            mTypeScoreMap.putAll(scores.toMap());
            return this;
        }

        /** */
        public Builder clearEntityTypes() {
            mTypeScoreMap.clear();
            return this;
        }

        /**
         * Adds an action that may be performed on the classified text. Actions should be added in
         * order of likelihood that the user will use them, with the most likely action being added
         * first.
         */
                public Builder addAction(RemoteAction action) {
            Preconditions.checkArgument(action != null);
            mActions.add(action);
            return this;
        }

        /** */
        public Builder addActions(Collection<RemoteAction> actions) {
            Objects.requireNonNull(actions);
            mActions.addAll(actions);
            return this;
        }

        /** */
        public Builder clearActions() {
            mActions.clear();
            return this;
        }

        /**
         * Sets the icon for the <i>primary</i> action that may be rendered on a widget used to act
         * on the classified text.
         *
         * <p><strong>NOTE: </strong>This field is not parcelled. If read from a parcel, the
         * returned icon represents the icon of the first {RemoteAction} (if one exists).
         *
         * Use {#addAction(RemoteAction)} instead.
         */
        @Deprecated
                public Builder setIcon(Drawable icon) {
            mLegacyIcon = icon;
            return this;
        }

        /**
         * Sets the label for the <i>primary</i> action that may be rendered on a widget used to
         * act on the classified text.
         *
         * <p><strong>NOTE: </strong>This field is not parcelled. If read from a parcel, the
         * returned label represents the label of the first {RemoteAction} (if one exists).
         *
         * Use {#addAction(RemoteAction)} instead.
         */
        @Deprecated
                public Builder setLabel(String label) {
            mLegacyLabel = label;
            return this;
        }

        /**
         * Sets the intent for the <i>primary</i> action that may be fired to act on the classified
         * text.
         *
         * <p><strong>NOTE: </strong>This field is not parcelled.
         *
         * Use {#addAction(RemoteAction)} instead.
         */
        @Deprecated
                public Builder setIntent(Intent intent) {
            mLegacyIntent = intent;
            return this;
        }

        /**
         * Sets the OnClickListener for the <i>primary</i> action that may be triggered to act on
         * the classified text.
         *
         * <p><strong>NOTE: </strong>This field is not parcelable. If read from a parcel, the
         * returned OnClickListener represents the first {RemoteAction} (if one exists).
         *
         * Use {#addAction(RemoteAction)} instead.
         */
        @Deprecated
                public Builder setOnClickListener(OnClickListener onClickListener) {
            mLegacyOnClickListener = onClickListener;
            return this;
        }

        /**
         * Sets an id for the TextClassification object.
         */
                public Builder setId(String id) {
            mId = id;
            return this;
        }

        /**
         * Sets the extended data.
         */
                public Builder setExtras(Bundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * Builds and returns a {TextClassification} object.
         */
                public TextClassification build() {
            EntityConfidence entityConfidence = new EntityConfidence(mTypeScoreMap);
            return new TextClassification(mText, mLegacyIcon, mLegacyLabel, mLegacyIntent,
                    mLegacyOnClickListener, mActions, entityConfidence, mId,
                    mExtras == null ? Bundle.EMPTY : mExtras);
        }
    }

    /**
     * A request object for generating TextClassification.
     */
    public static final class Request implements Parcelable {

        private final CharSequence mText;
        private final int mStartIndex;
        private final int mEndIndex;
        private final LocaleList mDefaultLocales;
        private final ZonedDateTime mReferenceTime;
        private final Bundle mExtras;
        private SystemTextClassifierMetadata mSystemTcMetadata;

        private Request(
                CharSequence text,
                int startIndex,
                int endIndex,
                LocaleList defaultLocales,
                ZonedDateTime referenceTime,
                Bundle extras) {
            mText = text;
            mStartIndex = startIndex;
            mEndIndex = endIndex;
            mDefaultLocales = defaultLocales;
            mReferenceTime = referenceTime;
            mExtras = extras;
        }

        /**
         * Returns the text providing context for the text to classify (which is specified
         *      by the sub sequence starting at startIndex and ending at endIndex)
         */
                public CharSequence getText() {
            return mText;
        }

        /**
         * Returns start index of the text to classify.
         */
                public int getStartIndex() {
            return mStartIndex;
        }

        /**
         * Returns end index of the text to classify.
         */
                public int getEndIndex() {
            return mEndIndex;
        }

        /**
         * ordered list of locale preferences that can be used to disambiguate
         *      the provided text.
         */
                public LocaleList getDefaultLocales() {
            return mDefaultLocales;
        }

        /**
         * reference time based on which relative dates (e.g. "tomorrow") should be
         *      interpreted.
         */
                public ZonedDateTime getReferenceTime() {
            return mReferenceTime;
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
         * Returns the extended data.
         *
         * <p><b>NOTE: </b>Do not modify this bundle.
         */
                public Bundle getExtras() {
            return mExtras;
        }

        /**
         * A builder for building TextClassification requests.
         */
        public static final class Builder {

            private final CharSequence mText;
            private final int mStartIndex;
            private final int mEndIndex;
            private Bundle mExtras;

            private LocaleList mDefaultLocales;
            private ZonedDateTime mReferenceTime;

            /**
             * text text providing context for the text to classify (which is specified
             *      by the sub sequence starting at startIndex and ending at endIndex)
             * startIndex start index of the text to classify
             * endIndex end index of the text to classify
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
             * this builder
             */
                        public Builder setDefaultLocales(LocaleList defaultLocales) {
                mDefaultLocales = defaultLocales;
                return this;
            }

            /**
             * referenceTime reference time based on which relative dates (e.g. "tomorrow"
             *      should be interpreted. This should usually be the time when the text was
             *      originally composed. If no reference time is set, now is used.
             *
             * this builder
             */
                        public Builder setReferenceTime(ZonedDateTime referenceTime) {
                mReferenceTime = referenceTime;
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
                        mDefaultLocales, mReferenceTime,
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
            dest.writeString(mReferenceTime == null ? null : mReferenceTime.toString());
            dest.writeBundle(mExtras);
            dest.writeParcelable(mSystemTcMetadata, flags);
        }

        private static Request readFromParcel(Parcel in) {
            final CharSequence text = in.readCharSequence();
            final int startIndex = in.readInt();
            final int endIndex = in.readInt();
            final LocaleList defaultLocales = in.readParcelable(null, android.os.LocaleList.class);
            final String referenceTimeString = in.readString();
            final ZonedDateTime referenceTime = referenceTimeString == null
                    ? null : ZonedDateTime.parse(referenceTimeString);
            final Bundle extras = in.readBundle();
            final SystemTextClassifierMetadata systemTcMetadata = in.readParcelable(null, android.view.textclassifier.SystemTextClassifierMetadata.class);

            final Request request = new Request(text, startIndex, endIndex,
                    defaultLocales, referenceTime, extras);
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

}
