/* PackageManager's choice of an app's ABI (frameworks/base/core/jni/com_android_internal_content_NativeLibraryHelper.cpp
 * findSupportedAbi, androidfw ApkParsing.cpp ValidLibraryPathLastSlash), on the APK's zip central directory, so it
 * is known before the runtime starts (whether the native bridge is needed at all). */

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "apk_abi.h"

static uint16_t le16(const uint8_t *p) { return p[0] | p[1] << 8; }
static uint32_t le32(const uint8_t *p) { return le16(p) | (uint32_t)le16(p + 2) << 16; }
static uint64_t le64(const uint8_t *p) { return le32(p) | (uint64_t)le32(p + 4) << 32; }

/* lib/<abi>/<file>: "lib/", one directory, a file name that is not empty or a path step (ApkParsing.cpp) */
static int library_abi(const char *name, size_t len, const char **abi, size_t *abi_len)
{
	static const size_t min_length = 4 + 2 + 3 + 1 + 3; /* APK_LIB, "x/", "lib", "x", ".so" */
	if (len < min_length || strncmp(name, "lib/", 4))
		return 0;
	const char *slash = memchr(name + 4, '/', len - 4);
	if (!slash || memchr(slash + 1, '/', name + len - slash - 1))
		return 0;
	size_t file_len = name + len - slash - 1;
	if (!file_len || (file_len == 1 && slash[1] == '.') || (file_len == 2 && !strncmp(slash + 1, "..", 2)))
		return 0;
	*abi = name + 4;
	*abi_len = slash - *abi;
	return 1;
}

/* the best (lowest) index among `abis` that this APK has libraries for, or `best` if none is better */
static int scan_apk(const char *path, const char *const *abis, int n_abis, int best)
{
	FILE *f = fopen(path, "rb");
	if (!f)
		return -2;
	uint8_t *buf = NULL;
	int ret = -2;
	if (fseeko(f, 0, SEEK_END))
		goto out;
	off_t size = ftello(f);
	size_t tail = size < 65536 + 22 ? (size_t)size : 65536 + 22;
	buf = malloc(tail);
	if (!buf || fseeko(f, size - tail, SEEK_SET) || fread(buf, 1, tail, f) != tail)
		goto out;
	/* end of central directory record, searched backwards (a comment may follow it) */
	ssize_t eocd = -1;
	for (ssize_t i = (ssize_t)tail - 22; i >= 0; i--) {
		if (le32(buf + i) == 0x06054b50) {
			eocd = i;
			break;
		}
	}
	if (eocd < 0)
		goto out;
	uint64_t cd_size = le32(buf + eocd + 12), cd_offset = le32(buf + eocd + 16);
	if ((cd_offset == 0xffffffff || cd_size == 0xffffffff) && eocd >= 20 && le32(buf + eocd - 20) == 0x07064b50) {
		uint8_t z64[56];
		if (fseeko(f, (off_t)le64(buf + eocd - 20 + 8), SEEK_SET) || fread(z64, 1, sizeof(z64), f) != sizeof(z64) || le32(z64) != 0x06064b50)
			goto out;
		cd_size = le64(z64 + 40);
		cd_offset = le64(z64 + 48);
	}
	if (cd_offset + cd_size > (uint64_t)size)
		goto out;
	free(buf);
	buf = malloc(cd_size ? cd_size : 1);
	if (!buf || fseeko(f, (off_t)cd_offset, SEEK_SET) || fread(buf, 1, cd_size, f) != cd_size)
		goto out;
	ret = best;
	for (uint64_t p = 0; p + 46 <= cd_size && le32(buf + p) == 0x02014b50;) {
		uint16_t name_len = le16(buf + p + 28), extra_len = le16(buf + p + 30), comment_len = le16(buf + p + 32);
		if (p + 46 + name_len > cd_size)
			break;
		const char *abi;
		size_t abi_len;
		if (library_abi((const char *)buf + p + 46, name_len, &abi, &abi_len)) {
			for (int i = 0; i < n_abis && (ret < 0 || i < ret); i++) {
				if (strlen(abis[i]) == abi_len && !strncmp(abi, abis[i], abi_len))
					ret = i;
			}
		}
		p += 46 + name_len + extra_len + comment_len;
	}
out:
	free(buf);
	fclose(f);
	return ret;
}

int apk_find_supported_abi(const char *apk_paths, const char *const *abis, int n_abis)
{
	int best = -1;
	char *paths = strdup(apk_paths), *save = NULL;
	for (char *path = strtok_r(paths, ":", &save); path; path = strtok_r(NULL, ":", &save)) {
		int r = scan_apk(path, abis, n_abis, best);
		if (r == -2) {
			best = -2;
			break;
		}
		best = r;
	}
	free(paths);
	return best;
}
