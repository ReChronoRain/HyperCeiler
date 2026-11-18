/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_parcelable_contract.h"
#include "targets/home/home_native_surface_contract.h"
#include "targets/home/home_native_surface_access.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==12);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream stream(argv[i],std::ios::binary);std::vector<char> bytes((std::istreambuf_iterator<char>(stream)),{});
        assert(!bytes.empty());Bytes file(bytes.size());std::memcpy(file.data(),bytes.data(),bytes.size());files.push_back(std::move(file));
    }
    const auto original=graph(files);const auto surface=home_native_surface::resolve(original);
    const auto parcelable=home_native_parcelable::resolve(original);assert(surface&&parcelable);
    const auto access=home_native_surface::resolve_access(original,*surface);assert(access);
    assert(surface->bytes&&surface->alignment==8&&surface->status_shift==32&&surface->error_tag==1);
    auto moved=files;const auto make=original.body(surface->constructor);assert(make);
    const auto paths=home_native_provider::return_paths(original,*make);assert(paths&&paths->size()==1);
    const auto allocation=trace(original,*make,false,false,paths->front());unsigned changed=0;
    for(const auto& call:allocation.calls)if(call.target==surface->allocator) {
        for(size_t i=call.index;i-->0;) {
            const auto word=make->words[i];
            if((word&0xffe0001fu)==0x52800000u) {
                write(moved,{make->address.image,make->address.va+i*4},word^(1u<<5));++changed;break;
            }
        }
    }
    assert(changed==1&&!home_native_surface::resolve(graph(moved)));
    moved=files;changed=0;const auto decode=original.body(surface->read);assert(decode);
    for(size_t i=0;i<decode->words.size();++i)if((decode->words[i]&0xff20001fu)==0xaa000000u
        &&((decode->words[i]>>10)&63)==surface->status_shift) {
        write(moved,{decode->address.image,decode->address.va+i*4},decode->words[i]^(1u<<10));++changed;
    }
    assert(changed==1&&!home_native_surface::resolve(graph(moved)));
    moved=files;const auto reader=original.body(surface->read);assert(reader);
    const auto reading=trace(original,*reader);assert(reading.calls.size()==1);changed=0;
    for(const auto& load:reading.loads)if(load.address.kind==Value::Self&&load.address.offset==0&&load.width==8) {
        write(moved,{reader->address.image,reader->address.va+load.index*4},shift_memory(reader->words[load.index],8));++changed;
    }
    assert(changed==1&&!home_native_surface::resolve(graph(moved)));
    moved=files;const auto parcel=original.body(parcelable->make_parcel);assert(parcel);
    const auto producing=trace(original,*parcel);assert(!producing.calls.empty());
    write(moved,{parcel->address.image,parcel->address.va+producing.calls[0].index*4},0xd503201fu);
    assert(!home_native_parcelable::resolve(graph(moved)));
    moved=files;const auto reference=original.body(surface->native_ref);assert(reference);changed=0;
    const auto borrowing=trace(original,*reference);
    for(const auto& load:borrowing.loads)if(load.address.kind==Value::Self&&load.address.offset==access->native_field&&load.width==8) {
        write(moved,{reference->address.image,reference->address.va+load.index*4},shift_memory(reference->words[load.index],8));++changed;
    }
    assert(changed==1&&!home_native_surface::resolve_access(graph(moved),*surface));
    moved=files;const auto main_body=original.body(access->main);assert(main_body);
    const auto output=trace(original,*main_body);assert(output.unknown.size()==1&&output.unknown.front()>=3);
    const auto shift_index=output.unknown.front()-3;
    write(moved,{main_body->address.image,main_body->address.va+shift_index*4},main_body->words[shift_index]^(1u<<16));
    assert(!home_native_surface::resolve_access(graph(moved),*surface));
    std::cout<<"PASS native SurfaceControl ownership, result/reference ABI, borrowed native-sp producer/consumer and owned main result, platform Parcel producer/destructor; incompatible contracts refused\n";
}
