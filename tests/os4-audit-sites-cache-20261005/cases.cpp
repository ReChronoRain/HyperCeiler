using namespace hometweaks;
int checks=0,failed=0;
void ok(bool v){++checks;if(!v)++failed;}
void nested_read(){chooseB=true;LocatedSites s;ok(LoadSitesCache(22,&s)&&s.overlayVa==2222);chooseB=false;}
LocatedSites valid(uint32_t va=1234){LocatedSites s;s.ok9=true;s.overlayVa=va;s.noClearCount=1;s.noClearSites[0]=va+16;return s;}
void put(const std::string& name,const std::vector<uint8_t>& bytes){std::ofstream f(root/name,std::ios::binary);f.write(reinterpret_cast<const char*>(bytes.data()),std::streamsize(bytes.size()));nodes[name]={};}
std::vector<uint8_t> get(const std::string& name){std::ifstream f(root/name,std::ios::binary);return {std::istreambuf_iterator<char>(f),{}};}
void u32(std::vector<uint8_t>& b,size_t off,uint32_t v){for(int i=0;i<4;i++)b[off+i]=uint8_t(v>>(i*8));}
void crc(std::vector<uint8_t>& b){u32(b,20,uint32_t(b.size()-24));u32(b,8,Fnv32(b.data()+24,b.size()-24));}
std::vector<uint8_t> cache(uint64_t id,uint32_t va=1234){
 std::vector<uint8_t> p,b;SerializeSites(valid(va),&p);b.insert(b.end(),kCacheMagic,kCacheMagic+4);AppendU32(&b,kCacheFormat);AppendU32(&b,Fnv32(p.data(),p.size()));AppendU64(&b,id);AppendU32(&b,uint32_t(p.size()));b.insert(b.end(),p.begin(),p.end());return b;
}
Image setup(){
 std::vector<uint8_t> b(262144,0x77);Elf64_Ehdr e{};std::memcpy(e.e_ident,ELFMAG,4);e.e_ident[EI_CLASS]=2;e.e_ident[EI_DATA]=1;e.e_ident[EI_VERSION]=1;e.e_type=3;e.e_machine=183;e.e_version=1;e.e_ehsize=64;e.e_phoff=64;e.e_phentsize=56;e.e_phnum=2;std::memcpy(b.data(),&e,64);
 Elf64_Phdr p{1,4,0,0,0,4096,4096,4096},x{1,5,4096,4096,0,b.size()-4096,b.size()-4096,4096};std::memcpy(b.data()+64,&p,56);std::memcpy(b.data()+120,&x,56);
 live=b;put("source",b);Image i{};i.base=reinterpret_cast<uintptr_t>(live.data());i.segments[0]={i.base,i.base+4096,4};i.segments[1]={i.base+4096,i.base+b.size(),5};i.segmentCount=2;std::strcpy(i.path,"fixture.so");i.sourceDevice=55;i.sourceInode=111;i.fileViewBytes=b.size();return i;
}
int main(int argc,char** argv){
 if(argc!=3)return 2;std::string c=argv[1];root=argv[2];Image image=setup();put("cacheA",cache(11));put("cacheB",cache(22,2222));if(c=="cache-public"||c=="drop-private")put("public",cache(11));put("victim",{'O','R','I','G'});
 LocatedSites out;out.overlayVa=9999;auto old=get("cacheA");
 if(c.starts_with("symbol-")){
  const std::vector<uint8_t> payload{1,2,3,4,5};std::vector<uint8_t> result{9};
  ok(SaveSymbolCache(77,payload));auto bytes=get("cacheSymbols");
  ok(get("cacheA")==old);ok(bytes.size()==29&&bytes[0]=='H'&&bytes[1]=='S');
  if(c=="symbol-valid"){ok(LoadSymbolCache(77,&result)&&result==payload);ok(!LoadSymbolCache(77,nullptr));}
  else if(c=="symbol-limit"){ok(!SaveSymbolCache(77,{}));ok(!SaveSymbolCache(0,payload));ok(!SaveSymbolCache(77,std::vector<uint8_t>(8192)));}
  else {
   if(c=="symbol-crc")bytes.back()^=1;
   if(c=="symbol-magic")bytes[0]^=1;
   if(c=="symbol-version")bytes[4]^=1;
   if(c=="symbol-length")bytes[20]^=1;
   if(c=="symbol-truncated")bytes.resize(23);
   if(c=="symbol-owner")fileBadUid=true;
   if(c=="symbol-mode")filePublic=true;
   if(c=="symbol-link")fileLink=true;
   put("cacheSymbols",bytes);
   ok(!LoadSymbolCache(c=="symbol-id"?78:77,&result)&&result==std::vector<uint8_t>{9});
  }
 }
 else if(c=="identity-valid"){auto id=ImageIdentity(image);ok(id!=0&&id==ImageIdentity(image));}
 else if(c=="identity-unsampled"||c=="identity-rodata"){
  auto id=ImageIdentity(image);size_t at=c=="identity-rodata"?0x300:0x1300;live[at]^=1;auto b=get("source");b[at]^=1;put("source",b);ok(id&&ImageIdentity(image)&&id!=ImageIdentity(image));
 }
 else if(c=="identity-live-patch"){auto id=ImageIdentity(image);live[4096]^=1;ok(id&&id==ImageIdentity(image));}
 else if(c=="identity-empty"){ok(ImageIdentity(Image{})==0);}
 else if(c=="identity-noexec"){image.segments[1].flags=4;ok(ImageIdentity(image)==0);}
 else if(c=="identity-wx"){image.segments[1].flags=7;ok(ImageIdentity(image)==0);}
 else if(c=="identity-livefault"){liveFault=true;ok(ImageIdentity(image)==0);}
 else if(c=="identity-partialfault"){partialLiveFault=true;ok(ImageIdentity(image)==0);}
 else if(c=="identity-inode"){swapSource=true;ok(ImageIdentity(image)==0);}
 else if(c=="identity-readfault"){readFault=true;ok(ImageIdentity(image)==0);}
 else if(c=="identity-statfault"){statFault=true;ok(ImageIdentity(image)==0);}
 else if(c=="identity-mutation"){lateMutation=true;ok(ImageIdentity(image)==0);}
 else if(c=="identity-closefault"){closeFault=true;ok(ImageIdentity(image)==0);}
 else if(c=="identity-eintr"){readEintr=true;shortIO=true;ok(ImageIdentity(image)!=0);}
 else if(c=="identity-extent"){--image.fileViewBytes;ok(ImageIdentity(image)==0);}
 else if(c=="identity-budget"){image.fileViewBytes=512u*1024u*1024u+1;ok(ImageIdentity(image)==0&&handles.empty());}
 else if(c=="identity-machine"||c=="identity-phoff"||c=="identity-load-extent"){
  auto b=get("source");if(c=="identity-machine")b[18]=0;else {uint64_t bad=UINT64_MAX;std::memcpy(b.data()+(c=="identity-phoff"?32:120+32),&bad,8);}std::memcpy(live.data(),b.data(),b.size());put("source",b);ok(ImageIdentity(image)==0);
 }
 else if(c=="identity-inventory"){image.segments[1].begin=image.base;ok(ImageIdentity(image)==0);}
 else if(c=="identity-apk"){
  auto id=ImageIdentity(image);auto content=get("source");std::vector<uint8_t> b(12345,0);b.insert(b.end(),content.begin(),content.end());b.insert(b.end(),300,0);put("source",b);image.fromApkEntry=true;image.apkEntryOffset=12345;ok(id&&id==ImageIdentity(image));
 }
 else if(c=="identity-real"){
  auto b=get("real");nodes["real"]={};Elf64_Ehdr e{};std::memcpy(&e,b.data(),64);size_t size=0;std::vector<Elf64_Phdr> loads;
  for(int j=0;j<e.e_phnum;++j){Elf64_Phdr p{};std::memcpy(&p,b.data()+e.e_phoff+j*56,56);if(p.p_type==1&&p.p_memsz){loads.push_back(p);size=std::max(size,size_t(p.p_vaddr+p.p_memsz));}}
  live.assign(size,0);image={};image.base=reinterpret_cast<uintptr_t>(live.data());for(auto& p:loads){std::memcpy(live.data()+p.p_vaddr,b.data()+p.p_offset,p.p_filesz);image.segments[image.segmentCount++]={image.base+p.p_vaddr,image.base+p.p_vaddr+p.p_memsz,p.p_flags};}
  std::strcpy(image.path,"real.so");image.sourceDevice=55;image.sourceInode=111;image.fileViewBytes=b.size();auto id=ImageIdentity(image);ok(id!=0);std::cout<<"REAL_7722 id="<<id<<" bytes="<<b.size()<<" max_read="<<maxRead<<" source_reads="<<sourceReads<<"\n";
 }
 else if(c=="cache-valid"){ok(LoadSitesCache(11,&out)&&out.overlayVa==1234);}
 else if(c=="cache-zero"){put("cacheA",cache(0));ok(!LoadSitesCache(0,&out)&&out.overlayVa==9999);ok(!SaveSitesCache(0,valid()));}
 else if(c=="cache-link"){fileLink=true;ok(!LoadSitesCache(11,&out)&&out.overlayVa==9999);}
 else if(c=="cache-owner"){fileBadUid=true;ok(!LoadSitesCache(11,&out)&&out.overlayVa==9999);}
 else if(c=="cache-mode"){filePublic=true;ok(!LoadSitesCache(11,&out)&&out.overlayVa==9999);}
 else if(c=="cache-fifo"){fileFifo=true;ok(!LoadSitesCache(11,&out)&&out.overlayVa==9999);}
 else if(c=="cache-directory-owner"){dirBad=true;ok(!LoadSitesCache(11,&out));ok(!SaveSitesCache(11,valid()));}
 else if(c=="cache-directory-link"){dirLink=true;ok(!LoadSitesCache(11,&out));}
 else if(c=="cache-public"){publicOnly=true;ok(!LoadSitesCache(11,&out));ok(!SaveSitesCache(11,valid()));}
 else if(c=="cache-eintr"){readEintr=true;shortIO=true;ok(LoadSitesCache(11,&out)&&out.overlayVa==1234);}
 else if(c=="cache-mutation"){statMutation=true;ok(!LoadSitesCache(11,&out)&&out.overlayVa==9999);}
 else if(c=="cache-reentrant"){nested=true;ok(LoadSitesCache(11,&out)&&out.overlayVa==1234);}
 else if(c=="cache-close"){closeFault=true;ok(!LoadSitesCache(11,&out)&&out.overlayVa==9999);}
 else if(c=="cache-tail"){auto b=get("cacheA");b.push_back(1);crc(b);put("cacheA",b);ok(!LoadSitesCache(11,&out)&&out.overlayVa==9999);}
 else if(c=="cache-boolean"){auto b=get("cacheA");u32(b,b.size()-16,2);crc(b);put("cacheA",b);ok(!LoadSitesCache(11,&out)&&out.overlayVa==9999);}
 else if(c=="cache-crc"){auto b=get("cacheA");b.back()^=1;put("cacheA",b);ok(!LoadSitesCache(11,&out)&&out.overlayVa==9999);}
 else if(c=="cache-id"){ok(!LoadSitesCache(22,&out)&&out.overlayVa==9999);}
 else if(c=="cache-user"){uid=1010001;ok(SaveSitesCache(11,valid()));ok(lastDir.find("/data/user/10/")!=std::string::npos);}
 else if(c=="save-valid"){ok(SaveSitesCache(33,valid(777)));ok(LoadSitesCache(33,&out)&&out.overlayVa==777);}
 else if(c=="save-temp"){ok(SaveSitesCache(11,valid(777)));ok(get("victim")==std::vector<uint8_t>({'O','R','I','G'})&&linksFollowed==0);}
 else if(c=="save-eintr"){writeEintr=syncEintr=true;shortIO=true;ok(SaveSitesCache(11,valid()));}
 else if(c=="save-chmod"||c=="save-filesync"||c=="save-dirsync"||c=="save-close"||c=="save-rename"||c=="save-write"||c=="save-zero"){
  chmodFault=c=="save-chmod";syncFileFault=c=="save-filesync";syncDirFault=c=="save-dirsync";closeFault=c=="save-close";renameFault=c=="save-rename";writeFault=c=="save-write";zeroWrite=c=="save-zero";
  ok(!SaveSitesCache(33,valid(777)));if(!syncDirFault)ok(get("cacheA")==old);
 }
 else if(c=="save-count"){auto s=valid();s.noClearCount=33;ok(!SaveSitesCache(11,s));}
 else if(c=="save-icons"){auto s=valid();s.iconSiteCount=3;s.iconFuncSiteBeg[0]=2;s.iconFuncSiteCount[0]=2;ok(!SaveSitesCache(11,s));}
 else if(c=="save-empty"){ok(!SaveSitesCache(11,LocatedSites{}));}
 else if(c=="drop-private"){DropSitesCache();ok(!std::filesystem::exists(root/"cacheA")&&std::filesystem::exists(root/"public"));}
 else return 2;
 ok(handles.empty());std::cout<<"CASE "<<c<<" checks="<<checks<<" failed="<<failed<<"\n";return failed?1:0;
}
