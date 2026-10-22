    test("phone shared key is published with the shared owner epoch",[]{
        assert(prepare_for_child());home_layout::Config c;c.grid_enabled=true;c.cell_x=3;c.cell_y=13;
        auto r=execute_config(c);assert(r.applied==PhoneGridDart&&r.declined==0&&hook_calls==3);
        assert(gate()&&hc_home_grid_config_key==0x30d);
        assert(installed_vas[0]==0xa4ab78&&installed_vas[1]==0xa49adc&&installed_vas[2]==0x15228e8);
        request_revoke();assert(!gate()&&hc_home_grid_config_key==0);
        service_revoke();assert(original_constants()&&!unhook_calls);
        for(unsigned i=15;i<18;i++)assert(originals[i]);
    });
    test("row-only late change cannot evade static word comparison",[]{
        assert(prepare_for_child());home_layout::Config c;c.grid_enabled=true;c.cell_x=6;c.cell_y=7;
        execute_config(c);c.cell_y=8;auto r=maintenance(&c,true,auth,nullptr);
        assert(r.error==Error::LateConfiguration&&!gate()&&hc_home_grid_config_key==0);
        assert(original_constants()&&!unhook_calls);
    });
    test("column late change closes the same shared key",[]{
        assert(prepare_for_child());home_layout::Config c;c.grid_enabled=true;c.cell_x=6;c.cell_y=7;
        execute_config(c);c.cell_x=7;auto r=maintenance(&c,true,auth,nullptr);
        assert(r.error==Error::LateConfiguration&&!gate()&&hc_home_grid_config_key==0&&original_constants());
    });
    test("phone feature disabled while another feature keeps epoch active",[]{
        assert(prepare_for_child());home_layout::Config c;c.grid_enabled=true;c.cell_x=6;c.cell_y=7;
        c.knobs[0]={true,12};execute_config(c);c.grid_enabled=false;
        auto r=maintenance(&c,true,auth,nullptr);
        assert(r.applied==HotseatMargin&&gate()&&hc_home_grid_config_key==0&&original_constants());
        assert(!unhook_calls&&originals[15]&&originals[16]&&originals[17]);
        c.grid_enabled=true;r=maintenance(&c,true,auth,nullptr);
        assert(r.error==Error::LateConfiguration&&!gate()&&hc_home_grid_config_key==0);
    });
    test("phone enabled after first snapshot requires new process",[]{
        assert(prepare_for_child());home_layout::Config c;execute_config(c);
        c.grid_enabled=true;c.cell_x=6;c.cell_y=7;auto r=maintenance(&c,true,auth,nullptr);
        assert(r.error==Error::LateConfiguration&&r.declined==PhoneGridDart&&r.applied==0);
        assert(hc_home_grid_config_key==0&&!hook_calls&&!write_calls);
    });
    test("stale phone publication after revoke remains denied and is cleared",[]{
        assert(prepare_for_child());home_layout::Config c;c.grid_enabled=true;c.cell_x=6;c.cell_y=7;
        revoke_at_publish=true;auto r=execute_config(c);
        assert(r.error==Error::Revoked&&!gate()&&hc_home_grid_config_key==0);
        assert(original_constants()&&original_hooks());
    });
    for(bool all: {false,true})test((std::string("admitted frame keeps resident hooks after final auth failure ")+std::to_string(all)).c_str(),[all]{
        assert(prepare_for_child());home_layout::Config c;
        if(all)c=all_config();else{c.grid_enabled=true;c.cell_x=6;c.cell_y=7;}
        reject_after_activation=true;auto r=execute_config(c);
        assert(r.error==Error::Revoked&&!gate()&&hc_home_grid_config_key==0);
        assert(original_constants()&&!unhook_calls&&hook_calls==(all?18u:3u));
        for(unsigned i=15;i<18;i++)assert(originals[i]);
        assert(!original_hooks());
        // A watchdog pass cannot silently turn closure into hook reclamation.
        service_revoke();assert(!unhook_calls&&originals[15]&&originals[16]&&originals[17]);
    });
    for(auto range: {std::pair{0x1520f40u,2392u},std::pair{0x1521898u,276u},
        std::pair{0xa49e60u,260u},std::pair{0xa49f64u,1568u},std::pair{0xa4a890u,648u},
        std::pair{0xa4a698u,288u},std::pair{0xa4a7b8u,132u},std::pair{0xa4a83cu,84u},
        std::pair{0xa4a650u,36u},std::pair{0xa4a584u,204u}})for(bool tail: {false,true})
        test((std::string("phone semantic callee guard ")+std::to_string(range.first)+":"+std::to_string(tail)).c_str(),[range,tail]{
            assert(prepare_for_child());home_layout::Config c;c.grid_enabled=true;c.cell_x=6;c.cell_y=7;
            mapped[range.first+(tail?range.second-4:0)]^=std::byte{1};
            const auto r=execute_config(c);assert(r.error==Error::GuardMismatch&&!hook_calls&&!write_calls);
            assert(!gate()&&hc_home_grid_config_key==0);
        });
    for(auto pair: {std::pair{2,4},std::pair{10,4},std::pair{3,3},std::pair{3,14},std::pair{-1,7}})
        test((std::string("phone invalid grid domain ")+std::to_string(pair.first)+":"+std::to_string(pair.second)).c_str(),[pair]{
            assert(prepare_for_child());home_layout::Config c;c.grid_enabled=true;c.cell_x=pair.first;c.cell_y=pair.second;
            auto r=execute_config(c);assert(r.error==Error::UnsupportedConfiguration&&!hook_calls&&!write_calls);
            assert(!gate()&&hc_home_grid_config_key==0);
        });
