int main(){
 reset();columns_ok=false;for(unsigned n=0;n<500;++n){clock_ms+=250;prime_native_columns(hook,nullptr);}assert(column_scans==1&&map_reads==1&&image_reads==1&&installs==0&&g_native_columns_prime_attempted);
 reset();back_ok=false;for(unsigned n=0;n<500;++n)prime_back_regions(hook,nullptr);assert(back_scans==1&&map_reads==1&&image_reads==1&&installs==0&&g_back_prime_attempted);
 reset();image_available=false;prime_native_columns(hook,nullptr);assert(!g_native_columns_prime_attempted&&column_scans==0);clock_ms+=250;image_available=true;prime_native_columns(hook,nullptr);assert(column_scans==1&&installs==1&&g_slots[0].backend_owned);
 reset();image_available=false;prime_back_regions(hook,nullptr);assert(!g_back_prime_attempted);image_available=true;prime_back_regions(hook,nullptr);assert(back_scans==1&&installs==1&&g_slots[1].backend_owned&&g_slots[2].backend_owned);
 reset();allow_install=false;for(unsigned n=0;n<500;++n){clock_ms+=250;prime_native_columns(hook,nullptr);prime_back_regions(hook,nullptr);}assert(column_scans==1&&back_scans==1&&installs==2);assert(g_slots[0].address&&g_slots[1].address&&g_slots[2].address);
 reset();config_ok=false;prime_native_columns(hook,nullptr);prime_back_regions(hook,nullptr);assert(map_reads==0&&!g_native_columns_prime_attempted&&!g_back_prime_attempted);config_ok=true;clock_ms+=250;prime_native_columns(hook,nullptr);prime_back_regions(hook,nullptr);assert(installs==2);
 reset();home_layout::cfg.grid_enabled=false;home_layout::cfg.back_gesture={60,100};prime_native_columns(hook,nullptr);prime_back_regions(hook,nullptr);assert(map_reads==0);home_layout::cfg.grid_enabled=true;home_layout::cfg.back_gesture={80,100};clock_ms+=250;prime_native_columns(hook,nullptr);prime_back_regions(hook,nullptr);assert(installs==2);
 reset();g_back_setup_complete=true;for(unsigned n=0;n<500;++n){clock_ms+=250;prime_native_columns(hook,nullptr);prime_back_regions(hook,nullptr);}assert(map_reads==0&&installs==0&&column_scans==0&&back_scans==0);
 reset();thread_id=2;prime_native_columns(hook,nullptr);prime_back_regions(hook,nullptr);assert(map_reads==0);
 std::cout<<"native primers PASS: 500 reads -> 1 column/back scan; unsupported, failed install, late image/config, disabled -> enabled, worker handoff and generation reset\n";
}
