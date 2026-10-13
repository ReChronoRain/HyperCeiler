#include <cstdint>
#include <cstdio>
#include <vector>
#include <mutex>
#include <atomic>
#include <string>
#define LOGI(...) ((void)0)
#define LOGW(...) ((void)0)
using pid_t=int;
struct Image {};
struct Config {uint32_t wanted=1;};
struct LocatedSites {bool ok9=false,ok16sq=false;bool AnyOk()const{return ok9;}};
struct AppliedPatch {};
struct PlanResult {};
struct PatchStats {};
struct CodeView {CodeView(const Image&) {}};
constexpr uint32_t kWantFolderCols=2;
int identityCalls=0,symbolCalls=0,scanCalls=0,resets=0;
uint64_t identityResult=0;
pid_t fakePid=1;
bool symbolsReady=false;
struct SymbolIndex {static SymbolIndex& Instance(){static SymbolIndex i;return i;}void ResetForTest(){++resets;}};
pid_t getpid(){return fakePid;}
// INSERT_PRODUCTION_STATE
State g_state;
std::mutex g_workMutex;
std::atomic<bool> g_tweaksStarted{false},g_imageKnown{false};
uint32_t WantedFeatureMask(const Config& c){return c.wanted;}
uint64_t ImageIdentity(const Image&){++identityCalls;return identityResult;}
uint32_t MaskOf(const LocatedSites& s){return s.ok9?1:0;}
size_t SitePackPaths(const char**,size_t){return 0;}
bool LoadSitePack(const char*,LocatedSites*){return false;}
struct FeatureSlot {uint32_t num;};
constexpr FeatureSlot kFeatureSlots[]={{9}};
void KeepOnlyFeature(LocatedSites*,uint32_t){}
bool ValidateSitesShape(const CodeView&,LocatedSites&){return true;}
bool ValidateSites(const CodeView&,LocatedSites&){return true;}
void CopyFeature(LocatedSites* a,const LocatedSites& b,uint32_t){*a=b;}
uint32_t AdoptInto(LocatedSites* a,const LocatedSites& b){*a=b;return MaskOf(*a);}
void AppendSource(State*,const char*){}
std::string DescribeMask(uint32_t){return "boundary";}
bool LoadSitesCache(uint64_t,LocatedSites*){return false;}
void DropSitesCache(){}
bool SaveSitesCache(uint64_t,const LocatedSites&){return false;}
void AcquireSitesBySymbols(State* s,const CodeView&,uint32_t){++symbolCalls;if(symbolsReady)s->sites.ok9=true;}
uint64_t NowMs(){return 0;}
void LocateSites(const CodeView&,const Config&,LocatedSites* out){++scanCalls;out->ok9=true;}
// INSERT_PRODUCTION_METHODS
int main(){
 int checks=0,failed=0;auto ok=[&](bool v){++checks;if(!v)++failed;};
 State off;off.config.wanted=0;AcquireSites(&off,true);ok(identityCalls==0&&scanCalls==0&&symbolCalls==0);
 State s;AcquireSites(&s,true);ok(identityCalls==1&&scanCalls==1&&s.sites.ok9);
 for(int i=0;i<3600;++i){s.cacheTried=false;s.packTried=false;AcquireSites(&s,false);}
 ok(identityCalls==1);ok(scanCalls==1);
 identityCalls=0;identityResult=42;State good;symbolsReady=true;AcquireSites(&good,false);ok(identityCalls==1&&good.imageId==42&&good.sites.ok9);
 for(int i=0;i<3600;++i)AcquireSites(&good,false);ok(identityCalls==1);
 g_state=good;g_tweaksStarted=true;g_imageKnown=true;fakePid=2;HomeTweaksPrepareForLauncherChild();ok(g_state.imageId==0&&!g_imageKnown&&resets==1);
 AcquireSites(&g_state,false);ok(identityCalls==2&&g_state.imageId==42);
 HomeTweaksPrepareForLauncherChild();ok(resets==1&&g_state.imageId==42);
 std::printf("ENTRY_IDENTITY_RETRY=%d checks; failed=%d; failed identity 3600 refreshes; symbols/scan retained; actual child reset\n",checks,failed);
 return failed?1:0;
}
