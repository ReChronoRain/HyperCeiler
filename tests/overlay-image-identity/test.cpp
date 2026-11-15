/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/tweaks/image.h"
#include "nativehook/native_image.h"
#include <elf.h>
#include <fcntl.h>
#include <fstream>
#include <iostream>
#include <sys/mman.h>
#include <unistd.h>

int main(int argc, char **argv) {
    if (argc != 3)
        return 2;
    const bool expected = std::string(argv[2]) == "ready";
    const std::string path = argv[1];
    for (const char *name : {"libapp.so", "libapp_launcher.so"}) {
        auto entry = nhk::zip_stored_entry(path, std::string("lib/arm64-v8a/") + name);
        if (!entry)
            return 2;
        int fd = open(path.c_str(), O_RDONLY | O_CLOEXEC);
        if (fd < 0)
            return 2;
        std::array<std::byte, 8192> header{};
        if (pread(fd, header.data(), header.size(), entry->first) != ssize_t(header.size()))
            return 2;
        auto segments = nhk::elf::parse_program_segments(header, true);
        if (!segments)
            return 2;
        uint64_t size = 0;
        for (const auto &s : *segments)
            if (s.type == PT_LOAD)
                size = std::max(size, nhk::page_up(s.vaddr + s.memsz));
        void *allocation = mmap(nullptr, size, PROT_NONE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (allocation == MAP_FAILED)
            return 2;
        uintptr_t base = reinterpret_cast<uintptr_t>(allocation);
        for (const auto &s : *segments) {
            if (s.type != PT_LOAD || !s.filesz)
                continue;
            uint64_t offset = nhk::page_down(s.offset);
            size_t bytes = nhk::page_up(s.offset + s.filesz) - offset;
            int protection = (s.flags & PF_R ? PROT_READ : 0) | (s.flags & PF_W ? PROT_WRITE : 0) |
                             (s.flags & PF_X ? PROT_EXEC : 0);
            if (mmap(reinterpret_cast<void *>(base + nhk::page_down(s.vaddr)), bytes, protection,
                     MAP_PRIVATE | MAP_FIXED, fd, entry->first + offset) == MAP_FAILED)
                return 2;
        }
        close(fd);
        home_layout_engine::Image image{};
        home_layout_engine::ImageDiscoveryDiagnostic diagnostic;
        bool ready = home_layout_engine::FindImageFromMappedApks(name, &image, &diagnostic);
        std::cout << name << " ready=" << ready << " stage=" << diagnostic.reason << '\n';
        if (ready != expected || (ready && image.base != base))
            return 1;
        if (ready) {
            std::array<std::byte, 64> bytes{};
            if (!home_layout_engine::ReadImageFile(image, 0, bytes.data(), bytes.size()))
                return 1;
            auto invalid = image;
            ++invalid.sourceInode;
            if (home_layout_engine::ReadImageFile(invalid, 0, bytes.data(), bytes.size()))
                return 1;
            invalid = image;
            invalid.sourceDevice = UINT64_MAX;
            if (home_layout_engine::ReadImageFile(invalid, 0, bytes.data(), bytes.size()))
                return 1;
            if (mprotect(allocation, nhk::host_page_size(), PROT_READ | PROT_WRITE))
                return 2;
            *static_cast<char *>(allocation) = 0;
            mprotect(allocation, nhk::host_page_size(), PROT_READ);
            if (home_layout_engine::FindImageFromMappedApks(name, &invalid, &diagnostic) ||
                std::string(diagnostic.reason) != "image-live-header")
                return 1;
            std::cout << "wrong inode, wrong device and changed live header rejected\n";
        }
        munmap(allocation, size);
    }
    return 0;
}
