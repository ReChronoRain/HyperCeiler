/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>

namespace home_dock {
struct Config {
    bool available=false,enabled=false;
    int32_t mode=1;uint32_t color=0;
    int32_t height=150,margin=25,bottom=15,radius=30,night=0,style=0;
    bool capsule_known=false,capsule_enabled=false;
    bool operator==(const Config&)const=default;
};
}
