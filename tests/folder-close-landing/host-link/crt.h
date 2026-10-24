/* SPDX-License-Identifier: AGPL-3.0-or-later */
#ifndef HOST_LINK_CRT_H
#define HOST_LINK_CRT_H

/* Defined by crt.c, referenced by host-shim/cassert. Never define it in the shim:
 * crt.obj already provides the external definition and a second one collides at
 * link time. The line number is accepted so a failing assert can name itself,
 * but crt.c deliberately only prints a fixed banner today. */
#ifdef __cplusplus
extern "C"
#endif
void test_fail(int line);

#endif /* HOST_LINK_CRT_H */
