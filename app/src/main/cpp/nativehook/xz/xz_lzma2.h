/* SPDX-License-Identifier: 0BSD */

/*
 * LZMA2 definitions
 *
 * Authors: Lasse Collin <lasse.collin@tukaani.org>
 *          Igor Pavlov <https://7-zip.org/>
 */

#ifndef XZ_LZMA2_H
#define XZ_LZMA2_H

#define RC_SHIFT_BITS 8
#define RC_TOP_BITS 24
#define RC_TOP_VALUE (1 << RC_TOP_BITS)
#define RC_BIT_MODEL_TOTAL_BITS 11
#define RC_BIT_MODEL_TOTAL (1 << RC_BIT_MODEL_TOTAL_BITS)
#define RC_MOVE_BITS 5

#define POS_STATES_MAX (1 << 4)

enum lzma_state {
	STATE_LIT_LIT,
	STATE_MATCH_LIT_LIT,
	STATE_REP_LIT_LIT,
	STATE_SHORTREP_LIT_LIT,
	STATE_MATCH_LIT,
	STATE_REP_LIT,
	STATE_SHORTREP_LIT,
	STATE_LIT_MATCH,
	STATE_LIT_LONGREP,
	STATE_LIT_SHORTREP,
	STATE_NONLIT_MATCH,
	STATE_NONLIT_REP
};

#define STATES 12

#define LIT_STATES 7

static inline void lzma_state_literal(enum lzma_state *state)
{
	if (*state <= STATE_SHORTREP_LIT_LIT)
		*state = STATE_LIT_LIT;
	else if (*state <= STATE_LIT_SHORTREP)
		*state -= 3;
	else
		*state -= 6;
}

static inline void lzma_state_match(enum lzma_state *state)
{
	*state = *state < LIT_STATES ? STATE_LIT_MATCH : STATE_NONLIT_MATCH;
}

static inline void lzma_state_long_rep(enum lzma_state *state)
{
	*state = *state < LIT_STATES ? STATE_LIT_LONGREP : STATE_NONLIT_REP;
}

static inline void lzma_state_short_rep(enum lzma_state *state)
{
	*state = *state < LIT_STATES ? STATE_LIT_SHORTREP : STATE_NONLIT_REP;
}

static inline bool lzma_state_is_literal(enum lzma_state state)
{
	return state < LIT_STATES;
}

#define LITERAL_CODER_SIZE 0x300

#define LITERAL_CODERS_MAX (1 << 4)

#define MATCH_LEN_MIN 2

#define LEN_LOW_BITS 3
#define LEN_LOW_SYMBOLS (1 << LEN_LOW_BITS)
#define LEN_MID_BITS 3
#define LEN_MID_SYMBOLS (1 << LEN_MID_BITS)
#define LEN_HIGH_BITS 8
#define LEN_HIGH_SYMBOLS (1 << LEN_HIGH_BITS)
#define LEN_SYMBOLS (LEN_LOW_SYMBOLS + LEN_MID_SYMBOLS + LEN_HIGH_SYMBOLS)

#define MATCH_LEN_MAX (MATCH_LEN_MIN + LEN_SYMBOLS - 1)

#define DIST_STATES 4

static inline size_t lzma_get_dist_state(size_t len)
{
	return len < DIST_STATES + MATCH_LEN_MIN
			? len - MATCH_LEN_MIN : DIST_STATES - 1;
}

#define DIST_SLOT_BITS 6
#define DIST_SLOTS (1 << DIST_SLOT_BITS)

#define DIST_MODEL_START 4

#define DIST_MODEL_END 14

#define FULL_DISTANCES_BITS (DIST_MODEL_END / 2)
#define FULL_DISTANCES (1 << FULL_DISTANCES_BITS)

#define ALIGN_BITS 4
#define ALIGN_SIZE (1 << ALIGN_BITS)
#define ALIGN_MASK (ALIGN_SIZE - 1)

#define PROBS_TOTAL (1846 + LITERAL_CODERS_MAX * LITERAL_CODER_SIZE)

#define REPS 4

#endif
