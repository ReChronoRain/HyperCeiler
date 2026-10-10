// Original Home 7719 start_load + worker + mode-change normal paths.
// Scheduler/DB switch/model body are explicit adapters; no production patch installed.
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wreturn-type"
#define main historical_publisher_main
#include "../../../acceptance/home-grid-next/evidence-v2/publisher_fixture.cpp"
#undef main
#pragma GCC diagnostic pop

struct Task { uint64_t epoch; Params params; uint64_t generation; };
static_assert(sizeof(Task)==32);
static std::array<uint64_t,4> channel{};
static std::array<uint64_t,5> controller{};
static std::vector<void*> allocations;
static std::vector<Task> submitted, entered;
static unsigned switch_calls, cancellation_calls;
static uint64_t pool_marker, switch_entry_epoch, cancel_epoch;
static uintptr_t switch_entry_mode, cancel_mode;
extern "C" void empty_external() {}
extern "C" void* allocate_external(size_t n) {
    void* p=std::calloc(1,n?n:1); assert(p); allocations.push_back(p); return p;
}
extern "C" void channel_external(uint64_t*out) {
    out[0]=0;out[1]=reinterpret_cast<uintptr_t>(channel.data());out[2]=0;
}
extern "C" uint64_t enqueue_external(uintptr_t,uintptr_t,void*p,void*) {
    submitted.push_back(*static_cast<Task*>(p)); return 0;
}
extern "C" void inner_external(const Params*p) { entered.push_back(Task{0,*p,0}); }
extern "C" void cancel_external() {
    ++cancellation_calls;cancel_epoch=current->data<uint64_t>(0xf22730);cancel_mode=controller[4];
}
extern "C" void switch_external() {
    ++switch_calls;switch_entry_epoch=current->data<uint64_t>(0xf22730);switch_entry_mode=controller[4];
    expect(controller[2]==0,"original mode lock released before switch_db");
    expect(pool_marker==0x1111,"original switch_db called while old pool is still selected by adapter");
    pool_marker=0x2222; // explicit stand-in, not original pool/SQL migration
}
static void configure(Image&i) {
    current=&i;allocations.clear();submitted.clear();entered.clear();
    cancellation_calls=switch_calls=0;pool_marker=0x1111;
    i.data<uint64_t>(0xf19518)=2;i.data<uint8_t>(0xf19510)=0;i.data<uint64_t>(0xf27370)=0;
    i.data<uint64_t>(0xf22730)=41;i.data<uint64_t>(0xf22a10)=9;
    i.jump(0x8fe2b8,reinterpret_cast<uintptr_t>(cancel_external));
    i.jump(0x8ff050,reinterpret_cast<uintptr_t>(empty_external));
    i.jump(0x77fe7c,reinterpret_cast<uintptr_t>(channel_external));
    i.jump(0xab3ad4,reinterpret_cast<uintptr_t>(enqueue_external));
    i.jump(0x9e8974,reinterpret_cast<uintptr_t>(empty_external));
    i.jump(0x91b8b0,reinterpret_cast<uintptr_t>(empty_external));
    i.jump(0xdf9f40,reinterpret_cast<uintptr_t>(allocate_external));
    i.jump(0x794f3c,reinterpret_cast<uintptr_t>(inner_external));
    i.jump(0x725e54,reinterpret_cast<uintptr_t>(switch_external));
    i.jump(0x91afb0,reinterpret_cast<uintptr_t>(empty_external));
    i.jump(0x9d5f38,reinterpret_cast<uintptr_t>(empty_external));
    i.data<uint32_t>(0xf194d0)=0; // actual DatabaseManager::destroy returns without external resources
    i.data<uint64_t>(0xf19910)=2;i.data<uint8_t>(0xf19920)=0;
    i.data<uint64_t>(0xf19928)=0;i.data<uint64_t>(0xf19930)=1;i.data<uint64_t>(0xf19938)=0;
    controller={1,1,0,1,reinterpret_cast<uintptr_t>(i.base+0xe42798)};
}
static void cleanup(){for(void*p:allocations)std::free(p);allocations.clear();}
int main(int argc,char**argv) {
    assert(argc==2||argc==3);std::string negative=argc==3?argv[2]:"";
    std::ifstream in(argv[1],std::ios::binary);std::vector<uint8_t>bytes((std::istreambuf_iterator<char>(in)),{});
    unsigned sequences=0;
    for(const Params&p:std::array<Params,4>{{{4,6,4,6},{7,9,4,6},{8,12,4,-1},{3,4,4,-1}}}) {
        Image i(bytes);configure(i);
        if(negative=="no-generation-increment")i.word(0x79186c,0xaa0903f5); // mov x21,x9
        if(negative=="no-epoch-check")i.word(0x68bea8,0xd503201f);
        if(negative=="no-generation-check")i.word(0x68bf4c,0xd503201f);
        if(negative=="lost-device-param")i.word(0x7918f8,0xd503201f);
        i.sync();auto start=i.fn<void(*)(const Params*)>(0x791848);auto poll=i.fn<void(*)(Task*)>(0x68be84);
        const Params old{4,5,0,0};i.fn<void(*)(const Params*)>(0x6c46e0)(&old);
        start(&p);
        expect(submitted.size()==1,"one original start_load enqueue");
        expect(submitted[0].epoch==41&&submitted[0].generation==10,"original start_load stamps epoch and new generation");
        expect(submitted[0].params==p,"original start_load carries all four typed fields unchanged");
        expect(i.read()==old,"enqueue does not prematurely overwrite old migration dimensions");
        poll(&submitted[0]);expect(entered.size()==1&&entered[0].params==p,"matching original worker delivers exact new input to inner boundary");
        Task saved=submitted[0];
        start(&p);expect(submitted.size()==2&&submitted[1].generation==11,"later original request gets distinct generation");
        poll(&saved);expect(entered.size()==1,"old generation filtered by original worker");
        i.data<uint64_t>(0xf22730)=42;poll(&submitted[1]);expect(entered.size()==1,"old epoch filtered by original worker");
        i.data<uint64_t>(0xf22730)=41;poll(&submitted[1]);expect(entered.size()==2&&entered[1].params==p,"current epoch and generation still pass full input");
        i.fn<void(*)(const Params*)>(0x6c46e0)(&p);
        expect(i.read()==p&&i.x()==p[0]&&i.y()==p[1],"original later publisher changes exact four words and getters");
        expect(i.data<uint8_t>(0xf1996c)==0,"original parameter mutex released");
        cleanup();++sequences;
    }
    {
        Image i(bytes);configure(i);
        if(negative=="mode-does-not-advance-epoch")i.word(0x722440,0xaa0803ea);
        i.sync();
        i.fn<void(*)(void*,void*)>(0x721da8)(controller.data(),i.base+0xe427f8);
        expect(switch_calls==1&&cancellation_calls==1,"complete normal mode change reaches cancellation and one original switch callsite");
        expect(cancel_mode==reinterpret_cast<uintptr_t>(i.base+0xe427f8)&&cancel_epoch==41,"new mode publication precedes epoch increment");
        expect(switch_entry_mode==reinterpret_cast<uintptr_t>(i.base+0xe427f8)&&switch_entry_epoch==42,"new mode and epoch precede actual DB switch callsite");
        expect(i.data<uint64_t>(0xf22a10)==9,"mode change uses epoch rather than advancing load generation");
        expect(pool_marker==0x2222,"DB identity transition is explicit adapter result");
        cleanup();++sequences;
    }
    std::cout<<"{\"assertions\":"<<checks<<",\"sequences\":"<<sequences<<",\"failed\":0,\"device_tested\":false}"<<std::endl;
}
