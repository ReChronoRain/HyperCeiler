/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>

namespace home_layout {
// Discovery only, owned by the existing layout setup lease. Hook health and
// pending rollback must never be gated by this cooldown. Late prerequisites
// retain recovery, while a permanently unsupported image cannot scan at 2 Hz.
class BindingRetry {
    uint64_t next_=0,last_=0;
    unsigned attempts_=0;
public:
    void reset(){*this={};}
    bool due(uint64_t now){
        if(attempts_&&(!now||now<next_||now==last_))return false;
        constexpr uint64_t delays[]={500,2000,10000,30000};
        const auto delay=delays[attempts_<4?attempts_:3];
        next_=now>UINT64_MAX-delay?UINT64_MAX:now+delay;
        last_=now;if(attempts_<4)++attempts_;
        return true;
    }
};
}
