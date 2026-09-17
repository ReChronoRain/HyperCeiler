// SPDX-License-Identifier: 0BSD

/*
 * LZMA2 decoder
 *
 * Authors: Lasse Collin <lasse.collin@tukaani.org>
 *          Igor Pavlov <https://7-zip.org/>
 */

#include "xz_private.h"
#include "xz_lzma2.h"

#define RC_INIT_BYTES 5

#define LZMA_IN_REQUIRED 21

struct dictionary {
	uint8_t *buf;

	size_t start;

	size_t pos;

	size_t full;

	size_t limit;

	size_t end;

	uint32_t size;

	uint32_t size_max;

	uint32_t allocated;

	enum xz_mode mode;
};

struct rc_dec {
	uint32_t range;
	uint32_t code;

	uint32_t init_bytes_left;

	const uint8_t *in;
	size_t in_pos;
	size_t in_limit;
};

struct lzma_len_dec {
	uint16_t choice;

	uint16_t choice2;

	uint16_t low[POS_STATES_MAX][LEN_LOW_SYMBOLS];

	uint16_t mid[POS_STATES_MAX][LEN_MID_SYMBOLS];

	uint16_t high[LEN_HIGH_SYMBOLS];
};

struct lzma_dec {
	uint32_t rep0;
	uint32_t rep1;
	uint32_t rep2;
	uint32_t rep3;

	size_t len;

	enum lzma_state state;

	uint32_t lc;
	uint32_t literal_pos_mask;
	uint32_t pos_mask;

	uint16_t is_match[STATES][POS_STATES_MAX];

	uint16_t is_rep[STATES];

	uint16_t is_rep0[STATES];

	uint16_t is_rep1[STATES];

	uint16_t is_rep2[STATES];

	uint16_t is_rep0_long[STATES][POS_STATES_MAX];

	uint16_t dist_slot[DIST_STATES][DIST_SLOTS];

	uint16_t dist_special[FULL_DISTANCES - DIST_MODEL_END];

	uint16_t dist_align[ALIGN_SIZE];

	struct lzma_len_dec match_len_dec;

	struct lzma_len_dec rep_len_dec;

	uint16_t literal[LITERAL_CODERS_MAX][LITERAL_CODER_SIZE];
};

struct lzma2_dec {
	enum lzma2_seq {
		SEQ_CONTROL,
		SEQ_UNCOMPRESSED_1,
		SEQ_UNCOMPRESSED_2,
		SEQ_COMPRESSED_0,
		SEQ_COMPRESSED_1,
		SEQ_PROPERTIES,
		SEQ_LZMA_PREPARE,
		SEQ_LZMA_RUN,
		SEQ_COPY
	} sequence;

	enum lzma2_seq next_sequence;

	size_t uncompressed;

	size_t compressed;

	bool need_dict_reset;

	bool need_props;

#ifdef XZ_DEC_MICROLZMA
	bool pedantic_microlzma;
#endif
};

struct xz_dec_lzma2 {
	struct rc_dec rc;
	struct dictionary dict;
	struct lzma2_dec lzma2;
	struct lzma_dec lzma;

	struct {
		size_t size;
		uint8_t buf[3 * LZMA_IN_REQUIRED];
	} temp;
};

static void dict_reset(struct dictionary *dict, struct xz_buf *b)
{
	if (DEC_IS_SINGLE(dict->mode)) {
		dict->buf = b->out + b->out_pos;
		dict->end = b->out_size - b->out_pos;
	}

	dict->start = 0;
	dict->pos = 0;
	dict->limit = 0;
	dict->full = 0;
}

static void dict_limit(struct dictionary *dict, size_t out_max)
{
	if (dict->end - dict->pos <= out_max)
		dict->limit = dict->end;
	else
		dict->limit = dict->pos + out_max;
}

static inline bool dict_has_space(const struct dictionary *dict)
{
	return dict->pos < dict->limit;
}

static inline uint32_t dict_get(const struct dictionary *dict, size_t dist)
{
	size_t offset = dict->pos - dist - 1;

	if (dist >= dict->pos)
		offset += dict->end;

	return dict->full > 0 ? dict->buf[offset] : 0;
}

static inline void dict_put(struct dictionary *dict, uint8_t byte)
{
	dict->buf[dict->pos++] = byte;

	if (dict->full < dict->pos)
		dict->full = dict->pos;
}

static bool dict_repeat(struct dictionary *dict, size_t *len, size_t dist)
{
	size_t back;
	size_t left;

	if (dist >= dict->full || dist >= dict->size)
		return false;

	left = min(dict->limit - dict->pos, *len);
	*len -= left;

	back = dict->pos - dist - 1;
	if (dist >= dict->pos)
		back += dict->end;

	do {
		dict->buf[dict->pos++] = dict->buf[back++];
		if (back == dict->end)
			back = 0;
	} while (--left > 0);

	if (dict->full < dict->pos)
		dict->full = dict->pos;

	return true;
}

static void dict_uncompressed(struct dictionary *dict, struct xz_buf *b,
			      size_t *left)
{
	size_t copy_size;

	while (*left > 0 && b->in_pos < b->in_size
			&& b->out_pos < b->out_size) {
		copy_size = min(b->in_size - b->in_pos,
				b->out_size - b->out_pos);
		if (copy_size > dict->end - dict->pos)
			copy_size = dict->end - dict->pos;
		if (copy_size > *left)
			copy_size = *left;

		*left -= copy_size;

		memmove(dict->buf + dict->pos, b->in + b->in_pos, copy_size);
		dict->pos += copy_size;

		if (dict->full < dict->pos)
			dict->full = dict->pos;

		if (DEC_IS_MULTI(dict->mode)) {
			if (dict->pos == dict->end)
				dict->pos = 0;

			memmove(b->out + b->out_pos, b->in + b->in_pos,
					copy_size);
		}

		dict->start = dict->pos;

		b->out_pos += copy_size;
		b->in_pos += copy_size;
	}
}

#ifdef XZ_DEC_MICROLZMA
#	define DICT_FLUSH_SUPPORTS_SKIPPING true
#else
#	define DICT_FLUSH_SUPPORTS_SKIPPING false
#endif

static size_t dict_flush(struct dictionary *dict, struct xz_buf *b)
{
	size_t copy_size = dict->pos - dict->start;

	if (DEC_IS_MULTI(dict->mode)) {
		if (dict->pos == dict->end)
			dict->pos = 0;

		if (!DICT_FLUSH_SUPPORTS_SKIPPING || b->out != NULL)
			memcpy(b->out + b->out_pos, dict->buf + dict->start,
					copy_size);
	}

	dict->start = dict->pos;
	b->out_pos += copy_size;
	return copy_size;
}

static void rc_reset(struct rc_dec *rc)
{
	rc->range = (uint32_t)-1;
	rc->code = 0;
	rc->init_bytes_left = RC_INIT_BYTES;
}

static bool rc_read_init(struct rc_dec *rc, struct xz_buf *b)
{
	while (rc->init_bytes_left > 0) {
		if (b->in_pos == b->in_size)
			return false;

		rc->code = (rc->code << 8) + b->in[b->in_pos++];
		--rc->init_bytes_left;
	}

	return true;
}

static inline bool rc_limit_exceeded(const struct rc_dec *rc)
{
	return rc->in_pos > rc->in_limit;
}

static inline bool rc_is_finished(const struct rc_dec *rc)
{
	return rc->code == 0;
}

static __always_inline void rc_normalize(struct rc_dec *rc)
{
	if (rc->range < RC_TOP_VALUE) {
		rc->range <<= RC_SHIFT_BITS;
		rc->code = (rc->code << RC_SHIFT_BITS) + rc->in[rc->in_pos++];
	}
}

static __always_inline int rc_bit(struct rc_dec *rc, uint16_t *prob)
{
	uint32_t bound;
	int bit;

	rc_normalize(rc);
	bound = (rc->range >> RC_BIT_MODEL_TOTAL_BITS) * *prob;
	if (rc->code < bound) {
		rc->range = bound;
		*prob += (RC_BIT_MODEL_TOTAL - *prob) >> RC_MOVE_BITS;
		bit = 0;
	} else {
		rc->range -= bound;
		rc->code -= bound;
		*prob -= *prob >> RC_MOVE_BITS;
		bit = 1;
	}

	return bit;
}

static __always_inline uint32_t rc_bittree(struct rc_dec *rc,
					   uint16_t *probs, uint32_t limit)
{
	uint32_t symbol = 1;

	do {
		if (rc_bit(rc, &probs[symbol]))
			symbol = (symbol << 1) + 1;
		else
			symbol <<= 1;
	} while (symbol < limit);

	return symbol;
}

static __always_inline void rc_bittree_reverse(struct rc_dec *rc,
					       uint16_t *probs,
					       uint32_t *dest, uint32_t limit)
{
	uint32_t symbol = 1;
	uint32_t i = 0;

	do {
		if (rc_bit(rc, &probs[symbol])) {
			symbol = (symbol << 1) + 1;
			*dest += 1 << i;
		} else {
			symbol <<= 1;
		}
	} while (++i < limit);
}

static inline void rc_direct(struct rc_dec *rc, uint32_t *dest, uint32_t limit)
{
	uint32_t mask;

	do {
		rc_normalize(rc);
		rc->range >>= 1;
		rc->code -= rc->range;
		mask = (uint32_t)0 - (rc->code >> 31);
		rc->code += rc->range & mask;
		*dest = (*dest << 1) + (mask + 1);
	} while (--limit > 0);
}

static uint16_t *lzma_literal_probs(struct xz_dec_lzma2 *s)
{
	uint32_t prev_byte = dict_get(&s->dict, 0);
	uint32_t low = prev_byte >> (8 - s->lzma.lc);
	uint32_t high = (s->dict.pos & s->lzma.literal_pos_mask) << s->lzma.lc;
	return s->lzma.literal[low + high];
}

static void lzma_literal(struct xz_dec_lzma2 *s)
{
	uint16_t *probs;
	uint32_t symbol;
	uint32_t match_byte;
	uint32_t match_bit;
	uint32_t offset;
	uint32_t i;

	probs = lzma_literal_probs(s);

	if (lzma_state_is_literal(s->lzma.state)) {
		symbol = rc_bittree(&s->rc, probs, 0x100);
	} else {
		symbol = 1;
		match_byte = dict_get(&s->dict, s->lzma.rep0) << 1;
		offset = 0x100;

		do {
			match_bit = match_byte & offset;
			match_byte <<= 1;
			i = offset + match_bit + symbol;

			if (rc_bit(&s->rc, &probs[i])) {
				symbol = (symbol << 1) + 1;
				offset &= match_bit;
			} else {
				symbol <<= 1;
				offset &= ~match_bit;
			}
		} while (symbol < 0x100);
	}

	dict_put(&s->dict, (uint8_t)symbol);
	lzma_state_literal(&s->lzma.state);
}

static void lzma_len(struct xz_dec_lzma2 *s, struct lzma_len_dec *l,
		     uint32_t pos_state)
{
	uint16_t *probs;
	uint32_t limit;

	if (!rc_bit(&s->rc, &l->choice)) {
		probs = l->low[pos_state];
		limit = LEN_LOW_SYMBOLS;
		s->lzma.len = MATCH_LEN_MIN;
	} else {
		if (!rc_bit(&s->rc, &l->choice2)) {
			probs = l->mid[pos_state];
			limit = LEN_MID_SYMBOLS;
			s->lzma.len = MATCH_LEN_MIN + LEN_LOW_SYMBOLS;
		} else {
			probs = l->high;
			limit = LEN_HIGH_SYMBOLS;
			s->lzma.len = MATCH_LEN_MIN + LEN_LOW_SYMBOLS
					+ LEN_MID_SYMBOLS;
		}
	}

	s->lzma.len += rc_bittree(&s->rc, probs, limit) - limit;
}

static void lzma_match(struct xz_dec_lzma2 *s, uint32_t pos_state)
{
	uint16_t *probs;
	uint32_t dist_slot;
	uint32_t limit;

	lzma_state_match(&s->lzma.state);

	s->lzma.rep3 = s->lzma.rep2;
	s->lzma.rep2 = s->lzma.rep1;
	s->lzma.rep1 = s->lzma.rep0;

	lzma_len(s, &s->lzma.match_len_dec, pos_state);

	probs = s->lzma.dist_slot[lzma_get_dist_state(s->lzma.len)];
	dist_slot = rc_bittree(&s->rc, probs, DIST_SLOTS) - DIST_SLOTS;

	if (dist_slot < DIST_MODEL_START) {
		s->lzma.rep0 = dist_slot;
	} else {
		limit = (dist_slot >> 1) - 1;
		s->lzma.rep0 = 2 + (dist_slot & 1);

		if (dist_slot < DIST_MODEL_END) {
			s->lzma.rep0 <<= limit;
			probs = s->lzma.dist_special + s->lzma.rep0
					- dist_slot - 1;
			rc_bittree_reverse(&s->rc, probs,
					&s->lzma.rep0, limit);
		} else {
			rc_direct(&s->rc, &s->lzma.rep0, limit - ALIGN_BITS);
			s->lzma.rep0 <<= ALIGN_BITS;
			rc_bittree_reverse(&s->rc, s->lzma.dist_align,
					&s->lzma.rep0, ALIGN_BITS);
		}
	}
}

static void lzma_rep_match(struct xz_dec_lzma2 *s, uint32_t pos_state)
{
	uint32_t tmp;

	if (!rc_bit(&s->rc, &s->lzma.is_rep0[s->lzma.state])) {
		if (!rc_bit(&s->rc, &s->lzma.is_rep0_long[
				s->lzma.state][pos_state])) {
			lzma_state_short_rep(&s->lzma.state);
			s->lzma.len = 1;
			return;
		}
	} else {
		if (!rc_bit(&s->rc, &s->lzma.is_rep1[s->lzma.state])) {
			tmp = s->lzma.rep1;
		} else {
			if (!rc_bit(&s->rc, &s->lzma.is_rep2[s->lzma.state])) {
				tmp = s->lzma.rep2;
			} else {
				tmp = s->lzma.rep3;
				s->lzma.rep3 = s->lzma.rep2;
			}

			s->lzma.rep2 = s->lzma.rep1;
		}

		s->lzma.rep1 = s->lzma.rep0;
		s->lzma.rep0 = tmp;
	}

	lzma_state_long_rep(&s->lzma.state);
	lzma_len(s, &s->lzma.rep_len_dec, pos_state);
}

static bool lzma_main(struct xz_dec_lzma2 *s)
{
	uint32_t pos_state;

	if (dict_has_space(&s->dict) && s->lzma.len > 0)
		dict_repeat(&s->dict, &s->lzma.len, s->lzma.rep0);

	while (dict_has_space(&s->dict) && !rc_limit_exceeded(&s->rc)) {
		pos_state = s->dict.pos & s->lzma.pos_mask;

		if (!rc_bit(&s->rc, &s->lzma.is_match[
				s->lzma.state][pos_state])) {
			lzma_literal(s);
		} else {
			if (rc_bit(&s->rc, &s->lzma.is_rep[s->lzma.state]))
				lzma_rep_match(s, pos_state);
			else
				lzma_match(s, pos_state);

			if (!dict_repeat(&s->dict, &s->lzma.len, s->lzma.rep0))
				return false;
		}
	}

	rc_normalize(&s->rc);

	return true;
}

static void lzma_reset(struct xz_dec_lzma2 *s)
{
	uint16_t *probs;
	size_t i;

	s->lzma.state = STATE_LIT_LIT;
	s->lzma.rep0 = 0;
	s->lzma.rep1 = 0;
	s->lzma.rep2 = 0;
	s->lzma.rep3 = 0;
	s->lzma.len = 0;

	probs = s->lzma.is_match[0];
	for (i = 0; i < PROBS_TOTAL; ++i)
		probs[i] = RC_BIT_MODEL_TOTAL / 2;

	rc_reset(&s->rc);
}

static bool lzma_props(struct xz_dec_lzma2 *s, uint8_t props)
{
	if (props > (4 * 5 + 4) * 9 + 8)
		return false;

	s->lzma.pos_mask = 0;
	while (props >= 9 * 5) {
		props -= 9 * 5;
		++s->lzma.pos_mask;
	}

	s->lzma.pos_mask = (1 << s->lzma.pos_mask) - 1;

	s->lzma.literal_pos_mask = 0;
	while (props >= 9) {
		props -= 9;
		++s->lzma.literal_pos_mask;
	}

	s->lzma.lc = props;

	if (s->lzma.lc + s->lzma.literal_pos_mask > 4)
		return false;

	s->lzma.literal_pos_mask = (1 << s->lzma.literal_pos_mask) - 1;

	lzma_reset(s);

	return true;
}

static bool lzma2_lzma(struct xz_dec_lzma2 *s, struct xz_buf *b)
{
	size_t in_avail;
	size_t tmp;

	in_avail = b->in_size - b->in_pos;
	if (s->temp.size > 0 || s->lzma2.compressed == 0) {
		tmp = 2 * LZMA_IN_REQUIRED - s->temp.size;
		if (tmp > s->lzma2.compressed - s->temp.size)
			tmp = s->lzma2.compressed - s->temp.size;
		if (tmp > in_avail)
			tmp = in_avail;

		memcpy(s->temp.buf + s->temp.size, b->in + b->in_pos, tmp);

		if (s->temp.size + tmp == s->lzma2.compressed) {
			memzero(s->temp.buf + s->temp.size + tmp,
					sizeof(s->temp.buf)
						- s->temp.size - tmp);
			s->rc.in_limit = s->temp.size + tmp;
		} else if (s->temp.size + tmp < LZMA_IN_REQUIRED) {
			s->temp.size += tmp;
			b->in_pos += tmp;
			return true;
		} else {
			s->rc.in_limit = s->temp.size + tmp - LZMA_IN_REQUIRED;
		}

		s->rc.in = s->temp.buf;
		s->rc.in_pos = 0;

		if (!lzma_main(s) || s->rc.in_pos > s->temp.size + tmp)
			return false;

		s->lzma2.compressed -= s->rc.in_pos;

		if (s->rc.in_pos < s->temp.size) {
			s->temp.size -= s->rc.in_pos;
			memmove(s->temp.buf, s->temp.buf + s->rc.in_pos,
					s->temp.size);
			return true;
		}

		b->in_pos += s->rc.in_pos - s->temp.size;
		s->temp.size = 0;
	}

	in_avail = b->in_size - b->in_pos;
	if (in_avail >= LZMA_IN_REQUIRED) {
		s->rc.in = b->in;
		s->rc.in_pos = b->in_pos;

		if (in_avail >= s->lzma2.compressed + LZMA_IN_REQUIRED)
			s->rc.in_limit = b->in_pos + s->lzma2.compressed;
		else
			s->rc.in_limit = b->in_size - LZMA_IN_REQUIRED;

		if (!lzma_main(s))
			return false;

		in_avail = s->rc.in_pos - b->in_pos;
		if (in_avail > s->lzma2.compressed)
			return false;

		s->lzma2.compressed -= in_avail;
		b->in_pos = s->rc.in_pos;
	}

	in_avail = b->in_size - b->in_pos;
	if (in_avail < LZMA_IN_REQUIRED) {
		if (in_avail > s->lzma2.compressed)
			in_avail = s->lzma2.compressed;

		memcpy(s->temp.buf, b->in + b->in_pos, in_avail);
		s->temp.size = in_avail;
		b->in_pos += in_avail;
	}

	return true;
}

enum xz_ret xz_dec_lzma2_run(struct xz_dec_lzma2 *s, struct xz_buf *b)
{
	uint32_t tmp;

	while (b->in_pos < b->in_size || s->lzma2.sequence == SEQ_LZMA_RUN) {
		switch (s->lzma2.sequence) {
		case SEQ_CONTROL:
			tmp = b->in[b->in_pos++];

			if (tmp == 0x00)
				return XZ_STREAM_END;

			if (tmp >= 0xE0 || tmp == 0x01) {
				s->lzma2.need_props = true;
				s->lzma2.need_dict_reset = false;
				dict_reset(&s->dict, b);
			} else if (s->lzma2.need_dict_reset) {
				return XZ_DATA_ERROR;
			}

			if (tmp >= 0x80) {
				s->lzma2.uncompressed = (tmp & 0x1F) << 16;
				s->lzma2.sequence = SEQ_UNCOMPRESSED_1;

				if (tmp >= 0xC0) {
					s->lzma2.need_props = false;
					s->lzma2.next_sequence
							= SEQ_PROPERTIES;

				} else if (s->lzma2.need_props) {
					return XZ_DATA_ERROR;

				} else {
					s->lzma2.next_sequence
							= SEQ_LZMA_PREPARE;
					if (tmp >= 0xA0)
						lzma_reset(s);
				}
			} else {
				if (tmp > 0x02)
					return XZ_DATA_ERROR;

				s->lzma2.sequence = SEQ_COMPRESSED_0;
				s->lzma2.next_sequence = SEQ_COPY;
			}

			break;

		case SEQ_UNCOMPRESSED_1:
			s->lzma2.uncompressed
					+= (size_t)b->in[b->in_pos++] << 8;
			s->lzma2.sequence = SEQ_UNCOMPRESSED_2;
			break;

		case SEQ_UNCOMPRESSED_2:
			s->lzma2.uncompressed
					+= (size_t)b->in[b->in_pos++] + 1;
			s->lzma2.sequence = SEQ_COMPRESSED_0;
			break;

		case SEQ_COMPRESSED_0:
			s->lzma2.compressed = (size_t)b->in[b->in_pos++] << 8;
			s->lzma2.sequence = SEQ_COMPRESSED_1;
			break;

		case SEQ_COMPRESSED_1:
			s->lzma2.compressed += (size_t)b->in[b->in_pos++] + 1;
			s->lzma2.sequence = s->lzma2.next_sequence;
			break;

		case SEQ_PROPERTIES:
			if (!lzma_props(s, b->in[b->in_pos++]))
				return XZ_DATA_ERROR;

			s->lzma2.sequence = SEQ_LZMA_PREPARE;

			fallthrough;

		case SEQ_LZMA_PREPARE:
			if (s->lzma2.compressed < RC_INIT_BYTES)
				return XZ_DATA_ERROR;

			if (!rc_read_init(&s->rc, b))
				return XZ_OK;

			s->lzma2.compressed -= RC_INIT_BYTES;
			s->lzma2.sequence = SEQ_LZMA_RUN;

			fallthrough;

		case SEQ_LZMA_RUN:
			dict_limit(&s->dict, min(b->out_size - b->out_pos,
						 s->lzma2.uncompressed));
			if (!lzma2_lzma(s, b))
				return XZ_DATA_ERROR;

			s->lzma2.uncompressed -= dict_flush(&s->dict, b);

			if (s->lzma2.uncompressed == 0) {
				if (s->lzma2.compressed > 0 || s->lzma.len > 0
						|| !rc_is_finished(&s->rc))
					return XZ_DATA_ERROR;

				rc_reset(&s->rc);
				s->lzma2.sequence = SEQ_CONTROL;

			} else if (b->out_pos == b->out_size
					|| (b->in_pos == b->in_size
						&& s->temp.size
						< s->lzma2.compressed)) {
				return XZ_OK;
			}

			break;

		case SEQ_COPY:
			dict_uncompressed(&s->dict, b, &s->lzma2.compressed);
			if (s->lzma2.compressed > 0)
				return XZ_OK;

			s->lzma2.sequence = SEQ_CONTROL;
			break;
		}
	}

	return XZ_OK;
}

struct xz_dec_lzma2 *xz_dec_lzma2_create(enum xz_mode mode, uint32_t dict_max)
{
	struct xz_dec_lzma2 *s = kmalloc_obj(*s);
	if (s == NULL)
		return NULL;

	s->dict.mode = mode;
	s->dict.size_max = dict_max;

	if (DEC_IS_PREALLOC(mode)) {
		s->dict.buf = vmalloc(dict_max);
		if (s->dict.buf == NULL) {
			kfree(s);
			return NULL;
		}
	} else if (DEC_IS_DYNALLOC(mode)) {
		s->dict.buf = NULL;
		s->dict.allocated = 0;
	}

	return s;
}

enum xz_ret xz_dec_lzma2_reset(struct xz_dec_lzma2 *s, uint8_t props)
{
	if (props > 39)
		return XZ_OPTIONS_ERROR;

	s->dict.size = 2 + (props & 1);
	s->dict.size <<= (props >> 1) + 11;

	if (DEC_IS_MULTI(s->dict.mode)) {
		if (s->dict.size > s->dict.size_max)
			return XZ_MEMLIMIT_ERROR;

		s->dict.end = s->dict.size;

		if (DEC_IS_DYNALLOC(s->dict.mode)) {
			if (s->dict.allocated < s->dict.size) {
				s->dict.allocated = s->dict.size;
				vfree(s->dict.buf);
				s->dict.buf = vmalloc(s->dict.size);
				if (s->dict.buf == NULL) {
					s->dict.allocated = 0;
					return XZ_MEM_ERROR;
				}
			}
		}
	}

	s->lzma2.sequence = SEQ_CONTROL;
	s->lzma2.need_dict_reset = true;

	s->temp.size = 0;

	return XZ_OK;
}

void xz_dec_lzma2_end(struct xz_dec_lzma2 *s)
{
	if (DEC_IS_MULTI(s->dict.mode))
		vfree(s->dict.buf);

	kfree(s);
}

#ifdef XZ_DEC_MICROLZMA
struct xz_dec_microlzma {
	struct xz_dec_lzma2 s;
};

enum xz_ret xz_dec_microlzma_run(struct xz_dec_microlzma *s_ptr,
				 struct xz_buf *b)
{
	struct xz_dec_lzma2 *s = &s_ptr->s;

	if (s->lzma2.sequence != SEQ_LZMA_RUN) {
		if (s->lzma2.sequence == SEQ_PROPERTIES) {
			if (b->in_pos >= b->in_size)
				return XZ_OK;

			if (!lzma_props(s, ~b->in[b->in_pos]))
				return XZ_DATA_ERROR;

			s->lzma2.sequence = SEQ_LZMA_PREPARE;
		}

		if (s->lzma2.compressed < RC_INIT_BYTES
				|| s->lzma2.compressed > (3U << 30))
			return XZ_DATA_ERROR;

		if (!rc_read_init(&s->rc, b))
			return XZ_OK;

		s->lzma2.compressed -= RC_INIT_BYTES;
		s->lzma2.sequence = SEQ_LZMA_RUN;

		dict_reset(&s->dict, b);
	}

	if (DEC_IS_SINGLE(s->dict.mode))
		s->dict.end = b->out_size - b->out_pos;

	while (true) {
		dict_limit(&s->dict, min(b->out_size - b->out_pos,
					 s->lzma2.uncompressed));

		if (!lzma2_lzma(s, b))
			return XZ_DATA_ERROR;

		s->lzma2.uncompressed -= dict_flush(&s->dict, b);

		if (s->lzma2.uncompressed == 0) {
			if (s->lzma2.pedantic_microlzma) {
				if (s->lzma2.compressed > 0 || s->lzma.len > 0
						|| !rc_is_finished(&s->rc))
					return XZ_DATA_ERROR;
			}

			return XZ_STREAM_END;
		}

		if (b->out_pos == b->out_size)
			return XZ_OK;

		if (b->in_pos == b->in_size
				&& s->temp.size < s->lzma2.compressed)
			return XZ_OK;
	}
}

struct xz_dec_microlzma *xz_dec_microlzma_alloc(enum xz_mode mode,
						uint32_t dict_size)
{
	struct xz_dec_microlzma *s;

	if (dict_size < 4096 || dict_size > (3U << 30))
		return NULL;

	s = kmalloc_obj(*s);
	if (s == NULL)
		return NULL;

	s->s.dict.mode = mode;
	s->s.dict.size = dict_size;

	if (DEC_IS_MULTI(mode)) {
		s->s.dict.end = dict_size;

		s->s.dict.buf = vmalloc(dict_size);
		if (s->s.dict.buf == NULL) {
			kfree(s);
			return NULL;
		}
	}

	return s;
}

void xz_dec_microlzma_reset(struct xz_dec_microlzma *s, uint32_t comp_size,
			    uint32_t uncomp_size, int uncomp_size_is_exact)
{
	s->s.lzma2.compressed = comp_size;
	s->s.lzma2.uncompressed = uncomp_size;
	s->s.lzma2.pedantic_microlzma = uncomp_size_is_exact;

	s->s.lzma2.sequence = SEQ_PROPERTIES;
	s->s.temp.size = 0;
}

void xz_dec_microlzma_end(struct xz_dec_microlzma *s)
{
	if (DEC_IS_MULTI(s->s.dict.mode))
		vfree(s->s.dict.buf);

	kfree(s);
}
#endif
