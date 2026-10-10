package com.android.systemui.plugins.statusbar;
public interface StatusBarStateController{interface StateListener {void onDozingChanged(boolean v);void onDozeAmountChanged(float a,float b);}void addCallback(StateListener l);void removeCallback(StateListener l);}
