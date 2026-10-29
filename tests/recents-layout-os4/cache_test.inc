int main(){
    Config source;source.cell_x=4;source.cell_y=6;source.dock_show_title=true;source.drawer_hide_search=true;source.policies=kSupportedPolicies;
    source.recent.values={42,65,51,150,60,70,130,80,22,int32_t(0xff2468ac),int32_t(0xffaabbcc),1,1};
    std::vector<uint8_t> bytes;serialize_config(source,bytes);Config got;assert(parse_config(bytes,got)&&got.recent==source.recent&&got.dock_show_title&&got.drawer_hide_search&&got.policies==source.policies);
    for(size_t n=1;n<=76;++n){auto cut=bytes;cut.resize(cut.size()-n);assert(!parse_config(cut,got));}
    // The core HCRC remains 52 bytes. Later independent trailers are removed
    // according to their actual version, rather than a stale fixed subtraction.
    auto legacy=source.recent;legacy.values[RecentConfig::HideIcon]=legacy.values[RecentConfig::HideText]=0;
    const size_t tail21=8,tail20=8,tail19=12,tail18=16,tail17=8,tail16=52,tail15=8;
    auto old=bytes;old.resize(old.size()-tail21);old[4]=20;
    assert(parse_config(old,got)&&got.recent==legacy);
    old.resize(old.size()-tail20);old[4]=19;assert(parse_config(old,got)&&got.recent==legacy&&!got.infinite_scroll);
    old.resize(old.size()-tail19);old[4]=18;assert(parse_config(old,got)&&got.policies==source.policies);
    old.resize(old.size()-tail18);old[4]=17;assert(parse_config(old,got)&&got.policies==0&&got.drawer_hide_search);
    old.resize(old.size()-tail17);old[4]=16;assert(parse_config(old,got)&&got.recent==legacy&&!got.drawer_hide_search);
    old.resize(old.size()-tail16);old[4]=15;assert(parse_config(old,got)&&got.recent==RecentConfig{}&&got.dock_show_title);
    old.resize(old.size()-tail15);old[4]=14;assert(parse_config(old,got)&&!got.dock_show_title&&got.recent==RecentConfig{});
    for(unsigned i=0;i<9;++i){auto bad=bytes;int32_t value=i==8?-2:999;
        std::memcpy(bad.data()+bad.size()-tail21-tail20-tail19-tail18-tail17-44+i*4,&value,4);assert(!parse_config(bad,got));}
    std::vector<int32_t> wire{kRecentMagic,kRecentWireCount};wire.insert(wire.end(),source.recent.values.begin(),source.recent.values.begin()+kRecentWireCount);
    for(size_t n=1;n<wire.size();++n){size_t at=0;auto read=[&](int32_t& v){if(at>=n)return false;v=wire[at++];return true;};RecentConfig result;
        assert(!read_recent_config(read,result));assert(result==RecentConfig{});}
    size_t at=0;auto read=[&](int32_t& v){if(at==wire.size())return false;v=wire[at++];return true;};RecentConfig result;bool present=false;
    assert(read_recent_config(read,result,true,&present)&&present&&result==legacy);
    auto missing=[](int32_t&){return false;};assert(read_recent_config(missing,result)&&result==RecentConfig{});
    for(int flags:{-1,0,1,2,3,4,7})for(size_t size:{size_t(0),size_t(1),size_t(2)}){
        int32_t words[]={kRecentHeaderMagic,flags};size_t pos=0;result=source.recent;present=false;
        auto take=[&](int32_t& word){if(pos==size)return false;word=words[pos++];return true;};
        bool valid=size==2&&flags>=0&&flags<=3;
        assert(read_recent_header(take,result,true,&present)==(size==0||valid));
        assert(present==valid);
        assert(result.values[RecentConfig::HideIcon]==(valid?(flags&1):0));
        assert(result.values[RecentConfig::HideText]==(valid?((flags>>1)&1):0));
        for(int i=0;i<kRecentWireCount;++i)assert(result.values[i]==source.recent.values[i]);
    }
    for(int32_t value:{0,1,2,-1}){std::vector<int32_t> drawer{kDrawerMagic,value};size_t pos=0;bool hidden=true,present=false;
        auto take=[&](int32_t& v){if(pos==drawer.size())return false;v=drawer[pos++];return true;};
        assert(read_drawer_config(take,hidden,true,&present)==(value==0||value==1));
        if(value==0||value==1)assert(present&&hidden==(value==1));}
    {bool hidden=true;assert(read_drawer_config(missing,hidden)&&!hidden);size_t pos=0;
     auto partial=[&](int32_t& v){v=kDrawerMagic;return pos++==0;};assert(!read_drawer_config(partial,hidden));}
    for(uint64_t value:{uint64_t(0),kHideReportPolicy,uint64_t(2),uint64_t(3),uint64_t(4),uint64_t(1)<<32}){
        std::vector<int32_t> packet{kPolicyMagic,2,int32_t(value),int32_t(value>>32)};
        size_t pos=0;uint64_t result=99;bool present=false;
        auto take=[&](int32_t& v){if(pos==packet.size())return false;v=packet[pos++];return true;};
        assert(read_policy_config(take,result,true,&present)==(value<=3));
        if(value<=3)assert(present&&result==value);else assert(!present&&result==0);
        for(size_t n=1;n<packet.size();++n){pos=0;auto cut=[&](int32_t& v){if(pos==n)return false;v=packet[pos++];return true;};assert(!read_policy_config(cut,result)&&result==0);}
    }
    {uint64_t result=99;assert(read_policy_config(missing,result)&&result==0);}
    std::cout<<"recents cache/wire PASS: v21/v20/v19/v18/v17/v16/v15/v14 + HCRH, exact count/order, complete packet publication, truncation/range rejection and neutral defaults\n";
}
