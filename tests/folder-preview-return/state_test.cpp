/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_folder_preview_return_state.h"
#include <cassert>
#include <cstring>
#include <iostream>
#include <limits>
#include <thread>
#include <type_traits>

using home_layout::FolderPreviewReturnState;
using home_layout::FolderPreviewReturnValue;
static uint64_t bits(double value) {
    uint64_t result=0;std::memcpy(&result,&value,sizeof(result));return result;
}
static void supplied(FolderPreviewReturnValue value,double expected) {
    assert(value.applied==1&&value.bits==bits(expected));
}
static void refused(FolderPreviewReturnValue value) {
    assert(value.applied==0&&value.bits==0);
}

int main() {
    static_assert(sizeof(FolderPreviewReturnValue)==16);
    static_assert(std::is_trivially_copyable_v<FolderPreviewReturnValue>);
    static_assert(std::is_standard_layout_v<FolderPreviewReturnValue>);
    static_assert(sizeof(FolderPreviewReturnState)<512);
    constexpr uintptr_t first=0x10000,second=0xff00;
    FolderPreviewReturnState state;
    refused(state.take(first,false));refused(state.take(first,true));

    // Rectangular previews and every supported shrink/enlarge factor retain
    // two distinct lengths. The neutral/off geometry is a valid original pair.
    for(double factor:{.6,.7,1.,1.25,1.4}) {
        state.capture(first,bits(64*factor),bits(48*factor));
        supplied(state.take(first,false),64*factor);
        supplied(state.take(first,true),48*factor);
        refused(state.take(first,false));refused(state.take(first,true));
    }

    // A partial installation or out-of-order consumer must use stock results.
    state.capture(first,bits(32),bits(24));
    refused(state.take(first,true));refused(state.take(first,false));
    state.capture(first,bits(32),bits(24));
    supplied(state.take(first,false),32);
    refused(state.take(first,false));refused(state.take(first,true));

    // Nested original frames, configuration updates and reused frames cannot
    // consume another invocation's scalar snapshot.
    state.capture(first,bits(44.8),bits(33.6));
    state.capture(second,bits(80),bits(60));
    supplied(state.take(second,false),80);supplied(state.take(second,true),60);
    supplied(state.take(first,false),44.8);supplied(state.take(first,true),33.6);
    state.capture(first,bits(44.8),bits(33.6));
    supplied(state.take(first,false),44.8);
    state.capture(second,bits(80),bits(60));
    // The later preference/factor does not replace the first frame's height.
    supplied(state.take(first,true),33.6);
    supplied(state.take(second,false),80);supplied(state.take(second,true),60);
    state.capture(first,bits(32),bits(24));
    state.capture(first,bits(80),bits(60));
    supplied(state.take(first,false),80);supplied(state.take(first,true),60);

    for(double invalid:{0.,-0.,-1.,65536.01,
            std::numeric_limits<double>::infinity(),
            -std::numeric_limits<double>::infinity(),
            std::numeric_limits<double>::quiet_NaN()}) {
        for(bool height:{false,true}) {
            state.capture(first,bits(32),bits(24));
            state.capture(first,bits(height?32:invalid),bits(height?invalid:24));
            refused(state.take(first,false));refused(state.take(first,true));
        }
    }
    for(uintptr_t invalid:{uintptr_t{0},first+1,first+4}) {
        state.capture(invalid,bits(32),bits(24));
        refused(state.take(invalid,false));refused(state.take(invalid,true));
    }
    state.capture(first,bits(65536),bits(1));
    supplied(state.take(first,false),65536);supplied(state.take(first,true),1);

    // Aborted captures and deep nesting have bounded storage. Eviction of an
    // unused pair makes both consumers fall back. Already supplied width keeps
    // its matching height even when later captures overflow the bank.
    state.reset();
    for(unsigned i=0;i<8;++i)state.capture(first+i*16,bits(32+i),bits(24+i));
    supplied(state.take(first,false),32);
    state.capture(first+8*16,bits(80),bits(60));
    supplied(state.take(first,true),24);
    refused(state.take(first+16,false));refused(state.take(first+16,true));
    supplied(state.take(first+8*16,false),80);supplied(state.take(first+8*16,true),60);

    state.reset();
    for(unsigned i=0;i<8;++i) {
        state.capture(first+i*16,bits(32+i),bits(24+i));
        supplied(state.take(first+i*16,false),32+i);
    }
    state.capture(first+8*16,bits(80),bits(60));
    refused(state.take(first+8*16,false));refused(state.take(first+8*16,true));
    for(unsigned i=0;i<8;++i)supplied(state.take(first+i*16,true),24+i);
    state.capture(first+8*16,bits(80),bits(60));
    supplied(state.take(first+8*16,false),80);supplied(state.take(first+8*16,true),60);
    for(unsigned i=0;i<10000;++i)state.capture(first+i*16,bits(32),bits(24));
    state.reset();refused(state.take(first+9999*16,false));

    // Instances used by distinct native TLS slots never share or overwrite data.
    FolderPreviewReturnState other;
    state.capture(first,bits(32),bits(24));other.capture(first,bits(80),bits(60));
    supplied(state.take(first,false),32);supplied(other.take(first,false),80);
    supplied(state.take(first,true),24);supplied(other.take(first,true),60);
    static thread_local FolderPreviewReturnState per_thread;
    per_thread.capture(first,bits(32),bits(24));
    std::thread worker([&] {
        refused(per_thread.take(first,false));
        per_thread.capture(first,bits(80),bits(60));
        supplied(per_thread.take(first,false),80);supplied(per_thread.take(first,true),60);
    });
    worker.join();
    supplied(per_thread.take(first,false),32);supplied(per_thread.take(first,true),24);
    state.capture(first,bits(32),bits(24));state.reset();
    refused(state.take(first,false));refused(state.take(first,true));
    std::cout<<"FOLDER_PREVIEW_RETURN_STATE PASS: paired scalar dimensions, rectangular/neutral/scaled geometry, nested/reused frames, invalid input, bounded overflow/abort, reset and isolated TLS state\n";
}
