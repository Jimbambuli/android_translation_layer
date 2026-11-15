package android.service.autofill;

import android.widget.RemoteViews;

/* AOSP Presentations (codegen data class with Builder); Parcel code left out, nothing in ATL parcels it */
public final class Presentations {

	private RemoteViews mMenuPresentation;
	private InlinePresentation mInlinePresentation;
	private RemoteViews mDialogPresentation;
	private InlinePresentation mInlineTooltipPresentation;

	/* AOSP Presentations.onConstructed */
	private void onConstructed() {
		if (mMenuPresentation == null && mInlinePresentation == null && mDialogPresentation == null) {
			throw new IllegalStateException("All presentations are null.");
		}
		if (mInlineTooltipPresentation != null && mInlinePresentation == null) {
			throw new IllegalStateException("The inline presentation is required for mInlineTooltipPresentation.");
		}
	}

	Presentations(RemoteViews menuPresentation, InlinePresentation inlinePresentation, RemoteViews dialogPresentation, InlinePresentation inlineTooltipPresentation) {
		this.mMenuPresentation = menuPresentation;
		this.mInlinePresentation = inlinePresentation;
		this.mDialogPresentation = dialogPresentation;
		this.mInlineTooltipPresentation = inlineTooltipPresentation;
		onConstructed();
	}

	public RemoteViews getMenuPresentation() {
		return mMenuPresentation;
	}

	public InlinePresentation getInlinePresentation() {
		return mInlinePresentation;
	}

	public RemoteViews getDialogPresentation() {
		return mDialogPresentation;
	}

	public InlinePresentation getInlineTooltipPresentation() {
		return mInlineTooltipPresentation;
	}

	public static final class Builder {

		private RemoteViews mMenuPresentation;
		private InlinePresentation mInlinePresentation;
		private RemoteViews mDialogPresentation;
		private InlinePresentation mInlineTooltipPresentation;
		private boolean mUsed = false;

		public Builder() {
		}

		public Builder setMenuPresentation(RemoteViews value) {
			checkNotUsed();
			mMenuPresentation = value;
			return this;
		}

		public Builder setInlinePresentation(InlinePresentation value) {
			checkNotUsed();
			mInlinePresentation = value;
			return this;
		}

		public Builder setDialogPresentation(RemoteViews value) {
			checkNotUsed();
			mDialogPresentation = value;
			return this;
		}

		public Builder setInlineTooltipPresentation(InlinePresentation value) {
			checkNotUsed();
			mInlineTooltipPresentation = value;
			return this;
		}

		public Presentations build() {
			checkNotUsed();
			mUsed = true;
			return new Presentations(mMenuPresentation, mInlinePresentation, mDialogPresentation, mInlineTooltipPresentation);
		}

		private void checkNotUsed() {
			if (mUsed) {
				throw new IllegalStateException("This Builder should not be reused. Use a new Builder instance instead");
			}
		}
	}
}
