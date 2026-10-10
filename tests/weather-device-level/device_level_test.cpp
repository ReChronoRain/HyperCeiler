// Pinned OS4 public/allocator ARM64 bytes + production adapter policy.
// Dynamic loader, Runtime/private provider, libc and authorization are fixtures.
#include "targets/weather/weather_device_level.h"
#include <elf.h>
#include <sys/mman.h>
#include <unistd.h>
#include <cassert>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <functional>
#include <iterator>
#include <string>
#include <unordered_set>
#include <vector>
using namespace weather_device_level;

struct Image {
    uintptr_t base;
    size_t extent;
    explicit Image(const char *path) {
        std::ifstream input(path, std::ios::binary);
        const std::string raw(std::istreambuf_iterator<char>{input}, {});
        assert(raw.size() > sizeof(Elf64_Ehdr));
        const auto *eh = reinterpret_cast<const Elf64_Ehdr *>(raw.data());
        assert(eh->e_machine == EM_AARCH64 && eh->e_phentsize == sizeof(Elf64_Phdr));
        const auto *ph = reinterpret_cast<const Elf64_Phdr *>(raw.data() + eh->e_phoff);
        extent = 0;
        for (int i = 0; i < eh->e_phnum; ++i)
            if (ph[i].p_type == PT_LOAD) extent = std::max(extent, size_t(ph[i].p_vaddr + ph[i].p_memsz));
        extent = (extent + 4095) & ~size_t(4095);
        void *memory = mmap(nullptr, extent, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        assert(memory != MAP_FAILED);
        base = reinterpret_cast<uintptr_t>(memory);
        for (int i = 0; i < eh->e_phnum; ++i) if (ph[i].p_type == PT_LOAD) {
            assert(ph[i].p_offset + ph[i].p_filesz <= raw.size());
            std::memcpy(reinterpret_cast<void *>(base + ph[i].p_vaddr), raw.data() + ph[i].p_offset, ph[i].p_filesz);
        }
    }
    template <class F> F function(uintptr_t va) const { return reinterpret_cast<F>(base + va); }
    template <class F> void slot(uintptr_t va, F target) {
        *reinterpret_cast<uintptr_t *>(base + va) = reinterpret_cast<uintptr_t>(target);
    }
    void execute() {
        __builtin___clear_cache(reinterpret_cast<char *>(base), reinterpret_cast<char *>(base + extent));
        // Fixture image data is immutable after relocation; only heap objects change.
        assert(mprotect(reinterpret_cast<void *>(base), extent, PROT_READ | PROT_EXEC) == 0);
    }
    ~Image() { munmap(reinterpret_cast<void *>(base), extent); }
};

static uint32_t packed;
static int32_t last_selector;
static void *last_runtime;
static size_t allocations, frees, provider_calls;
static bool fail_allocation;
static std::unordered_set<void *> owned;
static size_t offset(void *) { return 8; }
static void *from_runtime(void *value) { return value; }
static uint32_t provider(void *runtime, int32_t selector) {
    last_runtime = runtime; last_selector = selector; ++provider_calls; return packed;
}
static void *allocate(size_t bytes) {
    assert(bytes == 3); ++allocations;
    if (fail_allocation) return nullptr;
    void *memory = std::malloc(bytes); assert(memory); owned.insert(memory); return memory;
}
static void release(void *memory) {
    assert(owned.erase(memory) == 1); ++frees; std::free(memory);
}
[[noreturn]] static void allocation_error(size_t, size_t) { std::abort(); }

static unsigned tests;
static void test(const std::string &name, const std::function<void()> &body) {
    assert(owned.empty());
    allocations = frees = provider_calls = 0; fail_allocation = false;
    packed = 0x020301; last_selector = 0; last_runtime = nullptr;
    body(); assert(owned.empty()); ++tests; std::printf("PASS %s\n", name.c_str());
}

int main(int argc, char **argv) {
    assert(argc == 3);
    Image pub(argv[1]), standard(argv[2]);
    Api api;
    api.get = pub.function<decltype(api.get)>(0x10ec8);
    api.allocate = standard.function<decltype(api.allocate)>(0x630ac);
    api.allocation_shim = standard.function<decltype(api.allocation_shim)>(0x63284);
    api.drop = pub.function<decltype(api.drop)>(0x10e8c);
    standard.slot(0xe2310, allocate);
    standard.slot(0xe2320, release);
    pub.slot(0x18638, offset); pub.slot(0x18640, from_runtime); pub.slot(0x186e8, provider);
    pub.slot(0x18680, standard.function<void (*)(void *,size_t,size_t)>(0x63110));
    pub.slot(0x18690, api.allocation_shim); pub.slot(0x18698, api.allocate);
    pub.slot(0x186a0, allocation_error);
    standard.execute(); pub.execute();
    const auto ram = pub.function<int (*)(const void *)>(0x110fc);
    const auto cpu = pub.function<int (*)(const void *)>(0x10eac);
    const auto gpu = pub.function<int (*)(const void *)>(0x10fd8);
    uint64_t runtime_storage[2]{1, 0};
    void *runtime = runtime_storage + 1;
    auto selection = [](int level) {
        weather_snapshot::Snapshot s;
        s.available = s.allowed = true; s.epoch = 123; s.revision = 4;
        s.valid_until = 15'000'000'000LL; s.device_level = level;
        return s;
    };
    auto check_values = [&](void *object,int r,int c,int g) {
        assert(object && ram(object)==r && cpu(object)==c && gpu(object)==g);
    };
    for (int32_t selector : {INT32_MIN, -1, 0, 1, 2, 255, INT32_MAX}) {
        test("original arguments and owned bytes selector " + std::to_string(selector), [&] {
            const auto before = runtime_storage[0];
            auto result = invoke(api, &runtime, selector, [&](auto &s) {s=selection(3);return true;});
            assert(result.outcome == Outcome::Passthrough && provider_calls==1);
            assert(last_selector==selector && last_runtime==runtime && runtime_storage[0]==before+1);
            check_values(result.object,1,3,2); api.drop(result.object);
            assert(allocations==1 && frees==1);
        });
    }
    for (int level=0;level<3;++level) for (bool missing : {false,true}) {
        test("uniform grade " + std::to_string(level) + (missing?" original-null":" original-owned"), [&] {
            if (missing) packed=4;
            auto result=invoke(api,&runtime,77,[&](auto&s){s=selection(level);return true;});
            assert(result.outcome==Outcome::Applied && provider_calls==1 && last_selector==77);
            check_values(result.object,level+1,level+1,level+1); api.drop(result.object);
            assert(allocations==1 && frees==1);
        });
    }
    for (int raw : {-128,-1,0,1,2,3,4,127}) {
        test("existing signed bytes forced " + std::to_string(raw),[&] {
            packed=0x020001 | (static_cast<uint32_t>(static_cast<uint8_t>(raw))<<8);
            auto result=invoke(api,&runtime,1,[&](auto&s){s=selection(2);return true;});
            assert(result.outcome==Outcome::Applied);check_values(result.object,3,3,3);api.drop(result.object);
        });
    }
    for (bool missing : {false,true}) for (int denied=0;denied<5;++denied) {
        test("inactive selection " + std::to_string(denied) + (missing?" null":" owned"),[&] {
            if(missing)packed=4;
            auto result=invoke(api,&runtime,1,[&](auto&s){s=selection(2);
                if(denied==1)s.available=false;
                if(denied==2)s.allowed=false;
                if(denied==3)s.device_level=3;
                if(denied==4)s.device_level=-1;
                return denied!=0;});
            assert(result.outcome==Outcome::Passthrough && provider_calls==1);
            if(missing)assert(!result.object&&allocations==0);
            else {check_values(result.object,1,3,2);api.drop(result.object);}
        });
    }
    for (bool missing : {false,true}) for(int change=0;change<6;++change) {
        test("publication change " + std::to_string(change) + (missing?" null":" owned"),[&] {
            if(missing)packed=4;
            int reads=0;
            auto result=invoke(api,&runtime,1,[&](auto&s){s=selection(2);if(++reads==2){
                if(change==0)return false;
                if(change==1)s.epoch++;
                if(change==2)s.revision++;
                if(change==3)s.device_level=0;
                if(change==4)s.allowed=false;
                if(change==5)s.available=false;
            }return true;});
            assert(result.outcome==Outcome::AuthorizationChanged&&provider_calls==1);
            if(missing)assert(!result.object&&allocations==1&&frees==1);
            else {check_values(result.object,1,3,2);api.drop(result.object);}
        });
    }
    test("supplemental allocation failure is not applied",[&] {
        packed=4;fail_allocation=true;
        auto result=invoke(api,&runtime,1,[&](auto&s){s=selection(2);return true;});
        assert(result.outcome==Outcome::AllocationFailed&&!result.object&&allocations==1&&frees==0);
    });
    test("incomplete binding cannot partially apply",[&] {
        auto incomplete=api;incomplete.drop=nullptr;
        auto result=invoke(incomplete,&runtime,1,[&](auto&){assert(false);return false;});
        assert(result.outcome==Outcome::MissingApi&&provider_calls==1);
        check_values(result.object,1,3,2);api.drop(result.object);
    });
    std::printf("{\"passed\":true,\"cases\":%u}\n",tests);
}
