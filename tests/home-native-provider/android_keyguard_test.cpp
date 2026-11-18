/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Exercise the actual passive subscription/cleanup implementation in an
// independent process before using it in the launcher. No mock WMS or input.
#include "../../app/src/main/cpp/targets/home/home_native_keyguard.cpp"
#include <cstdio>
#include <array>
#include <unistd.h>
int main(int argc,char** argv) {
    if(argc!=4)return 1;
    std::array<uint32_t,3> codes{};
    for(size_t i=0;i<codes.size();++i){const auto n=std::stoul(argv[i+1]);if(n<FIRST_CALL_TRANSACTION||n>LAST_CALL_TRANSACTION)return 2;codes[i]=uint32_t(n);}
    if(codes[0]==codes[1])return 3;
    Session session({codes[0],codes[1],codes[2]});std::string reason;
    if(!session.start(nullptr,reason)){std::fprintf(stderr,"subscription: %s\n",reason.c_str());return 4;}
    std::puts("Subscribed real platform keyguard callback, passive observation only");
    sleep(3);
    const auto value=hc_native_keyguard_observation();
    std::printf("known=%d locked=%d callbacks=%llu observedUptimeNs=%llu uid=%u\n",value.known,value.locked,
        (unsigned long long)value.callbacks,(unsigned long long)value.observed_ns,getuid());
}
