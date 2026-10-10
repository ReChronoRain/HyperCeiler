/* Exercise the actual HCS1 decoder and atomic cache; no Android or phone execution. */
#include "targets/home/home_layout_config.h"
#include <array>
#include <atomic>
#include <bit>
#include <cstdint>
#include <mutex>
// Access control only: expose an odd publication version to test bounded reads.
// The production algorithms and atomic operations are compiled unchanged.
#define private public
#include "targets/hyos/hyos_snapshot.h"
#undef private
#include <chrono>
#include <cstdio>
#include <fstream>
#include <functional>
#include <limits>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

#define CHECK(v) do { if (!(v)) throw std::runtime_error(#v); } while (0)
using hyos_snapshot::Words;
using hyos_snapshot::Snapshot;
using hyos_snapshot::Cache;
constexpr int64_t kNow = 10'000'000'000LL;
void put_long(Words &words, size_t at, int64_t value) {
    const auto bits = std::bit_cast<uint64_t>(value);
    words[at] = std::bit_cast<int32_t>(static_cast<uint32_t>(bits));
    words[at + 1] = std::bit_cast<int32_t>(static_cast<uint32_t>(bits >> 32));
}
Words enabled(int64_t revision = 1) {
    Words words = hyos_snapshot::unavailable_words();
    words[1] = 1; words[2] = 63;
    put_long(words, 3, 123); put_long(words, 5, revision);
    put_long(words, 7, kNow + 5'000'000'000LL);
    words[9] = 1; words[10] = 7; words[11] = 10;
    for (size_t i = 0; i < 8; ++i) { words[13 + i * 2] = 1; words[14 + i * 2] = 20 + i; }
    const int32_t tweaks[]{4, 1, 11, 7, 1, 9, 6, 1, 115, 1, 1, 1, 1, 150, 1, 180, 1};
    for (size_t i = 0; i < 17; ++i) words[30 + i] = tweaks[i];
    return words;
}
int passed = 0, failed = 0;
void run(const std::string &name, const std::function<void()> &body) {
    try { body(); ++passed; std::printf("PASS %s\n", name.c_str()); }
    catch (const std::exception &e) { ++failed; std::printf("FAIL %s: %s\n", name.c_str(), e.what()); }
}
int main(int argc, char **argv) {
    if (argc == 3) {
        std::ifstream file(argv[1], std::ios::binary);
        std::vector<char> bytes((std::istreambuf_iterator<char>(file)), {});
        if (bytes.size() != 188) return 2;
        Words words{};
        for (size_t i = 0; i < words.size(); ++i) {
            uint32_t value = 0;
            for (size_t j = 0; j < 4; ++j)
                value |= static_cast<uint32_t>(static_cast<unsigned char>(bytes[i * 4 + j])) << (8 * j);
            words[i] = std::bit_cast<int32_t>(value);
        }
        Snapshot out;
        bool valid = hyos_snapshot::decode(words, std::stoll(argv[2]), out);
        std::printf("{\"valid\":%s,\"available\":%d,\"flags\":%u,\"epoch\":%lld,\"revision\":%lld,\"deadline\":%lld,\"grid\":[%d,%d,%d],\"knobs\":[",
            valid ? "true" : "false", out.available, out.flags, static_cast<long long>(out.epoch),
            static_cast<long long>(out.revision), static_cast<long long>(out.valid_until),
            out.config.grid_enabled, out.config.cell_x, out.config.cell_y);
        for (size_t i = 0; i < 8; ++i)
            std::printf("%s[%d,%d]", i ? "," : "", out.config.knobs[i].enabled, out.config.knobs[i].delta_dp);
        const auto &t = out.config.tweaks;
        std::printf("],\"tweaks\":[%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d]}\n",
            t.folder_cols,t.folder_enabled,t.pad_major,t.pad_minor,t.pad_enabled,t.fold_major,t.fold_minor,
            t.fold_enabled,t.icon_scale_code,t.icon_scale_enabled,t.recents_hide_clear,t.recents_no_clear,
            t.animation_open_enabled,t.animation_open_rate_percent,t.animation_recents_enabled,
            t.animation_recents_rate_percent,t.hotseat_unlimited);
        return 0;
    }
    run("all active fields and flags decoded", [] { Snapshot out; CHECK(hyos_snapshot::decode(enabled(), kNow, out)); CHECK(out.flags == 63 && out.config.cell_x == 7 && out.config.cell_y == 10); CHECK(out.config.knobs[7].delta_dp == 27 && out.config.tweaks.icon_scale_code == 115); });
    run("64-bit identity survives both halves", [] { auto w=enabled(); put_long(w,3,0x1122334455667788LL); put_long(w,5,0x2233445566778801LL); Snapshot out; CHECK(hyos_snapshot::decode(w,kNow,out)); CHECK(out.epoch==0x1122334455667788LL && out.revision==0x2233445566778801LL); });
    run("late delivery does not renew lease", [] { Snapshot out; auto w=enabled(); CHECK(hyos_snapshot::decode(w,kNow+4'000'000'000LL,out)); CHECK(out.valid_until==kNow+5'000'000'000LL); CHECK(!hyos_snapshot::decode(w,kNow+5'000'000'000LL,out)); });
    run("suspend-sized BOOTTIME jump expires cache", [] { Cache cache; CHECK(cache.publish(enabled(),kNow)); Snapshot out; CHECK(!cache.load_snapshot(kNow+60'000'000'000LL,out)); uint32_t flags=0; CHECK(!cache.authorization(kNow+60'000'000'000LL,flags)); });
    run("zero or failed clock cannot authorize", [] { Cache cache; CHECK(!cache.publish(enabled(),0)); Snapshot out; CHECK(!cache.load_snapshot(0,out)); uint32_t f=0; CHECK(!cache.authorization(-1,f)); });
    run("clock rollback and impossible future lease rejected", [] { Snapshot out; CHECK(!hyos_snapshot::decode(enabled(),kNow-1,out)); auto w=enabled(); put_long(w,7,INT64_MAX); CHECK(!hyos_snapshot::decode(w,kNow,out)); });
    run("disabled fields become neutral without stale cache", [] { Cache cache; CHECK(cache.publish(enabled(),kNow)); auto w=hyos_snapshot::unavailable_words(); w[1]=1;put_long(w,3,123);put_long(w,5,2); CHECK(cache.publish(w,kNow));Snapshot out;CHECK(cache.load_snapshot(kNow,out)&&out.available&&out.flags==0);CHECK(!out.config.grid_enabled&&out.config.cell_x==4&&out.config.cell_y==6);uint32_t f=0;CHECK(!cache.authorization(kNow,f)); });
    run("canonical unavailable revokes immediately", [] { Cache cache; CHECK(cache.publish(enabled(),kNow)); CHECK(cache.publish(hyos_snapshot::unavailable_words(),kNow)); Snapshot out; CHECK(cache.load_snapshot(kNow,out)&&!out.available); uint32_t f=0;CHECK(!cache.authorization(kNow,f)); });
    run("malformed publication clears previous authorization", [] {Cache cache; CHECK(cache.publish(enabled(),kNow));auto w=enabled();w[12]=7;CHECK(!cache.publish(w,kNow));uint32_t f=0;CHECK(!cache.authorization(kNow,f));});
    run("disabled stored deltas are canonicalized", [] {auto w=enabled();w[13]=0;w[14]=1234;Snapshot out;CHECK(hyos_snapshot::decode(w,kNow,out));CHECK(!out.config.knobs[0].enabled&&out.config.knobs[0].delta_dp==0);});
    run("same epoch revision rollback rejected across revocation", [] {Cache cache;CHECK(cache.publish(enabled(5),kNow));cache.revoke();CHECK(!cache.publish(enabled(4),kNow));uint32_t f=0;CHECK(!cache.authorization(kNow,f));CHECK(cache.publish(enabled(5),kNow));});
    run("different opaque epoch permits lower revision", [] {Cache cache;CHECK(cache.publish(enabled(10),kNow));auto w=enabled(1);put_long(w,3,1);CHECK(cache.publish(w,kNow));Snapshot out;CHECK(cache.load_snapshot(kNow,out)&&out.epoch==1&&out.revision==1);});
    run("reader never takes writer mutex", [] {Cache cache;CHECK(cache.publish(enabled(),kNow));std::lock_guard held(cache.writer_mutex_); Snapshot out;CHECK(cache.load_snapshot(kNow,out)&&out.flags==63);uint32_t f=0;CHECK(cache.authorization(kNow,f)&&f==63);});
    run("preempted odd version returns without waiting", [] {Cache cache;CHECK(cache.publish(enabled(),kNow));cache.sequence_.fetch_add(1);Snapshot out;uint32_t f=0;auto begin=std::chrono::steady_clock::now();CHECK(!cache.load_snapshot(kNow,out)&&!cache.authorization(kNow,f));CHECK(std::chrono::steady_clock::now()-begin<std::chrono::milliseconds(100));cache.sequence_.fetch_add(1);});
    for(int bits=0;bits<128;++bits)run("flags "+std::to_string(bits),[bits]{auto w=enabled();w[2]=bits;Snapshot out;const bool expected=bits>0&&bits<64&&(bits&1)&&(!(bits&8)||(bits&16));CHECK(hyos_snapshot::decode(w,kNow,out)==expected);});
    const std::array<std::pair<size_t,int32_t>,25> invalid{{{0,0},{1,2},{12,7},{29,16},{9,2},{10,2},{10,10},{11,3},{11,14},{13,2},{14,2401},{14,-2401},{14,INT32_MIN},{30,0},{31,2},{32,1},{34,2},{35,17},{37,2},{38,256},{39,2},{40,2},{43,29},{45,201},{46,2}}};
    for(size_t i=0;i<invalid.size();++i)run("invalid field "+std::to_string(i),[i,&invalid]{auto w=enabled();w[invalid[i].first]=invalid[i].second;Snapshot out;CHECK(!hyos_snapshot::decode(w,kNow,out));});
    for(size_t i=3;i<9;i+=2)run("invalid long "+std::to_string(i),[i]{auto w=enabled();put_long(w,i,-1);Snapshot out;CHECK(!hyos_snapshot::decode(w,kNow,out));});
    run("all inactive payload words must be canonical", [] {for(size_t i=9;i<47;++i){if(i==12||i==29)continue;auto w=hyos_snapshot::unavailable_words();w[i]=1;Snapshot out;CHECK(!hyos_snapshot::decode(w,kNow,out));}});
    run("real concurrent readers never mix publications", [] {
        Cache cache;std::atomic_bool done{false};std::atomic_int accepted{0},mismatched{0};
        std::vector<std::thread> readers;
        for(int i=0;i<4;++i)readers.emplace_back([&]{while(!done.load()){Snapshot out;if(cache.load_snapshot(kNow,out)&&out.available){++accepted;bool odd=(out.revision&1)!=0;if(out.config.cell_x!=(odd?3:9)||out.config.cell_y!=(odd?4:13)||out.flags!=(odd?3u:35u)||out.config.knobs[0].delta_dp!=(odd?111:222))++mismatched;}}});
        for(int64_t rev=1;rev<=20'000;++rev){auto w=enabled(rev);const bool odd=(rev&1)!=0;w[2]=odd?3:35;w[10]=odd?3:9;w[11]=odd?4:13;w[14]=odd?111:222;CHECK(cache.publish(w,kNow));}
        done=true;for(auto &t:readers)t.join();CHECK(accepted>0&&mismatched==0);
    });
    std::printf("RESULT passes=%d failures=%d; actual decoder/cache; simulated transport and BOOTTIME inputs; no Android injection\n",passed,failed);
    return failed?1:0;
}
