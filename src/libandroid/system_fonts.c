/* NDK system font API (android/system_fonts.h, android/font.h), after AOSP
 * frameworks/base/native/android/system_fonts.cpp. AOSP reads the fonts of /system/etc/fonts.xml;
 * ATL lists the desktop's fonts with fontconfig. Only the functions apps have needed so far:
 * Firefox calls ASystemFontIterator_open() without a null check from API 29 on. */

#include <stdlib.h>
#include <string.h>

#include <fontconfig/fontconfig.h>

struct ASystemFontIterator {
	FcFontSet *fonts;
	int next;
};

struct AFont {
	char *path;
};

struct ASystemFontIterator *ASystemFontIterator_open(void)
{
	struct ASystemFontIterator *ite = calloc(1, sizeof(*ite));
	FcPattern *pattern = FcPatternCreate();
	FcObjectSet *objects = FcObjectSetBuild(FC_FILE, NULL);
	ite->fonts = FcFontList(NULL, pattern, objects);
	FcObjectSetDestroy(objects);
	FcPatternDestroy(pattern);
	return ite;
}

struct AFont *ASystemFontIterator_next(struct ASystemFontIterator *ite)
{
	if (!ite || !ite->fonts)
		return NULL;
	while (ite->next < ite->fonts->nfont) {
		FcChar8 *file;
		if (FcPatternGetString(ite->fonts->fonts[ite->next++], FC_FILE, 0, &file) == FcResultMatch) {
			struct AFont *font = malloc(sizeof(*font));
			font->path = strdup((const char *)file);
			return font;
		}
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

const char *AFont_getFontFilePath(const struct AFont *font)
{
	return font->path;
}

void AFont_close(struct AFont *font)
{
	if (!font)
		return;
	free(font->path);
	free(font);
}
