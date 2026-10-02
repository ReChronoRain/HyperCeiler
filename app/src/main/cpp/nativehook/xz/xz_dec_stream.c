// SPDX-License-Identifier: 0BSD

/*
 * .xz Stream decoder
 *
 * Author: Lasse Collin <lasse.collin@tukaani.org>
 */

#include "xz_private.h"
#include "xz_stream.h"

struct xz_dec_hash {
	vli_type unpadded;
	vli_type uncompressed;
	uint32_t crc32;
};

struct xz_dec {
	enum {
		SEQ_STREAM_HEADER,
		SEQ_BLOCK_START,
		SEQ_BLOCK_HEADER,
		SEQ_BLOCK_UNCOMPRESS,
		SEQ_BLOCK_PADDING,
		SEQ_BLOCK_CHECK,
		SEQ_INDEX,
		SEQ_INDEX_PADDING,
		SEQ_INDEX_CRC32,
		SEQ_STREAM_FOOTER
	} sequence;

	uint32_t pos;

	vli_type vli;

	size_t in_start;
	size_t out_start;

	uint32_t crc32;

	enum xz_check check_type;

	enum xz_mode mode;

	bool allow_buf_error;

	struct {
		vli_type compressed;

		vli_type uncompressed;

		uint32_t size;
	} block_header;

	struct {
		vli_type compressed;

		vli_type uncompressed;

		vli_type count;

		struct xz_dec_hash hash;
	} block;

	struct {
		enum {
			SEQ_INDEX_COUNT,
			SEQ_INDEX_UNPADDED,
			SEQ_INDEX_UNCOMPRESSED
		} sequence;

		vli_type size;

		vli_type count;

		struct xz_dec_hash hash;
	} index;

	struct {
		size_t pos;
		size_t size;
		uint8_t buf[1024];
	} temp;

	struct xz_dec_lzma2 *lzma2;

#ifdef XZ_DEC_BCJ
	struct xz_dec_bcj *bcj;
	bool bcj_active;
#endif
};

#ifdef XZ_DEC_ANY_CHECK
static const uint8_t check_sizes[16] = {
	0,
	4, 4, 4,
	8, 8, 8,
	16, 16, 16,
	32, 32, 32,
	64, 64, 64
};
#endif

static bool fill_temp(struct xz_dec *s, struct xz_buf *b)
{
	size_t copy_size = min(b->in_size - b->in_pos,
			       s->temp.size - s->temp.pos);

	memcpy(s->temp.buf + s->temp.pos, b->in + b->in_pos, copy_size);
	b->in_pos += copy_size;
	s->temp.pos += copy_size;

	if (s->temp.pos == s->temp.size) {
		s->temp.pos = 0;
		return true;
	}

	return false;
}

static enum xz_ret dec_vli(struct xz_dec *s, const uint8_t *in,
			   size_t *in_pos, size_t in_size)
{
	uint8_t byte;

	if (s->pos == 0)
		s->vli = 0;

	while (*in_pos < in_size) {
		byte = in[*in_pos];
		++*in_pos;

		s->vli |= (vli_type)(byte & 0x7F) << s->pos;

		if ((byte & 0x80) == 0) {
			if (byte == 0 && s->pos != 0)
				return XZ_DATA_ERROR;

			s->pos = 0;
			return XZ_STREAM_END;
		}

		s->pos += 7;
		if (s->pos == 7 * VLI_BYTES_MAX)
			return XZ_DATA_ERROR;
	}

	return XZ_OK;
}

static enum xz_ret dec_block(struct xz_dec *s, struct xz_buf *b)
{
	enum xz_ret ret;

	s->in_start = b->in_pos;
	s->out_start = b->out_pos;

#ifdef XZ_DEC_BCJ
	if (s->bcj_active)
		ret = xz_dec_bcj_run(s->bcj, s->lzma2, b);
	else
#endif
		ret = xz_dec_lzma2_run(s->lzma2, b);

	s->block.compressed += b->in_pos - s->in_start;
	s->block.uncompressed += b->out_pos - s->out_start;

	if (s->block.compressed > s->block_header.compressed
			|| s->block.uncompressed
				> s->block_header.uncompressed)
		return XZ_DATA_ERROR;

	if (s->check_type == XZ_CHECK_CRC32)
		s->crc32 = xz_crc32(b->out + s->out_start,
				b->out_pos - s->out_start, s->crc32);

	if (ret == XZ_STREAM_END) {
		if (s->block_header.compressed != VLI_UNKNOWN
				&& s->block_header.compressed
					!= s->block.compressed)
			return XZ_DATA_ERROR;

		if (s->block_header.uncompressed != VLI_UNKNOWN
				&& s->block_header.uncompressed
					!= s->block.uncompressed)
			return XZ_DATA_ERROR;

		s->block.hash.unpadded += s->block_header.size
				+ s->block.compressed;

#ifdef XZ_DEC_ANY_CHECK
		s->block.hash.unpadded += check_sizes[s->check_type];
#else
		if (s->check_type == XZ_CHECK_CRC32)
			s->block.hash.unpadded += 4;
#endif

		s->block.hash.uncompressed += s->block.uncompressed;
		s->block.hash.crc32 = xz_crc32(
				(const uint8_t *)&s->block.hash,
				sizeof(s->block.hash), s->block.hash.crc32);

		++s->block.count;
	}

	return ret;
}

static void index_update(struct xz_dec *s, const struct xz_buf *b)
{
	size_t in_used = b->in_pos - s->in_start;
	s->index.size += in_used;
	s->crc32 = xz_crc32(b->in + s->in_start, in_used, s->crc32);
}

static enum xz_ret dec_index(struct xz_dec *s, struct xz_buf *b)
{
	enum xz_ret ret;

	do {
		ret = dec_vli(s, b->in, &b->in_pos, b->in_size);
		if (ret != XZ_STREAM_END) {
			index_update(s, b);
			return ret;
		}

		switch (s->index.sequence) {
		case SEQ_INDEX_COUNT:
			s->index.count = s->vli;

			if (s->index.count != s->block.count)
				return XZ_DATA_ERROR;

			s->index.sequence = SEQ_INDEX_UNPADDED;
			break;

		case SEQ_INDEX_UNPADDED:
			s->index.hash.unpadded += s->vli;
			s->index.sequence = SEQ_INDEX_UNCOMPRESSED;
			break;

		case SEQ_INDEX_UNCOMPRESSED:
			s->index.hash.uncompressed += s->vli;
			s->index.hash.crc32 = xz_crc32(
					(const uint8_t *)&s->index.hash,
					sizeof(s->index.hash),
					s->index.hash.crc32);
			--s->index.count;
			s->index.sequence = SEQ_INDEX_UNPADDED;
			break;
		}
	} while (s->index.count > 0);

	return XZ_STREAM_END;
}

static enum xz_ret crc32_validate(struct xz_dec *s, struct xz_buf *b)
{
	do {
		if (b->in_pos == b->in_size)
			return XZ_OK;

		if (((s->crc32 >> s->pos) & 0xFF) != b->in[b->in_pos++])
			return XZ_DATA_ERROR;

		s->pos += 8;

	} while (s->pos < 32);

	s->crc32 = 0;
	s->pos = 0;

	return XZ_STREAM_END;
}

#ifdef XZ_DEC_ANY_CHECK
static bool check_skip(struct xz_dec *s, struct xz_buf *b)
{
	while (s->pos < check_sizes[s->check_type]) {
		if (b->in_pos == b->in_size)
			return false;

		++b->in_pos;
		++s->pos;
	}

	s->pos = 0;

	return true;
}
#endif

static enum xz_ret dec_stream_header(struct xz_dec *s)
{
	if (!memeq(s->temp.buf, HEADER_MAGIC, HEADER_MAGIC_SIZE))
		return XZ_FORMAT_ERROR;

	if (xz_crc32(s->temp.buf + HEADER_MAGIC_SIZE, 2, 0)
			!= get_le32(s->temp.buf + HEADER_MAGIC_SIZE + 2))
		return XZ_DATA_ERROR;

	if (s->temp.buf[HEADER_MAGIC_SIZE] != 0)
		return XZ_OPTIONS_ERROR;

	if (s->temp.buf[HEADER_MAGIC_SIZE + 1] > XZ_CHECK_MAX)
		return XZ_OPTIONS_ERROR;

	s->check_type = s->temp.buf[HEADER_MAGIC_SIZE + 1];

#ifdef XZ_DEC_ANY_CHECK
	if (s->check_type > XZ_CHECK_CRC32)
		return XZ_UNSUPPORTED_CHECK;
#else
	if (s->check_type > XZ_CHECK_CRC32)
		return XZ_OPTIONS_ERROR;
#endif

	return XZ_OK;
}

static enum xz_ret dec_stream_footer(struct xz_dec *s)
{
	if (!memeq(s->temp.buf + 10, FOOTER_MAGIC, FOOTER_MAGIC_SIZE))
		return XZ_DATA_ERROR;

	if (xz_crc32(s->temp.buf + 4, 6, 0) != get_le32(s->temp.buf))
		return XZ_DATA_ERROR;

	if ((s->index.size >> 2) != get_le32(s->temp.buf + 4))
		return XZ_DATA_ERROR;

	if (s->temp.buf[8] != 0 || s->temp.buf[9] != s->check_type)
		return XZ_DATA_ERROR;

	return XZ_STREAM_END;
}

static enum xz_ret dec_block_header(struct xz_dec *s)
{
	enum xz_ret ret;

	s->temp.size -= 4;
	if (xz_crc32(s->temp.buf, s->temp.size, 0)
			!= get_le32(s->temp.buf + s->temp.size))
		return XZ_DATA_ERROR;

	s->temp.pos = 2;

#ifdef XZ_DEC_BCJ
	if (s->temp.buf[1] & 0x3E)
#else
	if (s->temp.buf[1] & 0x3F)
#endif
		return XZ_OPTIONS_ERROR;

	if (s->temp.buf[1] & 0x40) {
		if (dec_vli(s, s->temp.buf, &s->temp.pos, s->temp.size)
					!= XZ_STREAM_END)
			return XZ_DATA_ERROR;

		s->block_header.compressed = s->vli;
	} else {
		s->block_header.compressed = VLI_UNKNOWN;
	}

	if (s->temp.buf[1] & 0x80) {
		if (dec_vli(s, s->temp.buf, &s->temp.pos, s->temp.size)
				!= XZ_STREAM_END)
			return XZ_DATA_ERROR;

		s->block_header.uncompressed = s->vli;
	} else {
		s->block_header.uncompressed = VLI_UNKNOWN;
	}

#ifdef XZ_DEC_BCJ
	s->bcj_active = s->temp.buf[1] & 0x01;
	if (s->bcj_active) {
		if (s->temp.size - s->temp.pos < 2)
			return XZ_OPTIONS_ERROR;

		ret = xz_dec_bcj_reset(s->bcj, s->temp.buf[s->temp.pos++]);
		if (ret != XZ_OK)
			return ret;

		if (s->temp.buf[s->temp.pos++] != 0x00)
			return XZ_OPTIONS_ERROR;
	}
#endif

	if (s->temp.size - s->temp.pos < 2)
		return XZ_DATA_ERROR;

	if (s->temp.buf[s->temp.pos++] != 0x21)
		return XZ_OPTIONS_ERROR;

	if (s->temp.buf[s->temp.pos++] != 0x01)
		return XZ_OPTIONS_ERROR;

	if (s->temp.size - s->temp.pos < 1)
		return XZ_DATA_ERROR;

	ret = xz_dec_lzma2_reset(s->lzma2, s->temp.buf[s->temp.pos++]);
	if (ret != XZ_OK)
		return ret;

	while (s->temp.pos < s->temp.size)
		if (s->temp.buf[s->temp.pos++] != 0x00)
			return XZ_OPTIONS_ERROR;

	s->temp.pos = 0;
	s->block.compressed = 0;
	s->block.uncompressed = 0;

	return XZ_OK;
}

static enum xz_ret dec_main(struct xz_dec *s, struct xz_buf *b)
{
	enum xz_ret ret;

	s->in_start = b->in_pos;

	while (true) {
		switch (s->sequence) {
		case SEQ_STREAM_HEADER:
			if (!fill_temp(s, b))
				return XZ_OK;

			s->sequence = SEQ_BLOCK_START;

			ret = dec_stream_header(s);
			if (ret != XZ_OK)
				return ret;

			fallthrough;

		case SEQ_BLOCK_START:
			if (b->in_pos == b->in_size)
				return XZ_OK;

			if (b->in[b->in_pos] == 0) {
				s->in_start = b->in_pos++;
				s->sequence = SEQ_INDEX;
				break;
			}

			s->block_header.size
				= ((uint32_t)b->in[b->in_pos] + 1) * 4;

			s->temp.size = s->block_header.size;
			s->temp.pos = 0;
			s->sequence = SEQ_BLOCK_HEADER;

			fallthrough;

		case SEQ_BLOCK_HEADER:
			if (!fill_temp(s, b))
				return XZ_OK;

			ret = dec_block_header(s);
			if (ret != XZ_OK)
				return ret;

			s->sequence = SEQ_BLOCK_UNCOMPRESS;

			fallthrough;

		case SEQ_BLOCK_UNCOMPRESS:
			ret = dec_block(s, b);
			if (ret != XZ_STREAM_END)
				return ret;

			s->sequence = SEQ_BLOCK_PADDING;

			fallthrough;

		case SEQ_BLOCK_PADDING:
			while (s->block.compressed & 3) {
				if (b->in_pos == b->in_size)
					return XZ_OK;

				if (b->in[b->in_pos++] != 0)
					return XZ_DATA_ERROR;

				++s->block.compressed;
			}

			s->sequence = SEQ_BLOCK_CHECK;

			fallthrough;

		case SEQ_BLOCK_CHECK:
			if (s->check_type == XZ_CHECK_CRC32) {
				ret = crc32_validate(s, b);
				if (ret != XZ_STREAM_END)
					return ret;
			}
#ifdef XZ_DEC_ANY_CHECK
			else if (!check_skip(s, b)) {
				return XZ_OK;
			}
#endif

			s->sequence = SEQ_BLOCK_START;
			break;

		case SEQ_INDEX:
			ret = dec_index(s, b);
			if (ret != XZ_STREAM_END)
				return ret;

			s->sequence = SEQ_INDEX_PADDING;

			fallthrough;

		case SEQ_INDEX_PADDING:
			while ((s->index.size + (b->in_pos - s->in_start))
					& 3) {
				if (b->in_pos == b->in_size) {
					index_update(s, b);
					return XZ_OK;
				}

				if (b->in[b->in_pos++] != 0)
					return XZ_DATA_ERROR;
			}

			index_update(s, b);

			if (!memeq(&s->block.hash, &s->index.hash,
					sizeof(s->block.hash)))
				return XZ_DATA_ERROR;

			s->sequence = SEQ_INDEX_CRC32;

			fallthrough;

		case SEQ_INDEX_CRC32:
			ret = crc32_validate(s, b);
			if (ret != XZ_STREAM_END)
				return ret;

			s->temp.size = STREAM_HEADER_SIZE;
			s->sequence = SEQ_STREAM_FOOTER;

			fallthrough;

		case SEQ_STREAM_FOOTER:
			if (!fill_temp(s, b))
				return XZ_OK;

			return dec_stream_footer(s);
		}
	}

}

enum xz_ret xz_dec_run(struct xz_dec *s, struct xz_buf *b)
{
	size_t in_start;
	size_t out_start;
	enum xz_ret ret;

	if (DEC_IS_SINGLE(s->mode))
		xz_dec_reset(s);

	in_start = b->in_pos;
	out_start = b->out_pos;
	ret = dec_main(s, b);

	if (DEC_IS_SINGLE(s->mode)) {
		if (ret == XZ_OK)
			ret = b->in_pos == b->in_size
					? XZ_DATA_ERROR : XZ_BUF_ERROR;

		if (ret != XZ_STREAM_END) {
			b->in_pos = in_start;
			b->out_pos = out_start;
		}

	} else if (ret == XZ_OK && in_start == b->in_pos
			&& out_start == b->out_pos) {
		if (s->allow_buf_error)
			ret = XZ_BUF_ERROR;

		s->allow_buf_error = true;
	} else {
		s->allow_buf_error = false;
	}

	return ret;
}

struct xz_dec *xz_dec_init(enum xz_mode mode, uint32_t dict_max)
{
	struct xz_dec *s = kmalloc_obj(*s);
	if (s == NULL)
		return NULL;

	s->mode = mode;

#ifdef XZ_DEC_BCJ
	s->bcj = xz_dec_bcj_create(DEC_IS_SINGLE(mode));
	if (s->bcj == NULL)
		goto error_bcj;
#endif

	s->lzma2 = xz_dec_lzma2_create(mode, dict_max);
	if (s->lzma2 == NULL)
		goto error_lzma2;

	xz_dec_reset(s);
	return s;

error_lzma2:
#ifdef XZ_DEC_BCJ
	xz_dec_bcj_end(s->bcj);
error_bcj:
#endif
	kfree(s);
	return NULL;
}

void xz_dec_reset(struct xz_dec *s)
{
	s->sequence = SEQ_STREAM_HEADER;
	s->allow_buf_error = false;
	s->pos = 0;
	s->crc32 = 0;
	memzero(&s->block, sizeof(s->block));
	memzero(&s->index, sizeof(s->index));
	s->temp.pos = 0;
	s->temp.size = STREAM_HEADER_SIZE;
}

void xz_dec_end(struct xz_dec *s)
{
	if (s != NULL) {
		xz_dec_lzma2_end(s->lzma2);
#ifdef XZ_DEC_BCJ
		xz_dec_bcj_end(s->bcj);
#endif
		kfree(s);
	}
}
