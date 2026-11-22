/* NDK system font API (android/system_fonts.h, android/font.h, android/font_matcher.h), after AOSP
 * frameworks/base/native/android/system_fonts.cpp (android-16.0.0_r4). AOSP lists minikin's system font set
 * (fonts.xml); ATL lists the desktop's fonts with fontconfig, where ATL's Typeface finds its fonts too.
 * AFontMatcher_match() picks the family of android.graphics.Typeface's system font map (as minikin's
 * SystemFonts::findFontCollection()) and its fallback list from fontconfig (FcFontSort), and itemizes the
 * text like minikin's FontCollection::itemize() for the first run. */

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include <fontconfig/fontconfig.h>
#include <glib.h>

#include "ndk_jni.h"

#define AFONT_WEIGHT_NORMAL 400

struct AFont {
	char *path;
	char *locale;
	uint16_t weight;
	bool italic;
	uint32_t collection_index;
	uint32_t axis_count;
	uint32_t axis_tags[1];
	float axis_values[1];
};

struct ASystemFontIterator {
	FcFontSet *fonts;
	int next;
};

struct AFontMatcher {
	uint16_t weight;
	bool italic;
	char *lang;
	uint32_t family_variant;
};

/* the AFont of a fontconfig pattern; a named instance of a variable font gets its weight as wght axis value,
 * as fonts.xml gives <axis tag="wght" stylevalue="..."/> for the instances of Roboto */
static struct AFont *font_from_pattern(FcPattern *pattern)
{
	FcChar8 *file;
	int index = 0, weight = FC_WEIGHT_REGULAR, slant = FC_SLANT_ROMAN;
	FcBool variable = FcFalse;
	if (FcPatternGetString(pattern, FC_FILE, 0, &file) != FcResultMatch)
		return NULL;
	FcPatternGetInteger(pattern, FC_INDEX, 0, &index);
	FcPatternGetInteger(pattern, FC_WEIGHT, 0, &weight);
	FcPatternGetInteger(pattern, FC_SLANT, 0, &slant);
	FcPatternGetBool(pattern, FC_VARIABLE, 0, &variable);
	struct AFont *font = calloc(1, sizeof(*font));
	font->path = strdup((const char *)file);
	font->weight = FcWeightToOpenType(weight);
	font->italic = slant != FC_SLANT_ROMAN;
	font->collection_index = index & 0xffff; /* the upper bits number the named instance */
	if ((index >> 16) || variable) {
		font->axis_count = 1;
		font->axis_tags[0] = 'w' << 24 | 'g' << 16 | 'h' << 8 | 't';
		font->axis_values[0] = font->weight;
	}
	return font;
}

struct ASystemFontIterator *ASystemFontIterator_open(void)
{
	struct ASystemFontIterator *ite = calloc(1, sizeof(*ite));
	FcPattern *pattern = FcPatternCreate();
	FcObjectSet *objects = FcObjectSetBuild(FC_FILE, FC_INDEX, FC_WEIGHT, FC_SLANT, FC_VARIABLE, NULL);
	ite->fonts = FcFontList(NULL, pattern, objects);
	FcObjectSetDestroy(objects);
	FcPatternDestroy(pattern);
	return ite;
}

struct AFont *ASystemFontIterator_next(struct ASystemFontIterator *ite)
{
	if (!ite->fonts)
		return NULL;
	while (ite->next < ite->fonts->nfont) {
		FcPattern *pattern = ite->fonts->fonts[ite->next++];
		FcBool variable = FcFalse;
		int weight;
		/* the pattern of a whole variable font has a weight range instead of a weight; its named
		 * instances are listed on their own */
		FcPatternGetBool(pattern, FC_VARIABLE, 0, &variable);
		if (variable || FcPatternGetInteger(pattern, FC_WEIGHT, 0, &weight) != FcResultMatch)
			continue;
		struct AFont *font = font_from_pattern(pattern);
		if (font)
			return font;
	}
	return NULL;
}

void ASystemFontIterator_close(struct ASystemFontIterator *ite)
{
	if (!ite)
		return;
	if (ite->fonts)
		FcFontSetDestroy(ite->fonts);
	free(ite);
}

void AFont_close(struct AFont *font)
{
	if (!font)
		return;
	free(font->path);
	free(font->locale);
	free(font);
}

const char *AFont_getFontFilePath(const struct AFont *font)
{
	return font->path;
}

uint16_t AFont_getWeight(const struct AFont *font)
{
	return font->weight;
}

bool AFont_isItalic(const struct AFont *font)
{
	return font->italic;
}

/* AOSP: the lang of the font's family in fonts.xml, NULL for a family without one; fontconfig has no such
 * family language (only the languages a font covers), so it is NULL as for the families of the Latin fonts */
const char *AFont_getLocale(const struct AFont *font)
{
	return font->locale;
}

size_t AFont_getCollectionIndex(const struct AFont *font)
{
	return font->collection_index;
}

size_t AFont_getAxisCount(const struct AFont *font)
{
	return font->axis_count;
}

uint32_t AFont_getAxisTag(const struct AFont *font, uint32_t axisIndex)
{
	if (axisIndex >= font->axis_count)
		g_error("AFont_getAxisTag: given axis index is out of bounds. (< %u", font->axis_count);
	return font->axis_tags[axisIndex];
}

float AFont_getAxisValue(const struct AFont *font, uint32_t axisIndex)
{
	if (axisIndex >= font->axis_count)
		g_error("AFont_getAxisValue: given axis index is out of bounds. (< %u", font->axis_count);
	return font->axis_values[axisIndex];
}

struct AFontMatcher *AFontMatcher_create(void)
{
	struct AFontMatcher *matcher = calloc(1, sizeof(*matcher));
	matcher->weight = AFONT_WEIGHT_NORMAL;
	return matcher;
}

void AFontMatcher_destroy(struct AFontMatcher *matcher)
{
	free(matcher->lang);
	free(matcher);
}

void AFontMatcher_setStyle(struct AFontMatcher *matcher, uint16_t weight, bool italic)
{
	matcher->weight = weight;
	matcher->italic = italic;
}

/* languageTags: a comma separated BCP 47 list; fontconfig prefers the fonts of one language, the first */
void AFontMatcher_setLocales(struct AFontMatcher *matcher, const char *languageTags)
{
	free(matcher->lang);
	char *first = g_strndup(languageTags, strcspn(languageTags, ","));
	FcChar8 *lang = first[0] ? FcLangNormalize((const FcChar8 *)first) : NULL;
	matcher->lang = lang ? strdup((const char *)lang) : NULL;
	FcStrFree(lang);
	g_free(first);
}

/* fontconfig has no compact/elegant variants of a family (fonts.xml variant=); stored like AOSP */
void AFontMatcher_setFamilyVariant(struct AFontMatcher *matcher, uint32_t familyVariant)
{
	matcher->family_variant = familyVariant;
}

/* the Pango family list ATL's Typeface draws a system family with: Typeface.getSystemDefaultTypeface()
 * (the default typeface for an unknown name, as findFontCollection()); NULL is the default font */
static char *typeface_family(const char *family_name)
{
	JNIEnv *env = ndk_jni_env();
	if (!env)
		return NULL;
	jclass typeface = ndk_jni_class(env, "android/graphics/Typeface");
	jmethodID get = (*env)->GetStaticMethodID(env, typeface, "getSystemDefaultTypeface", "(Ljava/lang/String;)Landroid/graphics/Typeface;");
	jfieldID family_field = (*env)->GetFieldID(env, typeface, "family", "Ljava/lang/String;");
	jstring name = (*env)->NewStringUTF(env, family_name);
	jobject tf = (*env)->CallStaticObjectMethod(env, typeface, get, name);
	char *ret = NULL;
	if (!ndk_jni_threw(env, "Typeface.getSystemDefaultTypeface") && tf) {
		jstring family = (*env)->GetObjectField(env, tf, family_field);
		if (family) {
			const char *chars = (*env)->GetStringUTFChars(env, family, NULL);
			ret = strdup(chars);
			(*env)->ReleaseStringUTFChars(env, family, chars);
			(*env)->DeleteLocalRef(env, family);
		}
		(*env)->DeleteLocalRef(env, tf);
	}
	(*env)->DeleteLocalRef(env, name);
	return ret;
}

/* minikin FontCollection.cpp doesNotNeedFontSupport() */
static bool does_not_need_font_support(uint32_t c)
{
	return c == 0x00AD || c == 0x034F || c == 0x061C || (0x200C <= c && c <= 0x200F) ||
	       (0x202A <= c && c <= 0x202E) || (0x2066 <= c && c <= 0x2069) || c == 0xFEFF ||
	       (0xFE00 <= c && c <= 0xFE0F) || (0xE0100 <= c && c <= 0xE01EF);
}

/* minikin FontCollection.cpp stickyAllowlist */
static bool is_sticky_allowlisted(uint32_t c)
{
	static const uint32_t sticky[] = {'!', ',', '-', '.', ':', ';', '?', 0x00A0, 0x2010, 0x2011, 0x202F, 0x2640, 0x2642, 0x2695};
	for (size_t i = 0; i < G_N_ELEMENTS(sticky); i++)
		if (sticky[i] == c)
			return true;
	return false;
}

/* minikin getFamilyForChar(): the first font of the fallback list that has the character, else the first one */
static int font_for_char(FcFontSet *set, FcCharSet **charsets, uint32_t c)
{
	for (int i = 0; i < set->nfont; i++)
		if (charsets[i] && FcCharSetHasChar(charsets[i], c))
			return i;
	return 0;
}

static uint32_t next_char(const uint16_t *text, uint32_t length, uint32_t *pos)
{
	uint32_t c = text[(*pos)++];
	if (c >= 0xD800 && c <= 0xDBFF && *pos < length && text[*pos] >= 0xDC00 && text[*pos] <= 0xDFFF)
		return 0x10000 + ((c - 0xD800) << 10) + (text[(*pos)++] - 0xDC00);
	return (c >= 0xD800 && c <= 0xDFFF) ? 0xFFFD : c; /* a lone surrogate: REPLACEMENT CHARACTER */
}

struct AFont *AFontMatcher_match(const struct AFontMatcher *matcher, const char *familyName, const uint16_t *text,
                                 const uint32_t textLength, uint32_t *runLength)
{
	FcPattern *pattern = FcPatternCreate();
	char *family = typeface_family(familyName);
	/* the default font: Roboto, see add_android_fonts() and the gtk-font-name of main.c */
	char **names = g_strsplit(family ? family : "Roboto,sans-serif", ",", -1);
	for (char **n = names; *n; n++)
		FcPatternAddString(pattern, FC_FAMILY, (const FcChar8 *)*n);
	g_strfreev(names);
	free(family);
	FcPatternAddInteger(pattern, FC_WEIGHT, FcWeightFromOpenType(matcher->weight));
	FcPatternAddInteger(pattern, FC_SLANT, matcher->italic ? FC_SLANT_ITALIC : FC_SLANT_ROMAN);
	if (matcher->lang)
		FcPatternAddString(pattern, FC_LANG, (const FcChar8 *)matcher->lang);
	FcConfigSubstitute(NULL, pattern, FcMatchPattern);
	FcDefaultSubstitute(pattern);
	FcResult result;
	FcCharSet *coverage = NULL;
	FcFontSet *set = FcFontSort(NULL, pattern, FcTrue, &coverage, &result);
	if (coverage)
		FcCharSetDestroy(coverage);

	struct AFont *font = NULL;
	uint32_t end = 0;
	if (set && set->nfont > 0) {
		FcCharSet **charsets = calloc(set->nfont, sizeof(*charsets));
		for (int i = 0; i < set->nfont; i++)
			FcPatternGetCharSet(set->fonts[i], FC_CHARSET, 0, &charsets[i]);
		/* FontCollection::itemize() with runMax 1: the run ends where another font would be chosen */
		int run_font = -1;
		uint32_t pos = 0;
		while (pos < textLength) {
			uint32_t c = next_char(text, textLength, &pos);
			if (does_not_need_font_support(c))
				goto cont;
			if (run_font >= 0 && (is_sticky_allowlisted(c) || g_unichar_ismark(c)) && charsets[run_font] &&
			    FcCharSetHasChar(charsets[run_font], c))
				goto cont;
			int f = font_for_char(set, charsets, c);
			if (run_font >= 0 && f != run_font)
				break;
			run_font = f;
		cont:
			end = pos;
		}
		if (run_font < 0)
			run_font = 0;
		FcPattern *prepared = FcFontRenderPrepare(NULL, pattern, set->fonts[run_font]);
		font = font_from_pattern(prepared ? prepared : set->fonts[run_font]);
		if (prepared)
			FcPatternDestroy(prepared);
		free(charsets);
	}
	if (set)
		FcFontSetDestroy(set);
	FcPatternDestroy(pattern);
	if (!font) /* no font at all on this computer: AOSP always finds one */
		g_error("AFontMatcher_match: fontconfig knows no font");
	if (runLength)
		*runLength = end;
	return font;
}
