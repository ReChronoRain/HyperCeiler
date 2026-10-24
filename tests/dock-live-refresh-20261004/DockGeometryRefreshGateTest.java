package com.sevtinge.hyperceiler.libhook.rules.home.dock;

public final class DockGeometryRefreshGateTest {
    static int checks;
    static void check(boolean b) { checks++; if (!b) throw new AssertionError("check " + checks); }
    public static void main(String[] args) {
        DockGeometryRefreshGate g = new DockGeometryRefreshGate(150);
        check(g.request(0, false) == 0); check(g.request(0, false) == -1);
        check(g.begin(0)); check(g.finish(true, 1) == -1);
        for (int i=0;i<1000;i++) check(g.request(i+2, false) == -1); // idle cannot poll.
        check(g.request(10, true) == 140); // recent edits must be delayed, NOT discarded.
        for (int i=11;i<145;i++) check(g.request(i, true) == -1); // one queued slider read.
        check(g.begin(150)); check(g.request(151, true) == -1);
        check(g.finish(true, 152) == 148); // last write during IPC always gets a follow-up.
        check(g.begin(300)); check(g.finish(true, 301) == -1);
        check(g.request(302, false) == -1);
        check(g.request(310, true) == 140); check(g.begin(450));
        check(g.finish(false, 460) == 1500);
        check(g.begin(1960)); check(g.finish(false, 1961) == 3000);
        check(g.begin(4961)); check(g.finish(false, 4962) == 6000);
        check(g.begin(10962)); check(g.finish(false, 10963) == -1);
        for (int i=0;i<1000;i++) check(g.request(11000+i, false) == -1);
        check(g.request(13000, true) == 0); check(g.begin(13000)); check(g.finish(true,13001)==-1);
        check(g.request(14000, true)==0);g.rejected();check(g.request(14001,true)==0);
        g.close(); check(!g.begin(14002));check(g.finish(false,14003)==-1);check(g.request(15000,true)==-1);
        for(String k:new String[]{"home_dock_bg_custom_enable","home_dock_add_blur","home_dock_bg_color",
            "home_dock_bg_height","home_dock_bg_margin_horizontal","home_dock_bg_margin_bottom",
            "home_dock_bg_radius","home_other_home_mode"}) {
            check(DockGeometryRefreshGate.isGeometryKey(k));
            check(DockGeometryRefreshGate.isGeometryKey("prefs_key_"+k));
        }
        check(DockGeometryRefreshGate.isGeometryKey(null));
        check(!DockGeometryRefreshGate.isGeometryKey("prefs_key_home_folder_columns"));
        check(!DockGeometryRefreshGate.isGeometryKey("home_dock_unlock_style"));
        System.out.println("DOCK_REFRESH_GATE="+checks+" checks; failed=0; idle reads=0; recovery retries=3");
    }
}
