package com.sevtinge.hyperceiler.provision.state;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.provision.R;
import com.sevtinge.hyperceiler.provision.activity.BasicSettingsActivity;
import com.sevtinge.hyperceiler.provision.activity.CongratulationActivity;
import com.sevtinge.hyperceiler.provision.activity.DefaultActivity;
import com.sevtinge.hyperceiler.provision.activity.PermissionSettingsActivity;
import com.sevtinge.hyperceiler.provision.activity.TermsAndStatementActivity;
import com.sevtinge.hyperceiler.provision.utils.PageIntercepHelper;

import java.util.ArrayList;

import fan.provision.OobeUtils;

public class StateMachine {

    private static final String TAG = "StateMachine";

    private static final String PROVISION_STATE = "com.android.provision.STATE_";

    private final Context mContext;

    private State mCurrentState;
    private State mPermissionState;
    private State mTermsAndStatementState;
    private State mBasicState;
    private CongratulationState mCompleteState;


    private ArrayList<State> mStateStack;
    private SparseArray<StateInfo> mStates;

    public StateMachine(Context context) {
        mContext = context;
        init();
        //PreLoadManager.get().setCompleteDefaultActivityLoad();
    }

    public Context getContext() {
        return mContext;
    }

    private void init() {
        mStates = new SparseArray<>();
        mStateStack = new ArrayList<>();

        mCurrentState = new StartupState();
        mPermissionState = new PermissionState().setTargetClass(PermissionSettingsActivity.class);
        mTermsAndStatementState = new TermsAndStatementState().setTargetClass(TermsAndStatementActivity.class);
        mBasicState = new BasicState().setTargetClass(BasicSettingsActivity.class);
        mCompleteState = new CongratulationState();
        mCompleteState.setTargetClass(CongratulationActivity.class);

        addState(mCurrentState);
        addState(mPermissionState);
        addState(mTermsAndStatementState);
        addState(mBasicState);
        addState(mCompleteState);

        setNextState(mCurrentState, mPermissionState);
        setNextState(mPermissionState, mTermsAndStatementState);
        setNextState(mTermsAndStatementState, mBasicState);
        setNextState(mBasicState, mCompleteState);
    }

    private void addState(State state) {
        state.setStateMachine(this);
        mStates.put(state.getClass().hashCode(), new StateInfo(state));
        //PreLoadManager.get().addDefaultActivityClass(state);
    }

    public State getState(Class<? extends State> state) {
        return mStates.get(state.hashCode()).getCurrent();
    }

    public StateInfo getStateInfo(State state) {
        return mStates.get(state.getClass().hashCode());
    }

    private StateInfo getStateInfo(String state) {
        if (StartupState.class.getSimpleName().equals(state)) {
            return mStates.get(StartupState.class.hashCode());
        }
        if (PermissionState.class.getSimpleName().equals(state)) {
            return mStates.get(PermissionState.class.hashCode());
        }
        if (TermsAndStatementState.class.getSimpleName().equals(state)) {
            return mStates.get(TermsAndStatementState.class.hashCode());
        }
        if (BasicState.class.getSimpleName().equals(state)) {
            return mStates.get(BasicState.class.hashCode());
        }
        if (CongratulationState.class.getSimpleName().equals(state)) {
            return mStates.get(CongratulationState.class.hashCode());
        }
        return null;
    }

    private void setNextState(State state, State nextState) {
        StateInfo stateInfo = mStates.get(state.getClass().hashCode());
        stateInfo.setNext(nextState);
    }

    public void start(boolean z) {
        if (OobeUtils.shouldPersistOobeState(mContext)) {
            restoreState();
        }
        int size = mStateStack.size() - 1;
        if (z) {
            mCurrentState.onEnter(size >= 0 && mStateStack.get(size).canBackTo(), true);
            saveState();
        }
    }

    public void onResult(int i, Intent intent) {
            /*if (mCurrentState == mAccountState) {
                mCloudServiceState.onLoginResult(i, intent);
                mFindDeviceState.onLoginResult(i, intent);
            }*/
    }

    public void enterCurrentState(@Nullable Bundle activityOptions) {
        if (mCurrentState == null) {
            return;
        }
        int size = mStateStack.size() - 1;
        boolean canBack = size >= 0 && mStateStack.get(size).canBackTo();
        mCurrentState.onEnter(canBack, true, activityOptions);
    }

    public void run(int code) {
        AndroidLog.i("Provision_DefaultActivity", "run code: " + code);
        if (!PageIntercepHelper.getInstance().isIngoreCode(code)) {
            switch (code) {
                case -1 -> transitToNext();
                case 0 -> transitToPrevious();
                default -> transitToOthers();
            }
        }
    }

    public State getCurrentState() {
        return mCurrentState;
    }

    public ArrayList<State> getStateStack() {
        return mStateStack;
    }

    private State getNextAvailableState(State state) {
        do {
            State nextState = state.getNextState();
            state = nextState == null ? getStateInfo(state).getNext() : nextState;
            if (state == null) {
                break;
            }
        } while (!state.isAvailable(true));
        AndroidLog.d(TAG, "getNextAvailableState is " + state);
        return state;
    }

    private State getPreviousAvailableState(ArrayList<State> arrayList) {
        while (true) {
            int size = arrayList.size() - 1;
            State state2 = arrayList.get(size);
            if (!state2.canBackTo()) {
                break;
            }
            arrayList.remove(size);
            if (state2.isAvailable(false)) {
                mCurrentState = state2;
                break;
            }
            mCurrentState = state2;
        }
        if (!mCurrentState.isAvailable(false)) {
            AndroidLog.w("Provision_DefaultActivity", mCurrentState + " can not go back, stop here");
        }
        return mCurrentState;
    }

    public void transitToNext() {
        AndroidLog.d("Provision_DefaultActivity", "transitToNext mCurrentState is " + this.mCurrentState);
        State nextState = getNextAvailableState(mCurrentState);
        boolean deferStartupLeaveForPermission = mCurrentState instanceof StartupState && nextState instanceof PermissionState;
        if (!deferStartupLeaveForPermission) {
            mCurrentState.onLeave();
        }
        if (nextState == null) {
            DefaultActivity activity = (DefaultActivity) mContext;
            activity.finishSetup();
            clearState();
            return;
        }
        mStateStack.add(mCurrentState);
        mCurrentState = nextState;

        if (mCurrentState instanceof PermissionState) {
            AndroidLog.d(TAG, "transitToNext: PermissionState");
            //OobeUtils.checkAndActivateEsimAfterFactoryReset(mContext);
        } else {
            mCurrentState.onEnter(mCurrentState.canBackTo(), true);
            ((DefaultActivity) mContext).overridePendingTransition(R.anim.provision_slide_in_right, R.anim.provision_slide_out_left);
        }
        saveState();
    }

    private boolean needFinish() {
        return getNextAvailableState(mCurrentState) == null;
    }

    private void transitToPrevious() {
        if (mStateStack.size() <= 0) return;
        mCurrentState = getPreviousAvailableState(mStateStack);
        if (mCurrentState instanceof StartupState) {
            ((StartupState) mCurrentState).setBooted(true);
        } else {
            mCurrentState.onLeave();
        }
        int size = mStateStack.size() - 1;
        mCurrentState.onEnter(size >= 0 && mStateStack.get(size).canBackTo(), false);
        if (!(mCurrentState instanceof PermissionState)) {
            ((DefaultActivity) this.mContext).overridePendingTransition(R.anim.provision_slide_in_left, R.anim.provision_slide_out_right);
        }
        saveState();
    }

    private void transitToOthers() {
        State nextState = mCurrentState.getNextState();
        if (nextState != null && nextState != mCompleteState) {
            mStateStack.add(mCurrentState);
            mCurrentState = nextState;
            int size = mStateStack.size() - 1;
            mCurrentState.onEnter(size >= 0 && mStateStack.get(size).canBackTo(), true);
            saveState();
        }
    }

    private void clearState() {
        if (!OobeUtils.shouldPersistOobeState(mContext)) {
            return;
        }
        mContext.getSharedPreferences("pref_oobe_state", 0).edit().clear().apply();
    }

    public void resumeState() {
        ((Activity) mContext).overridePendingTransition(17432578, 17432579);
        if (mCurrentState != null && mCurrentState.isAvailable(true)) {
            mCurrentState.onEnter(true, true);
            saveState();
        } else {
            transitToPrevious();
        }
    }

    private void saveState() {
        if (!OobeUtils.shouldPersistOobeState(mContext)) {
            return;
        }
        SharedPreferences.Editor edit = mContext.getSharedPreferences("pref_oobe_state", 0).edit();
        edit.clear();
        for (int i = 0; i < mStateStack.size(); i++) {
            AndroidLog.w(TAG, " saveState is " + mStateStack.get(i).getClass().getSimpleName());
            edit.putString(PROVISION_STATE + i, mStateStack.get(i).getClass().getSimpleName());
        }
        edit.putString(PROVISION_STATE + mStateStack.size(), mCurrentState.getClass().getSimpleName());
        edit.apply();
    }

    private void restoreState() {
        int i = 0;
        SharedPreferences prefState = mContext.getSharedPreferences("pref_oobe_state", 0);
        String state = StartupState.class.getSimpleName();
        mCurrentState = getStateInfo(state).getCurrent();
        while (state != null) {
            state = null;
            try {
                state = prefState.getString(PROVISION_STATE + i, null);
            } catch (Exception ignored) {}
            if (state != null) {
                if (i != 0) {
                    mStateStack.add(mCurrentState);
                }
                AndroidLog.w(TAG, " state is " + state + " and getStateInfo(state) is " + getStateInfo(state));
                if (getStateInfo(state) != null) {
                    mCurrentState = getStateInfo(state).getCurrent();
                }
            }
            i++;
        }
        if (!mCurrentState.isAvailable(true)) {
            mCurrentState = getPreviousAvailableState(mStateStack);
        }
    }
}
