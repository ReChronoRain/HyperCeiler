/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <array>
#include <cstddef>
#include <cstdint>

namespace ambient_os4 {
// SHA-256 over bounded file bytes. No external crypto DSO or mutable shared state.
inline std::array<uint8_t, 32> sha256(const uint8_t* data, size_t length) noexcept {
    constexpr uint32_t k[64] = {
        0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
        0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
        0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
        0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
        0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
        0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
        0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
        0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2};
    uint32_t h[8] = {0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19};
    const auto rotate=[](uint32_t x, unsigned n) { return (x >> n) | (x << (32-n)); };
    const size_t blocks=(length+9+63)/64;
    for(size_t block=0; block<blocks; ++block) {
        uint32_t w[64]{};
        for(size_t i=0; i<64; ++i) {
            const size_t pos=block*64+i;
            uint8_t byte=0;
            if(pos<length) byte=data[pos];
            else if(pos==length) byte=0x80;
            else if(pos>=blocks*64-8) byte=static_cast<uint8_t>((static_cast<uint64_t>(length)*8) >> ((blocks*64-1-pos)*8));
            w[i/4] |= static_cast<uint32_t>(byte) << (24-(i%4)*8);
        }
        for(unsigned i=16;i<64;++i) {
            const uint32_t a=rotate(w[i-15],7)^rotate(w[i-15],18)^(w[i-15]>>3);
            const uint32_t b=rotate(w[i-2],17)^rotate(w[i-2],19)^(w[i-2]>>10);
            w[i]=w[i-16]+a+w[i-7]+b;
        }
        uint32_t a=h[0],b=h[1],c=h[2],d=h[3],e=h[4],f=h[5],g=h[6],z=h[7];
        for(unsigned i=0;i<64;++i) {
            const uint32_t t1=z+(rotate(e,6)^rotate(e,11)^rotate(e,25))+((e&f)^(~e&g))+k[i]+w[i];
            const uint32_t t2=(rotate(a,2)^rotate(a,13)^rotate(a,22))+((a&b)^(a&c)^(b&c));
            z=g;g=f;f=e;e=d+t1;d=c;c=b;b=a;a=t1+t2;
        }
        h[0]+=a;h[1]+=b;h[2]+=c;h[3]+=d;h[4]+=e;h[5]+=f;h[6]+=g;h[7]+=z;
    }
    std::array<uint8_t,32> result{};
    for(size_t i=0;i<32;++i) result[i]=static_cast<uint8_t>(h[i/4]>>(24-(i%4)*8));
    return result;
}
} // namespace ambient_os4
