package android.media.projection;

/* AOSP android/media/projection/AppContentRequest.java */
import android.util.Size;

import android.annotation.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Contains the requested characteristic for an app content projection.
 * <p>
 * An instance of this class is passed to
 * {@link AppContentProjectionService#onContentRequest(AppContentRequest)}
 * <p>
 * Available content must be returned by calling {@link #provideContent(List)}}
 */
public final class AppContentRequest {

    private final Size mThumbnailSize;
    private final Consumer<MediaProjectionAppContent[]> mContentConsumer;

    
    /**
     * Used to return the content to the requester. The content passed here will replace any
     * previously provided content.
     * <p>
     * If no more content is available to offer, an empty list can be passed.
     */
    public void provideContent(@NonNull List<MediaProjectionAppContent> content) {
        Objects.requireNonNull(content,
                "content must not be null. If all the content needs to be removed, an empty list"
                        + " can be passed");
        mContentConsumer.accept(content.toArray(new MediaProjectionAppContent[0]));
    }

    /**
     * @return The requested thumbnail size, in px, for each {@link MediaProjectionAppContent}
     * item.
     */
    @NonNull
    public Size getThumbnailSize() {
        return mThumbnailSize;
    }

}
