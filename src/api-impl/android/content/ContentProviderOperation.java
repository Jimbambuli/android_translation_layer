/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.content;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.SparseArray;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ContentProviderOperation implements Parcelable {

	public final static int TYPE_INSERT = 1;
	public final static int TYPE_UPDATE = 2;
	public final static int TYPE_DELETE = 3;
	public final static int TYPE_ASSERT = 4;
	public final static int TYPE_CALL = 5;

	private final int mType;
	private final Uri mUri;
	private final String mMethod;
	private final String mArg;
	private final ArrayMap<String, Object> mValues;
	private final ArrayMap<String, Object> mExtras;
	private final String mSelection;
	private final SparseArray<Object> mSelectionArgs;
	private final Integer mExpectedCount;
	private final boolean mYieldAllowed;
	private final boolean mExceptionAllowed;

	private ContentProviderOperation(Builder builder) {
		mType = builder.mType;
		mUri = builder.mUri;
		mMethod = builder.mMethod;
		mArg = builder.mArg;
		mValues = builder.mValues;
		mExtras = builder.mExtras;
		mSelection = builder.mSelection;
		mSelectionArgs = builder.mSelectionArgs;
		mExpectedCount = builder.mExpectedCount;
		mYieldAllowed = builder.mYieldAllowed;
		mExceptionAllowed = builder.mExceptionAllowed;
	}

	private ContentProviderOperation(Parcel source) throws ReflectiveOperationException {
		mType = source.readInt();
		mUri = Uri.CREATOR.createFromParcel(source);
		mMethod = source.readInt() != 0 ? source.readString() : null;
		mArg = source.readInt() != 0 ? source.readString() : null;
		final int valuesSize = source.readInt();
		if (valuesSize != -1) {
			mValues = new ArrayMap<>(valuesSize);
			source.readMap(mValues, null);
		} else {
			mValues = null;
		}
		final int extrasSize = source.readInt();
		if (extrasSize != -1) {
			mExtras = new ArrayMap<>(extrasSize);
			source.readMap(mExtras, null);
		} else {
			mExtras = null;
		}
		mSelection = source.readInt() != 0 ? source.readString() : null;
		mSelectionArgs = source.readSparseArray(null);
		mExpectedCount = source.readInt() != 0 ? source.readInt() : null;
		mYieldAllowed = source.readInt() != 0;
		mExceptionAllowed = source.readInt() != 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mType);
		Uri.writeToParcel(dest, mUri);
		if (mMethod != null) {
			dest.writeInt(1);
			dest.writeString(mMethod);
		} else {
			dest.writeInt(0);
		}
		if (mArg != null) {
			dest.writeInt(1);
			dest.writeString(mArg);
		} else {
			dest.writeInt(0);
		}
		if (mValues != null) {
			dest.writeInt(mValues.size());
			dest.writeMap(mValues);
		} else {
			dest.writeInt(-1);
		}
		if (mExtras != null) {
			dest.writeInt(mExtras.size());
			dest.writeMap(mExtras);
		} else {
			dest.writeInt(-1);
		}
		if (mSelection != null) {
			dest.writeInt(1);
			dest.writeString(mSelection);
		} else {
			dest.writeInt(0);
		}
		dest.writeSparseArray(mSelectionArgs);
		if (mExpectedCount != null) {
			dest.writeInt(1);
			dest.writeInt(mExpectedCount);
		} else {
			dest.writeInt(0);
		}
		dest.writeInt(mYieldAllowed ? 1 : 0);
		dest.writeInt(mExceptionAllowed ? 1 : 0);
	}

	public static final @android.annotation.NonNull Creator<ContentProviderOperation> CREATOR = new Creator<ContentProviderOperation>() {
		@Override
		public ContentProviderOperation createFromParcel(Parcel source) {
			try {
				return new ContentProviderOperation(source);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public ContentProviderOperation[] newArray(int size) {
			return new ContentProviderOperation[size];
		}
	};

	public Uri getUri() {
		return mUri;
	}

	public boolean isYieldAllowed() {
		return mYieldAllowed;
	}

	public ContentProviderResult apply(ContentProvider provider, ContentProviderResult[] backRefs, int numBackRefs)
	    throws OperationApplicationException {
		if (mExceptionAllowed) {
			try {
				return applyInternal(provider, backRefs, numBackRefs);
			} catch (Exception e) {
				return new ContentProviderResult(e);
			}
		}
		return applyInternal(provider, backRefs, numBackRefs);
	}

	private ContentProviderResult applyInternal(ContentProvider provider, ContentProviderResult[] backRefs, int numBackRefs)
	    throws OperationApplicationException {
		final ContentValues values = resolveValueBackReferences(backRefs, numBackRefs);
		final String[] selectionArgs = resolveSelectionArgsBackReferences(backRefs, numBackRefs);
		if (mType == TYPE_INSERT) {
			final Uri newUri = provider.insert(mUri, values);
			if (newUri == null)
				throw new OperationApplicationException("Insert into " + mUri + " returned no result");
			return new ContentProviderResult(newUri);
		}
		final int numRows;
		if (mType == TYPE_UPDATE) {
			numRows = provider.update(mUri, values, mSelection, selectionArgs);
		} else if (mType == TYPE_DELETE) {
			numRows = provider.delete(mUri, mSelection, selectionArgs);
		} else if (mType == TYPE_ASSERT) {
			/* AOSP applyInternal(): every row of the query must hold the expected values */
			String[] projection = null;
			if (values != null) {
				final java.util.ArrayList<String> projectionList = new java.util.ArrayList<String>();
				for (Map.Entry<String, Object> entry : values.valueSet()) {
					projectionList.add(entry.getKey());
				}
				projection = projectionList.toArray(new String[projectionList.size()]);
			}
			final android.database.Cursor cursor = provider.query(mUri, projection, mSelection, selectionArgs, null, null);
			try {
				numRows = cursor.getCount();
				if (projection != null) {
					while (cursor.moveToNext()) {
						for (int i = 0; i < projection.length; i++) {
							final String cursorValue = cursor.getString(i);
							final String expectedValue = values.getAsString(projection[i]);
							if (!android.text.TextUtils.equals(cursorValue, expectedValue)) {
								throw new OperationApplicationException("Found value " + cursorValue + " when expected " + expectedValue
								                                        + " for column " + projection[i]);
							}
						}
					}
				}
			} finally {
				cursor.close();
			}
		} else {
			throw new UnsupportedOperationException("ContentProviderOperation type " + mType + " not implemented");
		}
		if (mExpectedCount != null && mExpectedCount != numRows)
			throw new OperationApplicationException("Expected " + mExpectedCount + " rows but actual " + numRows);
		return new ContentProviderResult(numRows);
	}

	/* AOSP ContentProviderOperation.resolveValueBackReferences */
	public ContentValues resolveValueBackReferences(ContentProviderResult[] backRefs, int numBackRefs) {
		if (mValues != null) {
			final ContentValues values = new ContentValues();
			for (int i = 0; i < mValues.size(); i++) {
				final Object value = mValues.valueAt(i);
				final Object resolved;
				if (value instanceof BackReference) {
					resolved = ((BackReference) value).resolve(backRefs, numBackRefs);
				} else {
					resolved = value;
				}
				values.putObject(mValues.keyAt(i), resolved);
			}
			return values;
		} else {
			return null;
		}
	}

	/* AOSP ContentProviderOperation.resolveSelectionArgsBackReferences */
	public String[] resolveSelectionArgsBackReferences(ContentProviderResult[] backRefs, int numBackRefs) {
		if (mSelectionArgs != null) {
			int max = -1;
			for (int i = 0; i < mSelectionArgs.size(); i++) {
				max = Math.max(max, mSelectionArgs.keyAt(i));
			}

			final String[] selectionArgs = new String[max + 1];
			for (int i = 0; i < mSelectionArgs.size(); i++) {
				final Object value = mSelectionArgs.valueAt(i);
				final Object resolved;
				if (value instanceof BackReference) {
					resolved = ((BackReference) value).resolve(backRefs, numBackRefs);
				} else {
					resolved = value;
				}
				selectionArgs[mSelectionArgs.keyAt(i)] = String.valueOf(resolved);
			}
			return selectionArgs;
		} else {
			return null;
		}
	}

	/* AOSP ContentProviderOperation.BackReference (without its Parcel form) */
	public static class BackReference {
		private final int fromIndex;
		private final String fromKey;

		private BackReference(int fromIndex, String fromKey) {
			this.fromIndex = fromIndex;
			this.fromKey = fromKey;
		}

		public Object resolve(ContentProviderResult[] backRefs, int numBackRefs) {
			if (fromIndex >= numBackRefs) {
				throw new ArrayIndexOutOfBoundsException("asked for back ref " + fromIndex
				    + " but there are only " + numBackRefs + " back refs");
			}
			ContentProviderResult backRef = backRefs[fromIndex];
			Object backRefValue;
			if (backRef.extras != null) {
				backRefValue = backRef.extras.get(fromKey);
			} else if (backRef.uri != null) {
				backRefValue = ContentUris.parseId(backRef.uri);
			} else {
				backRefValue = (long) backRef.count;
			}
			return backRefValue;
		}
	}

	public static Builder newUpdate(Uri uri) {
		return new Builder(TYPE_UPDATE, uri);
	}

	public static Builder newInsert(Uri uri) {
		return new Builder(TYPE_INSERT, uri);
	}

	public static Builder newDelete(Uri uri) {
		return new Builder(TYPE_DELETE, uri);
	}

	/* AOSP ContentProviderOperation.newAssertQuery() */
	public static Builder newAssertQuery(Uri uri) {
		return new Builder(TYPE_ASSERT, uri);
	}

	/* AOSP ContentProviderOperation.isAssertQuery(), isReadOperation() */
	public boolean isAssertQuery() {
		return mType == TYPE_ASSERT;
	}

	public boolean isReadOperation() {
		return mType == TYPE_ASSERT;
	}

	public static class Builder {
		private final int mType;
		private final Uri mUri;
		private final String mMethod;
		private final String mArg;
		private ArrayMap<String, Object> mValues;
		private ArrayMap<String, Object> mExtras;
		private String mSelection;
		private SparseArray<Object> mSelectionArgs;
		private Integer mExpectedCount;
		private boolean mYieldAllowed;
		private boolean mExceptionAllowed;

		private Builder(int type, Uri uri) {
			this(type, uri, null, null);
		}

		private Builder(int type, Uri uri, String method, String arg) {
			mType = type;
			mUri = Objects.requireNonNull(uri);
			mMethod = method;
			mArg = arg;
		}

		/* AOSP Builder.setValue: a back reference is not replaced by a plain value */
		private void setValue(String key, Object value) {
			if (mValues == null)
				mValues = new ArrayMap<>();
			final boolean oldReference = mValues.get(key) instanceof BackReference;
			final boolean newReference = value instanceof BackReference;
			if (!oldReference || newReference) {
				mValues.put(key, value);
			}
		}

		private void setSelectionArg(int index, Object value) {
			if (mSelectionArgs == null)
				mSelectionArgs = new SparseArray<>();
			final boolean oldReference = mSelectionArgs.get(index) instanceof BackReference;
			final boolean newReference = value instanceof BackReference;
			if (!oldReference || newReference) {
				mSelectionArgs.put(index, value);
			}
		}

		private void assertValuesAllowed() {
			if (mType != TYPE_INSERT && mType != TYPE_UPDATE && mType != TYPE_ASSERT) {
				throw new IllegalArgumentException("Values not supported for type " + mType);
			}
		}

		private void assertSelectionAllowed() {
			if (mType != TYPE_UPDATE && mType != TYPE_DELETE && mType != TYPE_ASSERT) {
				throw new IllegalArgumentException("Selection not supported for type " + mType);
			}
		}

		/* AOSP Builder.withValueBackReference(s) / withSelectionBackReference */
		public Builder withValueBackReferences(ContentValues backReferences) {
			assertValuesAllowed();
			for (Map.Entry<String, Object> entry : backReferences.valueSet()) {
				setValue(entry.getKey(), new BackReference(((Number) entry.getValue()).intValue(), null));
			}
			return this;
		}

		public Builder withValueBackReference(String key, int fromIndex) {
			assertValuesAllowed();
			setValue(key, new BackReference(fromIndex, null));
			return this;
		}

		public Builder withValueBackReference(String key, int fromIndex, String fromKey) {
			assertValuesAllowed();
			setValue(key, new BackReference(fromIndex, fromKey));
			return this;
		}

		public Builder withSelectionBackReference(int index, int fromIndex) {
			assertSelectionAllowed();
			setSelectionArg(index, new BackReference(fromIndex, null));
			return this;
		}

		public Builder withSelectionBackReference(int index, int fromIndex, String fromKey) {
			assertSelectionAllowed();
			setSelectionArg(index, new BackReference(fromIndex, fromKey));
			return this;
		}

		public Builder withValue(String key, Object value) {
			if (mType != TYPE_INSERT && mType != TYPE_UPDATE && mType != TYPE_ASSERT) {
				throw new IllegalArgumentException("only inserts, updates, and asserts can have values");
			}
			if (value == null || value instanceof String || value instanceof Byte || value instanceof Short || value instanceof Integer
			    || value instanceof Long || value instanceof Float || value instanceof Double || value instanceof Boolean || value instanceof byte[]) {
				setValue(key, value);
			} else {
				throw new IllegalArgumentException("bad value type: " + value.getClass().getName());
			}
			return this;
		}

		public Builder withValues(ContentValues values) {
			final Set<Map.Entry<String, Object>> rawValues = values.valueSet();
			for (Map.Entry<String, Object> entry : rawValues) {
				setValue(entry.getKey(), entry.getValue());
			}
			return this;
		}

		public Builder withSelection(String selection, String[] selectionArgs) {
			mSelection = selection;
			if (mSelectionArgs == null)
				mSelectionArgs = new SparseArray<>();
			if (selectionArgs != null) {
				for (int i = 0; i < selectionArgs.length; i++) {
					setSelectionArg(i, selectionArgs[i]);
				}
			}
			return this;
		}

		/* AOSP Builder.withExpectedCount(), withYieldAllowed() */
		public Builder withExpectedCount(int count) {
			if (mType != TYPE_UPDATE && mType != TYPE_DELETE && mType != TYPE_ASSERT) {
				throw new IllegalArgumentException("only updates, deletes, and asserts can have expected counts");
			}
			mExpectedCount = count;
			return this;
		}

		public Builder withYieldAllowed(boolean yieldAllowed) {
			mYieldAllowed = yieldAllowed;
			return this;
		}

		public ContentProviderOperation build() {
			if (mType == TYPE_UPDATE) {
				if ((mValues == null || mValues.isEmpty())) {
					throw new IllegalArgumentException("Empty values");
				}
			}
			if (mType == TYPE_ASSERT) {
				if ((mValues == null || mValues.isEmpty())
				    && (mExpectedCount == null)) {
					throw new IllegalArgumentException("Empty values");
				}
			}
			return new ContentProviderOperation(this);
		}
	}
}
