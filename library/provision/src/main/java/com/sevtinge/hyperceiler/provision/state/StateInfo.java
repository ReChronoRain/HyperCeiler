package com.sevtinge.hyperceiler.provision.state;

public class StateInfo {

    private final State mCurrent;
    private State mNext;

    public StateInfo(State current) {
        mCurrent = current;
    }

    public State getCurrent() {
        return mCurrent;
    }

    public void setNext(State next) {
        mNext = next;
    }

    public State getNext() {
        return mNext;
    }
}
