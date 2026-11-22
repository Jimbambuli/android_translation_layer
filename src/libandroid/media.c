
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

/*
 * Copyright (C) 2014 The Android Open Source Project
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

/*
 * This file defines an NDK API.
 * Do not remove methods.
 * Do not change method signatures.
 * Do not change the value of constants.
 * Do not change the size of any of the classes defined in here.
 * Do not reference types that are not part of the NDK.
 * Do not #include files that aren't part of the NDK.
 */

typedef enum {
	AMEDIA_OK = 0,

	AMEDIA_ERROR_BASE = -10000,
	AMEDIA_ERROR_UNKNOWN = AMEDIA_ERROR_BASE,
	AMEDIA_ERROR_MALFORMED = AMEDIA_ERROR_BASE - 1,
	AMEDIA_ERROR_UNSUPPORTED = AMEDIA_ERROR_BASE - 2,
	AMEDIA_ERROR_INVALID_OBJECT = AMEDIA_ERROR_BASE - 3,
	AMEDIA_ERROR_INVALID_PARAMETER = AMEDIA_ERROR_BASE - 4,
	AMEDIA_ERROR_INVALID_OPERATION = AMEDIA_ERROR_BASE - 5,

	AMEDIA_DRM_ERROR_BASE = -20000,
	AMEDIA_DRM_NOT_PROVISIONED = AMEDIA_DRM_ERROR_BASE - 1,
	AMEDIA_DRM_RESOURCE_BUSY = AMEDIA_DRM_ERROR_BASE - 2,
	AMEDIA_DRM_DEVICE_REVOKED = AMEDIA_DRM_ERROR_BASE - 3,
	AMEDIA_DRM_SHORT_BUFFER = AMEDIA_DRM_ERROR_BASE - 4,
	AMEDIA_DRM_SESSION_NOT_OPENED = AMEDIA_DRM_ERROR_BASE - 5,
	AMEDIA_DRM_TAMPER_DETECTED = AMEDIA_DRM_ERROR_BASE - 6,
	AMEDIA_DRM_VERIFY_FAILED = AMEDIA_DRM_ERROR_BASE - 7,
	AMEDIA_DRM_NEED_KEY = AMEDIA_DRM_ERROR_BASE - 8,
	AMEDIA_DRM_LICENSE_EXPIRED = AMEDIA_DRM_ERROR_BASE - 9,

	AMEDIA_IMGREADER_ERROR_BASE = -30000,
	AMEDIA_IMGREADER_NO_BUFFER_AVAILABLE = AMEDIA_IMGREADER_ERROR_BASE - 1,
	AMEDIA_IMGREADER_MAX_IMAGES_ACQUIRED = AMEDIA_IMGREADER_ERROR_BASE - 2,
	AMEDIA_IMGREADER_CANNOT_LOCK_IMAGE = AMEDIA_IMGREADER_ERROR_BASE - 3,
	AMEDIA_IMGREADER_CANNOT_UNLOCK_IMAGE = AMEDIA_IMGREADER_ERROR_BASE - 4,
	AMEDIA_IMGREADER_IMAGE_NOT_LOCKED = AMEDIA_IMGREADER_ERROR_BASE - 5,
} media_status_t;

struct AMediaFormat;
typedef struct AMediaFormat AMediaFormat;

AMediaFormat *AMediaFormat_new()
{
	return NULL;
}

media_status_t AMediaFormat_delete(AMediaFormat *)
{
	return AMEDIA_ERROR_UNKNOWN;
}

/**
 * Human readable representation of the format. The returned string is owned by the format,
 * and remains valid until the next call to toString, or until the format is deleted.
 */
const char *AMediaFormat_toString(AMediaFormat *)
{
	return NULL;
}

bool AMediaFormat_getInt32(AMediaFormat *, const char *name, int32_t *out)
{
	return false;
}

bool AMediaFormat_getInt64(AMediaFormat *, const char *name, int64_t *out)
{
	return false;
}

bool AMediaFormat_getFloat(AMediaFormat *, const char *name, float *out)
{
	return false;
}

/**
 * The returned data is owned by the format and remains valid as long as the named entry
 * is part of the format.
 */
bool AMediaFormat_getBuffer(AMediaFormat *, const char *name, void **data, size_t *size)
{
	return false;
}

/**
 * The returned string is owned by the format, and remains valid until the next call to getString,
 * or until the format is deleted.
 */
bool AMediaFormat_getString(AMediaFormat *, const char *name, const char **out)
{
	return false;
}

void AMediaFormat_setInt32(AMediaFormat *, const char *name, int32_t value) {}
void AMediaFormat_setInt64(AMediaFormat *, const char *name, int64_t value) {}
void AMediaFormat_setFloat(AMediaFormat *, const char *name, float value) {}
/**
 * The provided string is copied into the format.
 */
void AMediaFormat_setString(AMediaFormat *, const char *name, const char *value) {}
/**
 * The provided data is copied into the format.
 */
void AMediaFormat_setBuffer(AMediaFormat *, const char *name, void *data, size_t size) {}

/* AOSP media/ndk/NdkMediaFormat.cpp: all keys with their values */
const char *AMEDIAFORMAT_KEY_AAC_DRC_ATTENUATION_FACTOR = "aac-drc-cut-level";
const char *AMEDIAFORMAT_KEY_AAC_DRC_BOOST_FACTOR = "aac-drc-boost-level";
const char *AMEDIAFORMAT_KEY_AAC_DRC_HEAVY_COMPRESSION = "aac-drc-heavy-compression";
const char *AMEDIAFORMAT_KEY_AAC_DRC_TARGET_REFERENCE_LEVEL = "aac-target-ref-level";
const char *AMEDIAFORMAT_KEY_AAC_ENCODED_TARGET_LEVEL = "aac-encoded-target-level";
const char *AMEDIAFORMAT_KEY_AAC_MAX_OUTPUT_CHANNEL_COUNT = "aac-max-output-channel_count";
const char *AMEDIAFORMAT_KEY_AAC_PROFILE = "aac-profile";
const char *AMEDIAFORMAT_KEY_AAC_SBR_MODE = "aac-sbr-mode";
const char *AMEDIAFORMAT_KEY_ALBUM = "album";
const char *AMEDIAFORMAT_KEY_ALBUMART = "albumart";
const char *AMEDIAFORMAT_KEY_ALBUMARTIST = "albumartist";
const char *AMEDIAFORMAT_KEY_ALLOW_FRAME_DROP = "allow-frame-drop";
const char *AMEDIAFORMAT_KEY_ARTIST = "artist";
const char *AMEDIAFORMAT_KEY_AUDIO_PRESENTATION_INFO = "audio-presentation-info";
const char *AMEDIAFORMAT_KEY_AUDIO_PRESENTATION_PRESENTATION_ID = "audio-presentation-presentation-id";
const char *AMEDIAFORMAT_KEY_AUDIO_PRESENTATION_PROGRAM_ID = "audio-presentation-program-id";
const char *AMEDIAFORMAT_KEY_AUDIO_SESSION_ID = "audio-session-id";
const char *AMEDIAFORMAT_KEY_AUTHOR = "author";
const char *AMEDIAFORMAT_KEY_BITRATE_MODE = "bitrate-mode";
const char *AMEDIAFORMAT_KEY_BIT_RATE = "bitrate";
const char *AMEDIAFORMAT_KEY_BITS_PER_SAMPLE = "bits-per-sample";
const char *AMEDIAFORMAT_KEY_CAPTURE_RATE = "capture-rate";
const char *AMEDIAFORMAT_KEY_CDTRACKNUMBER = "cdtracknum";
const char *AMEDIAFORMAT_KEY_CHANNEL_COUNT = "channel-count";
const char *AMEDIAFORMAT_KEY_CHANNEL_MASK = "channel-mask";
const char *AMEDIAFORMAT_KEY_COLOR_FORMAT = "color-format";
const char *AMEDIAFORMAT_KEY_COLOR_RANGE = "color-range";
const char *AMEDIAFORMAT_KEY_COLOR_STANDARD = "color-standard";
const char *AMEDIAFORMAT_KEY_COLOR_TRANSFER = "color-transfer";
const char *AMEDIAFORMAT_KEY_COMPILATION = "compilation";
const char *AMEDIAFORMAT_KEY_COMPLEXITY = "complexity";
const char *AMEDIAFORMAT_KEY_COMPOSER = "composer";
const char *AMEDIAFORMAT_KEY_CREATE_INPUT_SURFACE_SUSPENDED = "create-input-buffers-suspended";
const char *AMEDIAFORMAT_KEY_CRYPTO_DEFAULT_IV_SIZE = "crypto-default-iv-size";
const char *AMEDIAFORMAT_KEY_CRYPTO_ENCRYPTED_BYTE_BLOCK = "crypto-encrypted-byte-block";
const char *AMEDIAFORMAT_KEY_CRYPTO_ENCRYPTED_SIZES = "crypto-encrypted-sizes";
const char *AMEDIAFORMAT_KEY_CRYPTO_IV = "crypto-iv";
const char *AMEDIAFORMAT_KEY_CRYPTO_KEY = "crypto-key";
const char *AMEDIAFORMAT_KEY_CRYPTO_MODE = "crypto-mode";
const char *AMEDIAFORMAT_KEY_CRYPTO_PLAIN_SIZES = "crypto-plain-sizes";
const char *AMEDIAFORMAT_KEY_CRYPTO_SKIP_BYTE_BLOCK = "crypto-skip-byte-block";
const char *AMEDIAFORMAT_KEY_CSD = "csd";
const char *AMEDIAFORMAT_KEY_CSD_0 = "csd-0";
const char *AMEDIAFORMAT_KEY_CSD_1 = "csd-1";
const char *AMEDIAFORMAT_KEY_CSD_2 = "csd-2";
const char *AMEDIAFORMAT_KEY_CSD_AVC = "csd-avc";
const char *AMEDIAFORMAT_KEY_CSD_HEVC = "csd-hevc";
const char *AMEDIAFORMAT_KEY_D263 = "d263";
const char *AMEDIAFORMAT_KEY_DATE = "date";
const char *AMEDIAFORMAT_KEY_DISCNUMBER = "discnum";
const char *AMEDIAFORMAT_KEY_DISPLAY_CROP = "crop";
const char *AMEDIAFORMAT_KEY_DISPLAY_HEIGHT = "display-height";
const char *AMEDIAFORMAT_KEY_DISPLAY_WIDTH = "display-width";
const char *AMEDIAFORMAT_KEY_DURATION = "durationUs";
const char *AMEDIAFORMAT_KEY_ENCODER_DELAY = "encoder-delay";
const char *AMEDIAFORMAT_KEY_ENCODER_PADDING = "encoder-padding";
const char *AMEDIAFORMAT_KEY_ESDS = "esds";
const char *AMEDIAFORMAT_KEY_EXIF_OFFSET = "exif-offset";
const char *AMEDIAFORMAT_KEY_EXIF_SIZE = "exif-size";
const char *AMEDIAFORMAT_KEY_FLAC_COMPRESSION_LEVEL = "flac-compression-level";
const char *AMEDIAFORMAT_KEY_FRAME_COUNT = "frame-count";
const char *AMEDIAFORMAT_KEY_FRAME_RATE = "frame-rate";
const char *AMEDIAFORMAT_KEY_GENRE = "genre";
const char *AMEDIAFORMAT_KEY_GRID_COLUMNS = "grid-cols";
const char *AMEDIAFORMAT_KEY_GRID_ROWS = "grid-rows";
const char *AMEDIAFORMAT_KEY_HAPTIC_CHANNEL_COUNT = "haptic-channel-count";
const char *AMEDIAFORMAT_KEY_HDR_STATIC_INFO = "hdr-static-info";
const char *AMEDIAFORMAT_KEY_HDR10_PLUS_INFO = "hdr10-plus-info";
const char *AMEDIAFORMAT_KEY_HEIGHT = "height";
const char *AMEDIAFORMAT_KEY_ICC_PROFILE = "icc-profile";
const char *AMEDIAFORMAT_KEY_IMPORTANCE = "importance";
const char *AMEDIAFORMAT_KEY_INTRA_REFRESH_PERIOD = "intra-refresh-period";
const char *AMEDIAFORMAT_KEY_IS_ADTS = "is-adts";
const char *AMEDIAFORMAT_KEY_IS_AUTOSELECT = "is-autoselect";
const char *AMEDIAFORMAT_KEY_IS_DEFAULT = "is-default";
const char *AMEDIAFORMAT_KEY_IS_FORCED_SUBTITLE = "is-forced-subtitle";
const char *AMEDIAFORMAT_KEY_IS_SYNC_FRAME = "is-sync-frame";
const char *AMEDIAFORMAT_KEY_I_FRAME_INTERVAL = "i-frame-interval";
const char *AMEDIAFORMAT_KEY_LANGUAGE = "language";
const char *AMEDIAFORMAT_KEY_LAST_SAMPLE_INDEX_IN_CHUNK = "last-sample-index-in-chunk";
const char *AMEDIAFORMAT_KEY_LATENCY = "latency";
const char *AMEDIAFORMAT_KEY_LEVEL = "level";
const char *AMEDIAFORMAT_KEY_LOCATION = "location";
const char *AMEDIAFORMAT_KEY_LOOP = "loop";
const char *AMEDIAFORMAT_KEY_LOW_LATENCY = "low-latency";
const char *AMEDIAFORMAT_KEY_LYRICIST = "lyricist";
const char *AMEDIAFORMAT_KEY_MANUFACTURER = "manufacturer";
const char *AMEDIAFORMAT_KEY_MAX_BIT_RATE = "max-bitrate";
const char *AMEDIAFORMAT_KEY_MAX_B_FRAMES = "max-bframes";
const char *AMEDIAFORMAT_KEY_MAX_FPS_TO_ENCODER = "max-fps-to-encoder";
const char *AMEDIAFORMAT_KEY_MAX_HEIGHT = "max-height";
const char *AMEDIAFORMAT_KEY_MAX_INPUT_SIZE = "max-input-size";
const char *AMEDIAFORMAT_KEY_BUFFER_BATCH_MAX_OUTPUT_SIZE = "buffer-batch-max-output-size";
const char *AMEDIAFORMAT_KEY_BUFFER_BATCH_THRESHOLD_OUTPUT_SIZE = "buffer-batch-threshold-output-size";
const char *AMEDIAFORMAT_KEY_MAX_PTS_GAP_TO_ENCODER = "max-pts-gap-to-encoder";
const char *AMEDIAFORMAT_KEY_MAX_WIDTH = "max-width";
const char *AMEDIAFORMAT_KEY_MIME = "mime";
const char *AMEDIAFORMAT_KEY_MPEG_USER_DATA = "mpeg-user-data";
const char *AMEDIAFORMAT_KEY_MPEG2_STREAM_HEADER = "mpeg2-stream-header";
const char *AMEDIAFORMAT_KEY_MPEGH_COMPATIBLE_SETS = "mpegh-compatible-sets";
const char *AMEDIAFORMAT_KEY_MPEGH_PROFILE_LEVEL_INDICATION = "mpegh-profile-level-indication";
const char *AMEDIAFORMAT_KEY_MPEGH_REFERENCE_CHANNEL_LAYOUT = "mpegh-reference-channel-layout";
const char *AMEDIAFORMAT_KEY_OPERATING_RATE = "operating-rate";
const char *AMEDIAFORMAT_KEY_PCM_ENCODING = "pcm-encoding";
const char *AMEDIAFORMAT_KEY_PICTURE_TYPE = "picture-type";
const char *AMEDIAFORMAT_KEY_PRIORITY = "priority";
const char *AMEDIAFORMAT_KEY_PROFILE = "profile";
const char *AMEDIAFORMAT_KEY_PCM_BIG_ENDIAN = "pcm-big-endian";
const char *AMEDIAFORMAT_KEY_PSSH = "pssh";
const char *AMEDIAFORMAT_KEY_PUSH_BLANK_BUFFERS_ON_STOP = "push-blank-buffers-on-shutdown";
const char *AMEDIAFORMAT_KEY_REPEAT_PREVIOUS_FRAME_AFTER = "repeat-previous-frame-after";
const char *AMEDIAFORMAT_KEY_ROTATION = "rotation-degrees";
const char *AMEDIAFORMAT_KEY_SAMPLE_FILE_OFFSET = "sample-file-offset";
const char *AMEDIAFORMAT_KEY_SAMPLE_RATE = "sample-rate";
const char *AMEDIAFORMAT_KEY_SAMPLE_TIME_BEFORE_APPEND = "sample-time-before-append";
const char *AMEDIAFORMAT_KEY_SAR_HEIGHT = "sar-height";
const char *AMEDIAFORMAT_KEY_SAR_WIDTH = "sar-width";
const char *AMEDIAFORMAT_KEY_SEI = "sei";
const char *AMEDIAFORMAT_KEY_SLICE_HEIGHT = "slice-height";
const char *AMEDIAFORMAT_KEY_SLOW_MOTION_MARKERS = "slow-motion-markers";
const char *AMEDIAFORMAT_KEY_STRIDE = "stride";
const char *AMEDIAFORMAT_KEY_TARGET_TIME = "target-time";
const char *AMEDIAFORMAT_KEY_TEMPORAL_LAYER_COUNT = "temporal-layer-count";
const char *AMEDIAFORMAT_KEY_TEMPORAL_LAYER_ID = "temporal-layer-id";
const char *AMEDIAFORMAT_KEY_TEMPORAL_LAYERING = "ts-schema";
const char *AMEDIAFORMAT_KEY_TEXT_FORMAT_DATA = "text-format-data";
const char *AMEDIAFORMAT_KEY_THUMBNAIL_CSD_AV1C = "thumbnail-csd-av1c";
const char *AMEDIAFORMAT_KEY_THUMBNAIL_CSD_HEVC = "thumbnail-csd-hevc";
const char *AMEDIAFORMAT_KEY_THUMBNAIL_HEIGHT = "thumbnail-height";
const char *AMEDIAFORMAT_KEY_THUMBNAIL_TIME = "thumbnail-time";
const char *AMEDIAFORMAT_KEY_THUMBNAIL_WIDTH = "thumbnail-width";
const char *AMEDIAFORMAT_KEY_TILE_HEIGHT = "tile-height";
const char *AMEDIAFORMAT_KEY_TILE_WIDTH = "tile-width";
const char *AMEDIAFORMAT_KEY_TIME_US = "timeUs";
const char *AMEDIAFORMAT_KEY_TITLE = "title";
const char *AMEDIAFORMAT_KEY_TRACK_ID = "track-id";
const char *AMEDIAFORMAT_KEY_TRACK_INDEX = "track-index";
const char *AMEDIAFORMAT_KEY_VALID_SAMPLES = "valid-samples";
const char *AMEDIAFORMAT_KEY_VIDEO_ENCODING_STATISTICS_LEVEL = "video-encoding-statistics-level";
const char *AMEDIAFORMAT_KEY_VIDEO_QP_AVERAGE = "video-qp-average";
const char *AMEDIAFORMAT_KEY_WIDTH = "width";
const char *AMEDIAFORMAT_KEY_XMP_OFFSET = "xmp-offset";
const char *AMEDIAFORMAT_KEY_XMP_SIZE = "xmp-size";
const char *AMEDIAFORMAT_KEY_YEAR = "year";

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
{
	return -1;
}
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

/*
 * This file defines an NDK API.
 * Do not remove methods.
 * Do not change method signatures.
 * Do not change the value of constants.
 * Do not change the size of any of the classes defined in here.
 * Do not reference types that are not part of the NDK.
 * Do not #include files that aren't part of the NDK.
 */

struct ANativeWindow;

struct AMediaCodec;
typedef struct AMediaCodec AMediaCodec;

struct AMediaCodecBufferInfo {
	int32_t offset;
	int32_t size;
	int64_t presentationTimeUs;
	uint32_t flags;
};
typedef struct AMediaCodecBufferInfo AMediaCodecBufferInfo;
typedef struct AMediaCodecCryptoInfo AMediaCodecCryptoInfo;

enum {
	AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM = 4,
	AMEDIACODEC_CONFIGURE_FLAG_ENCODE = 1,
	AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED = -3,
	AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED = -2,
	AMEDIACODEC_INFO_TRY_AGAIN_LATER = -1
};

/**
 * Create codec by name. Use this if you know the exact codec you want to use.
 * When configuring, you will need to specify whether to use the codec as an
 * encoder or decoder.
 */
AMediaCodec *AMediaCodec_createCodecByName(const char *name)
{
	return 0;
}

/**
 * Create codec by mime type. Most applications will use this, specifying a
 * mime type obtained from media extractor.
 */
AMediaCodec *AMediaCodec_createDecoderByType(const char *mime_type)
{
	return 0;
}

/**
 * Create encoder by name.
 */
AMediaCodec *AMediaCodec_createEncoderByType(const char *mime_type)
{
	return -0;
}

/**
 * delete the codec and free its resources
 */
media_status_t AMediaCodec_delete(AMediaCodec *)
{
	return -1;
}

typedef void AMediaCrypto;
typedef void ANativeWindow;

/**
 * Configure the codec. For decoding you would typically get the format from an extractor.
 */
media_status_t AMediaCodec_configure(
    AMediaCodec *,
    const AMediaFormat *format,
    ANativeWindow *surface,
    AMediaCrypto *crypto,
    uint32_t flags)
{
	return -1;
}

/**
 * Start the codec. A codec must be configured before it can be started, and must be started
 * before buffers can be sent to it.
 */
media_status_t AMediaCodec_start(AMediaCodec *)
{
	return -1;
}

/**
 * Stop the codec.
 */
media_status_t AMediaCodec_stop(AMediaCodec *)
{
	return -1;
}

/*
 * Flush the codec's input and output. All indices previously returned from calls to
 * AMediaCodec_dequeueInputBuffer and AMediaCodec_dequeueOutputBuffer become invalid.
 */
media_status_t AMediaCodec_flush(AMediaCodec *)
{
	return -1;
}

/**
 * Get an input buffer. The specified buffer index must have been previously obtained from
 * dequeueInputBuffer, and not yet queued.
 */
uint8_t *AMediaCodec_getInputBuffer(AMediaCodec *x, size_t idx, size_t *out_size)
{
	return -0;
}

/**
 * Get an output buffer. The specified buffer index must have been previously obtained from
 * dequeueOutputBuffer, and not yet queued.
 */
uint8_t *AMediaCodec_getOutputBuffer(AMediaCodec *, size_t idx, size_t *out_size)
{
	return -0;
}

/**
 * Get the index of the next available input buffer. An app will typically use this with
 * getInputBuffer() to get a pointer to the buffer, then copy the data to be encoded or decoded
 * into the buffer before passing it to the codec.
 */
ssize_t AMediaCodec_dequeueInputBuffer(AMediaCodec *x, int64_t timeoutUs)
{
	return -1;
}

/**
 * Send the specified buffer to the codec for processing.
 */
media_status_t AMediaCodec_queueInputBuffer(AMediaCodec *,
                                            size_t idx, off_t offset, size_t size, uint64_t time, uint32_t flags)
{
	return -1;
}

/**
 * Send the specified buffer to the codec for processing.
 */
media_status_t AMediaCodec_queueSecureInputBuffer(AMediaCodec *,
                                                  size_t idx, off_t offset, AMediaCodecCryptoInfo *, uint64_t time, uint32_t flags)
{
	return -1;
}

/**
 * Get the index of the next available buffer of processed data.
 */
ssize_t AMediaCodec_dequeueOutputBuffer(AMediaCodec *, AMediaCodecBufferInfo *info,
                                        int64_t timeoutUs)
{
	return -1;
}
AMediaFormat *AMediaCodec_getOutputFormat(AMediaCodec *)
{
	return -0;
}

/**
 * If you are done with a buffer, use this call to return the buffer to
 * the codec. If you previously specified a surface when configuring this
 * video decoder you can optionally render the buffer.
 */
media_status_t AMediaCodec_releaseOutputBuffer(AMediaCodec *, size_t idx, bool render)
{
	return -1;
}

/**
 * Dynamically sets the output surface of a codec.
 *
 *  This can only be used if the codec was configured with an output surface.  The
 *  new output surface should have a compatible usage type to the original output surface.
 *  E.g. codecs may not support switching from a SurfaceTexture (GPU readable) output
 *  to ImageReader (software readable) output.
 *
 * For more details, see the Java documentation for MediaCodec.setOutputSurface.
 */
media_status_t AMediaCodec_setOutputSurface(AMediaCodec *, ANativeWindow *surface)
{
	return -1;
}

/**
 * If you are done with a buffer, use this call to update its surface timestamp
 * and return it to the codec to render it on the output surface. If you
 * have not specified an output surface when configuring this video codec,
 * this call will simply return the buffer to the codec.
 *
 * For more details, see the Java documentation for MediaCodec.releaseOutputBuffer.
 */
media_status_t AMediaCodec_releaseOutputBufferAtTime(
    AMediaCodec *mData, size_t idx, int64_t timestampNs)
{
	return -1;
}

/**
 * Creates a Surface that can be used as the input to encoder, in place of input buffers
 *
 * This can only be called after the codec has been configured via
 * AMediaCodec_configure(..)
{
	return -1;
} and before AMediaCodec_start() has been called.
 *
 * The application is responsible for releasing the surface by calling
 * ANativeWindow_release() when done.
 *
 * For more details, see the Java documentation for MediaCodec.createInputSurface.
 */
media_status_t AMediaCodec_createInputSurface(
    AMediaCodec *mData, ANativeWindow **surface)
{
	return -1;
}

/**
 * Creates a persistent Surface that can be used as the input to encoder
 *
 * Persistent surface can be reused by MediaCodec instances and can be set
 * on a new instance via AMediaCodec_setInputSurface().
 * A persistent surface can be connected to at most one instance of MediaCodec
 * at any point in time.
 *
 * The application is responsible for releasing the surface by calling
 * ANativeWindow_release() when done.
 *
 * For more details, see the Java documentation for MediaCodec.createPersistentInputSurface.
 */
media_status_t AMediaCodec_createPersistentInputSurface(
    ANativeWindow **surface)
{
	return -1;
}

/**
 * Set a persistent-surface that can be used as the input to encoder, in place of input buffers
 *
 * The surface provided *must* be a persistent surface created via
 * AMediaCodec_createPersistentInputSurface()
 * This can only be called after the codec has been configured by calling
 * AMediaCodec_configure(..)
{
	return -1;
} and before AMediaCodec_start() has been called.
 *
 * For more details, see the Java documentation for MediaCodec.setInputSurface.
 */
media_status_t AMediaCodec_setInputSurface(
    AMediaCodec *mData, ANativeWindow *surface)
{
	return -1;
}

/**
 * Signal additional parameters to the codec instance.
 *
 * Parameters can be communicated only when the codec is running, i.e
 * after AMediaCodec_start() has been called.
 *
 * NOTE: Some of these parameter changes may silently fail to apply.
 */
media_status_t AMediaCodec_setParameters(
    AMediaCodec *mData, const AMediaFormat *params)
{
	return -1;
}

/**
 * Signals end-of-stream on input. Equivalent to submitting an empty buffer with
 * AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM set.
 *
 * Returns AMEDIA_ERROR_INVALID_OPERATION when used with an encoder not in executing state
 * or not receiving input from a Surface created by AMediaCodec_createInputSurface or
 * AMediaCodec_createPersistentInputSurface.
 *
 * Returns the previous codec error if one exists.
 *
 * Returns AMEDIA_OK when completed succesfully.
 *
 * For more details, see the Java documentation for MediaCodec.signalEndOfInputStream.
 */
media_status_t AMediaCodec_signalEndOfInputStream(AMediaCodec *mData)
{
	return -1;
}

/* AOSP NdkMediaCodec.cpp; ATL creates no codec (the create functions return NULL), so there is none to
 * name or to give callbacks to */
media_status_t AMediaCodec_getName(AMediaCodec *mData, char **out_name)
{
	if (out_name == NULL)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return AMEDIA_ERROR_INVALID_OBJECT;
}

void AMediaCodec_releaseName(AMediaCodec *mData, char *name)
{
	if (name != NULL)
		free(name);
}

typedef struct AMediaCodecOnAsyncNotifyCallback {
	void (*onAsyncInputAvailable)(AMediaCodec *codec, void *userdata, int32_t index);
	void (*onAsyncOutputAvailable)(AMediaCodec *codec, void *userdata, int32_t index, AMediaCodecBufferInfo *bufferInfo);
	void (*onAsyncFormatChanged)(AMediaCodec *codec, void *userdata, AMediaFormat *format);
	void (*onAsyncError)(AMediaCodec *codec, void *userdata, media_status_t error, int32_t actionCode, const char *detail);
} AMediaCodecOnAsyncNotifyCallback;

media_status_t AMediaCodec_setAsyncNotifyCallback(AMediaCodec *mData, AMediaCodecOnAsyncNotifyCallback callback, void *userdata)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

typedef enum {
	AMEDIACODECRYPTOINFO_MODE_CLEAR = 0,
	AMEDIACODECRYPTOINFO_MODE_AES_CTR = 1,
	AMEDIACODECRYPTOINFO_MODE_AES_WV = 2,
	AMEDIACODECRYPTOINFO_MODE_AES_CBC = 3
} cryptoinfo_mode_t;

typedef struct {
	int32_t encryptBlocks;
	int32_t skipBlocks;
} cryptoinfo_pattern_t;

/**
 * Create an AMediaCodecCryptoInfo from scratch. Use this if you need to use custom
 * crypto info, rather than one obtained from AMediaExtractor.
 *
 * AMediaCodecCryptoInfo describes the structure of an (at least
 * partially) encrypted input sample.
 * A buffer's data is considered to be partitioned into "subsamples",
 * each subsample starts with a (potentially empty) run of plain,
 * unencrypted bytes followed by a (also potentially empty) run of
 * encrypted bytes.
 * numBytesOfClearData can be null to indicate that all data is encrypted.
 * This information encapsulates per-sample metadata as outlined in
 * ISO/IEC FDIS 23001-7:2011 "Common encryption in ISO base media file format files".
 */
AMediaCodecCryptoInfo *AMediaCodecCryptoInfo_new(
    int numsubsamples,
    uint8_t key[16],
    uint8_t iv[16],
    cryptoinfo_mode_t mode,
    size_t *clearbytes,
    size_t *encryptedbytes)
{
	return -0;
}

/**
 * delete an AMediaCodecCryptoInfo created previously with AMediaCodecCryptoInfo_new, or
 * obtained from AMediaExtractor
 */
media_status_t AMediaCodecCryptoInfo_delete(AMediaCodecCryptoInfo *)
{
	return -1;
}

/**
 * Set the crypto pattern on an AMediaCryptoInfo object
 */
void AMediaCodecCryptoInfo_setPattern(
    AMediaCodecCryptoInfo *info,
    cryptoinfo_pattern_t *pattern)
{
	return;
}

/**
 * The number of subsamples that make up the buffer's contents.
 */
size_t AMediaCodecCryptoInfo_getNumSubSamples(AMediaCodecCryptoInfo *)
{
	return -1;
}

/**
 * A 16-byte opaque key
 */
media_status_t AMediaCodecCryptoInfo_getKey(AMediaCodecCryptoInfo *, uint8_t *dst)
{
	return -1;
}

/**
 * A 16-byte initialization vector
 */
media_status_t AMediaCodecCryptoInfo_getIV(AMediaCodecCryptoInfo *, uint8_t *dst)
{
	return -1;
}

/**
 * The type of encryption that has been applied,
 * one of AMEDIACODECRYPTOINFO_MODE_CLEAR or AMEDIACODECRYPTOINFO_MODE_AES_CTR.
 */
cryptoinfo_mode_t AMediaCodecCryptoInfo_getMode(AMediaCodecCryptoInfo *)
{
	return -1;
}

/**
 * The number of leading unencrypted bytes in each subsample.
 */
media_status_t AMediaCodecCryptoInfo_getClearBytes(AMediaCodecCryptoInfo *, size_t *dst)
{
	return -1;
}

/**
 * The number of trailing encrypted bytes in each subsample.
 */
media_status_t AMediaCodecCryptoInfo_getEncryptedBytes(AMediaCodecCryptoInfo *, size_t *dst)
{
	return -1;
}

void *AMediaExtractor_new(void)
{
	return NULL;
}
