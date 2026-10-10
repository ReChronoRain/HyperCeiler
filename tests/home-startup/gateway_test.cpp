/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include <cassert>
#include <array>
#include <cstdint>
#include <cstdio>
#include <cstring>
extern "C" {
alignas(8) uint64_t hc_layout_startup_epoch_gate = 0;
uint64_t gateway_registers[100]{};
uint64_t gateway_marker = 0, gateway_expected_sp = 0;
void gateway_probe(void *,uint64_t);
void gateway_expected_lr();
#define DECL(n) void hc_home_startup_##n(); void original_##n();
DECL(0) DECL(1) DECL(2) DECL(3) DECL(4) DECL(5) DECL(6) DECL(7)
DECL(8) DECL(9) DECL(10) DECL(11) DECL(12) DECL(13) DECL(14)
#undef DECL
void *hc_layout_dart_HotseatMargin_original = reinterpret_cast<void *>(original_0);
void *hc_layout_dart_FolderRowSpacing_original = reinterpret_cast<void *>(original_1);
void *hc_layout_dart_WorkspaceTop_original = reinterpret_cast<void *>(original_2);
void *hc_layout_dart_WorkspaceBottom_original = reinterpret_cast<void *>(original_3);
void *hc_layout_dart_WorkspaceSide_original = reinterpret_cast<void *>(original_4);
void *hc_layout_dart_IndicatorMargin_original = reinterpret_cast<void *>(original_5);
void *hc_layout_dart_SearchBarMargin_original = reinterpret_cast<void *>(original_6);
void *hc_layout_dart_SearchBarWidth_original = reinterpret_cast<void *>(original_7);
void *hc_layout_dart_IndicatorDot_original = reinterpret_cast<void *>(original_8);
void *hc_layout_folder_original[] = {reinterpret_cast<void *>(original_9),reinterpret_cast<void *>(original_10),
    reinterpret_cast<void *>(original_11),reinterpret_cast<void *>(original_12),reinterpret_cast<void *>(original_13)};
void *hc_layout_magic_original = reinterpret_cast<void *>(original_14);
}
int main() {
    void (*gateways[])() = {hc_home_startup_0,hc_home_startup_1,hc_home_startup_2,hc_home_startup_3,
        hc_home_startup_4,hc_home_startup_5,hc_home_startup_6,hc_home_startup_7,hc_home_startup_8,
        hc_home_startup_9,hc_home_startup_10,hc_home_startup_11,hc_home_startup_12,hc_home_startup_13,hc_home_startup_14};
    unsigned tests = 0;
    for(unsigned index=0;index<15;++index) for(uint64_t generation: std::array<uint64_t,4>{0,2,24,UINT64_MAX-1})
        for(unsigned enabled=0;enabled<2;++enabled) for(uint64_t flags: std::array<uint64_t,3>{0,0xa0000000,0xf0000000}) {
            std::memset(gateway_registers,0,sizeof(gateway_registers));
            __atomic_store_n(&hc_layout_startup_epoch_gate,generation|enabled,__ATOMIC_RELEASE);
            gateway_probe(reinterpret_cast<void *>(gateways[index]),flags);
            assert(gateway_marker==(enabled?index:100+index));
            if (gateway_registers[17] != 0x111) {
                std::fprintf(stderr, "x17 mismatch: gateway=%u enabled=%u actual=%#llx expected=0x111\n",
                    index, enabled, static_cast<unsigned long long>(gateway_registers[17]));
                return 1;
            }
            for(unsigned reg=0;reg<30;++reg) if(reg!=16) assert(gateway_registers[reg]==0x100+reg);
            assert(gateway_registers[30]==reinterpret_cast<uintptr_t>(gateway_expected_lr));
            assert(gateway_registers[31]==flags && gateway_registers[32]==gateway_expected_sp);
            const auto *simd=reinterpret_cast<const uint8_t *>(gateway_registers)+272;
            for(unsigned q=0;q<32;++q)for(unsigned b=0;b<16;++b)assert(simd[q*16+b]==32+q);
            ++tests;
        }
    std::printf("{\"gateway_cases\":%u,\"passed\":%u,\"failed\":0,\"scope\":\"actual production gateways, controlled target/original destinations\"}\n",tests,tests);
    return 0;
}
