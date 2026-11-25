#ifndef VIDEO_ENCODER_H
#define VIDEO_ENCODER_H

#include <libavcodec/avcodec.h>
#include <stdbool.h>

struct AHardwareBuffer;

/* a video encoder of FFmpeg (video_encoder.c) for MediaCodec and MediaRecorder: frames from the CPU in any
 * layout the producers of ATL make, timestamps in microseconds */
struct atl_venc;

/* the FFmpeg encoder ATL uses for a type: the first that opens, a VA-API one if hardware, else a software one;
 * NULL if there is none */
const char *atl_venc_find(const char *mime, bool hardware);
struct atl_venc *atl_venc_open(const char *encoder, int width, int height, int frame_rate, int bit_rate, int bitrate_mode,
                               float i_frame_interval, bool global_header);
AVCodecContext *atl_venc_context(struct atl_venc *venc);
/* a frame in format (data/linesize as AVFrame's); NULL data drains the encoder. Frames that do not go forward in time
 * are dropped (AOSP GraphicBufferSource) */
int atl_venc_send(struct atl_venc *venc, enum AVPixelFormat format, uint8_t *const data[4], const int linesize[4], int width, int height, int64_t pts_us);
/* an AHardwareBuffer that the CPU can read (an ImageReader image of a GPU or decoder producer) */
int atl_venc_send_buffer(struct atl_venc *venc, struct AHardwareBuffer *buffer, int64_t pts_us);
/* the layout of an Image's planes (ImageReader.queueImage(): camera, Surface.lockCanvas()); AV_PIX_FMT_NONE if unknown */
enum AVPixelFormat atl_venc_planes_format(int n_planes, uint8_t *const data[], const int pixel_stride[]);
void atl_venc_request_sync_frame(struct atl_venc *venc);
void atl_venc_free(struct atl_venc *venc);

#endif
