/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/weather/weather_card_binding.h"
#include "fixture.generated.h"
#include <algorithm>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <new>
#include <stdexcept>
#include <string>
#include <vector>

static bool forbid_alloc = false;
void *operator new(std::size_t n) {
    if (forbid_alloc) throw std::bad_alloc();
    if (void *p = std::malloc(n ? n : 1)) return p;
    throw std::bad_alloc();
}
void operator delete(void *p) noexcept {std::free(p);}
void operator delete(void *p, std::size_t) noexcept {std::free(p);}
void *operator new[](std::size_t n) {return ::operator new(n);}
void operator delete[](void *p) noexcept {::operator delete(p);}
void operator delete[](void *p, std::size_t) noexcept {::operator delete(p);}

namespace wc = weather_card;
namespace wg = weather_card_guard;
static int passed = 0, failed = 0;
static uint64_t bit_rejections = 0;
void require(bool ok, const char *why = "assertion") {if (!ok) throw std::runtime_error(why);}
void run(const std::string &name, auto test) {
    try {test(); ++passed; std::cout << "PASS " << name << '\n';}
    catch (const std::exception &e) {++failed; std::cout << "FAIL " << name << ": " << e.what() << '\n';}
}
struct Segment {uintptr_t va; std::vector<std::byte> data;};
struct Image {uintptr_t base; std::vector<Segment> segments;};
struct Reader {
    std::array<Image, 2> images;
    uintptr_t mutation = 0, unreadable = 0, change_site = 0;
    uint8_t flip = 0;
    unsigned site_reads = 0, change_after = 0;
    size_t calls = 0, max_read = 0;
    explicit Reader(const char *const *paths) {
        for (size_t i = 0; i < 2; ++i) {
            images[i].base = fixture::bases[i];
            std::ifstream f(paths[i], std::ios::binary);
            require(bool(f), "Missing actual ROM ELF");
            std::vector<char> file((std::istreambuf_iterator<char>(f)), {});
            for (const auto &load : fixture::loads[i]) {
                require(load.offset + load.filesz <= file.size());
                Segment s{load.va, std::vector<std::byte>(load.memsz)};
                std::memcpy(s.data.data(), file.data() + load.offset, load.filesz);
                images[i].segments.push_back(std::move(s));
            }
        }
    }
    bool operator()(uintptr_t address, std::span<std::byte> out) {
        ++calls; max_read = std::max(max_read, out.size());
        if (unreadable >= address && unreadable - address < out.size()) return false;
        for (auto &image : images) {
            if (address < image.base) continue;
            const auto va = address - image.base;
            for (auto &s : image.segments) {
                if (va < s.va || va - s.va > s.data.size() || out.size() > s.data.size() - (va - s.va)) continue;
                std::memcpy(out.data(), s.data.data() + va - s.va, out.size());
                if (mutation >= address && mutation - address < out.size()) out[mutation - address] ^= std::byte{flip};
                if (change_site >= address && change_site - address < out.size()
                    && ++site_reads >= change_after) out[change_site - address] ^= std::byte{4};
                return true;
            }
        }
        return false;
    }
    void word(uintptr_t address, uint32_t value) {
        for (auto &image : images) {
            if (address < image.base) continue;
            const auto va = address - image.base;
            for (auto &s : image.segments) if (va >= s.va && va - s.va + 4 <= s.data.size()) {
                std::memcpy(s.data.data() + va - s.va, &value, 4); return;
            }
        }
        throw std::runtime_error("Invalid fixture write");
    }
    void clear() {mutation = unreadable = change_site = 0; flip = 0; site_reads = change_after = 0;}
};

wc::Binding sentinel() {return {11, 22, {33, 44}};}
bool same(const wc::Binding &a, const wc::Binding &b) {
    return a.dart_base == b.dart_base && a.flutter_base == b.flutter_base && a.epilogue == b.epilogue;
}
bool no_alloc(auto action) {
    forbid_alloc = true;
    try {bool result = action(); forbid_alloc = false; return result;}
    catch (...) {forbid_alloc = false; throw;}
}
bool resolve(Reader &r, wc::Binding &out, uintptr_t dart = fixture::bases[0], uintptr_t flutter = fixture::bases[1]) {
    return no_alloc([&] {return wc::resolve(dart, flutter, r, out);});
}
void rejected(Reader &r, uintptr_t dart = fixture::bases[0], uintptr_t flutter = fixture::bases[1]) {
    auto out = sentinel();
    require(!resolve(r, out, dart, flutter), "Invalid binding accepted");
    require(same(out, sentinel()), "Output changed on failed resolve");
}
wc::BranchWords branch_plan(const wc::Binding &b) {
    wc::BranchWords result{};
    for (size_t i = 0; i < 2; ++i) {
        const auto target = b.dart_base + 0x2000000 + i * 0x1000;
        result[i] = 0x14000000u | static_cast<uint32_t>((target - b.epilogue[i]) / 4);
    }
    return result;
}
void state(Reader &r, const wc::Binding &b, const wc::BranchWords &plan, unsigned mask) {
    for (size_t i = 0; i < 2; ++i) r.word(b.epilogue[i], mask & (1u << i) ? plan[i] : 0xaa1603e0u);
}
bool verify(Reader &r, const wc::Binding &b, const wc::BranchWords &plan, unsigned mask) {
    return no_alloc([&] {return wc::verify_state(b, r, plan, mask);});
}

int main(int argc, char **argv) {
    require(argc == 3, "Dart and Flutter ELF paths required");
    Reader r(const_cast<const char *const *>(argv + 1));
    wc::Binding binding;
    run("actual two ELF images bind without allocation", [&] {
        require(resolve(r, binding));
        require(binding.dart_base == fixture::bases[0] && binding.flutter_base == fixture::bases[1]);
        require(binding.epilogue == std::array<uintptr_t, 2>{fixture::bases[0]+0x3a8408, fixture::bases[0]+0x3aece0});
        require(r.max_read <= 256);
    });
    auto plan = branch_plan(binding);
    run("exact ABI constants and full recompute extent", [] {
        require(wg::kClassId == 254 && wg::kInstanceBytes == 192 && wg::kModeTaggedOffset == 0x8b);
        require(wg::kLiveStackLimitOffset == 0x48 && wg::kSavedStackLimitOffset == 0x678 && wg::kGatewayScratchBytes == 96);
        bool found = false;
        for (auto g : wg::kDartGuards) if (g.va == 0x3a7ae0) {found=true; require(g.bytes.size()==2560);}
        require(found);
    });
    run("every original guard bit rejected and output unchanged", [&] {
        for (auto g : fixture::regions) for (size_t offset=0; offset<g.size; ++offset) {
            r.mutation = fixture::bases[g.image] + g.va + offset;
            for (uint8_t bit=1; bit; bit<<=1) {r.flip=bit; rejected(r); ++bit_rejections;}
        }
        r.clear(); require(bit_rejections == fixture::guard_bits);
    });
    run("guard first middle last unreadable rejected", [&] {
        for (auto g : fixture::regions) for (size_t offset : {size_t{0}, g.size/2, g.size-1}) {
            r.unreadable = fixture::bases[g.image] + g.va + offset; rejected(r);
        }
        r.clear();
    });
    for (unsigned mask=0; mask<4; ++mask) run("exact ownership state " + std::to_string(mask), [&,mask] {
        state(r,binding,plan,mask); require(verify(r,binding,plan,mask));
        require(wc::verify_owned(binding,r,plan)==(mask==3));
        for (unsigned other=0;other<4;++other) if(other!=mask) require(!verify(r,binding,plan,other));
        if(mask) rejected(r);
        state(r,binding,plan,0);
    });
    run("owned guards preserve every surrounding original byte", [&] {
        state(r,binding,plan,3);
        for(auto g:fixture::regions) for(size_t n=0;n<g.size;++n) {
            r.mutation=fixture::bases[g.image]+g.va+n; r.flip=1;
            require(!verify(r,binding,plan,3),"Owned mutation accepted");
        }
        r.clear(); state(r,binding,plan,0);
    });
    for(size_t i=0;i<2;++i) run("wrong branch target rejected " + std::to_string(i),[&,i] {
        state(r,binding,plan,3); r.word(binding.epilogue[i],plan[i]^1u);
        require(!verify(r,binding,plan,3)); state(r,binding,plan,0);
    });
    run("BL opcode rejected as approved plan", [&] {
        auto bad=plan; bad[0]|=0x80000000u; state(r,binding,bad,3);
        require(!verify(r,binding,bad,3)); state(r,binding,plan,0);
    });
    run("self branch and original word rejected as plans", [&] {
        for(auto word:{0x14000000u,0xaa1603e0u,0u,0xd61f0200u}) {
            auto bad=plan;bad[0]=word;state(r,binding,bad,3);require(!verify(r,binding,bad,3));
        }
        state(r,binding,plan,0);
    });
    run("jump into guarded source image rejected", [&] {
        auto bad=plan;bad[0]=0x14000001u;state(r,binding,bad,3);
        require(!verify(r,binding,bad,3));state(r,binding,plan,0);
    });
    run("jump plan into Flutter rejected even with exact matching words", [&] {
        auto near=binding;near.flutter_base=binding.dart_base+0x2000000;
        auto bad=branch_plan(near);
        const auto calls=r.calls;require(!verify(r,near,bad,3));require(calls==r.calls);
    });
    run("branch decode overflow underflow rejected before target assignment", [] {
        uintptr_t target=99;
        require(!wc::detail::branch_target(UINTPTR_MAX&~uintptr_t{3},0x14000001u,target)&&target==99);
        require(!wc::detail::branch_target(4,0x17ffffffu,target)&&target==99);
        require(!wc::detail::branch_target(5,0x14000001u,target)&&target==99);
        require(wc::detail::branch_target(0x10000000,0x16000000u,target)&&target==0x08000000);
        require(wc::detail::branch_target(0x10000000,0x15ffffffu,target)&&target==0x17fffffc);
    });
    for(unsigned mask:{4u,255u,256u,UINT32_MAX}) run("invalid nontruncated ownership mask "+std::to_string(mask),[&,mask] {
        const auto calls=r.calls;require(!verify(r,binding,plan,mask));require(calls==r.calls);
    });
    for(uintptr_t base:{uintptr_t{0},uintptr_t{1},fixture::bases[0]+1,UINTPTR_MAX,UINTPTR_MAX&~uintptr_t{4095}})
        run("invalid Dart base "+std::to_string(base),[&,base] {auto calls=r.calls;rejected(r,base);require(calls==r.calls);});
    for(uintptr_t base:{uintptr_t{0},uintptr_t{1},fixture::bases[1]+1,UINTPTR_MAX&~uintptr_t{4095}})
        run("invalid Flutter base "+std::to_string(base),[&,base] {auto calls=r.calls;rejected(r,fixture::bases[0],base);require(calls==r.calls);});
    run("cross-image identity mismatch rejected", [&] {rejected(r,fixture::bases[1],fixture::bases[0]);});
    run("overlapping and same image ranges rejected before reader", [&] {
        for(auto base:{fixture::bases[0],fixture::bases[0]+4096}) {auto calls=r.calls;rejected(r,fixture::bases[0],base);require(calls==r.calls);}
    });
    run("binding sites cannot be forged", [&] {
        for(size_t i=0;i<2;++i) {auto bad=binding;bad.epilogue[i]+=4;auto calls=r.calls;require(!verify(r,bad,plan,0));require(calls==r.calls);}
    });
    run("bounded guard region rejects out-of-image before read", [&] {
        std::array<uint8_t,4> bytes{};wg::GuardBytes bad{wg::kDartImageSpan-1,bytes};
        auto calls=r.calls;require(!wc::detail::match(binding.dart_base,wg::kDartImageSpan,bad,plan,0,true,r));require(calls==r.calls);
    });
    run("checked address preserves output on overflow", [] {
        uintptr_t out=99;require(!wc::detail::address(UINTPTR_MAX-7,8,0,out)&&out==99);
        require(!wc::detail::address(UINTPTR_MAX-7,0,8,out)&&out==99);
    });
    for(unsigned mask:{0u,1u,2u,3u}) for(size_t i=0;i<2;++i)
        run("final site reread catches change mask "+std::to_string(mask)+" site "+std::to_string(i),[&,mask,i] {
            state(r,binding,plan,mask);r.change_site=binding.epilogue[i];r.change_after=2;
            require(!verify(r,binding,plan,mask));r.clear();state(r,binding,plan,0);
        });
    run("output published only after all original reads", [&] {
        auto out=sentinel();auto reader=[&](uintptr_t address,std::span<std::byte> bytes) {
            require(same(out,sentinel()),"Early output publication");return r(address,bytes);
        };
        require(wc::resolve(fixture::bases[0],fixture::bases[1],reader,out));require(same(out,binding));
    });
    run("no partial-word normalization", [&] {
        std::array<uint8_t,3> expected{};wg::GuardBytes partial{0x3a8408,expected};
        state(r,binding,plan,3);require(!wc::detail::match(binding.dart_base,wg::kDartImageSpan,partial,plan,3,true,r));
        state(r,binding,plan,0);
    });
    std::cout<<"RESULT passes="<<passed<<" failures="<<failed<<" guard_bit_mutations="<<bit_rejections
             <<"; actual ROM data and production binding, simulated memory reads; no installer/VM/device\n";
    return failed?1:0;
}
