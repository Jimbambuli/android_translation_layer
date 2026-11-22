#ifndef SQLITE3_ANDROID_PHONE_H
#define SQLITE3_ANDROID_PHONE_H

#include <sqlite3.h>
#include <stdbool.h>

/* external/sqlite android/PhoneNumberUtils.h and register_android_functions() for PHONE_NUMBERS_EQUAL */
bool phone_number_compare_loose(const char *a, const char *b);
bool phone_number_compare_loose_with_minmatch(const char *a, const char *b, int min_match);
bool phone_number_compare_strict(const char *a, const char *b);
int register_phone_numbers_equal(sqlite3 *handle);

#endif
