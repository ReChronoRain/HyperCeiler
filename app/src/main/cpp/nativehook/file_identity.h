/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include <cstdint>
#include <cstdio>
#include <fstream>
#if defined(__linux__)
#include <linux/stat.h>
#include <fcntl.h>
#include <sys/syscall.h>
#include <sys/sysmacros.h>
#include <sys/vfs.h>
#include <unistd.h>
#endif
#include <string>
#include <sys/stat.h>

namespace nhk {

inline bool mapped_device_matches(int fd, uint64_t device, const struct stat &st) {
    if (uint64_t(st.st_dev) == device)
        return true;

#if defined(__linux__) && defined(__NR_statx) && defined(STATX_MNT_ID)
    // OverlayFS can expose a file's lower-layer device through stat while maps
    // reports the overlay superblock. Resolve that specific alias by mount ID.
    struct statfs fs{};
    struct statx sx{};
    if (fstatfs(fd, &fs) != 0 || fs.f_type != 0x794c7630 ||
        syscall(__NR_statx, fd, "", AT_EMPTY_PATH, STATX_MNT_ID, &sx) != 0 || !(sx.stx_mask & STATX_MNT_ID))
        return false;

    std::ifstream mounts("/proc/self/mountinfo");
    std::string line;
    size_t bytes = 0;
    while (std::getline(mounts, line)) {
        bytes += line.size() + 1;
        if (bytes > 4 * 1024 * 1024)
            return false;
        unsigned long long id = 0, parent = 0;
        unsigned major_id = 0, minor_id = 0;
        if (std::sscanf(line.c_str(), "%llu %llu %u:%u", &id, &parent, &major_id, &minor_id) != 4)
            return false;
        if (id == sx.stx_mnt_id)
            return uint64_t(makedev(major_id, minor_id)) == device;
    }
#else
    (void)fd;
#endif
    return false;
}

} // namespace nhk
