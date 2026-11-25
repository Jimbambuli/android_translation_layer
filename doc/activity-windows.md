# Activity windows in ATL: AOSP's window stack in one GTK window

On Android every activity has a window of its own; the window manager stacks them per task, decides which are
visible, dims behind dialogs and sends input to the focused one. ATL shows all activities of a process in one
GtkWindow. This file states the AOSP rules and how ATL maps them (`src/api-impl-jni/app/android_app_Activity.c`).

## AOSP rules (Android 16)

| Topic | AOSP | Source |
|---|---|---|
| Visibility | From the top of the task down, activities are visible up to and including the first one that occludes its parent; an activity occludes unless it is translucent or floating. Visible activities keep drawing (videos, games, animations); those below are invisible and stopped. A visible activity that is not on top stays paused. | `TaskFragment.updateActivityVisibilities()`, `ActivityRecord.occludesParent()`, `ActivityInfo.isTranslucentOrFloating()` |
| Window of a visible activity | The window of an activity that becomes visible is added even if it never was on top (e.g. it started a translucent activity from `onCreate()`). | `ActivityThread.handleResumeActivity()` / `Activity.makeVisible()` |
| Floating window | `windowIsFloating`: the window is `WRAP_CONTENT` x `WRAP_CONTENT`; measured with `getRootMeasureSpec()` (AT_MOST the parent for WRAP_CONTENT), for WRAP_CONTENT width first at `config_prefDialogWidth`; the decor view applies `windowMinWidthMajor/Minor`; placed with `Gravity.apply()` (middle for gravity 0) and kept inside the parent (`Gravity.applyDisplay()`). Every new size of the parent lays it out again. | `PhoneWindow.generateLayout()`, `ViewRootImpl.measureHierarchy()`, `WindowLayout.computeFrames()`, `DecorView.onMeasure()` |
| Dim | `backgroundDimEnabled` (default: floating) sets `FLAG_DIM_BEHIND` and `dimAmount = backgroundDimAmount`. One dim layer, right below the topmost visible window with `FLAG_DIM_BEHIND`; it fades when it appears, changes or goes. | `PhoneWindow.generateLayout()`, `Dimmer` (services, not in the local AOSP copy: fade duration 200 ms from the handover, not checked) |
| Input and focus | Only the top (focused) window gets keys and touches. A floating window is touch-modal (no `FLAG_NOT_TOUCH_MODAL`): touches outside of it go to it, with coordinates outside its frame; `windowCloseOnTouchOutside` finishes the activity on such a touch. | `Activity.onTouchEvent()`, `Window.shouldCloseOnTouch()` |
| Window animations | Only windows that change visibility animate. Opening a translucent activity animates only its window (`activity_translucent_open_enter`), closing one only its window (`activity_translucent_close_exit`); the window below stays as it is. | `TransitionAnimation.loadAnimationAttr()`, `updateToTranslucentAnimIfNeeded()` |
| Lifecycle | see `stop_when_idle()` (37df2f2cd): an activity under a translucent or floating one gets `onPause()` only. | `TaskFragment.updateActivityVisibilities()` |

## ATL mapping

- **AtlPages** (GtkWidget) holds the pages (decor views) of the activities in back stack order, bottom first, next to
  the popovers of `WindowManagerImpl` (windows of their own, not pages). Each page carries its `Window` and whether it
  is floating (`PageInfo`).
- **`pages_update()`** applies the visibility rule with `activity_occludes()`, the same predicate `stop_when_idle()`
  uses for the lifecycle. Visible pages are child-visible and drawn live, bottom to top, each frame; hidden pages stay
  children but are neither drawn nor laid out. A visible activity without a page gets it now (`makeVisible()`), except
  the top of the back stack, whose page is added by `pages_show()` after `onResume()` (`handleResumeActivity()`).
  It runs when an activity is shown, closed, recreated, or goes to or comes back from picture-in-picture.
- **Layout**: a page that is not floating fills AtlPages. A floating page gets the frame of `Window.layoutFloating()`
  (the AOSP measure and gravity above) in every `size_allocate`, so a new window size lays it out again. AtlPages
  measures like GtkStack, from the pages that are not floating.
- **Dim**: one black layer of `Window.getDimBehind()` right below the topmost visible page with `FLAG_DIM_BEHIND`,
  fading in 200 ms times `ValueAnimator.getDurationScale()` (at once with animations off). When that page is closed
  the layer fades out over the pages below; when an opaque page covers it, it goes at once (it is below that page).
  Read when the pages change (`pages_update()`); a later `setDimAmount()` shows with the next change.
- **Input and focus**: only the top visible page can be targeted and focused (`can_target`, `can_focus`); the pages
  below see no input. Presses next to a floating top page go to its decor view as a `MotionEvent` outside its frame
  (`on_touch_outside()`), so `windowCloseOnTouchOutside` works. Keys go to the focused activity as before. The focus
  of a page that is left is kept and given back in place when it is on top again (`pages_show()`).
- **Window animations**: `Activity.startWindowTransition(…, translucent, …)`. Opening an opaque activity: a snapshot of
  all visible pages animates out below the new page; opening a translucent or floating one: no snapshot, the pages
  below stay live. Closing: a snapshot of the closed page animates out above the revealed pages. Animations off
  (duration scale 0): none.
- **Back and finish**: `finish()`/Back remove the activity from the back stack; the page below that was visible is
  resumed (37df2f2cd) and needs no new drawing. A finished page that is not on top (middle of a stack) just goes away.

## Cases

| Case | Visible pages | Dim | Input |
|---|---|---|---|
| Opaque activity | it alone | none | it |
| Translucent (`Theme.Translucent`) over A | A, translucent | if `backgroundDimEnabled` | translucent |
| Dialog-themed (floating) over A | A, dialog (wrapped, centred) | below the dialog (0.5 by default theme value) | dialog; press outside → dialog |
| Dialog over dialog over A | A, dialog 1, dialog 2 | one layer, below dialog 2 | dialog 2 |
| Opaque over a dialog | the opaque one | none | it |
| Floating activity started from `onCreate()` of A | A (page added, never on top), floating | below the floating one | floating |
| Activity recreated below a translucent one | new page of the new instance | unchanged | top |

Not covered: other tasks and the launcher below a task whose bottom activity is translucent (ATL shows the window
background there), floating windows larger than the app's window (`FLAG_LAYOUT_NO_LIMITS`).
