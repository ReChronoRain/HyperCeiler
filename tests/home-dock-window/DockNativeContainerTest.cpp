/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Container-backed AOT discovery regression checks.
//
// HYOS maps the launcher's `libapp.so` out of the application APK without
// extracting it, so `/proc/self/maps` reports the APK path for several unrelated
// libraries at once. These checks cover the inventory parsing, the ZIP entry
// lookup that recovers the image's container view, the range attribution that
// keeps sibling libraries out of the inventory, and - when a real container is
// supplied as an argument - the complete path from container offsets to the
// semantically resolved motion targets.
#include "../../app/src/main/cpp/targets/home/dock_native_resolver.h"
#include "../../app/src/main/cpp/targets/home/dock_native_runtime.h"

#include <cassert>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <iterator>
#include <sstream>
#include <string>
#include <vector>

namespace {

using dock_motion::ExecutableMapping;
using dock_motion::FileMapping;
using dock_motion::LibappContainer;

std::string line(uintptr_t begin, uintptr_t end, const char *permissions,
    uint64_t file_offset, uint64_t inode, const std::string &path) {
    std::ostringstream text;
    text << std::hex << begin << '-' << end << ' ' << permissions << ' '
         << file_offset << " fd:01 " << std::dec << inode << "  " << path << '\n';
    return text.str();
}

std::vector<FileMapping> inventory(const std::string &text) {
    std::istringstream maps{text};
    return dock_motion::parse_file_mappings(maps);
}

constexpr uint64_t kView = 0x15e4000;
constexpr uint64_t kImageBytes = 0x1b1f8b0;

/// A container-backed inventory. The writable executable range is the only
/// difference between the healthy and the unverifiable snapshot.
std::string apk_inventory(bool writable_executable = false) {
    const std::string apk = "/data/app/~~x==/com.miui.home-y==/base.apk";
    std::string text =
        line(0x7000000000, 0x70007f4000, "r--p", kView, 4242, apk)
        + line(0x7010000000, 0x70112778b0, "r-xp", kView + 0x800000, 4242, apk)
        + line(0x7030000000, 0x7030001000, "rw-p", kView + 0x1a80000, 4242, apk)
        // Sibling images of the same file must never contribute code.
        + line(0x7100000000, 0x7100100000, "r-xp", 0x700000, 4242, apk)
        + line(0x7200000000, 0x7200100000, "r-xp", 0x32c0000, 4242, apk);
    if (writable_executable) {
        text += line(0x7300000000, 0x7300100000, "rwxp", kView + 0x40, 4242, apk);
    }
    // Anonymous and unowned paths are ignored.
    text += "7000000000-7000100000 r-xp 00000000 00:00 0  [anon:linker_alloc]\n"
        + line(0x7400000000, 0x7400100000, "r-xp", 0, 77, "/system/lib64/libc.so");
    return text;
}

void test_inventory_flags() {
    const auto entries = inventory(apk_inventory(true));
    assert(entries.size() == 7);
    assert(!entries[0].executable);
    assert(entries[1].executable);
    assert(!entries[1].writable);
    assert(entries[2].writable);
    assert(entries[5].writable && entries[5].executable);
    // The anonymous mapping has no path at all.
    for (const auto &entry : entries) assert(!entry.path.empty() && entry.path[0] == '/');
}

void test_container_attribution() {
    std::vector<LibappContainer> owned{
        {"/data/app/~~x==/com.miui.home-y==/base.apk", kView, kView + kImageBytes}};

    // Only the AOT image's own executable range survives. The read-only and
    // writable segments are not executable, and the two sibling images of the
    // same container fall outside the image view.
    const auto mappings =
        dock_motion::executable_libapp_mappings(inventory(apk_inventory()), owned);
    assert(mappings.size() == 1);
    assert(mappings[0].begin == 0x7010000000);
    assert(mappings[0].file_offset == 0x800000);
    assert(mappings[0].inode == 4242);

    // A writable executable range inside the owned view is an unverifiable
    // snapshot and must discard the whole inventory, not just that range.
    assert(dock_motion::executable_libapp_mappings(
        inventory(apk_inventory(true)), owned).empty());

    // Two images claiming one range is ambiguous and must fail closed.
    std::vector<LibappContainer> overlapping{
        {"/data/app/~~x==/com.miui.home-y==/base.apk", kView, kView + kImageBytes},
        {"/data/app/~~x==/com.miui.home-y==/base.apk", kView + 0x1000, kView + kImageBytes},
    };
    assert(dock_motion::executable_libapp_mappings(
        inventory(apk_inventory()), overlapping).empty());

    // A zero inode is a malformed snapshot for an owned range only.
    const std::string dangling = line(0x7500000000, 0x7500100000, "r-xp",
        kView + 0x800000, 0, "/data/app/~~x==/com.miui.home-y==/base.apk");
    assert(dock_motion::executable_libapp_mappings(inventory(dangling), owned).empty());
}

void test_embedded_image_bounds() {
    // A minimal ELF64 header with two PT_LOAD segments, one of them executable.
    std::vector<std::byte> bytes(64 + 2 * 56, std::byte{0});
    const std::byte elf[] = {std::byte{0x7f}, std::byte{'E'}, std::byte{'L'}, std::byte{'F'}};
    std::copy(std::begin(elf), std::end(elf), bytes.begin());
    bytes[4] = std::byte{2};
    bytes[5] = std::byte{1};
    const auto put16 = [&](size_t at, uint16_t value) {
        bytes[at] = std::byte{static_cast<unsigned char>(value & 0xff)};
        bytes[at + 1] = std::byte{static_cast<unsigned char>((value >> 8) & 0xff)};
    };
    const auto put32 = [&](size_t at, uint32_t value) {
        put16(at, static_cast<uint16_t>(value & 0xffff));
        put16(at + 2, static_cast<uint16_t>(value >> 16));
    };
    const auto put64 = [&](size_t at, uint64_t value) {
        put32(at, static_cast<uint32_t>(value));
        put32(at + 4, static_cast<uint32_t>(value >> 32));
    };
    put64(32, 64);   // e_phoff
    put16(54, 56);   // e_phentsize
    put16(56, 2);    // e_phnum
    put32(64, 1);    // PT_LOAD
    put32(64 + 4, 4); // r--
    put64(64 + 8, 0);
    put64(64 + 32, 0x4000);
    put32(120, 1);    // PT_LOAD
    put32(120 + 4, 5); // r-x
    put64(120 + 8, 0x4000);
    put64(120 + 32, 0x9000);

    const auto image = dock_motion::parse_embedded_image(bytes, kView);
    assert(image && image->has_executable);
    assert(image->view_begin == kView);
    assert(image->view_end == kView + 0xd000);

    // A header whose table is truncated, an unaligned entry size and a
    // non-executable image all fail closed.
    assert(!dock_motion::parse_embedded_image(
        std::span(bytes).first(64), kView));
    put16(54, 32);
    assert(!dock_motion::parse_embedded_image(bytes, kView));
    put16(54, 56);
    put32(120 + 4, 4);
    assert(!dock_motion::parse_embedded_image(bytes, kView));
    put32(120 + 4, 5);

    std::vector<std::byte> other = bytes;
    other[2] = std::byte{'x'};
    assert(!dock_motion::parse_embedded_image(other, kView));
}

/// Build a minimal single-entry ZIP so the container lookup is always exercised.
std::vector<std::byte> build_zip(const std::string &name, uint32_t size,
    uint16_t method, uint16_t flags, bool duplicate) {
    const auto put16 = [](std::vector<std::byte> &out, uint16_t value) {
        out.push_back(std::byte{static_cast<unsigned char>(value & 0xff)});
        out.push_back(std::byte{static_cast<unsigned char>((value >> 8) & 0xff)});
    };
    const auto put32 = [&put16](std::vector<std::byte> &out, uint32_t value) {
        put16(out, static_cast<uint16_t>(value & 0xffff));
        put16(out, static_cast<uint16_t>(value >> 16));
    };
    std::vector<std::byte> out;
    const auto append_name = [](std::vector<std::byte> &target, const std::string &value) {
        for (const char byte : value) {
            target.push_back(std::byte{static_cast<unsigned char>(byte)});
        }
    };
    std::vector<std::pair<uint32_t, uint32_t>> records;
    const int copies = duplicate ? 2 : 1;
    for (int copy = 0; copy < copies; ++copy) {
        const uint32_t local_offset = static_cast<uint32_t>(out.size());
        put32(out, 0x04034b50U);
        put16(out, 20);
        put16(out, flags);
        put16(out, method);
        put16(out, 0);
        put16(out, 0);
        put32(out, 0);
        put32(out, size);
        put32(out, size);
        put16(out, static_cast<uint16_t>(name.size()));
        put16(out, 0);
        append_name(out, name);
        for (uint32_t index = 0; index < size; ++index) out.push_back(std::byte{0x41});
        records.emplace_back(local_offset, size);
    }
    const uint32_t directory_offset = static_cast<uint32_t>(out.size());
    for (const auto &[local_offset, size] : records) {
        put32(out, 0x02014b50U);
        put16(out, 20);
        put16(out, 20);
        put16(out, flags);
        put16(out, method);
        put16(out, 0);
        put16(out, 0);
        put32(out, 0);
        put32(out, size);
        put32(out, size);
        put16(out, static_cast<uint16_t>(name.size()));
        put16(out, 0);
        put16(out, 0);
        put16(out, 0);
        put16(out, 0);
        put32(out, 0);
        put32(out, local_offset);
        append_name(out, name);
    }
    const uint32_t directory_bytes = static_cast<uint32_t>(out.size()) - directory_offset;
    put32(out, 0x06054b50U);
    put16(out, 0);
    put16(out, 0);
    put16(out, static_cast<uint16_t>(records.size()));
    put16(out, static_cast<uint16_t>(records.size()));
    put32(out, directory_bytes);
    put32(out, directory_offset);
    put16(out, 0);
    return out;
}

void write_file(const std::filesystem::path &path, const std::vector<std::byte> &bytes) {
    std::ofstream file(path, std::ios::binary | std::ios::trunc);
    file.write(reinterpret_cast<const char *>(bytes.data()),
        static_cast<std::streamsize>(bytes.size()));
}

void test_zip_entry_lookup() {
    const std::filesystem::path directory =
        std::filesystem::temp_directory_path() / "hyperceiler-container-test";
    std::filesystem::create_directories(directory);
    const std::string name = "lib/arm64-v8a/libapp.so";
    const auto expect_offset = static_cast<uint64_t>(30 + name.size());

    const auto good = directory / "good.apk";
    write_file(good, build_zip(name, 128, 0, 0, false));
    const auto found = dock_motion::zip_stored_libapp(good.string());
    assert(found && found->first == expect_offset && found->second == 128);

    // A compressed entry, a data-descriptor entry and a duplicated image are all
    // unverifiable or ambiguous and must be refused rather than guessed at.
    const auto compressed = directory / "compressed.apk";
    write_file(compressed, build_zip(name, 128, 8, 0, false));
    assert(!dock_motion::zip_stored_libapp(compressed.string()));

    const auto descriptor = directory / "descriptor.apk";
    write_file(descriptor, build_zip(name, 128, 0, 0x08, false));
    assert(!dock_motion::zip_stored_libapp(descriptor.string()));

    const auto duplicated = directory / "duplicated.apk";
    write_file(duplicated, build_zip(name, 64, 0, 0, true));
    assert(!dock_motion::zip_stored_libapp(duplicated.string()));

    // A container without the image, and a file that is not a ZIP at all.
    const auto unrelated = directory / "unrelated.apk";
    write_file(unrelated, build_zip("lib/arm64-v8a/libother.so", 64, 0, 0, false));
    assert(!dock_motion::zip_stored_libapp(unrelated.string()));
    const auto text = directory / "text.bin";
    std::vector<std::byte> filler(4096, std::byte{0x7f});
    write_file(text, filler);
    assert(!dock_motion::zip_stored_libapp(text.string()));
    assert(!dock_motion::zip_stored_libapp((directory / "absent.apk").string()));

    std::filesystem::remove_all(directory);
}

/// Resolve the motion graph straight out of a real container when one is supplied.
void test_real_container(const std::string &path) {
    const auto entry = dock_motion::zip_stored_libapp(path);
    assert(entry);
    std::ifstream file(path, std::ios::binary);
    assert(file.is_open());
    std::vector<std::byte> elf(static_cast<size_t>(entry->second));
    file.seekg(static_cast<std::streamoff>(entry->first));
    file.read(reinterpret_cast<char *>(elf.data()),
        static_cast<std::streamsize>(elf.size()));
    assert(static_cast<uint64_t>(file.gcount()) == entry->second);

    // Addresses here are synthetic: the container arithmetic, not a launcher
    // address, is what this check validates.
    const auto put16 = [&](size_t at, uint16_t value) {
        elf[at] = std::byte{static_cast<unsigned char>(value & 0xff)};
        elf[at + 1] = std::byte{static_cast<unsigned char>((value >> 8) & 0xff)};
    };
    const auto read16 = [&](size_t at) {
        return static_cast<uint16_t>(std::to_integer<uint16_t>(elf[at]))
            | static_cast<uint16_t>(std::to_integer<uint16_t>(elf[at + 1]) << 8);
    };
    const auto read32 = [&](size_t at) {
        return static_cast<uint32_t>(read16(at))
            | (static_cast<uint32_t>(read16(at + 2)) << 16);
    };
    const auto read64 = [&](size_t at) {
        return static_cast<uint64_t>(read32(at))
            | (static_cast<uint64_t>(read32(at + 4)) << 32);
    };
    (void)put16;
    assert(read32(0) == 0x464c457fU);

    const uint64_t program_offset = read64(32);
    const uint16_t program_entry = read16(54);
    const uint16_t program_count = read16(56);
    constexpr uintptr_t bias = 0x7a00000000ULL;
    std::vector<std::vector<uint32_t>> storage;
    std::vector<dock_motion::CodeRange> ranges;
    for (uint16_t index = 0; index < program_count; ++index) {
        const size_t header = static_cast<size_t>(program_offset)
            + static_cast<size_t>(index) * program_entry;
        if (read32(header) != 1 || (read32(header + 4) & 5) != 5) continue;
        const uint64_t offset = read64(header + 8);
        const uint64_t address = read64(header + 16);
        const uint64_t size = read64(header + 32);
        assert(size % 4 == 0 && offset + size <= elf.size());
        storage.emplace_back(size / 4);
        std::memcpy(storage.back().data(), elf.data() + offset, size);
        ranges.push_back({bias + address, storage.back()});
    }
    const auto resolution = dock_motion::resolve(ranges);
    assert(resolution);
    assert(resolution->layout.tagged_header_offset < 0);
    assert(resolution->layout.class_id_mask != 0);
    std::cout << path << " container=" << std::hex << entry->first
        << " scale=" << resolution->scale << " animate=" << resolution->animate
        << " set=" << resolution->set << std::dec
        << " CID=" << resolution->layout.params_class_id << '\n';
}

} // namespace

int main(int argc, char **argv) {
    test_inventory_flags();
    test_container_attribution();
    test_embedded_image_bounds();
    test_zip_entry_lookup();
    for (int arg = 1; arg < argc; ++arg) test_real_container(argv[arg]);
    std::cout << "Container discovery tests passed\n";
}
