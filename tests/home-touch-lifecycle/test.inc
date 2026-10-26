unsigned cases=0;
void send(int action,int64_t time,float y=1000,float x=500,int count=1,uintptr_t owner=0x100) {
 const int full=action==5||action==6?action|256:action;
 Event event{action,full,count,time,x,y,7};hc_workspace_scene(reinterpret_cast<void*>(owner),&event);
 assert(event.masked==action&&event.full==full);
}
void claim(int fingers=1,int direction=-1){send(0,1000);assert(workspace_state.authorize(1000,1));if(fingers==2)send(5,1040,1000,500,2);send(2,1100,1000+direction*80,500,fingers);assert(!stock_active&&forwarded==(fingers==1?std::vector<int>({0,3}):std::vector<int>({0,5,3})));}
void recovered_stock_stream(){assert(stock_active);send(2,2100,1000,600);send(1,2200,1000,650);assert(!stock_active&&emitted.empty());assert(forwarded[forwarded.size()-2]==2&&forwarded.back()==1);}
int main(){
 // Exact reproduction: a genuine CANCEL and next DOWN arrived while the Dart
 // source bank was unready. The old adapter retained the first stream and
 // swallowed MOVE/UP from the next, leaving the original receiver pressed.
 for(bool genuine_cancel:{false,true})for(bool workspace_gate:{false,true}){
  reset_fixture();claim();
  if(workspace_gate)hc_workspace_gestures_ready(false);else hc_workspace_scene_ready(false);
  if(genuine_cancel)send(3,1150);
  send(0,2000);assert(stock_block_start==0&&stock_block_end==0);
  assert(!workspace_state.authorize(2000,1));
  if(workspace_gate)hc_workspace_gestures_ready(true);else hc_workspace_scene_ready(true);
  recovered_stock_stream();++cases;
 }
 // A cancelled old stream still drains while disabled; it cannot fire an
 // action at release, nor expose an orphan MOVE to the stock receiver.
 for(bool workspace_gate:{false,true}){
  reset_fixture();claim();if(workspace_gate)hc_workspace_gestures_ready(false);else hc_workspace_scene_ready(false);
  send(2,1150,890);send(1,1200,880);assert(!stock_active&&emitted.empty()&&forwarded==std::vector<int>({0,3}));
  if(workspace_gate)hc_workspace_gestures_ready(true);else hc_workspace_scene_ready(true);
  send(0,2000);recovered_stock_stream();++cases;
 }
 // Loss before a custom claim must retain the complete original sequence.
 reset_fixture();send(0,1000);assert(workspace_state.authorize(1000,1));hc_workspace_scene_ready(false);send(2,1100,995,600);hc_workspace_scene_ready(true);send(1,1200,995,650);assert(!stock_active&&emitted.empty()&&forwarded==std::vector<int>({0,2,1}));++cases;
 // Settings changes and editing have the same lifecycle invariant.
 for(bool editing:{false,true}){
  reset_fixture();claim();if(editing)dock_edit_packed=1;else {settings.mask=0;++settings.revision;publish_workspace_locked();}
  send(0,2000);assert(!workspace_state.authorize(2000,settings.revision));
  if(editing)dock_edit_packed=0;else {settings.mask=30;++settings.revision;publish_workspace_locked();}
  recovered_stock_stream();++cases;
 }
 // A two-finger sequence interrupted on pointer-up is drained too. Full
 // action/index bits are restored after the synthetic borrowed CANCEL.
 reset_fixture();claim(2);hc_workspace_scene_ready(false);send(6,1150,910,500,2);send(3,1200,910);send(0,2000);hc_workspace_scene_ready(true);recovered_stock_stream();++cases;
 // Unknown API/generation cannot keep an earlier cancelled stream around.
 reset_fixture();claim();api.masked=nullptr;send(0,2000);assert(!workspace_state.tracking()&&stock_block_start==0);install_api();recovered_stock_stream();++cases;
 // Existing custom gestures still cancel once and dispatch once, on release.
 for(int fingers:{1,2})for(int direction:{-1,1}){
  reset_fixture();claim(fingers,direction);send(2,1120,1000+direction*100,500,fingers);
  if(fingers==2)send(6,1150,1000+direction*100,500,2);
  send(1,1200,1000+direction*100);const int expected=direction<0?(fingers==1?2:4):(fingers==1?3:5);
  assert(!stock_active&&emitted==std::vector<int>({expected})&&forwarded==(fingers==1?std::vector<int>({0,3}):std::vector<int>({0,5,3})));
  send(1,1201);assert(emitted.size()==1);++cases;
 }
 // Horizontal paging, scope rejection, long gaps and a subsequent fresh DOWN
 // never inherit interception from a previous custom sequence.
 reset_fixture();send(0,1000);assert(workspace_state.authorize(1000,1));send(2,1100,980,650);send(2,1150,800,650);send(1,1200,800,650);assert(!stock_active&&emitted.empty()&&forwarded==std::vector<int>({0,2,2,1}));++cases;
 reset_fixture();send(0,1000);send(2,1100,900);send(1,1200,900);assert(!stock_active&&emitted.empty()&&forwarded==std::vector<int>({0,2,1}));++cases;
 reset_fixture();claim();send(1,5000,800);assert(!stock_active&&emitted.empty());send(0,6000);send(2,6100,1000,650);send(1,6200,1000,650);assert(!stock_active&&emitted.empty());++cases;
 std::cout<<"touch lifecycle PASS: "<<cases<<" production adapter sequences; readiness/CANCEL/new DOWN recovery, settings/editing, two fingers, API reset, stock paging and original gesture dispatch\n";
}
