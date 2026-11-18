/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_local_config_channel.h"
#include "home_local_config_protocol.h"
#include "home_local_config_lease.h"
#include "home_native_vibrator.h"
#include <array>
#include <android/log.h>
#include <sys/system_properties.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <fcntl.h>
#include <cerrno>
#include <cstdio>
#include <cstring>
#include <mutex>
#include <chrono>
#include <algorithm>
#include <atomic>
#include <poll.h>
#include <time.h>

namespace home_local_config {
namespace {
constexpr std::array kinds={"config7","config6","config5","config"};
std::array<int,kinds.size()> listeners{-1,-1,-1,-1};
int peer=-1;
uint32_t peer_version=kVersion;
std::atomic<int> action_listener{-1};
Lease lease;
std::atomic_bool cards_ready{false};
std::atomic_bool connected{false};
std::atomic<uint64_t> haptic_revision{0};
std::shared_ptr<const HapticWavePack> published_waves;
uint64_t acknowledged_sequence=0;
uint32_t acknowledged_version=0,sent_status=0;
bool acknowledged_acceptance=false;
Decoder decoder;
unsigned reports=0;
std::chrono::steady_clock::time_point retry;
int64_t lease_time(){timespec now{};return clock_gettime(CLOCK_BOOTTIME,&now)==0?int64_t(now.tv_sec)*1000+now.tv_nsec/1000000:-1;}
void disconnected(){connected.store(false,std::memory_order_release);lease.disconnect();std::atomic_store_explicit(&published_waves,std::shared_ptr<const HapticWavePack>{},std::memory_order_release);if(peer>=0)close(peer);peer=-1;decoder.reset();
    acknowledged_sequence=0;acknowledged_version=0;acknowledged_acceptance=false;sent_status=0;}
bool send_status(){
    const uint32_t flags=acknowledged_acceptance?(kAckAccepted
        |(acknowledged_version>=4&&cards_ready.load(std::memory_order_acquire)?kAckNativeCards:0u)):0u;
    uint8_t ack[16];put_word(ack,kAck);put_word(ack+4,static_cast<uint32_t>(acknowledged_sequence>>32));
    put_word(ack+8,static_cast<uint32_t>(acknowledged_sequence));put_word(ack+12,flags);
    if(send(peer,ack,sizeof(ack),MSG_DONTWAIT|MSG_NOSIGNAL)!=sizeof(ack))return false;
    sent_status=flags;return true;
}
int make_listener(const char* kind) {
    int fd=socket(AF_UNIX,SOCK_STREAM|SOCK_NONBLOCK|SOCK_CLOEXEC,0);
    if(fd<0)return -1;
    sockaddr_un address{};address.sun_family=AF_UNIX;
    const int count=std::snprintf(address.sun_path+1,sizeof(address.sun_path)-1,
        "hc.home.%s.%d.%d",kind,static_cast<int>(getuid()),static_cast<int>(getpid()));
    const auto length=static_cast<socklen_t>(offsetof(sockaddr_un,sun_path)+1+count);
    if(count<=0||static_cast<size_t>(count)>=sizeof(address.sun_path)-1
        ||bind(fd,reinterpret_cast<sockaddr*>(&address),length)!=0||listen(fd,1)!=0) {
        close(fd);return -1;
    }
    return fd;
}
int accept_root(int fd) {
    if(fd<0)return -1;
    int candidate=accept4(fd,nullptr,nullptr,SOCK_NONBLOCK|SOCK_CLOEXEC);
    if(candidate<0)return -1;
    ucred identity{};socklen_t size=sizeof(identity);
    if(getsockopt(candidate,SOL_SOCKET,SO_PEERCRED,&identity,&size)!=0||identity.uid!=0) {
        close(candidate);return -1;
    }
    return candidate;
}

}
uint64_t publication_generation(){return lease.generation();}
bool peer_connected(){return connected.load(std::memory_order_acquire);}
void set_cards_ready(bool ready){cards_ready.store(ready,std::memory_order_release);}
bool publication_current(uint64_t generation){return lease.current(generation,lease_time());}
bool requested() {
#ifdef HYPERCEILER_HYOS_BACKEND
    return false; // The separate authenticated HCS backend does not use this APK experiment.
#else
    char value[PROP_VALUE_MAX]={};
    return __system_property_get("debug.hyperceiler.home_config_backend",value)>0
        &&std::strcmp(value,"root-socket")==0;
#endif
}
void poll(Publish publish) {
    static std::mutex mutex;
    std::lock_guard<std::mutex> lock(mutex);
    if(std::ranges::any_of(listeners,[](int fd){return fd<0;})||action_listener.load(std::memory_order_acquire)<0) {
        const auto now=std::chrono::steady_clock::now();
        if(now<retry)return;
        const unsigned delays[]={500,2000,10000,30000};
        retry=now+std::chrono::milliseconds(delays[std::min(reports,3u)]);
        for(size_t i=0;i<listeners.size();++i)if(listeners[i]<0)listeners[i]=make_listener(kinds[i]);
        if(action_listener.load(std::memory_order_acquire)<0)
            action_listener.store(make_listener("haptic"),std::memory_order_release);
        if(std::ranges::any_of(listeners,[](int fd){return fd<0;})||action_listener.load(std::memory_order_acquire)<0) {
            if(reports++<3)__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig",
                "local socket listener unavailable errno=%d",errno);
            return;
        }
        reports=0;
        __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","local socket ready pid=%d",getpid());
    }
    // All calls are on the existing configuration worker, never the Dart/UI thread.
    if(peer<0) {
        int candidate=-1;
        for(size_t i=0;i<listeners.size();++i) {
            candidate=accept_root(listeners[i]);
            if(candidate>=0){peer_version=kVersion-static_cast<uint32_t>(i);break;}
        }
        if(candidate<0)return;
        peer=candidate;decoder.reset();lease.connect();connected.store(true,std::memory_order_release);
    }
    hc_native_vibrator_refresh();
    unsigned char data[16*1024];
    // Bound work per native worker turn; partial frames stay private until complete.
    for(unsigned budget=0;budget<32;++budget) {
        const ssize_t size=recv(peer,data,sizeof(data),MSG_DONTWAIT);
        if(size<0&&(errno==EAGAIN||errno==EWOULDBLOCK)){
            lease.observed(lease_time());
            // Re-advertise capability changes against the same accepted frame;
            // no extra heartbeat, snapshot publication or polling thread.
            const uint32_t status=kAckAccepted|(cards_ready.load(std::memory_order_acquire)?kAckNativeCards:0u);
            if(acknowledged_acceptance&&acknowledged_version>=4&&sent_status!=status&&!send_status())disconnected();
            return;
        }
        if(size<0&&errno==EINTR)continue;
        if(size<=0){disconnected();return;}
        const bool good=decoder.feed(data,static_cast<size_t>(size),[&](const uint8_t* bytes,size_t length,uint64_t sequence,uint32_t version){
            if(version>peer_version)return false;
            std::atomic_store_explicit(&published_waves,std::shared_ptr<const HapticWavePack>{},std::memory_order_release);
            haptic_revision.fetch_add(1,std::memory_order_acq_rel);
            std::shared_ptr<const HapticWavePack> waves;
            const bool accepted=publish(bytes,length,version,waves);
            lease.publication(accepted,lease_time());
            if(accepted)std::atomic_store_explicit(&published_waves,std::move(waves),std::memory_order_release);
            acknowledged_sequence=sequence;acknowledged_version=version;acknowledged_acceptance=accepted;
            return send_status();
        });
        if(!good){disconnected();return;}
    }
}
bool perform_haptic(uint32_t profile,int operation,int64_t timestamp,int count) {
    static std::mutex mutex;
    static int channel=-1;
    static uint64_t generation=0;
    std::lock_guard<std::mutex> lock(mutex);
    const uint64_t live_generation=lease.generation();
    if(generation!=live_generation){if(channel>=0)close(channel);channel=-1;generation=live_generation;}
    const auto age=[timestamp] {
        return std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count()-timestamp;
    };
    const bool direct=hc_native_vibrator_ready();
    const bool configured=operation==1?(profile&1)!=0:operation==2&&(profile&8)!=0&&count==1;
    const bool cached=direct&&hc_native_vibrator_cache_selected()&&requested()&&configured&&count>=1&&count<=8;
    const auto revision=haptic_revision.load(std::memory_order_acquire);
    static uint64_t cached_generation=0,cached_revision=0;
    static uint32_t cached_profile=0;
    static std::array<std::vector<uint8_t>,8> waves;
    static unsigned cache_reports=0;
    std::vector<uint8_t>* entry=nullptr;
    if(cached) {
        if(cached_generation!=generation||cached_revision!=revision||cached_profile!=profile) {
            cached_generation=generation;cached_revision=revision;cached_profile=profile;
            for(auto& wave:waves)wave.clear();
        }
        entry=&waves[static_cast<size_t>(count-1)];
        auto pack=hc_native_vibrator_published_selected()?std::atomic_load_explicit(&published_waves,std::memory_order_acquire):nullptr;
        const auto& prepared=pack&&pack->profile==profile?pack->waves[static_cast<size_t>(count-1)]:*entry;
        if(!prepared.empty()&&publication_current(generation)) {
            const auto allowed=hc_native_vibrator_policy_allows();
            if(allowed) {
                const auto elapsed=age();
                const bool current=hc_native_vibrator_cache_selected()&&requested()&&elapsed>=0&&elapsed<200
                    &&publication_current(generation)&&revision==haptic_revision.load(std::memory_order_acquire);
                const bool performed=current&&*allowed&&hc_native_vibrator_play(prepared);
                if(cache_reports++<12)__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeLayout",
                    "launcher haptic %s count=%d allowed=%d current=%d submitted=%d",
                    &prepared==entry?"cache hit":"published waveform",count,*allowed,current,performed);
                return performed;
            }
            // Unknown local policy uses the original authenticated preparation.
        }
    }
    if(channel<0)channel=accept_root(action_listener.load(std::memory_order_acquire));
    if(channel<0)return false;
    auto failed=[&](){close(channel);channel=-1;return false;};
    uint8_t request[24];put_word(request,direct?kHapticPrepare:kHapticRequest);put_word(request+4,profile);
    put_word(request+8,static_cast<uint32_t>(operation));
    put_word(request+12,static_cast<uint32_t>(uint64_t(timestamp)>>32));
    put_word(request+16,static_cast<uint32_t>(timestamp));put_word(request+20,static_cast<uint32_t>(count));
    if(send(channel,request,sizeof(request),MSG_DONTWAIT|MSG_NOSIGNAL)!=sizeof(request))return failed();
    const auto read_reply=[&](std::span<uint8_t> bytes) {
        while(!bytes.empty()) {
            const auto elapsed=age();
            if(elapsed<0||elapsed>=200)return false;
            pollfd descriptor{channel,POLLIN,0};
            const int status=::poll(&descriptor,1,static_cast<int>(200-elapsed));
            if(status<0&&errno==EINTR)continue;
            if(status<=0)return false;
            const auto received=recv(channel,bytes.data(),bytes.size(),MSG_DONTWAIT);
            if(received<0&&(errno==EINTR||errno==EAGAIN||errno==EWOULDBLOCK))continue;
            if(received<=0)return false;
            bytes=bytes.subspan(static_cast<size_t>(received));
        }
        return true;
    };
    uint8_t ack[16];
    if(!read_reply(ack))return failed();
    const uint64_t echoed=(uint64_t(word(ack+4))<<32)|word(ack+8);
    const auto length=word(ack+12);
    if(word(ack)!=(direct?kHapticWave:kHapticAck)||echoed!=uint64_t(timestamp))return failed();
    if(!direct)return length<=1?length==1:failed();
    if(!length)return false; // Policy refusal never falls through to playback.
    if(length>kMaxHapticWave||(length&3)!=0)return failed();
    std::array<uint8_t,kMaxHapticWave> wave;
    const auto payload=std::span(wave).first(length);
    if(!read_reply(payload))return failed();
    const auto elapsed=age();
    if(elapsed<0||elapsed>=200||!publication_current(generation)
        ||(cached&&revision!=haptic_revision.load(std::memory_order_acquire)))return false;
    const bool performed=hc_native_vibrator_play(payload);
    if(entry&&performed)entry->assign(payload.begin(),payload.end());
    return performed;
}

}
