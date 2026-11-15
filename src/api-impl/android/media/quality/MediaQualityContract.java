package android.media.quality;

/* AOSP frameworks/base/media/java/android/media/quality/MediaQualityContract.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public class MediaQualityContract {
    public static final String LEVEL_HIGH = "level_high";
    public static final String LEVEL_LOW = "level_low";
    public static final String LEVEL_MEDIUM = "level_medium";
    public static final String LEVEL_OFF = "level_off";

    public static final class PictureQuality {
        public static final String PARAMETER_AUTO_PICTURE_QUALITY_ENABLED = "auto_picture_quality_enabled";
        public static final String PARAMETER_AUTO_SUPER_RESOLUTION_ENABLED = "auto_super_resolution_enabled";
        public static final String PARAMETER_BLUE_STRETCH = "blue_stretch";
        public static final String PARAMETER_BRIGHTNESS = "brightness";
        public static final String PARAMETER_COLOR_TEMPERATURE = "color_temperature";
        public static final String PARAMETER_COLOR_TUNE = "color_tune";
        public static final String PARAMETER_COLOR_TUNER_BLUE_GAIN = "color_tuner_blue_gain";
        public static final String PARAMETER_COLOR_TUNER_BLUE_OFFSET = "color_tuner_blue_offset";
        public static final String PARAMETER_COLOR_TUNER_BRIGHTNESS = "color_tuner_brightness";
        public static final String PARAMETER_COLOR_TUNER_GREEN_GAIN = "color_tuner_green_gain";
        public static final String PARAMETER_COLOR_TUNER_GREEN_OFFSET = "color_tuner_green_offset";
        public static final String PARAMETER_COLOR_TUNER_HUE = "color_tuner_hue";
        public static final String PARAMETER_COLOR_TUNER_RED_GAIN = "color_tuner_red_gain";
        public static final String PARAMETER_COLOR_TUNER_RED_OFFSET = "color_tuner_red_offset";
        public static final String PARAMETER_COLOR_TUNER_SATURATION = "color_tuner_saturation";
        public static final String PARAMETER_CONTRAST = "contrast";
        public static final String PARAMETER_DECONTOUR = "decontour";
        public static final String PARAMETER_DYNAMIC_LUMA_CONTROL = "dynamic_luma_control";
        public static final String PARAMETER_FILM_MODE = "film_mode";
        public static final String PARAMETER_FLESH_TONE = "flesh_tone";
        public static final String PARAMETER_GLOBAL_DIMMING = "global_dimming";
        public static final String PARAMETER_HUE = "hue";
        public static final String PARAMETER_MPEG_NOISE_REDUCTION = "mpeg_noise_reduction";
        public static final String PARAMETER_NOISE_REDUCTION = "noise_reduction";
        public static final String PARAMETER_SATURATION = "saturation";
        public static final String PARAMETER_SHARPNESS = "sharpness";
    }

    public static final class SoundQuality {
        public static final String PARAMETER_AUTO_VOLUME_CONTROL = "auto_volume_control";
        public static final String PARAMETER_BALANCE = "balance";
        public static final String PARAMETER_BASS = "bass";
        public static final String PARAMETER_DIALOGUE_ENHANCER = "dialogue_enhancer";
        public static final String PARAMETER_DIGITAL_OUTPUT_DELAY_MILLIS = "digital_output_delay_millis";
        public static final String PARAMETER_DIGITAL_OUTPUT_MODE = "digital_output_mode";
        public static final String PARAMETER_DOLBY_AUDIO_PROCESSING_DOLBY_ATMOS = "dolby_audio_processing_dolby_atmos";
        public static final String PARAMETER_DOLBY_AUDIO_PROCESSING_SOUND_MODE = "dolby_audio_processing_sound_mode";
        public static final String PARAMETER_DOLBY_AUDIO_PROCESSING_SURROUND_VIRTUALIZER = "dolby_audio_processing_surround_virtualizer";
        public static final String PARAMETER_DOLBY_AUDIO_PROCESSING_VOLUME_LEVELER = "dolby_audio_processing_volume_leveler";
        public static final String PARAMETER_DOWN_MIX_MODE = "down_mix_mode";
        public static final String PARAMETER_DTS_DRC = "dts_drc";
        public static final String PARAMETER_DTS_VIRTUAL_X_DEFINITION = "dts_virtual_x_definition";
        public static final String PARAMETER_DTS_VIRTUAL_X_DIALOG_CLARITY = "dts_virtual_x_dialog_clarity";
        public static final String PARAMETER_DTS_VIRTUAL_X_HEIGHT = "dts_virtual_x_height";
        public static final String PARAMETER_DTS_VIRTUAL_X_LIMITER = "dts_virtual_x_limiter";
        public static final String PARAMETER_DTS_VIRTUAL_X_TBHDX = "dts_virtual_x_tbhdx";
        public static final String PARAMETER_DTS_VIRTUAL_X_TRU_SURROUND_X = "dts_virtual_x_tru_surround_x";
        public static final String PARAMETER_DTS_VIRTUAL_X_TRU_VOLUME_HD = "dts_virtual_x_tru_volume_hd";
        public static final String PARAMETER_EARC = "earc";
        public static final String PARAMETER_SPEAKERS = "speakers";
        public static final String PARAMETER_SPEAKERS_DELAY_MILLIS = "speakers_delay_millis";
        public static final String PARAMETER_SURROUND_SOUND = "surround_sound";
        public static final String PARAMETER_TREBLE = "treble";
    }
}
