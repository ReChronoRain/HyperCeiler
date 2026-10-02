/* SPDX-License-Identifier: 0BSD */

/*
 * Definitions for handling the .xz file format
 *
 * Author: Lasse Collin <lasse.collin@tukaani.org>
 */

#ifndef XZ_STREAM_H
#define XZ_STREAM_H

#if defined(__KERNEL__) && !XZ_INTERNAL_CRC32
#	include <linux/crc32.h>
#	undef crc32
#	define xz_crc32(buf, size, crc) \
		(~crc32_le(~(uint32_t)(crc), buf, size))
#endif

#define STREAM_HEADER_SIZE 12

#define HEADER_MAGIC "\3757zXZ"
#define HEADER_MAGIC_SIZE 6

#define FOOTER_MAGIC "YZ"
#define FOOTER_MAGIC_SIZE 2

typedef uint64_t vli_type;

#define VLI_MAX ((vli_type)-1 / 2)
#define VLI_UNKNOWN ((vli_type)-1)

#define VLI_BYTES_MAX (sizeof(vli_type) * 8 / 7)

enum xz_check {
	XZ_CHECK_NONE = 0,
	XZ_CHECK_CRC32 = 1,
	XZ_CHECK_CRC64 = 4,
	XZ_CHECK_SHA256 = 10
};

#define XZ_CHECK_MAX 15

#endif
