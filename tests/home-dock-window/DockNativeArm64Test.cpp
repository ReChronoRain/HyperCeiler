/* SPDX-License-Identifier: AGPL-3.0-or-later */
#if defined(__GNUC__) && !defined(__clang__)
#pragma GCC diagnostic ignored "-Wunused-member"
#endif
#include <atomic>
#include <bit>
#include <cassert>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <initializer_list>
#include <sys/eventfd.h>
#include <unistd.h>
#include "../../app/src/main/cpp/targets/home/dock_native_layout.h"
#include "../../app/src/main/cpp/targets/home/dock_native_resolver.h"

extern "C" {
void dock_test_trampoline();
void dock_test_invoke(void (*)(), void *, uint64_t *);
void dock_test_invoke_unlock(void (*)(), void *, double, uint64_t *);
alignas(8) std::atomic<uint64_t> dock_motion_value{0x3ff0000000000000ULL};
alignas(8) std::atomic<uint64_t> dock_motion_entry_hits{0};
alignas(8) std::atomic<uint64_t> dock_motion_publish_hits{0};
alignas(8) std::atomic<uint64_t> dock_auto_aim_value{0x3ff0000000000003ULL};
alignas(8) std::atomic<uint64_t> dock_auto_aim_entry_hits{0};
alignas(8) std::atomic<uint64_t> dock_auto_aim_publish_hits{0};
alignas(8) std::atomic<uint64_t> dock_motion_active_callbacks{0};
alignas(4) std::atomic<uint32_t> dock_motion_subscribed{0};
int dock_motion_event = -1;
extern const uint64_t dock_motion_one = 1;
#define DEFINE_TEST_BANK(bank) \
    void *dock_motion_scale_original_##bank = reinterpret_cast<void *>(dock_test_trampoline); \
    void *dock_motion_anim_original_##bank = reinterpret_cast<void *>(dock_test_trampoline); \
    void *dock_motion_set_original_##bank = reinterpret_cast<void *>(dock_test_trampoline); \
    uint32_t dock_params_class_id_##bank = 0; \
    uint32_t dock_double_class_id_##bank = 0; \
    int32_t dock_tagged_header_offset_##bank = 0; \
    uint32_t dock_class_id_shift_##bank = 0; \
    uint32_t dock_class_id_mask_##bank = 0; \
    uint32_t dock_alpha_offset_##bank = 0; \
    uint32_t dock_scale_offset_##bank = 0; \
    uint32_t dock_surface_offset_##bank = 0; \
    uint32_t dock_recents_offset_##bank = 0; \
    uint32_t dock_double_value_offset_##bank = 0; \
    uint32_t dock_false_from_null_##bank = 0; \
    uint32_t dock_unlock_state_widget_offset_##bank = 0; \
    uint32_t dock_unlock_widget_cell_offset_##bank = 0; \
    uint32_t dock_unlock_cell_container_offset_##bank = 0; \
    uintptr_t dock_unlock_call_return_##bank = 0; \
    int64_t dock_unlock_hotseat_container_0_##bank = 0; \
    int64_t dock_unlock_hotseat_container_1_##bank = 0; \
    int64_t dock_unlock_hotseat_container_2_##bank = 0; \
    int64_t dock_unlock_hotseat_container_3_##bank = 0; \
    int64_t dock_unlock_hotseat_container_4_##bank = 0; \
    void *dock_motion_unlock_scale_original_##bank = \
        reinterpret_cast<void *>(dock_test_trampoline);
DOCK_MOTION_BANKS(DEFINE_TEST_BANK)
#undef DEFINE_TEST_BANK
uint64_t dock_test_null_object = 0;
uint64_t dock_test_heap_base = 0;
}

struct alignas(16) TestBool { uint64_t header; uint64_t payload; };
TestBool false_object{};
TestBool true_object{};
TestBool not_bool_object{};
uint32_t false_value = 0;
uint32_t true_value = 0;

#define PUBLISH_BANK(bank) \
    void publish##bank(const dock_motion::Layout &layout) { \
        dock_params_class_id_##bank = layout.params_class_id; \
        dock_double_class_id_##bank = layout.double_class_id; \
        dock_tagged_header_offset_##bank = layout.tagged_header_offset; \
        dock_class_id_shift_##bank = layout.class_id_shift; \
        dock_class_id_mask_##bank = layout.class_id_mask; \
        dock_alpha_offset_##bank = layout.alpha_offset; \
        dock_scale_offset_##bank = layout.scale_offset; \
        dock_surface_offset_##bank = layout.surface_offset; \
        dock_recents_offset_##bank = layout.recents_offset; \
        dock_double_value_offset_##bank = layout.double_value_offset; \
        dock_false_from_null_##bank = layout.false_from_null; \
    }
PUBLISH_BANK(0)
PUBLISH_BANK(1)
#undef PUBLISH_BANK

#define PUBLISH_UNLOCK_BANK(bank) \
    void publishUnlock##bank(const dock_motion::UnlockLayout &layout) { \
        dock_unlock_state_widget_offset_##bank = layout.state_widget_offset; \
        dock_unlock_widget_cell_offset_##bank = layout.widget_cell_offset; \
        dock_unlock_cell_container_offset_##bank = layout.cell_container_offset; \
        dock_unlock_hotseat_container_0_##bank = layout.hotseat_containers[0]; \
        dock_unlock_hotseat_container_1_##bank = layout.hotseat_containers[1]; \
        dock_unlock_hotseat_container_2_##bank = layout.hotseat_containers[2]; \
        dock_unlock_hotseat_container_3_##bank = layout.hotseat_containers[3]; \
        dock_unlock_hotseat_container_4_##bank = layout.hotseat_containers[4]; \
    }
PUBLISH_UNLOCK_BANK(0)
PUBLISH_UNLOCK_BANK(1)
#undef PUBLISH_UNLOCK_BANK

struct alignas(16) Fixture {
    uint64_t header = uint64_t(1777) << 12;
    double alpha = 1;
    double scale_x = .95;
    double scale_y = .95;
    double damping = 1;
    double response = .5;
    uint32_t surface = true_value;
    uint32_t recents = false_value;
    uint32_t reason = 0;
    uint32_t anim = false_value;
};
static_assert(sizeof(Fixture) == 64 && offsetof(Fixture, recents) == 0x34);

void invoke(void (*entry)(), void *tagged) {
    uint64_t registers[18]{};
    dock_test_invoke(entry, tagged, registers);
    for (int i = 0; i < 15; ++i) {
        const uint64_t expected = i == 2 ? reinterpret_cast<uint64_t>(tagged) : 100 + i;
        assert(registers[i] == expected);
    }
    assert(registers[15] == registers[17]);
    assert(registers[16] == 0xa0000000);
    assert(dock_motion_active_callbacks.load() == 0);
}
void invoke_unlock(void (*entry)(), void *tagged, double projected_scale) {
    uint64_t registers[19]{};
    dock_test_invoke_unlock(entry, tagged, projected_scale, registers);
    for (int i = 0; i < 15; ++i) {
        const uint64_t expected = i == 1 ? reinterpret_cast<uint64_t>(tagged) : 100 + i;
        assert(registers[i] == expected);
    }
    assert(registers[15] == registers[17]);
    assert(registers[16] == 0xa0000000);
    assert(std::bit_cast<double>(registers[18]) == projected_scale);
    assert(dock_motion_active_callbacks.load() == 0);
}
uint32_t scene() { return dock_motion_value.load() & 3; }
double scale() { return std::bit_cast<double>(dock_motion_value.load() & ~3ULL); }
uint32_t aim_scene() { return dock_auto_aim_value.load() & 3; }
double aim_scale() { return std::bit_cast<double>(dock_auto_aim_value.load() & ~3ULL); }
template<class T> void invoke_fixture(void (*entry)(), T &fixture) {
    invoke(entry, reinterpret_cast<char *>(&fixture) + 1);
}
int main() {
    dock_motion_event = eventfd(0, EFD_NONBLOCK | EFD_CLOEXEC);
    assert(dock_motion_event >= 0);
    const uint64_t initial_entries = dock_motion_entry_hits.load();
    const uint64_t initial_publishes = dock_motion_publish_hits.load();
    const uint64_t initial_aim_entries = dock_auto_aim_entry_hits.load();
    const uint64_t initial_aim_publishes = dock_auto_aim_publish_hits.load();
    constexpr uint32_t bool_cid = 91;
    false_object.header = uint64_t(bool_cid) << 12;
    true_object.header = uint64_t(bool_cid) << 12;
    not_bool_object.header = 123ULL << 12;
    const uintptr_t false_tagged = reinterpret_cast<uintptr_t>(&false_object) + 1;
    const uintptr_t true_tagged = reinterpret_cast<uintptr_t>(&true_object) + 1;
    dock_test_heap_base = false_tagged >> 32;
    assert((true_tagged >> 32) == dock_test_heap_base);
    false_value = static_cast<uint32_t>(false_tagged);
    true_value = static_cast<uint32_t>(true_tagged);
    dock_test_null_object = false_tagged - 0x20;
    publish0({1777, 62, -1, 12, 0xfffff,
        static_cast<uint32_t>(offsetof(Fixture, alpha) - 1),
        static_cast<uint32_t>(offsetof(Fixture, scale_x) - 1),
        static_cast<uint32_t>(offsetof(Fixture, surface) - 1),
        static_cast<uint32_t>(offsetof(Fixture, recents) - 1), 7, 0x20});
    for (uint32_t subscribed : {0u, 1u}) {
        dock_motion_subscribed.store(subscribed);
        Fixture value;
        const Fixture original = value;
        const uint64_t entries_before_valid = dock_motion_entry_hits.load();
        const uint64_t publishes_before_valid = dock_motion_publish_hits.load();
        invoke_fixture(dock_motion_anim_entry_0, value);
        assert(scene() == 1);
        assert(dock_motion_entry_hits.load() == entries_before_valid + 1);
        assert(dock_motion_publish_hits.load() == publishes_before_valid + 1);
        assert(std::memcmp(&original, &value, sizeof(value)) == 0);
        invoke_fixture(dock_motion_set_entry_0, value);
        assert(scene() == 1 && std::abs(scale() - .95) < 1e-12);
        struct alignas(16) { uint64_t header; double value; } boxed{62ULL << 12, .945};
        invoke_fixture(dock_motion_scale_entry_0, boxed);
        assert(boxed.header == 62ULL << 12);
        assert(scene() == 1 && std::abs(scale() - .945) < 1e-12);
        const uint64_t publishes_after_scale = dock_motion_publish_hits.load();
        invoke_fixture(dock_motion_scale_entry_0, boxed); // unchanged fast path
        assert(dock_motion_publish_hits.load() == publishes_after_scale);
        value.scale_x = 1;
        invoke_fixture(dock_motion_anim_entry_0, value);
        assert(scene() == 2); // Also clear recents on home's default-true flag.
        value.recents = true_value;
        invoke_fixture(dock_motion_set_entry_0, value);
        assert(scene() == 2 && scale() == 1);
        value.scale_x = .8;
        value.recents = false_value;
        invoke_fixture(dock_motion_anim_entry_0, value);
        assert(scene() == 0);
        value.scale_x = .95;
        value.alpha = 0;
        const uint64_t packed_before_invalid_alpha = dock_motion_value.load();
        const uint64_t publishes_before_invalid_alpha = dock_motion_publish_hits.load();
        const uint64_t entries_before_invalid_alpha = dock_motion_entry_hits.load();
        invoke_fixture(dock_motion_anim_entry_0, value);
        assert(dock_motion_value.load() == packed_before_invalid_alpha);
        assert(dock_motion_entry_hits.load() == entries_before_invalid_alpha + 1);
        assert(dock_motion_publish_hits.load() == publishes_before_invalid_alpha);
        value.alpha = 1;
        value.surface = false_value;
        invoke_fixture(dock_motion_anim_entry_0, value);
        assert(scene() == 1); // OS4 uses either bool polarity for equivalent recents paths.
        value.recents = true_value;
        invoke_fixture(dock_motion_anim_entry_0, value);
        assert(scene() == 1);
        const uintptr_t invalid_tagged = reinterpret_cast<uintptr_t>(&not_bool_object) + 1;
        assert((invalid_tagged >> 32) == dock_test_heap_base);
        value.surface = static_cast<uint32_t>(invalid_tagged);
        const uint64_t packed_before_invalid_bool = dock_motion_value.load();
        const uint64_t publishes_before_invalid_bool = dock_motion_publish_hits.load();
        invoke_fixture(dock_motion_anim_entry_0, value);
        assert(dock_motion_value.load() == packed_before_invalid_bool);
        assert(dock_motion_publish_hits.load() == publishes_before_invalid_bool);
        value.surface = true_value;
        value.recents = false_value;
        value.header = 45ULL << 12;
        const uint64_t packed_before_invalid_cid = dock_motion_value.load();
        const uint64_t publishes_before_invalid_cid = dock_motion_publish_hits.load();
        invoke_fixture(dock_motion_set_entry_0, value);
        assert(dock_motion_value.load() == packed_before_invalid_cid);
        assert(dock_motion_publish_hits.load() == publishes_before_invalid_cid);
        const uint64_t publishes_before_smi = dock_motion_publish_hits.load();
        invoke(dock_motion_set_entry_0, reinterpret_cast<void *>(2)); // Smi must not be dereferenced
        invoke(dock_motion_scale_entry_0, reinterpret_cast<void *>(2));
        assert(dock_motion_publish_hits.load() == publishes_before_smi);
        eventfd_t notifications = 0;
        const auto received = eventfd_read(dock_motion_event, &notifications);
        if (subscribed) assert(received == 0 && notifications > 0);
        else assert(received == -1);
    }

    // AUTO_AIM is a separate ABI: x1 is the _UnlockWidgetState receiver and d0 is the exact
    // conversionValueFrom3DTo2D result. The callback follows only the dynamically published
    // state -> widget -> CellLocationInfo chain and accepts the five discovered Hotseat IDs.
    struct alignas(16) AimState {
        uint64_t header{};
        uint32_t padding{};
        uint32_t widget{};
    } aim_state;
    struct alignas(16) AimWidget {
        uint64_t header{};
        uint32_t padding[3]{};
        uint32_t cell{};
    } aim_widget;
    struct alignas(16) AimCell {
        uint64_t header{};
        uint64_t padding[8]{};
        int64_t container{};
    } aim_cell;
    static_assert(offsetof(AimState, widget) == 12);
    static_assert(offsetof(AimWidget, cell) == 20);
    static_assert(offsetof(AimCell, container) == 72);
    constexpr uintptr_t aim_tag = 1;
    const uintptr_t aim_state_tagged = reinterpret_cast<uintptr_t>(&aim_state) + aim_tag;
    const uintptr_t aim_widget_tagged = reinterpret_cast<uintptr_t>(&aim_widget) + aim_tag;
    const uintptr_t aim_cell_tagged = reinterpret_cast<uintptr_t>(&aim_cell) + aim_tag;
    assert((aim_widget_tagged >> 32) == (aim_state_tagged >> 32));
    assert((aim_cell_tagged >> 32) == (aim_state_tagged >> 32));
    dock_test_heap_base = aim_state_tagged >> 32;
    dock_test_null_object = aim_state_tagged - 0x80;
    aim_state.widget = static_cast<uint32_t>(aim_widget_tagged);
    aim_widget.cell = static_cast<uint32_t>(aim_cell_tagged);
    constexpr std::array<int64_t, 5> hotseat{-101, -102, -103, -105, -106};
    publishUnlock0({static_cast<uint32_t>(offsetof(AimState, widget) - aim_tag),
        static_cast<uint32_t>(offsetof(AimWidget, cell) - aim_tag),
        static_cast<uint32_t>(offsetof(AimCell, container) - aim_tag), hotseat});
    dock_motion_subscribed.store(0);
    const uint64_t recents_value_before_aim = dock_motion_value.load();
    const uint64_t recents_entries_before_aim = dock_motion_entry_hits.load();
    const uint64_t recents_publishes_before_aim = dock_motion_publish_hits.load();
    for (size_t index = 0; index < hotseat.size(); ++index) {
        aim_cell.container = hotseat[index];
        const double projected = .31 + static_cast<double>(index) * .09;
        invoke_unlock(dock_motion_unlock_scale_entry_0,
            reinterpret_cast<void *>(aim_state_tagged), projected);
        assert(aim_scene() == 3 && std::abs(aim_scale() - projected) < 1e-12);
    }
    assert(dock_motion_value.load() == recents_value_before_aim);
    assert(dock_motion_entry_hits.load() == recents_entries_before_aim);
    assert(dock_motion_publish_hits.load() == recents_publishes_before_aim);
    dock_motion_subscribed.store(1);
    aim_cell.container = hotseat[0];
    invoke_unlock(dock_motion_unlock_scale_entry_0,
        reinterpret_cast<void *>(aim_state_tagged), .79);
    eventfd_t aim_notifications = 0;
    assert(eventfd_read(dock_motion_event, &aim_notifications) == 0
        && aim_notifications > 0);
    dock_motion_subscribed.store(0);
    const uint64_t before_foreign = dock_auto_aim_publish_hits.load();
    aim_cell.container = -900;
    invoke_unlock(dock_motion_unlock_scale_entry_0,
        reinterpret_cast<void *>(aim_state_tagged), .83);
    assert(dock_auto_aim_publish_hits.load() == before_foreign);
    aim_state.widget = 2; // Smi: never dereference it as a compressed pointer.
    invoke_unlock(dock_motion_unlock_scale_entry_0,
        reinterpret_cast<void *>(aim_state_tagged), .84);
    assert(dock_auto_aim_publish_hits.load() == before_foreign);
    aim_state.widget = static_cast<uint32_t>(aim_widget_tagged);
    aim_widget.cell = 2;
    invoke_unlock(dock_motion_unlock_scale_entry_0,
        reinterpret_cast<void *>(aim_state_tagged), .85);
    assert(dock_auto_aim_publish_hits.load() == before_foreign);
    invoke_unlock(dock_motion_unlock_scale_entry_0, reinterpret_cast<void *>(2), .86);
    assert(dock_auto_aim_publish_hits.load() == before_foreign);
    aim_widget.cell = static_cast<uint32_t>(aim_cell_tagged);

    // A different CID/tag layout, tagged-header position, bool singleton offset
    // and relocated payload must work without changing assembly.
    constexpr uint32_t moved_shift = 9;
    false_object.header = uint64_t(bool_cid) << moved_shift;
    true_object.header = uint64_t(bool_cid) << moved_shift;
    const uintptr_t moved_false_tagged = reinterpret_cast<uintptr_t>(&false_object) + 9;
    const uintptr_t moved_true_tagged = reinterpret_cast<uintptr_t>(&true_object) + 9;
    dock_test_heap_base = moved_false_tagged >> 32;
    assert((moved_true_tagged >> 32) == dock_test_heap_base);
    false_value = static_cast<uint32_t>(moved_false_tagged);
    true_value = static_cast<uint32_t>(moved_true_tagged);
    dock_test_null_object = moved_false_tagged - 0x68;
    struct alignas(16) Moved {
        uint64_t header = 2011ULL << moved_shift;
        uint64_t padding[2]{};
        double scale = .96;
        double alpha = 1;
        uint32_t recents = false_value;
        uint32_t surface = true_value;
    } moved;
    constexpr uint32_t moved_tag = 9;
    publish1({2011, 77, -static_cast<int32_t>(moved_tag), moved_shift, 0x3fffff,
        static_cast<uint32_t>(offsetof(Moved, alpha) - moved_tag),
        static_cast<uint32_t>(offsetof(Moved, scale) - moved_tag),
        static_cast<uint32_t>(offsetof(Moved, surface) - moved_tag),
        static_cast<uint32_t>(offsetof(Moved, recents) - moved_tag), 15, 0x68});
    invoke(dock_motion_set_entry_1, reinterpret_cast<char *>(&moved) + moved_tag);
    assert(moved.header == 2011ULL << moved_shift);
    assert(scene() == 1 && std::abs(scale() - .96) < 1e-12);
    struct alignas(16) MovedDouble {
        uint64_t header = 77ULL << moved_shift;
        uint64_t padding[2]{};
        double value = .97;
    } boxed;
    invoke(dock_motion_scale_entry_1, reinterpret_cast<char *>(&boxed) + moved_tag);
    assert(boxed.header == 77ULL << moved_shift);
    assert(scene() == 1 && std::abs(scale() - .97) < 1e-12);

    // A second immutable bank changes the tagged bias and every AUTO_AIM field. No offset or
    // Hotseat container identity is compiled into the assembly.
    struct alignas(16) MovedAimState {
        uint64_t header{};
        uint64_t padding[2]{};
        uint32_t widget{};
    } moved_aim_state;
    struct alignas(16) MovedAimWidget {
        uint64_t header{};
        uint64_t padding[3]{};
        uint32_t cell{};
    } moved_aim_widget;
    struct alignas(16) MovedAimCell {
        uint64_t header{};
        uint64_t padding[4]{};
        int64_t container{};
    } moved_aim_cell;
    static_assert(offsetof(MovedAimState, widget) == 24);
    static_assert(offsetof(MovedAimWidget, cell) == 32);
    static_assert(offsetof(MovedAimCell, container) == 40);
    const uintptr_t moved_state_tagged = reinterpret_cast<uintptr_t>(&moved_aim_state) + moved_tag;
    const uintptr_t moved_widget_tagged = reinterpret_cast<uintptr_t>(&moved_aim_widget) + moved_tag;
    const uintptr_t moved_cell_tagged = reinterpret_cast<uintptr_t>(&moved_aim_cell) + moved_tag;
    assert((moved_widget_tagged >> 32) == (moved_state_tagged >> 32));
    assert((moved_cell_tagged >> 32) == (moved_state_tagged >> 32));
    dock_test_heap_base = moved_state_tagged >> 32;
    dock_test_null_object = moved_state_tagged - 0x68;
    moved_aim_state.widget = static_cast<uint32_t>(moved_widget_tagged);
    moved_aim_widget.cell = static_cast<uint32_t>(moved_cell_tagged);
    constexpr std::array<int64_t, 5> moved_hotseat{-201, -203, -207, -211, -219};
    moved_aim_cell.container = moved_hotseat[3];
    publishUnlock1({static_cast<uint32_t>(offsetof(MovedAimState, widget) - moved_tag),
        static_cast<uint32_t>(offsetof(MovedAimWidget, cell) - moved_tag),
        static_cast<uint32_t>(offsetof(MovedAimCell, container) - moved_tag), moved_hotseat});
    invoke_unlock(dock_motion_unlock_scale_entry_1,
        reinterpret_cast<void *>(moved_state_tagged), .413);
    assert(aim_scene() == 3 && std::abs(aim_scale() - .413) < 1e-12);

    // Bank 0 remains immutable and callable after bank 1 is configured.
    dock_test_heap_base = moved_false_tagged >> 32;
    dock_test_null_object = moved_false_tagged - 0x68;
    Fixture original_bank;
    invoke_fixture(dock_motion_anim_entry_0, original_bank);
    assert(scene() == 1);
    assert(std::abs(aim_scale() - .413) < 1e-12);
    assert(dock_motion_entry_hits.load() > initial_entries);
    assert(dock_motion_publish_hits.load() > initial_publishes);
    assert(dock_auto_aim_entry_hits.load() > initial_aim_entries);
    assert(dock_auto_aim_publish_hits.load() > initial_aim_publishes);
    assert(dock_motion_active_callbacks.load() == 0);
    close(dock_motion_event);
    puts("Dock ARM64 tests passed: registers, NZCV, Dart stack, independent live Hotseat projection, scene guards, scalar writes, nonblocking notification");
}
