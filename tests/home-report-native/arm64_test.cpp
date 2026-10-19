/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_report_guard.h"
#include "targets/home/home_report_hooks.h"

#include <array>
#include <cassert>
#include <cstring>
#include <fstream>
#include <iostream>
#include <sys/mman.h>
#include <vector>

extern "C" {
extern void *hc_home_report_original;
void hc_home_report_entry();
void report_equality_fixture();
uint32_t report_fixture_equal = 0;
uint32_t report_fixture_calls = 0;
struct DartCall {
    uintptr_t function, receiver, list, null_value, heap_base, pool, dispatch, dart_stack;
    uintptr_t result, x9, x10, x11, flags, after_dart_stack, after_native_stack, lr;
    uintptr_t native_stack, expected_lr, frame;
};
void call_dart_report_fixture(DartCall *call);
}

int main(int argc, char **argv) {
    assert(argc==2);
    std::array<std::byte, home_report::kBodyBytes> body{};
    std::ifstream input(argv[1], std::ios::binary);
    input.read(reinterpret_cast<char *>(body.data()), body.size());
    assert(input.gcount()==static_cast<std::streamsize>(body.size()));
    assert(home_report::accepts(home_report::kSymbol, body));
    void *original = mmap(nullptr, 4096, PROT_READ|PROT_WRITE, MAP_PRIVATE|MAP_ANONYMOUS,-1,0);
    assert(original!=MAP_FAILED);
    std::memcpy(original,body.data(),body.size());
    __builtin___clear_cache(static_cast<char *>(original),static_cast<char *>(original)+body.size());
    assert(mprotect(original,4096,PROT_READ|PROT_EXEC)==0);
    hc_home_report_original=original;
    std::vector<uint64_t> dart_stack(1024), heap(128), pool(0x18000/8), dispatch(4);
    auto *memory=reinterpret_cast<std::byte *>(heap.data());
    const uintptr_t heap_base=reinterpret_cast<uintptr_t>(memory)>>32;
    const uintptr_t list=reinterpret_cast<uintptr_t>(memory)+1;
    const uintptr_t array=reinterpret_cast<uintptr_t>(memory+128)+1;
    const uintptr_t item=reinterpret_cast<uintptr_t>(memory+256)+1;
    const uintptr_t installer=reinterpret_cast<uintptr_t>(memory+640)+1;
    auto write32=[](uintptr_t p,uint32_t value){std::memcpy(reinterpret_cast<void *>(p),&value,4);};
    write32(list+0xf,static_cast<uint32_t>(array));
    write32(array+0xf,static_cast<uint32_t>(item));
    write32(item+0xc7,static_cast<uint32_t>(installer));
    uint64_t tag=1u<<12;
    std::memcpy(reinterpret_cast<void *>(installer-1),&tag,8);
    dispatch[1]=reinterpret_cast<uintptr_t>(&report_equality_fixture);
    pool[0x17370/8]=installer;
    const uintptr_t null_value=0x123400001ULL;
    auto invoke=[&](bool wrapped, uintptr_t list_argument){
        DartCall call{};
        call.function=wrapped ? reinterpret_cast<uintptr_t>(&hc_home_report_entry)
                              : reinterpret_cast<uintptr_t>(original);
        call.list=list_argument;
        call.null_value=null_value;
        call.heap_base=heap_base;
        call.pool=reinterpret_cast<uintptr_t>(pool.data());
        call.dispatch=reinterpret_cast<uintptr_t>(dispatch.data());
        call.dart_stack=reinterpret_cast<uintptr_t>(dart_stack.data()+dart_stack.size());
        call_dart_report_fixture(&call);
        assert(call.x9==0x9999 && call.x10==0xaaaa && call.x11==0xbbbb);
        assert(call.after_dart_stack==call.dart_stack && call.frame==call.dart_stack);
        assert(call.after_native_stack==call.native_stack && call.lr==call.expected_lr);
        return call;
    };
    unsigned cases=0;
    for(unsigned length:{0u,1u,2u}) for(unsigned equality:{0u,1u}) {
        write32(list+0xb,length*2);
        report_fixture_equal=equality;
        const auto baseline=invoke(false,list);
        assert(baseline.result==null_value+((length==1 && equality==0)?0x20:0x30));
        home_report::set_enabled(false);
        auto result=invoke(true,list);
        assert(result.result==baseline.result && result.flags==baseline.flags); ++cases;
        home_report::set_enabled(true);
        const auto equality_calls=report_fixture_calls;
        result=invoke(true,list);
        assert(result.result==null_value+0x30 && result.flags==0xa0000000);
        assert(report_fixture_calls==equality_calls); ++cases;
        home_report::set_enabled(false);
        result=invoke(true,list);
        assert(result.result==baseline.result && result.flags==baseline.flags); ++cases;
    }
    home_report::set_enabled(true);
    auto result=invoke(true,1); // Invalid object must not be dereferenced on replacement path.
    assert(result.result==null_value+0x30 && result.flags==0xa0000000); ++cases;
    home_report::set_enabled(false);
    hc_home_report_original=nullptr;
    result=invoke(true,1);
    assert(result.result==null_value+0x30 && result.flags==0xa0000000); ++cases;
    assert(munmap(original,4096)==0);
    std::cout<<"PASS "<<cases<<" ARM64 original/tail-passthrough/enabled/reset/fallback cases; x9-x11, LR and both stacks preserved, original disabled NZCV and enabled entry NZCV retained\n";
}
