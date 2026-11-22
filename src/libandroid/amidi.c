/*
 * The native MIDI API (libamidi, amidi/AMidi.h). ATL's MidiManager has no devices, so there is no MidiDevice a
 * native AMidiDevice could be made from; with no device, the functions answer as AOSP's amidi.cpp does for a null
 * device or port.
 */

#include <jni.h>
#include <stdint.h>
#include <sys/types.h>

typedef int32_t media_status_t;
#define AMEDIA_ERROR_INVALID_PARAMETER (-10000 - 4)
#define AMIDI_DEVICE_PROTOCOL_UNKNOWN (-1)

typedef struct AMidiDevice AMidiDevice;
typedef struct AMidiInputPort AMidiInputPort;
typedef struct AMidiOutputPort AMidiOutputPort;

media_status_t AMidiDevice_fromJava(JNIEnv *env, jobject midiDeviceObj, AMidiDevice **outDevicePtrPtr)
{
	if (outDevicePtrPtr)
		*outDevicePtrPtr = NULL;
	return AMEDIA_ERROR_INVALID_PARAMETER;
}

media_status_t AMidiDevice_release(const AMidiDevice *midiDevice) { return AMEDIA_ERROR_INVALID_PARAMETER; }
int32_t AMidiDevice_getType(const AMidiDevice *device) { return AMEDIA_ERROR_INVALID_PARAMETER; }
ssize_t AMidiDevice_getNumInputPorts(const AMidiDevice *device) { return AMEDIA_ERROR_INVALID_PARAMETER; }
ssize_t AMidiDevice_getNumOutputPorts(const AMidiDevice *device) { return AMEDIA_ERROR_INVALID_PARAMETER; }
int32_t AMidiDevice_getDefaultProtocol(const AMidiDevice *device) { return AMIDI_DEVICE_PROTOCOL_UNKNOWN; }

media_status_t AMidiOutputPort_open(const AMidiDevice *device, int32_t portNumber, AMidiOutputPort **outOutputPortPtr)
{
	if (outOutputPortPtr)
		*outOutputPortPtr = NULL;
	return AMEDIA_ERROR_INVALID_PARAMETER;
}

void AMidiOutputPort_close(const AMidiOutputPort *outputPort) {}

ssize_t AMidiOutputPort_receive(const AMidiOutputPort *outputPort, int32_t *opcodePtr, uint8_t *buffer, size_t maxBytes,
                                size_t *numBytesReceivedPtr, int64_t *outTimestampPtr)
{
	return AMEDIA_ERROR_INVALID_PARAMETER;
}

media_status_t AMidiInputPort_open(const AMidiDevice *device, int32_t portNumber, AMidiInputPort **outInputPortPtr)
{
	if (outInputPortPtr)
		*outInputPortPtr = NULL;
	return AMEDIA_ERROR_INVALID_PARAMETER;
}

ssize_t AMidiInputPort_send(const AMidiInputPort *inputPort, const uint8_t *buffer, size_t numBytes) { return AMEDIA_ERROR_INVALID_PARAMETER; }
ssize_t AMidiInputPort_sendWithTimestamp(const AMidiInputPort *inputPort, const uint8_t *buffer, size_t numBytes, int64_t timestamp) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AMidiInputPort_sendFlush(const AMidiInputPort *inputPort) { return AMEDIA_ERROR_INVALID_PARAMETER; }
void AMidiInputPort_close(const AMidiInputPort *inputPort) {}
