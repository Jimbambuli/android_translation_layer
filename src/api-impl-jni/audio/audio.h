#include <alsa/asoundlib.h>

#define PCM_DEVICE "default"

void helper_hw_params_init(snd_pcm_t *pcm_handle, snd_pcm_hw_params_t *params, unsigned int rate, int channel_config, snd_pcm_format_t format, unsigned int *channels);

/* In a Flatpak the microphone is PulseAudio's (--socket=pulseaudio); outside only with ATL_UGLY_ENABLE_MICROPHONE */
int microphone_allowed(void);
