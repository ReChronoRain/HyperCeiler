/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_cards_sdk.h"
#include <map>

namespace home_layout::cards::sdk {
// A bounded data-flow trace for the native AAPCS64 SDK, not the Dart VM ABI.
// Unknown writes discard provenance. Only explicit memory/call relationships
// may authorize a field; an unknown instruction is never treated as a move.
struct Value {
    enum Kind {Unknown,Self,Stack,Parcel,Constant,Field,Nullable,Result,RelativeField,Returned,Selected,IndirectField,
        RelativeResult,ScaledResult,PointerEnd,Distance,Count,Literal,ScalarResult,Argument} kind=Unknown;
    int64_t offset=0;
    uint64_t number=0;
    unsigned width=0;
    int64_t extra=0;
    uint64_t factor=0;
    bool operator==(const Value&)const=default;
};
struct Call {size_t index;std::optional<Address> target;Value receiver,parcel,output,third,fourth;std::string_view linkage;Value fifth{},sixth{},float0{},float1{},float2{},float3{};};
struct Store {size_t index;Value address,value;unsigned width;};
struct Load {size_t index;Value address;unsigned width;};
struct Compare {size_t index;Value field,value;};
struct Select {size_t index;unsigned destination,condition;Value yes,no,field,value;};
struct Step {size_t index;unsigned destination;Value input;int64_t delta;bool flags;};
struct Trace {
    std::array<Value,32> regs{},floats{};
    std::vector<Call> calls;
    std::vector<Store> stores;
    std::vector<Load> loads;
    std::vector<Compare> comparisons;
    std::vector<Compare> branch_comparisons;
    std::vector<Select> selects;
    std::vector<Step> steps;
    std::vector<size_t> branches,unknown;
    Value compared_field,compared_value;
    bool returned=false;
};
inline Value plus(Value value,int64_t delta) {
    if(value.kind==Value::Result||value.kind==Value::RelativeResult) {
        if(value.kind==Value::Result){value.kind=Value::RelativeResult;value.extra=0;}
        if((delta>0&&value.extra>INT64_MAX-delta)||(delta<0&&value.extra<INT64_MIN-delta))return {};
        value.extra+=delta;
        if(value.extra<-(1<<20)||value.extra>(1<<20))return {};
        return value;
    }
    if(value.kind==Value::Literal) {
        if((delta<0&&value.number<static_cast<uint64_t>(-delta))
            ||(delta>0&&value.number>UINT64_MAX-static_cast<uint64_t>(delta)))return {};
        value.number+=static_cast<uint64_t>(delta);return value;
    }
    if(value.kind==Value::Self||value.kind==Value::Stack||value.kind==Value::Returned||value.kind==Value::Argument){
        if((delta>0&&value.offset>INT64_MAX-delta)||(delta<0&&value.offset<INT64_MIN-delta))return {};
        value.offset+=delta;
        // Abstract interpretation budget, not a field or allocator ABI constant.
        if(value.offset<-(1<<20)||value.offset>(1<<20))return {};
        return value;
    }
    if(value.kind==Value::Field){value.kind=Value::RelativeField;value.number=static_cast<uint64_t>(delta);return value;}
    if(value.kind==Value::Constant){value.number+=static_cast<uint64_t>(delta);return value;}
    return {};
}
inline std::optional<uint64_t> logical_immediate(uint32_t w) {
    const unsigned bits=(w>>31)?64:32,n=(w>>22)&1,r=(w>>16)&63,s=(w>>10)&63;
    if(bits==32&&n)return {};
    const unsigned encoded=(n<<6)|((~s)&63);
    if(encoded<2)return {};
    unsigned len=0;for(unsigned v=encoded;v>>=1;)++len;
    const unsigned width=1u<<len,levels=width-1,ones=(s&levels)+1,rotate=r&levels;
    if(width>bits||ones==width)return {};
    const uint64_t mask=width==64?UINT64_MAX:(uint64_t(1)<<width)-1;
    uint64_t element=(uint64_t(1)<<ones)-1;
    if(rotate)element=((element>>rotate)|(element<<(width-rotate)))&mask;
    uint64_t result=0;for(unsigned shift=0;shift<bits;shift+=width)result|=element<<shift;
    return result;
}
inline Trace trace(const Graph& graph,const Body& body,bool parcel_input=false,bool sret_output=false,
    std::span<const size_t> path={},std::span<const std::pair<unsigned,Value>> inputs={},
    std::span<const std::pair<unsigned,Value>> float_inputs={}) {
    Trace out;out.regs[0]={parcel_input?Value::Parcel:Value::Self};
    if(!parcel_input)out.regs[1]={Value::Parcel};
    if(sret_output)out.regs[8]={Value::Self};out.regs[31]={Value::Stack};
    for(const auto& input:inputs)if(input.first<31)out.regs[input.first]=input.second;
    struct Byte {Value value;unsigned index;};
    std::map<int64_t,Byte> stack;
    auto& vector_regs=out.floats;
    for(const auto& input:float_inputs)if(input.first<32)vector_regs[input.first]=input.second;
    const auto clobber_float=[&] {
        // AAPCS64 preserves the low 64 bits of v8-v15 across native calls.
        for(unsigned n=0;n<32;++n)if(n<8||n>15||vector_regs[n].width>8)vector_regs[n]={};
    };
    const auto write_stack=[&](int64_t at,Value value,unsigned width) {
        for(unsigned n=0;n<width;++n)stack[at+n]={value,n};
    };
    const auto read_stack=[&](int64_t at,unsigned width)->Value {
        const auto first=stack.find(at);if(first==stack.end())return {};
        const auto origin=first->second;
        for(unsigned n=0;n<width;++n) {
            const auto next=stack.find(at+n);if(next==stack.end())return {};
            const auto part=next->second;
            if(origin.value.kind==Value::Result||origin.value.kind==Value::Field) {
                if(part.value.kind!=origin.value.kind||part.value.number!=origin.value.number
                    ||part.value.offset+part.index!=origin.value.offset+origin.index+n)return {};
            } else if(part.value!=origin.value||part.index!=origin.index+n)return {};
        }
        if(origin.value.kind==Value::Result||origin.value.kind==Value::Field)
            return {origin.value.kind,origin.value.offset+origin.index,origin.value.number,width};
        if(origin.index==0&&origin.value.width==width)return origin.value;
        return {};
    };
    const auto reg=[&](unsigned n,bool sp=false){return n==31&&!sp?Value{Value::Constant}:out.regs[n];};
    const auto compare=[&](size_t index,Value a,Value b){
        if(b.kind==Value::Field)std::swap(a,b);
        out.compared_field=a;out.compared_value=b;
        out.comparisons.push_back({index,a,b});
    };
    const size_t steps=path.empty()?body.words.size():path.size();
    for(size_t step=0;step<steps;++step) {
        const size_t i=path.empty()?step:path[step];
        const uint32_t w=body.words[i];const unsigned rd=w&31,rn=(w>>5)&31,rm=(w>>16)&31;
        const bool wide=(w>>31)!=0;
        if(w==0xd65f03c0u){out.returned=true;continue;}
        if((w&0x9f000000u)==0x10000000u||(w&0x9f000000u)==0x90000000u) {
            const uint32_t encoded=(((w>>5)&0x7ffff)<<2)|((w>>29)&3);
            const int64_t delta=int32_t(encoded<<11)>>11;
            const int64_t pc=static_cast<int64_t>(body.address.va+i*4);
            const int64_t address=(w&(1u<<31))?(pc&~int64_t(4095))+delta*4096:pc+delta;
            out.regs[rd]=address>=0?Value{Value::Literal,static_cast<int64_t>(body.address.image),static_cast<uint64_t>(address),8}:Value{};
            continue;
        }
        if((w&0xfffffc1fu)==0xd63f0000u) {
            // An indirect call still clobbers AAPCS64 volatile registers. Its
            // unknown target cannot authorize an allocator/serializer result.
            out.calls.push_back({i,{},reg(0),reg(1),reg(8),reg(2),reg(3),{},reg(4),reg(5),vector_regs[0],vector_regs[1],vector_regs[2],vector_regs[3]});
            const auto output=reg(8);if(output.kind==Value::Stack)write_stack(output.offset,{},256);
            clobber_float();for(unsigned n=0;n<=18;++n)out.regs[n]={};out.regs[30]={};
            out.unknown.push_back(i);out.compared_field={};out.compared_value={};continue;
        }
        if((w&0xfc000000u)==0x94000000u) {
            const auto raw=graph.raw_callee(body,i);
            const auto name=raw?graph.import_name(*raw):std::nullopt;
            out.calls.push_back({i,graph.callee(body,i),reg(0),reg(1),reg(8),reg(2),reg(3),name.value_or(""),reg(4),reg(5),vector_regs[0],vector_regs[1],vector_regs[2],vector_regs[3]});
            const auto output=reg(8);
            if(output.kind==Value::Stack) {
                // Symbolic propagation budget, never a runtime object-size claim.
                const Value result=out.calls.back().target?Value{Value::Result,0,out.calls.size()-1,256}:Value{};
                write_stack(output.offset,result,256);
            }
            clobber_float();
            for(unsigned n=0;n<=18;++n)out.regs[n]={};
            out.regs[0]={Value::Returned,0,out.calls.size()-1,8};
            out.regs[30]={};out.compared_field={};out.compared_value={};continue;
        }
        if((w&0x7c000000u)==0x14000000u||(w&0xff000010u)==0x54000000u
            ||(w&0x7e000000u)==0x34000000u||(w&0x7e000000u)==0x36000000u){
            out.branches.push_back(i);
            if((w&0xff000010u)==0x54000000u)out.branch_comparisons.push_back({i,out.compared_field,out.compared_value});
            continue;
        }
        if(w==0xd503233fu||w==0xd50323bfu||w==0xd503201fu||w==0xd503245fu)continue;
        if((w&0x1f800000u)==0x12000000u) {
            const auto bits=logical_immediate(w);const auto input=reg(rn);
            if(!bits)out.unknown.push_back(i);
            if(!bits||input.kind!=Value::Constant)out.regs[rd]={};
            else {
                const unsigned op=(w>>29)&3;
                out.regs[rd]={Value::Constant,0,op==1?(input.number|*bits):op==2?(input.number^*bits):(input.number&*bits)};
            }
            if(((w>>29)&3)==3){out.compared_field={};out.compared_value={};}
            continue;
        }
        if((w&0x7f800000u)==0x52800000u||(w&0x7f800000u)==0x12800000u||(w&0x7f800000u)==0x72800000u) {
            const auto shift=((w>>21)&3)*16;const auto immediate=uint64_t((w>>5)&65535)<<shift;
            if(!wide&&shift>=32){out.unknown.push_back(i);out.regs[rd]={};continue;}
            const auto op=(w>>29)&3;
            if(op==3) {
                if(out.regs[rd].kind==Value::Constant)out.regs[rd].number=(out.regs[rd].number&~(uint64_t(65535)<<shift))|immediate;
                else out.regs[rd]={};
            } else out.regs[rd]={Value::Constant,0,op==0?~immediate:immediate};
            if(!wide&&out.regs[rd].kind==Value::Constant)out.regs[rd].number&=UINT32_MAX;
            continue;
        }
        if((w&0x7fe0ffe0u)==0x2a0003e0u) {
            auto value=reg(rm);
            if(!wide) {
                if(value.kind==Value::Constant)value.number&=UINT32_MAX;
                else if(value.kind==Value::Argument&&value.width==4) {}
                else if(value.kind==Value::Returned||value.kind==Value::ScalarResult) {
                    // A narrowed return value is a scalar, never allocator or
                    // object-pointer provenance. Keep its call index intact.
                    value.kind=Value::ScalarResult;value.width=4;
                } else value={};
            }
            out.regs[rd]=value;continue;
        }
        if((w&0x1f000000u)==0x11000000u) {
            const int64_t imm=int64_t((w>>10)&4095)<<(((w>>22)&1)*12);
            const auto input=reg(rn,true);
            out.steps.push_back({i,rd,input,(w&0x40000000u)?-imm:imm,(w&0x20000000u)!=0});
            if((w&0x20000000u)&&rd==31) {
                auto field=input;if(!wide)field.width=4;
                if(w&0x40000000u)compare(i,field,{Value::Constant,0,static_cast<uint64_t>(imm)});
                else {out.compared_field={};out.compared_value={};}
            }
            else {
                out.regs[rd]=wide?plus(input,(w&0x40000000u)?-imm:imm):Value{};
                if(w&0x20000000u){out.compared_field={};out.compared_value={};}
            }
            continue;
        }
        if((w&0x7fe0fc1fu)==0x6b00001fu) {
            auto field=reg(rn),value=reg(rm);if(!wide){field.width=4;value.width=4;value.number&=UINT32_MAX;}
            compare(i,field,value);continue;
        }
        if((w&0xff200000u)==0x8b000000u) {
            if((w>>22)&3){out.unknown.push_back(i);out.regs[rd]={};continue;}
            const auto rhs=reg(rm);
            if(rhs.kind==Value::Constant)out.steps.push_back({i,rd,reg(rn),static_cast<int64_t>(rhs.number<<((w>>10)&63)),false});
            const auto lhs=reg(rn);
            if(rhs.kind==Value::Constant)out.regs[rd]=plus(lhs,static_cast<int64_t>(rhs.number<<((w>>10)&63)));
            else if(!((w>>10)&63)&&lhs.kind==Value::Result&&rhs.kind==Value::ScaledResult&&lhs.number==rhs.number)
                out.regs[rd]={Value::PointerEnd,lhs.offset,lhs.number,8,rhs.offset,rhs.factor};
            else out.regs[rd]={};
            continue;
        }
        if((w&0xffe0fc00u)==0x9b007c00u) {
            auto lhs=reg(rn),rhs=reg(rm);if(lhs.kind==Value::Constant)std::swap(lhs,rhs);
            if(lhs.kind==Value::Result&&rhs.kind==Value::Constant&&rhs.number&&rhs.number<=(1u<<20))
                out.regs[rd]={Value::ScaledResult,lhs.offset,lhs.number,8,0,rhs.number};
            else if(lhs.kind==Value::Constant&&rhs.kind==Value::Constant)out.regs[rd]={Value::Constant,0,lhs.number*rhs.number};
            else out.regs[rd]={};
            continue;
        }
        if((w&0xdf200000u)==0xcb000000u) {
            const auto lhs=reg(rn),rhs=reg(rm);
            if(!((w>>10)&63)&&!((w>>22)&3)&&(lhs.kind==Value::Result||lhs.kind==Value::RelativeResult)
                &&rhs.kind==Value::Result&&lhs.number==rhs.number&&lhs.offset==rhs.offset)
                out.regs[rd]={Value::Distance,lhs.offset,lhs.number,8,lhs.kind==Value::RelativeResult?lhs.extra:0};
            else out.regs[rd]={};
            if(w&(1u<<29)){out.compared_field={};out.compared_value={};}
            continue;
        }
        // CSET is CSINC with two zero registers. Preserve its comparison as a
        // boolean selection, including the encoded (inverted alias) condition.
        if((w&0x7fff0fe0u)==0x1a9f07e0u) {
            out.selects.push_back({i,rd,(w>>12)&15,{Value::Constant,0,0},{Value::Constant,0,1},out.compared_field,out.compared_value});
            out.regs[rd]={Value::Selected,0,out.selects.size()-1,wide?8u:4u};continue;
        }
        if((w&0xffe00c00u)==0x9a800000u) {
            const auto yes=reg(rn),no=reg(rm);
            out.selects.push_back({i,rd,(w>>12)&15,yes,no,out.compared_field,out.compared_value});
            if(((w>>12)&15)==0&&yes.kind==Value::Constant&&yes.number==0
                &&no.kind==Value::Self&&out.compared_field.kind==Value::Field
                &&out.compared_field.offset==no.offset&&out.compared_field.width==8&&out.compared_value.kind==Value::Constant)
                out.regs[rd]={Value::Nullable,no.offset,out.compared_value.number};
            else out.regs[rd]={Value::Selected,0,out.selects.size()-1,8};
            continue;
        }
        const auto load=[&](unsigned dst,Value from,unsigned width,bool fp=false) {
            out.loads.push_back({i,from,width});
            Value value;
            if(from.kind==Value::Self)value={Value::Field,from.offset,0,width};
            if(from.kind==Value::RelativeField||from.kind==Value::Argument)value={Value::IndirectField,from.offset,from.number,width};
            if(from.kind==Value::Stack)value=read_stack(from.offset,width);
            if(fp)vector_regs[dst]=value;else if(dst!=31)out.regs[dst]=value;
        };
        const auto store=[&](unsigned src,Value to,unsigned width,bool fp=false) {
            auto value=fp?vector_regs[src]:reg(src);
            if((value.kind==Value::Result||value.kind==Value::Field)&&value.width<width)value={};
            value.width=width;
            out.stores.push_back({i,to,value,width});
            if(to.kind==Value::Stack)write_stack(to.offset,value,width);
        };
        // Native C result writers can select a union field with a proven
        // constant X-register offset. Support only unsigned UXTX addressing;
        // unknown/signed indexes must not acquire field provenance.
        if((w&0x3b200c00u)==0x38200800u&&!(w&(1u<<26))&&((w>>13)&7)==3) {
            const auto index=reg(rm);const unsigned width=1u<<(w>>30),op=(w>>22)&3;
            const unsigned shift=(w&(1u<<12))?(w>>30):0;
            if(index.kind!=Value::Constant||index.number>(uint64_t(1)<<20)>>shift||op>1) {
                out.unknown.push_back(i);if(op&&rd!=31)out.regs[rd]={};continue;
            }
            const auto address=plus(reg(rn,true),int64_t(index.number<<shift));
            if(op==1)load(rd,address,width);else store(rd,address,width);
            continue;
        }
        if((w&0x3b000000u)==0x39000000u) {
            const bool fp=(w&(1u<<26))!=0;
            if(fp&&((w>>23)&1)&&(w>>30)){out.unknown.push_back(i);continue;}
            const unsigned width=fp&&((w>>23)&1)?16:1u<<(w>>30),op=(w>>22)&3;
            const auto from=plus(reg(rn,true),int64_t((w>>10)&4095)*width);
            if((fp&&(op&1))||op==1)load(rd,from,width,fp);
            else if(fp||op==0)store(rd,from,width,fp);else out.regs[rd]={};
            continue;
        }
        if((w&0x3b200000u)==0x38000000u) {
            const bool fp=(w&(1u<<26))!=0;
            if(fp&&((w>>23)&1)&&(w>>30)){out.unknown.push_back(i);continue;}
            const unsigned mode=(w>>10)&3,width=fp&&((w>>23)&1)?16:1u<<(w>>30),op=(w>>22)&3;
            const int64_t delta=int32_t(w<<11)>>23;
            const auto base=reg(rn,true),from=mode==1?base:plus(base,delta);
            if(mode==1||mode==3){out.regs[rn]=plus(base,delta);out.steps.push_back({i,rn,base,delta,false});}
            if((fp&&(op&1))||op==1)load(rd,from,width,fp);
            else if(fp||op==0)store(rd,from,width,fp);else out.regs[rd]={};
            continue;
        }
        if((w&0x3a000000u)==0x28000000u) {
            const bool fp=(w&(1u<<26))!=0;
            const auto size=(w>>30)&3;
            if(size==3||(!fp&&size==1)){out.unknown.push_back(i);continue;}
            const unsigned width=fp?4u<<((w>>30)&3):((w>>31)?8:4),mode=(w>>23)&3,rt2=(w>>10)&31;
            const int64_t delta=(int32_t(w<<10)>>25)*int64_t(width);
            const auto base=reg(rn,true),from=mode==1?base:plus(base,delta);
            if(mode==1||mode==3)out.regs[rn]=plus(base,delta);
            if(w&(1u<<22)){load(rd,from,width,fp);load(rt2,plus(from,width),width,fp);}
            else {store(rd,from,width,fp);store(rt2,plus(from,width),width,fp);}
            continue;
        }
        if((w&0xfffffc00u)==0x1e204000u) { // FMOV Sn, Sm, scalar 32-bit ABI forwarding.
            auto value=vector_regs[rn];
            if(value.width>4)value={};
            vector_regs[rd]=value;continue;
        }
        // Other SIMD data instructions do not write integer registers. Integer
        // operations not decoded above lose their destination provenance.
        if((w&0x1e000000u)==0x0e000000u){vector_regs[rd]={};out.compared_field={};out.compared_value={};continue;}
        if((w&0x1f800000u)==0x13000000u){
            const auto input=reg(rn);
            if((w&0xffc00000u)==0xd3400000u&&((w>>10)&63)==63&&input.kind==Value::Distance)
                out.regs[rd]={Value::Count,input.offset,input.number,8,input.extra,uint64_t(1)<<((w>>16)&63)};
            else if(rd!=31)out.regs[rd]={};
            continue;
        }
        out.unknown.push_back(i);
        out.compared_field={};out.compared_value={};
        if(rd!=31)out.regs[rd]={};
    }
    return out;
}
inline std::optional<std::vector<std::vector<size_t>>> forward_paths(const Body& body) {
    if(body.words.empty()||body.words.size()>512)return {};
    struct Pending {size_t pc;std::vector<size_t> path;};
    std::vector<Pending> pending{{0,{}}};std::vector<std::vector<size_t>> complete;
    while(!pending.empty()) {
        auto current=std::move(pending.back());pending.pop_back();
        for(;;) {
            if(current.pc>=body.words.size())return {};
            const auto at=current.pc;const auto w=body.words[at];current.path.push_back(at);
            if(w==0xd65f03c0u){complete.push_back(std::move(current.path));break;}
            int64_t target=-1;bool conditional=false,branch=true;
            if((w&0xfc000000u)==0x14000000u)target=int64_t(at)+(int32_t(w<<6)>>6);
            else if((w&0xff000010u)==0x54000000u||(w&0x7e000000u)==0x34000000u)
                {target=int64_t(at)+(int32_t(w<<8)>>13);conditional=true;}
            else if((w&0x7e000000u)==0x36000000u){target=int64_t(at)+(int32_t(w<<13)>>18);conditional=true;}
            else if((w&0xfffffc1fu)==0xd61f0000u)return {}; // No guessed indirect successor.
            else branch=false;
            if(branch) {
                if(target<0||target<=int64_t(at)||target>=int64_t(body.words.size()))return {};
                if(conditional)pending.push_back({at+1,current.path});current.pc=static_cast<size_t>(target);
            } else ++current.pc;
            if(pending.size()+complete.size()>=32)return {};
        }
    }
    return complete;
}
struct OptionalField {
    uint32_t offset=0;uint64_t none=0;
    bool operator==(const OptionalField&)const=default;
};
inline std::optional<OptionalField> optional_field(const Graph& graph,const Body& body) {
    const auto flow=trace(graph,body);const auto value=flow.regs[0];
    if(!flow.returned||!flow.calls.empty()||!flow.branches.empty()||!flow.unknown.empty()
        ||value.kind!=Value::Nullable||value.offset<0||value.offset>65536)return {};
    for(const auto& store:flow.stores)if(store.address.kind!=Value::Stack)return {};
    return OptionalField{static_cast<uint32_t>(value.offset),value.number};
}
struct StringFields {uint32_t pointer=0,length=0;bool operator==(const StringFields&)const=default;};
inline std::optional<StringFields> string_fields(const Graph& graph,const Body& body) {
    const auto flow=trace(graph,body);const auto pointer=flow.regs[0],length=flow.regs[1];
    if(!flow.returned||!flow.calls.empty()||!flow.branches.empty()||!flow.unknown.empty()
        ||pointer.kind!=Value::Field||length.kind!=Value::Field
        ||pointer.width!=8||length.width!=8||pointer.offset<0||length.offset<0||pointer.offset>65536||length.offset>65536)return {};
    for(const auto& store:flow.stores)if(store.address.kind!=Value::Stack)return {};
    return StringFields{static_cast<uint32_t>(pointer.offset),static_cast<uint32_t>(length.offset)};
}
}
