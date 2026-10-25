int main(){
    Config source;source.cell_x=4;source.cell_y=6;source.dock_show_title=true;source.drawer_hide_search=true;source.policies=kHideReportPolicy;
    source.recent.values={42,65,51,150,60,70,130,80,22,int32_t(0xff2468ac),int32_t(0xffaabbcc)};
    std::vector<uint8_t> bytes;serialize_config(source,bytes);Config got;assert(parse_config(bytes,got)&&got.recent==source.recent&&got.dock_show_title&&got.drawer_hide_search&&got.policies==source.policies);
    for(size_t n=1;n<=76;++n){auto cut=bytes;cut.resize(cut.size()-n);assert(!parse_config(cut,got));}
    auto old=bytes;old.resize(old.size()-16);old[4]=17;assert(parse_config(old,got)&&got.policies==0&&got.drawer_hide_search);
    old.resize(old.size()-8);old[4]=16;assert(parse_config(old,got)&&got.recent==source.recent&&!got.drawer_hide_search);
    old.resize(old.size()-52);old[4]=15;assert(parse_config(old,got)&&got.recent==RecentConfig{}&&got.dock_show_title);
    old.resize(old.size()-8);old[4]=14;assert(parse_config(old,got)&&!got.dock_show_title&&got.recent==RecentConfig{});
    for(unsigned i=0;i<9;++i){auto bad=bytes;int32_t value=i==8?-2:999;std::memcpy(bad.data()+bad.size()-16-8-44+i*4,&value,4);assert(!parse_config(bad,got));}
    std::vector<int32_t> wire{kRecentMagic,11};wire.insert(wire.end(),source.recent.values.begin(),source.recent.values.end());
    for(size_t n=1;n<wire.size();++n){size_t at=0;auto read=[&](int32_t& v){if(at>=n)return false;v=wire[at++];return true;};RecentConfig result;
        assert(!read_recent_config(read,result));assert(result==RecentConfig{});}
    size_t at=0;auto read=[&](int32_t& v){if(at==wire.size())return false;v=wire[at++];return true;};RecentConfig result;bool present=false;
    assert(read_recent_config(read,result,true,&present)&&present&&result==source.recent);
    auto missing=[](int32_t&){return false;};assert(read_recent_config(missing,result)&&result==RecentConfig{});
    for(int32_t value:{0,1,2,-1}){std::vector<int32_t> drawer{kDrawerMagic,value};size_t pos=0;bool hidden=true,present=false;
        auto take=[&](int32_t& v){if(pos==drawer.size())return false;v=drawer[pos++];return true;};
        assert(read_drawer_config(take,hidden,true,&present)==(value==0||value==1));
        if(value==0||value==1)assert(present&&hidden==(value==1));}
    {bool hidden=true;assert(read_drawer_config(missing,hidden)&&!hidden);size_t pos=0;
     auto partial=[&](int32_t& v){v=kDrawerMagic;return pos++==0;};assert(!read_drawer_config(partial,hidden));}
    for(uint64_t value:{uint64_t(0),kHideReportPolicy,uint64_t(2),uint64_t(1)<<32}){
        std::vector<int32_t> packet{kPolicyMagic,2,int32_t(value),int32_t(value>>32)};
        size_t pos=0;uint64_t result=99;bool present=false;
        auto take=[&](int32_t& v){if(pos==packet.size())return false;v=packet[pos++];return true;};
        assert(read_policy_config(take,result,true,&present)==(value<=1));
        if(value<=1)assert(present&&result==value);else assert(!present&&result==0);
        for(size_t n=1;n<packet.size();++n){pos=0;auto cut=[&](int32_t& v){if(pos==n)return false;v=packet[pos++];return true;};assert(!read_policy_config(cut,result)&&result==0);}
    }
    {uint64_t result=99;assert(read_policy_config(missing,result)&&result==0);}
    std::cout<<"recents cache/wire PASS: v18/v17/v16/v15/v14, exact count/order, complete packet publication, truncation/range rejection and neutral defaults\n";
}
