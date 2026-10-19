package com.android.systemui.plugins.statusbar;
public class FakeStatus implements StatusBarStateController {public int adds,removes;public StateListener current;public void addCallback(StateListener l){adds++;current=l;}public void removeCallback(StateListener l){removes++;if(current==l)current=null;} }
