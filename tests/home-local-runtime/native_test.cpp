/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_layout_config.h"
#include "targets/home/home_endpoint_diagnostics.h"
#include "targets/home/home_widget_move.h"
#include "targets/home/home_local_config_protocol.h"
#include "targets/home/home_local_config_channel.h"
#include "targets/home/home_local_config_lease.h"
#include <cassert>
#include <cstdarg>
#include <cstring>
#include <fstream>
#include <future>
#include <iterator>
#include <mutex>
#include <optional>
#include <vector>

// Host transport fixture only. The packet is produced by the actual Java Provider
// and HCL4 writer, and all decoding/publication/query behavior is production code.
struct AParcel { std::vector<uint8_t> bytes; mutable size_t position = 0; };
using binder_status_t = int;
constexpr int STATUS_OK=0, ANDROID_LOG_WARN=1, ANDROID_LOG_ERROR=2, ANDROID_LOG_INFO=3;
int AParcel_readInt32(const AParcel* p,int32_t* out) {
    if (p->position+4>p->bytes.size()) return -1;
    std::memcpy(out,p->bytes.data()+p->position,4);p->position+=4;return STATUS_OK;
}
int AParcel_getDataPosition(const AParcel* p){return static_cast<int>(p->position);}
int AParcel_getDataSize(const AParcel* p){return static_cast<int>(p->bytes.size());}
namespace home_local_config {
Lease live;
int64_t clock_ms=100;
bool selected_backend=true;
bool requested(){return selected_backend;}
uint64_t publication_generation(){return live.generation();}
bool publication_current(uint64_t generation){return live.current(generation,clock_ms);}
}
namespace home_layout {
static int binder_reads=0, disk_writes=0;
static bool disk_available=false;
static bool binder_available=true;
static uint64_t observer_revision=0;
static Config disk;
static void config_diagnostic(int,int,const char*,...) { }
static bool debug_override_enabled(){return false;}
static void apply_debug_override(Config&) { }
static bool query_binder(Config& value){++binder_reads;value={};return binder_available;}
uint64_t config_observer_revision(){return observer_revision;}
static bool read_config_cache(Config& value){if(!disk_available)return false;value=disk;return true;}
static bool write_config_cache(const Config& value){++disk_writes;disk=value;disk.local_haptic={};disk.local_dock={};disk.local_recent_cards.reset();disk.local_cards_generation=0;disk_available=true;return true;}
}
#include "actual.h"

static AParcel load(const std::string& path) {
    std::ifstream in(path,std::ios::binary);
    assert(in);return {{std::istreambuf_iterator<char>(in),{}}};
}
int main(int argc,char** argv) {
    assert(argc==2);
    using namespace home_layout;
    auto enabled=load(std::string(argv[1])+"/enabled.bin");
    auto reset=load(std::string(argv[1])+"/reset.bin");
    Config decoded;
    assert(read_layout_reply(&enabled,decoded,true));
    assert(decoded.grid_enabled&&decoded.cell_x==7&&decoded.cell_y==8);
    assert(decoded.knobs[2].enabled&&decoded.knobs[2].delta_dp==25);
    assert(decoded.object_scales[0].enabled&&decoded.object_scales[0].percent==83);
    assert(decoded.object_scales[1].enabled&&decoded.object_scales[1].percent==121);
    assert(decoded.object_scales[2].enabled&&decoded.object_scales[2].percent==130);
    assert(decoded.drawer_hide_search&&decoded.infinite_scroll&&decoded.dock_show_title);
    assert(decoded.title_custom_labels.size()==1);
    // Native reader must preserve both halves of the Java UTF-16 surrogate pair.
    assert(decoded.title_custom_labels[0].label==u"本地😀标题");
    int rejected=0;
    for(size_t size=0;size<enabled.bytes.size();size+=4){
        AParcel short_reply=enabled;short_reply.position=0;short_reply.bytes.resize(size);
        Config previous=decoded;
        assert(!read_layout_reply(&short_reply,previous,true));
        assert(previous.cell_x==7&&previous.object_scales[2].percent==130);
        ++rejected;
    }
    AParcel trailing=enabled;trailing.position=0;trailing.bytes.push_back(0);
    assert(!read_layout_reply(&trailing,decoded,true));

    // Observe the production layout cache with transport events at its boundary.
    observer_revision=1;
    assert(query_config(decoded,true)&&binder_reads==1);
    for(int pass=0;pass<10;++pass) assert(query_config(decoded,true)&&binder_reads==1);
    observer_revision=2;
    assert(query_config(decoded,true)&&binder_reads==2);
    observer_revision=3;binder_available=false;
    assert(query_config(decoded,true)&&binder_reads==3); // Last good layout, still retry this revision.
    binder_available=true;
    assert(query_config(decoded,true)&&binder_reads==4);
    assert(query_config(decoded,true)&&binder_reads==4);
    observer_revision=0;
    assert(query_config(decoded,true)&&binder_reads==5); // Retired or unsupported endpoint.
    assert(query_config(decoded,true)&&binder_reads==6);
    g_process_config.latest.reset();g_process_config.persisted.clear();
    binder_reads=0;disk_writes=0;disk_available=false;

    enabled.position=0;
    assert(!publish_local_reply(&enabled,1)); // No second backend may seize ownership implicitly.
    g_process_config.local_owned=true;
    enabled.position=0;
    assert(publish_local_reply(&enabled,1));
    assert(query_config(decoded)&&decoded.cell_x==7&&binder_reads==0&&disk_writes==1);
    enabled.position=0;
    assert(!publish_local_reply(&enabled,1)); // Duplicate and older revisions cannot overwrite.
    enabled.position=0;
    assert(!publish_local_reply(&enabled,0));
    enabled.position=0;
    assert(publish_local_reply(&enabled,2)&&disk_writes==1); // Same values avoid another disk write.
    AParcel bad=reset;bad.bytes[0]^=1;
    assert(!publish_local_reply(&bad,3));
    assert(query_config(decoded)&&decoded.cell_x==7&&binder_reads==0);
    reset.position=0;
    assert(publish_local_reply(&reset,4));
    assert(query_config(decoded)&&!decoded.grid_enabled&&decoded.cell_x==4);
    assert(!decoded.drawer_hide_search&&!decoded.infinite_scroll&&!decoded.dock_show_title);
    assert(decoded.title_custom_labels.empty()&&!decoded.object_scales[2].enabled);
    g_process_config.latest.reset(); // Process bootstrap from persisted full reset remains local.
    assert(query_config(decoded)&&!decoded.grid_enabled&&binder_reads==0);
    g_process_config.latest.reset();disk_available=false;
    assert(!query_config(decoded)&&binder_reads==0); // No live WMS fallback when local is unavailable.
    g_process_config.local_source=2;g_process_config.local_revision=0;
    home_local_config::Decoder channel;
    int64_t sequence=0;
    home_local_config::live.connect();
    auto root_publish=[&](const uint8_t* bytes,size_t size,uint64_t,uint32_t version){
        home_local_config::Snapshot packet;
        if(!home_local_config::read_snapshot(version,{bytes,size},packet)) {
            home_local_config::live.publication(false,home_local_config::clock_ms);return false;
        }
        AParcel parcel{{packet.layout.begin(),packet.layout.end()}};
        const bool accepted=publish_local_reply(&parcel,++sequence,2,packet.profile,std::move(packet.cards),home_local_config::publication_generation(),packet.dock);
        home_local_config::live.publication(accepted,home_local_config::clock_ms);return accepted;
    };
    auto root=load(std::string(argv[1])+"/root-enabled.bin");
    for(size_t start=0;start<root.bytes.size();start+=7)
        assert(channel.feed(root.bytes.data()+start,std::min<size_t>(7,root.bytes.size()-start),root_publish));
    assert(query_config(decoded)&&decoded.grid_enabled&&decoded.local_haptic.available);
    assert(decoded.local_haptic.enabled&&decoded.local_haptic.close&&decoded.local_haptic.type==3&&decoded.local_haptic.strength==70);
    assert(decoded.local_haptic.encoded()==0x46f);
    assert(decoded.local_recent_cards&&decoded.local_recent_cards->packages==std::vector<std::string>({"a.app","z.app"}));
    home_dock::Config dock;
    assert(local_dock_config(dock)&&dock==decoded.local_dock);
    assert((dock==home_dock::Config{true,true,3,0x8844aaffu,113,31,22,45,2,7,true,true}));
    auto corrupt=root.bytes;home_local_config::Snapshot dock_frame;
    assert(home_local_config::read_snapshot(home_local_config::kVersion,{corrupt.data()+home_local_config::kHeaderSize,
        corrupt.size()-home_local_config::kHeaderSize},dock_frame));
    const auto dock_at=dock_frame.layout.data()-corrupt.data()-4-home_local_config::kDockSize;
    assert(!dock_frame.haptic_waves);
    auto waves=load(std::string(argv[1])+"/root-wave-enabled.bin").bytes;
    auto read_waves=[&](const auto& data,home_local_config::Snapshot& packet) {
        return home_local_config::read_snapshot(home_local_config::kVersion,{data.data()+home_local_config::kHeaderSize,
            data.size()-home_local_config::kHeaderSize},packet);
    };
    home_local_config::Snapshot wave_packet;
    assert(read_waves(waves,wave_packet)&&wave_packet.haptic_waves&&wave_packet.haptic_waves->profile==0x46f);
    for(unsigned i=0;i<8;++i)assert((wave_packet.haptic_waves->waves[i]==std::vector<uint8_t>{uint8_t(i+1),2,3,4}));
    assert(wave_packet.layout.size()==dock_frame.layout.size());
    const size_t extension=wave_packet.layout.data()-waves.data()-4-8*8;
    for(auto [offset,value]:std::array<std::pair<size_t,uint32_t>,4>{{{0,1},{4,0},{4,3},{4,8196}}}) {
        auto bad=waves;home_local_config::put_word(bad.data()+extension+offset,value);
        assert(!read_waves(bad,wave_packet));
    }
    auto close_wave=load(std::string(argv[1])+"/root-wave-close.bin").bytes;
    assert(read_waves(close_wave,wave_packet)&&wave_packet.haptic_waves&&wave_packet.haptic_waves->profile==0x46e);
    assert(!wave_packet.haptic_waves->waves[0].empty()&&wave_packet.haptic_waves->waves[1].empty());
    auto v6=load(std::string(argv[1])+"/root-v6-enabled.bin").bytes;
    assert(home_local_config::read_snapshot(6,{v6.data()+home_local_config::kHeaderSize,
        v6.size()-home_local_config::kHeaderSize},wave_packet)&&!wave_packet.haptic_waves);
    for(auto [offset,value]:std::array<std::pair<size_t,uint32_t>,9>{{{0,2},{4,4},{8,2},{16,301},{20,151},{24,151},{28,61},{32,3},{36,8}}}) {
        auto bad_dock=root.bytes;home_local_config::put_word(bad_dock.data()+dock_at+offset,value);
        channel.reset();assert(!channel.feed(bad_dock.data(),bad_dock.size(),root_publish));
        assert(local_dock_config(dock)&&dock.height==113&&dock.style==7);
    }
    channel.reset();assert(channel.feed(root.bytes.data(),root.bytes.size(),root_publish));
    assert(query_config(decoded));
    auto selected=local_recent_cards();assert(selected==decoded.local_recent_cards);
    std::vector<uint8_t> cacheWithCards,cacheWithoutCards;
    serialize_config(decoded,cacheWithCards);
    decoded.local_recent_cards.reset();decoded.local_dock={};serialize_config(decoded,cacheWithoutCards);
    assert(cacheWithCards==cacheWithoutCards); // Live authorization never leaks to disk.
    auto broken=root.bytes;
    home_local_config::Snapshot framing;
    assert(home_local_config::read_snapshot(home_local_config::word(broken.data()+4),{broken.data()+home_local_config::kHeaderSize,
        broken.size()-home_local_config::kHeaderSize},framing));
    broken[framing.layout.data()-broken.data()]^=1;
    channel.reset();assert(!channel.feed(broken.data(),broken.size(),root_publish));
    assert(!local_recent_cards()&&query_config(decoded)&&decoded.grid_enabled&&decoded.local_recent_cards==selected);
    cards::Plan pending;
    std::array<void*,1> owned{reinterpret_cast<void*>(0x1000)};
    assert(cards::prepare(owned,selected,[](void*,auto visit){return visit(cards::TaskPackage{"a.app",{}, {}});},pending));
    assert(pending.removed==1);
    size_t kept=99;
    auto noDrop=+[](void*){assert(false);};
    // A complete same-value publication invalidates the captured query too.
    channel.reset();assert(channel.feed(root.bytes.data(),root.bytes.size(),root_publish));
    assert(local_recent_cards()!=selected);
    assert(!commit_local_recent_cards(owned,pending,noDrop,kept)&&kept==99&&owned[0]==reinterpret_cast<void*>(0x1000));
    auto unavailable=load(std::string(argv[1])+"/root-cards-unavailable.bin");channel.reset();
    assert(channel.feed(unavailable.bytes.data(),unavailable.bytes.size(),root_publish));
    assert(query_config(decoded)&&decoded.grid_enabled&&!decoded.local_recent_cards);
    assert(!commit_local_recent_cards(owned,pending,noDrop,kept)&&kept==99);
    assert(!local_recent_cards()); // Unavailable must clear, never merge the preceding selection.
    channel.reset();assert(channel.feed(root.bytes.data(),root.bytes.size(),root_publish));
    assert(local_recent_cards()&&local_recent_cards()!=selected);
    assert(cards::prepare(owned,local_recent_cards(),[](void*,auto visit){return visit(cards::TaskPackage{"a.app",{}, {}});},pending));
    std::promise<void> locked,release;
    auto wait=release.get_future();
    std::thread publisher([&]{std::lock_guard lock(g_process_config.mutex);locked.set_value();wait.wait();});
    locked.get_future().wait();
    assert(!local_recent_cards());
    auto retained=pending.selection;
    assert(!inspect_local_recent_cards(retained)&&retained==pending.selection);
    assert(!commit_local_recent_cards(owned,pending,noDrop,kept)&&kept==99);
    release.set_value();publisher.join();
    home_local_config::clock_ms+=home_local_config::kLeaseMs;
    assert(!local_recent_cards()&&!commit_local_recent_cards(owned,pending,noDrop,kept));
    assert(query_config(decoded)&&decoded.grid_enabled); // Layout cache remains available.
    home_local_config::live.observed(home_local_config::clock_ms);
    assert(local_recent_cards());
    home_local_config::selected_backend=false;
    assert(!local_recent_cards()&&!commit_local_recent_cards(owned,pending,noDrop,kept));
    home_local_config::selected_backend=true;
    auto old_generation=home_local_config::publication_generation();
    home_local_config::live.disconnect();home_local_config::live.connect();
    assert(home_local_config::publication_generation()!=old_generation&&!local_recent_cards());
    assert(!commit_local_recent_cards(owned,pending,noDrop,kept));
    channel.reset();assert(channel.feed(root.bytes.data(),root.bytes.size(),root_publish));
    assert(local_recent_cards()&&local_recent_cards()!=pending.selection);
    assert(cards::prepare(owned,local_recent_cards(),[](void*,auto visit){return visit(cards::TaskPackage{"a.app",{}, {}});},pending));
    static int drops=0;
    assert(commit_local_recent_cards(owned,pending,+[](void* value){assert(value==reinterpret_cast<void*>(0x1000));++drops;},kept));
    assert(kept==0&&drops==1);
    haptic::Profile profile;
    assert(local_haptic_profile(profile)&&profile.encoded()==0x46f);
    enabled.position=0;assert(!publish_local_reply(&enabled,99)); // Java cannot replace a Root publication.
    auto neutral=load(std::string(argv[1])+"/root-reset.bin");channel.reset();
    assert(channel.feed(neutral.bytes.data(),neutral.bytes.size(),root_publish));
    assert(query_config(decoded)&&!decoded.grid_enabled&&decoded.local_haptic.available&&!decoded.local_haptic.enabled&&!decoded.local_haptic.close);
    assert(local_recent_cards()&&local_recent_cards()->packages.empty());
    assert(local_dock_config(dock)&&!dock.enabled&&dock.height==150&&dock.margin==25&&dock.bottom==15&&dock.radius==30);
    auto capsule_off=load(std::string(argv[1])+"/root-dock-without-capsule.bin");channel.reset();
    assert(channel.feed(capsule_off.bytes.data(),capsule_off.bytes.size(),root_publish));
    assert(local_dock_config(dock)&&dock.enabled&&dock.capsule_known&&!dock.capsule_enabled);
    auto v5=load(std::string(argv[1])+"/root-v5-enabled.bin");channel.reset();
    assert(channel.feed(v5.bytes.data(),v5.bytes.size(),root_publish));
    assert(local_dock_config(dock)&&dock.enabled&&!dock.capsule_known&&!dock.capsule_enabled);
    auto unavailable_dock=load(std::string(argv[1])+"/root-dock-unavailable.bin");channel.reset();
    assert(channel.feed(unavailable_dock.bytes.data(),unavailable_dock.bytes.size(),root_publish));
    assert(query_config(decoded)&&!decoded.grid_enabled&&!local_dock_config(dock)&&!dock.available);
    auto legacy=root.bytes;
    assert(home_local_config::read_snapshot(home_local_config::word(legacy.data()+4),{legacy.data()+home_local_config::kHeaderSize,
        legacy.size()-home_local_config::kHeaderSize},framing));
    legacy.erase(legacy.begin()+home_local_config::kHeaderSize+home_local_config::kProfileSize,
        legacy.begin()+(framing.layout.data()-legacy.data()));
    home_local_config::put_word(legacy.data()+4,2);
    home_local_config::put_word(legacy.data()+8,static_cast<uint32_t>(legacy.size()-home_local_config::kHeaderSize));
    channel.reset();assert(channel.feed(legacy.data(),legacy.size(),root_publish));
    assert(query_config(decoded)&&decoded.grid_enabled&&!local_recent_cards()&&!local_dock_config(dock));
    for(uint32_t version:{3u,4u}) {
        legacy=root.bytes;
        assert(home_local_config::read_snapshot(home_local_config::kVersion,{legacy.data()+home_local_config::kHeaderSize,
            legacy.size()-home_local_config::kHeaderSize},framing));
        const auto at=framing.layout.data()-legacy.data();
        legacy.erase(legacy.begin()+at-4-home_local_config::kDockSize,legacy.begin()+at);
        home_local_config::put_word(legacy.data()+4,version);
        home_local_config::put_word(legacy.data()+8,static_cast<uint32_t>(legacy.size()-home_local_config::kHeaderSize));
        channel.reset();assert(channel.feed(legacy.data(),legacy.size(),root_publish));
        assert(query_config(decoded)&&decoded.grid_enabled&&local_recent_cards()&&!local_dock_config(dock));
    }
    auto emitted_v4=load(std::string(argv[1])+"/root-v4-enabled.bin");channel.reset();
    assert(channel.feed(emitted_v4.bytes.data(),emitted_v4.bytes.size(),root_publish));
    assert(query_config(decoded)&&decoded.grid_enabled&&local_recent_cards()&&!local_dock_config(dock));
    channel.reset();assert(channel.feed(neutral.bytes.data(),neutral.bytes.size(),root_publish));
    auto malformed=root.bytes;home_local_config::put_word(malformed.data()+home_local_config::kHeaderSize,4);
    channel.reset();assert(!channel.feed(malformed.data(),malformed.size(),root_publish));
    assert(query_config(decoded)&&!decoded.grid_enabled&&!decoded.local_haptic.enabled);
    g_process_config.latest.reset();
    assert(!local_recent_cards());
    assert(local_haptic_profile(profile)&&!profile.available); // Ownership remains local while unavailable.
    assert(binder_reads==0);
    std::printf("PASS Java/native full payload, %d truncations, UTF16, reset, monotonic publication, zero WMS queries after local ownership\n",rejected);
}
