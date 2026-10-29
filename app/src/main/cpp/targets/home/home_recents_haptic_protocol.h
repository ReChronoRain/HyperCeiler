/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
namespace home_layout::haptic {
constexpr int32_t kTransaction=0x00484356,kAck=0x48435631;
constexpr int32_t kTransactionV2=0x00484357,kAckV2=0x48435632;
struct Reply {bool available=false,enabled=false,performed=false;int type=0,strength=50;bool close=false;};
template<class Read> bool read_reply(Read read,Reply& out,bool extended=false){
    out={};int32_t ack=0,available=0,enabled=0,type=0,strength=0,result=0,close=0;
    if(!read(ack)||!read(available)||!read(enabled)||(extended&&!read(close))||!read(type)||!read(strength)||!read(result)
        ||ack!=(extended?kAckV2:kAck)||close<0||close>1||available<0||available>1||enabled<0||enabled>1||type<0||type>3
        ||strength<10||strength>100||result<0||result>1||(!available&&(enabled||close||result)))return false;
    out={available!=0,enabled!=0,result!=0,type,strength,close!=0};return true;
}
}
