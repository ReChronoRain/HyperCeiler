"""Workers must not reacquire the linker lock to identify an unchanged module."""
from pathlib import Path
import os, shlex, subprocess, tempfile

root = Path(__file__).resolve().parents[2]
source = (root / 'app/src/main/cpp/nativehook/android_diagnostics.h').read_text()
function = source[source.index('inline const std::string& module_identity()'):source.index('inline Identity identity()')]
process = source[source.index('inline const Identity& process_identity()'):source.index('// Called only from initialization/maintenance transitions')]
fixture = r'''
#include <cassert>
#include <cstdint>
#include <cstring>
#include <future>
#include <mutex>
#include <string>
#include <chrono>
#include <atomic>
#include <memory>
constexpr unsigned PT_LOAD=1, PT_NOTE=4;
struct Phdr { unsigned p_type; uintptr_t p_vaddr, p_memsz; };
struct dl_phdr_info { uintptr_t dlpi_addr; unsigned dlpi_phnum; Phdr* dlpi_phdr; };
std::mutex linker_lock;
unsigned walks=0;
int dl_iterate_phdr(int (*callback)(dl_phdr_info*,size_t,void*),void* data) {
    std::lock_guard lock(linker_lock); ++walks;
    const uint32_t note[]={4,4,3,0x00554e47,0x78563412};
    Phdr headers[]={{PT_LOAD,0,UINTPTR_MAX},{PT_NOTE,reinterpret_cast<uintptr_t>(note),sizeof(note)}};
    dl_phdr_info image{0,2,headers}; return callback(&image,sizeof(image),data);
}
''' + function + r'''
struct Identity {int pid; std::string module;};
std::atomic<int> pid{1};
int getpid(){return pid;}
Identity identity(){return {getpid(),module_identity()};}
''' + process + r'''
int main() {
    // native_init warms the module identity before any maintenance worker.
    const auto* parent=&process_identity();
    assert(parent->module=="elf:12345678");
    // A load callback owns this lock while the first-render bank is prepared.
    std::unique_lock loader(linker_lock);
    auto worker=std::async(std::launch::async,[]{return &process_identity();});
    assert(worker.wait_for(std::chrono::seconds(1))==std::future_status::ready);
    assert(worker.get()==parent && walks==1);
    pid=2; // Model a specialized child: process identity changes, module does not.
    auto child=std::async(std::launch::async,[]{return &process_identity();});
    assert(child.wait_for(std::chrono::seconds(1))==std::future_status::ready);
    const auto* specialized=child.get();
    assert(specialized->pid==2 && specialized!=parent && parent->pid==1 && walks==1);
}
'''
with tempfile.TemporaryDirectory(prefix='hc-module-identity-') as temp:
    src=Path(temp)/'test.cpp'; binary=Path(temp)/'test.exe'
    src.write_text(fixture)
    compiler=shlex.split(os.environ.get('CXX','c++'))
    subprocess.run([*compiler,'-std=c++20','-O2','-UNDEBUG',str(src),'-o',str(binary)],check=True)
    subprocess.run([str(binary)],check=True,timeout=5)
print('Module identity: workers reuse build ID without waiting for linker lock')
