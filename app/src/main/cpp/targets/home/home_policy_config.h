/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
namespace home_layout {
constexpr int32_t kPolicyMagic=0x4843504f; // HCPO, two 32-bit words, low word first.
constexpr uint64_t kHideReportPolicy=1;
constexpr uint64_t kSupportedPolicies=kHideReportPolicy;
template<class Read> bool read_policy_config(Read read,uint64_t& flags,bool optional=true,bool* present=nullptr) {
    flags=0;if(present)*present=false;int32_t magic=0,count=0,lo=0,hi=0;
    if(!read(magic))return optional;
    if(magic!=kPolicyMagic||!read(count)||count!=2||!read(lo)||!read(hi))return false;
    const auto value=uint64_t(uint32_t(lo))|(uint64_t(uint32_t(hi))<<32);
    if(value&~kSupportedPolicies)return false;
    flags=value;if(present)*present=true;return true;
}
}
