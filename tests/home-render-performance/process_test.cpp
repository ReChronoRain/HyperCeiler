/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_process_identity.h"
#include <cassert>
#include <cstring>
#include <iostream>
#include <string>
int main(){
 home_layout::LauncherProcessIdentity cache;std::string name("com.miui.home\0extra",19);unsigned reads=0;
 auto read=[&](char* buffer,size_t capacity){++reads;assert(name.size()<capacity);memcpy(buffer,name.data(),name.size());return name.size();};
 for(unsigned i=0;i<10000;++i)assert(cache.matches(100,true,read));assert(reads==1);
 name="other.application";assert(!cache.matches(101,true,read));assert(reads==2);assert(!cache.matches(101,true,read));assert(reads==2);
 // The same PID must be re-evaluated across an inherited setprogname hook.
 cache.invalidate();name="com.miui.home";assert(cache.matches(101,true,read));assert(reads==3);
 name="other.application";assert(!cache.matches(101,false,read));name="com.miui.home";assert(cache.matches(101,false,read));assert(reads==5);
 name="other.application";assert(!cache.matches(101,true,read));assert(reads==6);
 for(const char* transient:{"usap64","usap32","hyos_spawner","zygote64","zygote"}) {
  cache.invalidate();name=transient;assert(!cache.matches(102,true,read));name="com.miui.home";assert(cache.matches(102,true,read));
 }
 cache.invalidate();name.clear();assert(!cache.matches(103,true,read));name="com.miui.home";assert(cache.matches(103,true,read));
 // A read begun before a rename cannot republish its old classification later.
 cache.invalidate();bool first=true;
 auto racing=[&](char* buffer,size_t capacity){auto n=read(buffer,capacity);if(first){first=false;cache.invalidate();}return n;};
 assert(cache.matches(104,true,racing));name="other.application";assert(!cache.matches(104,true,racing));
 const auto before=reads;assert(!cache.matches(0,true,read));assert(reads==before);
 std::cout<<"process identity PASS: 10000 reads -> 1 proc lookup; PID/fork, rename, invalidation race, NUL argv, transient names, read failure and hook-unavailable fallback\n";
}
