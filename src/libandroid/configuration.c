/* NDK AConfiguration (android/configuration.h) after AOSP frameworks/base/native/android/configuration.cpp
 * (android-16.0.0_r4): an AConfiguration is a ResTable_config, the configuration of the AssetManager comes from
 * AssetManager2::GetConfigurations()[0], diff/match/isBetterThan are ResTable_config's. */

#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include <androidfw/androidfw_c_api.h>

typedef struct ResTable_config AConfiguration;

/* frameworks/base/libs/androidfw/include/androidfw/ResourceTypes.h */
#define MASK_KEYSHIDDEN 0x0003
#define MASK_NAVHIDDEN 0x000c
#define SHIFT_NAVHIDDEN 2
#define MASK_SCREENSIZE 0x0f
#define MASK_SCREENLONG 0x30
#define SHIFT_SCREENLONG 4
#define MASK_LAYOUTDIR 0xC0
#define SHIFT_LAYOUTDIR 6
#define MASK_UI_MODE_TYPE 0x0f
#define MASK_UI_MODE_NIGHT 0x30
#define SHIFT_UI_MODE_NIGHT 4
#define MASK_SCREENROUND 0x03
#define GRAMMATICAL_INFLECTION_GENDER_MASK 0b11
/* the C struct calls ResTable_config::grammaticalInflection (the 4th byte of input) inputPad0 */
#define grammaticalInflection inputPad0

AConfiguration *AConfiguration_new(void)
{
	return calloc(1, sizeof(AConfiguration));
}

void AConfiguration_delete(AConfiguration *config)
{
	free(config);
}

void AConfiguration_fromAssetManager(AConfiguration *out, struct AssetManager *am)
{
	AssetManager_getConfiguration(am, out);
}

void AConfiguration_copy(AConfiguration *dest, AConfiguration *src)
{
	*dest = *src;
}

int32_t AConfiguration_getMcc(AConfiguration *config) { return config->mcc; }
int32_t AConfiguration_getMnc(AConfiguration *config) { return config->mnc; }

void AConfiguration_getLanguage(AConfiguration *config, char *outLanguage)
{
	outLanguage[0] = config->language[0];
	outLanguage[1] = config->language[1];
}

void AConfiguration_getCountry(AConfiguration *config, char *outCountry)
{
	outCountry[0] = config->country[0];
	outCountry[1] = config->country[1];
}

int32_t AConfiguration_getOrientation(AConfiguration *config) { return config->orientation; }
int32_t AConfiguration_getTouchscreen(AConfiguration *config) { return config->touchscreen; }
int32_t AConfiguration_getDensity(AConfiguration *config) { return config->density; }
int32_t AConfiguration_getKeyboard(AConfiguration *config) { return config->keyboard; }
int32_t AConfiguration_getNavigation(AConfiguration *config) { return config->navigation; }
int32_t AConfiguration_getKeysHidden(AConfiguration *config) { return config->inputFlags & MASK_KEYSHIDDEN; }
int32_t AConfiguration_getNavHidden(AConfiguration *config) { return (config->inputFlags & MASK_NAVHIDDEN) >> SHIFT_NAVHIDDEN; }
int32_t AConfiguration_getSdkVersion(AConfiguration *config) { return config->sdkVersion; }
int32_t AConfiguration_getScreenSize(AConfiguration *config) { return config->screenLayout & MASK_SCREENSIZE; }
int32_t AConfiguration_getScreenLong(AConfiguration *config) { return (config->screenLayout & MASK_SCREENLONG) >> SHIFT_SCREENLONG; }
int32_t AConfiguration_getScreenRound(AConfiguration *config) { return config->screenLayout2 & MASK_SCREENROUND; }
int32_t AConfiguration_getUiModeType(AConfiguration *config) { return config->uiMode & MASK_UI_MODE_TYPE; }
int32_t AConfiguration_getUiModeNight(AConfiguration *config) { return (config->uiMode & MASK_UI_MODE_NIGHT) >> SHIFT_UI_MODE_NIGHT; }
int32_t AConfiguration_getScreenWidthDp(AConfiguration *config) { return config->screenWidthDp; }
int32_t AConfiguration_getScreenHeightDp(AConfiguration *config) { return config->screenHeightDp; }
int32_t AConfiguration_getSmallestScreenWidthDp(AConfiguration *config) { return config->smallestScreenWidthDp; }
int32_t AConfiguration_getLayoutDirection(AConfiguration *config) { return (config->screenLayout & MASK_LAYOUTDIR) >> SHIFT_LAYOUTDIR; }
int32_t AConfiguration_getGrammaticalGender(AConfiguration *config) { return config->grammaticalInflection; }

void AConfiguration_setMcc(AConfiguration *config, int32_t mcc) { config->mcc = mcc; }
void AConfiguration_setMnc(AConfiguration *config, int32_t mnc) { config->mnc = mnc; }

void AConfiguration_setLanguage(AConfiguration *config, const char *language)
{
	config->language[0] = language[0];
	config->language[1] = language[1];
}

void AConfiguration_setCountry(AConfiguration *config, const char *country)
{
	config->country[0] = country[0];
	config->country[1] = country[1];
}

void AConfiguration_setOrientation(AConfiguration *config, int32_t orientation) { config->orientation = orientation; }
void AConfiguration_setTouchscreen(AConfiguration *config, int32_t touchscreen) { config->touchscreen = touchscreen; }
void AConfiguration_setDensity(AConfiguration *config, int32_t density) { config->density = density; }
void AConfiguration_setKeyboard(AConfiguration *config, int32_t keyboard) { config->keyboard = keyboard; }
void AConfiguration_setNavigation(AConfiguration *config, int32_t navigation) { config->navigation = navigation; }

void AConfiguration_setKeysHidden(AConfiguration *config, int32_t keysHidden)
{
	config->inputFlags = (config->inputFlags & ~MASK_KEYSHIDDEN) | (keysHidden & MASK_KEYSHIDDEN);
}

void AConfiguration_setNavHidden(AConfiguration *config, int32_t navHidden)
{
	config->inputFlags = (config->inputFlags & ~MASK_NAVHIDDEN) | ((navHidden << SHIFT_NAVHIDDEN) & MASK_NAVHIDDEN);
}

void AConfiguration_setSdkVersion(AConfiguration *config, int32_t sdkVersion) { config->sdkVersion = sdkVersion; }

void AConfiguration_setScreenSize(AConfiguration *config, int32_t screenSize)
{
	config->screenLayout = (config->screenLayout & ~MASK_SCREENSIZE) | (screenSize & MASK_SCREENSIZE);
}

void AConfiguration_setScreenLong(AConfiguration *config, int32_t screenLong)
{
	config->screenLayout = (config->screenLayout & ~MASK_SCREENLONG) | ((screenLong << SHIFT_SCREENLONG) & MASK_SCREENLONG);
}

void AConfiguration_setScreenRound(AConfiguration *config, int32_t screenRound)
{
	config->screenLayout2 = (config->screenLayout2 & ~MASK_SCREENROUND) | (screenRound & MASK_SCREENROUND);
}

void AConfiguration_setUiModeType(AConfiguration *config, int32_t uiModeType)
{
	config->uiMode = (config->uiMode & ~MASK_UI_MODE_TYPE) | (uiModeType & MASK_UI_MODE_TYPE);
}

void AConfiguration_setUiModeNight(AConfiguration *config, int32_t uiModeNight)
{
	config->uiMode = (config->uiMode & ~MASK_UI_MODE_NIGHT) | ((uiModeNight << SHIFT_UI_MODE_NIGHT) & MASK_UI_MODE_NIGHT);
}

void AConfiguration_setScreenWidthDp(AConfiguration *config, int32_t value) { config->screenWidthDp = value; }
void AConfiguration_setScreenHeightDp(AConfiguration *config, int32_t value) { config->screenHeightDp = value; }
void AConfiguration_setSmallestScreenWidthDp(AConfiguration *config, int32_t value) { config->smallestScreenWidthDp = value; }

void AConfiguration_setLayoutDirection(AConfiguration *config, int32_t value)
{
	config->screenLayout = (config->screenLayout & ~MASK_LAYOUTDIR) | ((value << SHIFT_LAYOUTDIR) & MASK_LAYOUTDIR);
}

void AConfiguration_setGrammaticalGender(AConfiguration *config, int32_t value)
{
	config->grammaticalInflection = value & GRAMMATICAL_INFLECTION_GENDER_MASK;
}

int32_t AConfiguration_diff(AConfiguration *config1, AConfiguration *config2)
{
	return ResTable_config_diff(config1, config2);
}

int32_t AConfiguration_match(AConfiguration *base, AConfiguration *requested)
{
	return ResTable_config_match(base, requested);
}

int32_t AConfiguration_isBetterThan(AConfiguration *base, AConfiguration *test, AConfiguration *requested)
{
	return ResTable_config_isBetterThan(base, test, requested);
}
