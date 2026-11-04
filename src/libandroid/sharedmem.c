/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* frameworks/base/native/android/sharedmem.cpp (android-14) on the host ashmem of libcutils, which
 * keeps the region in an unlinked file; ASharedMemory_dupFromJava() is left out (no android.os.SharedMemory) */

#include <stddef.h>

/* libcutils (art_standalone), cutils/ashmem.h */
int ashmem_create_region(const char *name, size_t size);
int ashmem_set_prot_region(int fd, int prot);
int ashmem_get_size_region(int fd);

#define BAD_VALUE (-22) /* android::BAD_VALUE, -EINVAL */

int ASharedMemory_create(const char *name, size_t size)
{
	if (size == 0)
		return BAD_VALUE;
	return ashmem_create_region(name, size);
}

/* AOSP checks ashmem_valid() first; the host libcutils has none, its ashmem_get_size_region() fails
 * on an invalid fd */
size_t ASharedMemory_getSize(int fd)
{
	int size = ashmem_get_size_region(fd);
	return size < 0 ? 0 : size;
}

int ASharedMemory_setProt(int fd, int prot)
{
	return ashmem_set_prot_region(fd, prot);
}
