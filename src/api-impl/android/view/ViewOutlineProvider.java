package android.view;

import android.graphics.Outline;
import android.graphics.drawable.Drawable;

/* Copyright (C) 2014 The Android Open Source Project */
public abstract class ViewOutlineProvider {
	/* the outline of the background drawable, or no outline without a background */
	public static final ViewOutlineProvider BACKGROUND = new ViewOutlineProvider() {
		@Override
		public void getOutline(View view, Outline outline) {
			Drawable background = view.getBackground();
			if (background != null) {
				background.getOutline(outline);
			} else {
				outline.setRect(0, 0, view.getWidth(), view.getHeight());
				outline.setAlpha(0.0f);
			}
		}
	};

	public static final ViewOutlineProvider BOUNDS = new ViewOutlineProvider() {
		@Override
		public void getOutline(View view, Outline outline) {
			outline.setRect(0, 0, view.getWidth(), view.getHeight());
		}
	};

	public static final ViewOutlineProvider PADDED_BOUNDS = new ViewOutlineProvider() {
		@Override
		public void getOutline(View view, Outline outline) {
			outline.setRect(view.getPaddingLeft(), view.getPaddingTop(),
			                view.getWidth() - view.getPaddingRight(), view.getHeight() - view.getPaddingBottom());
		}
	};

	public abstract void getOutline(View view, Outline outline);
}
