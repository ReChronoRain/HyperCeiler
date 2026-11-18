/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_context_contract.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==3);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream stream(argv[i],std::ios::binary);
        std::vector<char> bytes((std::istreambuf_iterator<char>(stream)),{});
        assert(!bytes.empty());Bytes file(bytes.size());std::memcpy(file.data(),bytes.data(),bytes.size());files.push_back(std::move(file));
    }
    const auto original=graph(files);const auto contract=home_native_context::resolve(original);
    if(!contract) {
        for(auto name:{"Runtime_get_context","context_get_uid","context_get_pid"}) {
            const auto at=original.exported(name);std::cerr<<name<<" address="<<bool(at);
            if(at){const auto field=home_native_context::scalar(original,*at);std::cerr<<" scalar="<<(field?int(*field):-1);}
            std::cerr<<'\n';
        }
        return 1;
    }
    assert(contract->uid_field!=contract->pid_field);
    auto changed=files;
    auto consumer=original.body(contract->uid);assert(consumer);
    for(unsigned depth=0;depth<4&&(consumer->words.back()&0xfc000000u)==0x14000000u;++depth) {
        const auto next=original.callee(*consumer,consumer->words.size()-1,true);assert(next);
        consumer=original.body(*next);assert(consumer);
    }
    const auto identity_flow=trace(original,*consumer);unsigned moved=0;
    for(const auto& load:identity_flow.loads)if(load.address.kind==Value::Self&&load.address.offset==contract->uid_field) {
        write(changed,{consumer->address.image,consumer->address.va+load.index*4},
            shift_memory(consumer->words[load.index],16));++moved;
    }
    assert(moved==1);const auto relocated=home_native_context::resolve(graph(changed));
    assert(relocated&&relocated->uid_field==contract->uid_field+16&&relocated->pid_field==contract->pid_field);
    changed=files;
    const auto body=original.body(contract->context);assert(body);
    // A public getter returning a different owned/offset object is not a
    // borrow of this Runtime, even if the exported symbol is unchanged.
    write(changed,{contract->context.image,contract->context.va},0x91002000u); // ADD X0,X0,#8
    assert(!home_native_context::resolve(graph(changed)));
    changed=files;write(changed,{contract->context.image,contract->context.va},0xd2800000u); // MOV X0,#0
    assert(!home_native_context::resolve(graph(changed)));
    changed=files;write(changed,{contract->context.image,contract->context.va},0xf9000000u); // STR X0,[X0]
    assert(!home_native_context::resolve(graph(changed)));
    changed=files;write(changed,{contract->context.image,contract->context.va},0x14000000u); // B self
    assert(!home_native_context::resolve(graph(changed)));
    const auto uid=original.body(contract->uid),pid=original.body(contract->pid);assert(uid&&pid);
    // Same symbol names do not authorize a 64-bit/mutating identity getter.
    changed=files;write(changed,{contract->uid.image,contract->uid.va},0xf9400000u); // LDR X0,[X0]
    assert(!home_native_context::resolve(graph(changed)));
    changed=files;write(changed,{contract->pid.image,contract->pid.va},0xf9000000u);
    assert(!home_native_context::resolve(graph(changed)));
    std::cout<<"PASS real Context borrow/identity consumer contract, changed return/width/write/control flow declined\n";
}
