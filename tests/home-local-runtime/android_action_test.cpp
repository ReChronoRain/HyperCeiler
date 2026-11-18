/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Android transport regression using the shipping socket implementation. Only
// the Java vibration service is represented by an acknowledgement-producing peer.
#include "targets/home/home_local_config_channel.h"
#include "targets/home/home_local_config_protocol.h"
#include "targets/home/home_local_config_lease.h"
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <unistd.h>
#include <chrono>
#include <thread>
#include <cassert>
#include <cstdio>
#include <cstring>
#include <cerrno>
using namespace home_local_config;
static int64_t now(){return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now().time_since_epoch()).count();}
static int connect_peer(const char* kind="haptic"){
    int fd=socket(AF_UNIX,SOCK_STREAM|SOCK_CLOEXEC,0);assert(fd>=0);
    sockaddr_un address{};address.sun_family=AF_UNIX;
    int n=std::snprintf(address.sun_path+1,sizeof(address.sun_path)-1,"hc.home.%s.0.%d",kind,getpid());
    assert(connect(fd,reinterpret_cast<sockaddr*>(&address),offsetof(sockaddr_un,sun_path)+1+n)==0);return fd;
}
static bool read_exact(int fd,uint8_t* out,size_t count){
    while(count){ssize_t n=recv(fd,out,count,0);if(n<0&&errno==EINTR)continue;if(n<=0)return false;out+=n;count-=n;}return true;
}
static void reply(int fd,int kind){
    uint8_t request[24];assert(read_exact(fd,request,sizeof(request)));
    assert(word(request)==kHapticRequest&&word(request+4)==0x46f&&word(request+8)==1&&word(request+20)==1);
    uint8_t ack[16];put_word(ack,kind==2?0:kHapticAck);
    std::memcpy(ack+4,request+12,8);put_word(ack+12,kind==1?0:1);
    assert(send(fd,ack,3,MSG_NOSIGNAL)==3);
    assert(send(fd,ack+3,13,MSG_NOSIGNAL)==13);
}
static bool snapshot(const unsigned char* bytes,size_t size,uint32_t version,std::shared_ptr<const HapticWavePack>&){return version>=2&&version<=kVersion&&size==1&&bytes[0]==0;}
static void configuration(int peer,uint32_t sequence,bool accepted,uint32_t version=kVersion,uint32_t flags=UINT32_MAX) {
    uint8_t frame[21]{};put_word(frame,kMagic);put_word(frame+4,version);put_word(frame+8,1);
    put_word(frame+16,sequence);frame[20]=accepted?0:1;
    assert(send(peer,frame,sizeof(frame),MSG_NOSIGNAL)==sizeof(frame));
    home_local_config::poll(snapshot);
    uint8_t ack[16];assert(read_exact(peer,ack,sizeof(ack)));
    assert(word(ack)==kAck&&word(ack+8)==sequence&&word(ack+12)==(flags==UINT32_MAX?(accepted?1u:0u):flags));
}
static void status(int peer,uint32_t sequence,uint32_t flags){
    home_local_config::poll(snapshot);uint8_t ack[16];assert(read_exact(peer,ack,sizeof(ack)));
    assert(word(ack)==kAck&&word(ack+8)==sequence&&word(ack+12)==flags);
}
static void no_status(int peer){home_local_config::poll(snapshot);uint8_t byte;assert(recv(peer,&byte,1,MSG_DONTWAIT)<0&&(errno==EAGAIN||errno==EWOULDBLOCK));}
int main(){
    assert(getuid()==0);
    assert(!peer_connected());
    home_local_config::poll([](auto,auto,auto,auto&){return true;});
    int peer=connect_peer();
    std::thread server([peer]{reply(peer,0);reply(peer,1);reply(peer,0);reply(peer,2);close(peer);});
    assert(perform_haptic(0x46f,1,now(),1));
    assert(!perform_haptic(0x46f,1,now(),1));
    assert(perform_haptic(0x46f,1,now(),1));
    assert(!perform_haptic(0x46f,1,now(),1));server.join();
    peer=connect_peer();
    std::thread slow([peer]{uint8_t request[24];assert(read_exact(peer,request,sizeof(request)));
        std::this_thread::sleep_for(std::chrono::milliseconds(250));uint8_t byte;assert(recv(peer,&byte,1,0)==0);close(peer);});
    const auto started=now();assert(!perform_haptic(0x46f,1,started,1));
    assert(now()-started>=150&&now()-started<1000);slow.join();
    peer=connect_peer();std::thread recovered([peer]{reply(peer,0);close(peer);});
    assert(perform_haptic(0x46f,1,now(),1));recovered.join();
    // Drain the just-closed peer without retrying its event.
    assert(!perform_haptic(0x46f,1,now(),1));
    peer=connect_peer();
    std::thread old_session([peer]{reply(peer,0);uint8_t byte;assert(recv(peer,&byte,1,0)==0);close(peer);});
    assert(perform_haptic(0x46f,1,now(),1));
    int config_peer=connect_peer("config7");
    home_local_config::poll([](auto,auto,auto,auto&){return true;});
    assert(peer_connected());
    int fresh_peer=connect_peer();
    std::thread new_session([fresh_peer]{reply(fresh_peer,0);close(fresh_peer);});
    assert(perform_haptic(0x46f,1,now(),1));old_session.join();new_session.join();
    const auto generation=publication_generation();assert(!publication_current(generation));
    configuration(config_peer,1,true);assert(publication_current(generation));
    set_cards_ready(true);status(config_peer,1,3);no_status(config_peer);
    set_cards_ready(false);status(config_peer,1,1);no_status(config_peer);
    configuration(config_peer,2,false);assert(!publication_current(generation));
    assert(peer_connected()); // Bad preferences do not mean the service is absent.
    set_cards_ready(true);no_status(config_peer);assert(!publication_current(generation));
    home_local_config::poll(snapshot);assert(!publication_current(generation));
    configuration(config_peer,3,true,3);assert(publication_current(generation));no_status(config_peer);
    configuration(config_peer,4,true,kVersion,3);no_status(config_peer);
    set_cards_ready(false);status(config_peer,4,1);
    std::this_thread::sleep_for(std::chrono::milliseconds(kLeaseMs+10));
    assert(!publication_current(generation));
    assert(peer_connected()); // Expiry must not trigger another Provider wakeup.
    home_local_config::poll(snapshot);assert(publication_current(generation));
    close(config_peer);home_local_config::poll(snapshot);assert(!publication_current(generation));
    assert(!peer_connected());
    config_peer=connect_peer("config6");home_local_config::poll(snapshot);
    configuration(config_peer,1,true,6);assert(publication_current(publication_generation()));
    close(config_peer);home_local_config::poll(snapshot);
    config_peer=connect_peer("config5");home_local_config::poll(snapshot);
    configuration(config_peer,1,true,5);assert(publication_current(publication_generation()));
    close(config_peer);home_local_config::poll(snapshot);
    config_peer=connect_peer("config");home_local_config::poll(snapshot);
    const auto replacement=publication_generation();assert(replacement!=generation&&!publication_current(replacement));
    configuration(config_peer,1,true,4);assert(publication_current(replacement)&&!publication_current(generation));
    close(config_peer);home_local_config::poll(snapshot);
    assert(!perform_haptic(0x46f,1,now(),1));
    int signal[2];assert(pipe(signal)==0);const int parent=getpid();
    pid_t child=fork();assert(child>=0);
    if(child==0){
        close(signal[0]);assert(setgid(2000)==0&&setuid(2000)==0);
        int fd=socket(AF_UNIX,SOCK_STREAM|SOCK_CLOEXEC,0);assert(fd>=0);
        sockaddr_un address{};address.sun_family=AF_UNIX;
        int n=std::snprintf(address.sun_path+1,sizeof(address.sun_path)-1,"hc.home.haptic.0.%d",parent);
        assert(connect(fd,reinterpret_cast<sockaddr*>(&address),offsetof(sockaddr_un,sun_path)+1+n)==0);
        uint8_t byte=1;assert(write(signal[1],&byte,1)==1);assert(recv(fd,&byte,1,0)==0);close(fd);_exit(0);
    }
    close(signal[1]);uint8_t byte;assert(read(signal[0],&byte,1)==1);close(signal[0]);
    assert(!perform_haptic(0x46f,1,now(),1));int status;assert(waitpid(child,&status,0)==child&&WIFEXITED(status)&&WEXITSTATUS(status)==0);
    puts("Android production transport PASS feedback, config v7, v6, v5 and legacy v4 sockets, readiness negotiation without republish, rejected publication, expiry/renewal, disconnect/epoch and non-root rejection");
}
