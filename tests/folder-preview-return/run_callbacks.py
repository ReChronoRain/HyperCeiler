#!/usr/bin/env python3
"""Compile the shipping callback bodies with the real bounded scalar state."""
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parents[2]
source=(ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
start=source.index('namespace {\nstd::atomic_uint64_t g_folder_preview_generation')
end=source.index('extern "C" void hc_drawer_body',start)
callbacks=source[start:end]
prefix=r'''
#include "targets/home/home_folder_preview_return_state.h"
#include <atomic>
#include <cassert>
#include <cstring>
#include <iostream>
#include <thread>
#include <chrono>
#include <array>
#include <algorithm>
constexpr int ANDROID_LOG_INFO=4;
constexpr const char* kTag="test";
void __android_log_print(int,const char*,const char*,...) {}
uint64_t hc_small_folder_scale_bits=0x3ff0000000000000ULL;
uint64_t hc_big_folder_scale_bits=0x3ff0000000000000ULL;
uint32_t hc_folder_preview_return_enabled=0;
uint64_t hc_folder_preview_return_hits=0;
uint64_t bits(double d){uint64_t v;std::memcpy(&v,&d,8);return v;}
void stock(home_layout::FolderPreviewReturnValue v){assert(!v.applied);}
void actual(home_layout::FolderPreviewReturnValue v,double d){assert(v.applied&&v.bits==bits(d));}
'''
checks=r'''
int main(){
    constexpr uintptr_t frame=0x10000;
    hc_folder_preview_return_enabled=1;
    // Feature off/neutral: the actual shipping gate keeps original results.
    hc_folder_preview_capture(frame,bits(30),bits(20));
    stock(hc_folder_preview_take(frame,0));stock(hc_folder_preview_take(frame,1));
    hc_big_folder_scale_bits=bits(.7);
    hc_folder_preview_capture(frame,bits(44.8),bits(33.6));
    actual(hc_folder_preview_take(frame,0),44.8);
    // A preference update or readiness change cannot mix one accepted pair.
    hc_big_folder_scale_bits=bits(1);hc_folder_preview_return_enabled=0;
    actual(hc_folder_preview_take(frame,1),33.6);
    assert(hc_folder_preview_return_hits==1);
    stock(hc_folder_preview_take(frame,1));
    hc_big_folder_scale_bits=bits(1.25);
    hc_folder_preview_capture(frame,bits(80),bits(60));
    stock(hc_folder_preview_take(frame,0));
    hc_folder_preview_return_enabled=1;
    stock(hc_folder_preview_take(frame,1)); // no width accepted while unready
    stock(hc_folder_preview_take(frame,0)); // in-flight installation: no capture
    hc_folder_preview_capture(frame,bits(80),bits(60));
    g_folder_preview_generation.fetch_add(1,std::memory_order_release);
    stock(hc_folder_preview_take(frame,0));stock(hc_folder_preview_take(frame,1));
    // Native threads using the same stack identity cannot exchange dimensions.
    hc_folder_preview_capture(frame,bits(80),bits(60));
    std::thread worker([&]{
        stock(hc_folder_preview_take(frame,0));
        hc_folder_preview_capture(frame,bits(35),bits(21));
        actual(hc_folder_preview_take(frame,0),35);actual(hc_folder_preview_take(frame,1),21);
    });worker.join();
    actual(hc_folder_preview_take(frame,0),80);actual(hc_folder_preview_take(frame,1),60);
    hc_small_folder_scale_bits=bits(.6);hc_big_folder_scale_bits=bits(1);
    hc_folder_preview_capture(frame,bits(24),bits(18));
    actual(hc_folder_preview_take(frame,0),24);actual(hc_folder_preview_take(frame,1),18);
    hc_small_folder_scale_bits=bits(1);
    hc_folder_preview_capture(frame,bits(24),bits(18));
    stock(hc_folder_preview_take(frame,0));stock(hc_folder_preview_take(frame,1));
    // Informational host microbenchmark of the actual warm callback bodies.
    // It excludes the ARM gateway, Android TLS and logger/device scheduling;
    // do not use it as device frame-time or cold-start evidence.
    hc_big_folder_scale_bits=bits(.7);
    std::array<double,9> batches{};
    for(auto& ns:batches){
        const auto begin=std::chrono::steady_clock::now();
        for(unsigned i=0;i<10000;++i){
            hc_folder_preview_capture(frame,bits(44.8),bits(33.6));
            actual(hc_folder_preview_take(frame,0),44.8);
            actual(hc_folder_preview_take(frame,1),33.6);
        }
        ns=std::chrono::duration<double,std::nano>(std::chrono::steady_clock::now()-begin).count()/10000;
    }
    std::sort(batches.begin(),batches.end());
    std::cout<<"host warm scalar callbacks: median="<<batches[4]<<" ns/pair, slowest batch="<<batches.back()<<" ns/pair (not device frame time)\n";
    std::cout<<"FOLDER_PREVIEW_RETURN_CALLBACKS PASS: shipping gates, scalar snapshot, partial installation, disabled/neutral, image generation and TLS isolation\n";
}
'''
with tempfile.TemporaryDirectory(prefix='hc-folder-preview-callbacks-') as temp:
    folder=Path(temp);cpp=folder/'test.cpp';binary=folder/'test'
    cpp.write_text(prefix+callbacks+checks)
    subprocess.run([shutil.which('c++'),'-std=c++20','-O2','-pthread','-Wall','-Wextra','-Werror',
                    '-I'+str(ROOT/'app/src/main/cpp'),str(cpp),'-o',str(binary)],check=True)
    subprocess.run([str(binary)],check=True)
