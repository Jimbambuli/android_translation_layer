/* apk_abi_test APKS ABI...: prints the index apk_find_supported_abi() gives */
#include <stdio.h>

#include "../src/main-executable/apk_abi.h"

int main(int argc, char **argv)
{
	if (argc < 2)
		return 2;
	printf("%d\n", apk_find_supported_abi(argv[1], (const char *const *)argv + 2, argc - 2));
	return 0;
}
