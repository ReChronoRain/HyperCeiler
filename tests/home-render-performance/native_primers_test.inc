int main(){
 reset();columns_ok=false;for(unsigned n=0;n<500;++n){clock_ms+=250;prime_native_columns(hook,nullptr);}assert(column_scans==1&&map_reads==1&&image_reads==1&&installs==0&&g_native_columns_prime_attempted);
 reset();image_available=false;prime_native_columns(hook,nullptr);assert(!g_native_columns_prime_attempted&&column_scans==0);clock_ms+=250;image_available=true;prime_native_columns(hook,nullptr);assert(column_scans==1&&installs==1&&g_slots[0].backend_owned);
 reset();allow_install=false;for(unsigned n=0;n<500;++n){clock_ms+=250;prime_native_columns(hook,nullptr);}assert(column_scans==1&&installs==1&&g_slots[0].address);
 reset();g_back_setup_complete=true;for(unsigned n=0;n<500;++n){clock_ms+=250;prime_native_columns(hook,nullptr);}assert(map_reads==0&&installs==0&&column_scans==0);
 reset();thread_id=2;prime_native_columns(hook,nullptr);assert(map_reads==0);
 std::cout<<"native columns primer PASS: bounded discovery, late image, install failure and worker handoff\n";
}
