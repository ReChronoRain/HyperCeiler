package com.sevtinge.hyperceiler.provision.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.provision.fragment.StartupFragment;
import com.sevtinge.hyperceiler.provision.utils.IKeyEvent;
import com.sevtinge.hyperceiler.provision.utils.IOnFocusListener;
import com.sevtinge.hyperceiler.provision.utils.OobeUtils;
import com.sevtinge.hyperceiler.provision.utils.PageIntercepHelper;
import com.sevtinge.hyperceiler.provision.utils.ProvisionStateHolder;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class ProvisionActivity extends ProvisionBaseActivity {

    private static final String TAG = "ProvisionActivity";

    private StateMachine mStateMachine;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isDeviceIsProvisioned()) {
            PageIntercepHelper.getInstance().register(this);
            PageIntercepHelper.getInstance().setCallback(ProvisionActivity.this::onActivityResult);

            mStateMachine = new StateMachine(this);
            mStateMachine.start(savedInstanceState == null ||
                    savedInstanceState.getBoolean("com.android.provision:state_enter_currentstate", true));
            ProvisionStateHolder.getInstance().setStateMachine(mStateMachine);
            if (mNewBackBtn != null) {
                Log.i(TAG, "back button set accessibility no");
                mNewBackBtn.setImportantForAccessibility(2);
                if (mNewBackBtn.getParent() != null) {
                    Log.i(TAG, "back button remove");
                    ((ViewGroup) mNewBackBtn.getParent()).removeView(mNewBackBtn);
                }
            }
        } else {
            finishSetup();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mStateMachine.getCurrentState() instanceof IKeyEvent) {
            Log.i(TAG, " here is onWindowFocusChanged ");
            ((StartupState) mStateMachine.getCurrentState()).onWindowFocusChanged(hasFocus);
        }
    }

    public void run(int i) {
        mStateMachine.run(i);
    }

    private boolean isDeviceIsProvisioned() {
        return false;
        //return Utils.isProvisioned(this);
    }

    public void finishSetup() {
        //enableStatusBar(true);
        if (!OobeUtils.isGestureLineShow(this)) {
            //Utils.hideGestureLine(this, false);
        }
        PackageManager pm = getPackageManager();
        //Utils.setWallperProvisioned(this, true);
        /*ComponentName componentName = new ComponentName(this, ProvisionActivity.class);
        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        Intent intent = new Intent("android.provision.action.PROVISION_COMPLETE");
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
        Utils.sendBroadcastAsUser(this, intent);*/
        finish();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("com.android.provision:state_enter_currentstate", mStateMachine.mCurrentState instanceof StartupState);
    }

    @Override
    public void onDestroy() {
        PageIntercepHelper.getInstance().unregisterReceiver(this);
        //enableStatusBar(true);
        //unRegisterNetworkChangedReceiver();
        super.onDestroy();
        if (isDeviceIsProvisioned()) {
            //Process.killProcess(Process.myPid());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult requestCode: " + requestCode + " resultCode =  " + resultCode);
        if (requestCode == 33 && OobeUtils.shouldNotFinishDefaultActivity()) {
            mStateMachine.resumeState();
        } else if (requestCode == 1) {
            if (resultCode == -1) {
                nextAction(124);
            }
        } else if (requestCode == 3510) {
            //onAuraActivityResult(requestCode, resultCode);
        } else {
            mStateMachine.onResult(resultCode, data);
            if (data != null) {
                //mStateMachine.setMultiSimSettingsSkiped(data.getBooleanExtra("extra_mutisimsettings_force_skiped", false));
                //mStateMachine.setBootVideoSkiped(data.getBooleanExtra("extra_bootvideo_force_skiped", false));
            }
            mStateMachine.run(resultCode);
        }
    }

    void nextAction(int i) {
        Log.i(TAG, " here is nextAction ");
        //startActivityForResult(WizardManagerHelper.getNextIntent(getIntent(), i), 10000);
    }

    @Override
    public boolean hasPreview() {
        return false;
    }

    @Override
    public boolean hasTitle() {
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public static class StartupState extends State implements IKeyEvent, IOnFocusListener {

        private boolean mHasBooted;
        private Fragment.SavedState mSavedState;
        private StartupFragment mStartupFragment;

        public void setBooted(boolean booted) {
            mHasBooted = booted;
        }

        @Override
        public boolean isAvailable(boolean available) {
            return true;
        }

        @Override
        public void onEnter(boolean z, boolean z2) {
            FragmentManager fragmentManager = ((ProvisionActivity) mContext).getSupportFragmentManager();
            mStartupFragment = new StartupFragment();
            if (fragmentManager.findFragmentByTag(StartupFragment.class.getSimpleName()) == null) {
                buildStartupFragment(mStartupFragment, fragmentManager);
            } else {
                mStartupFragment.setInitialSavedState(mSavedState);
                buildStartupFragment(mStartupFragment, fragmentManager);
            }
        }

        @Override
        public void onLeave() {
            FragmentManager supportFragmentManager = ((ProvisionActivity) mContext).getSupportFragmentManager();
            Fragment fragmentByTag = supportFragmentManager.findFragmentByTag(StartupFragment.class.getSimpleName());
            if (fragmentByTag != null) {
                mSavedState = supportFragmentManager.saveFragmentInstanceState(supportFragmentManager.findFragmentByTag(StartupFragment.class.getSimpleName()));
                FragmentTransaction beginTransaction = supportFragmentManager.beginTransaction();
                beginTransaction.setCustomAnimations(0, R.anim.provision_slide_out_left_animator);
                beginTransaction.remove(fragmentByTag);
                beginTransaction.commitAllowingStateLoss();
            }
        }

        private void buildStartupFragment(Fragment fragment, FragmentManager fragmentManager) {
            FragmentTransaction beginTransaction = fragmentManager.beginTransaction();
            beginTransaction.replace(android.R.id.content, fragment, StartupFragment.class.getSimpleName());
            beginTransaction.commitAllowingStateLoss();
        }

        @Override
        public void keyDownDispatcher(int keyCode, KeyEvent event) {
            mStartupFragment.onKeyDownChild(keyCode, event);
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            if (mStartupFragment != null) {
                mStartupFragment.onWindowFocusChanged(hasFocus);
            }
        }
    }

    public static class PermissionState extends State {}

    public static class TermsAndStatementState extends State {

        @Override
        public String getPageTag() {
            return "terms";
        }
    }
    public static class BasicState extends State {}

    public static class CongratulationState extends State {
        @Override
        public void onLeave() {
            super.onLeave();
            /*Utils.setNavigationBarFullScreen(mContext, false);
            if (mContext != null && !Utils.isGestureLineShow(mContext)) {
                Utils.hideGestureLine(mContext, false);
            }*/
        }
    }
    public static class StateMachine {

        private Context mContext;

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

        private void init() {
            mStates = new SparseArray<>();
            mStateStack = new ArrayList<>();

            mCurrentState = State.create("StartupState");
            mPermissionState = State.create("PermissionState").setTargetClass(PermissionSettingsActivity.class);
            mTermsAndStatementState = State.create("TermsAndStatementState").setTargetClass(TermsAndStatementActivity.class);
            mBasicState = State.create("BasicState").setTargetClass(BasicSettingsActivity.class);
            mCompleteState = (CongratulationState) State.create("CongratulationState").setTargetClass(CongratulationActivity.class);

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
            return mStates.get(state.hashCode()).mCurrent;
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
            mStates.get(state.getClass().hashCode()).mNext = nextState;
        }

        public void start(boolean z) {
            restoreState();
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

        public void run(int code) {
            Log.i("Provision_DefaultActivity", "run code: " + code);
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
                state = nextState == null ? getStateInfo(state).mNext : nextState;
                if (state == null) {
                    break;
                }
            } while (!state.isAvailable(true));
            Log.d("Provision_DefaultActivity", "getNextAvailableState is " + state);
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
                Log.w("Provision_DefaultActivity", mCurrentState + " can not go back, stop here");
            }
            return mCurrentState;
        }

        public void transitToNext() {
            Log.d("Provision_DefaultActivity", "transitToNext mCurrentState is " + this.mCurrentState);
            this.mCurrentState.onLeave();
            if (needFinish()) {
                ProvisionActivity activity = (ProvisionActivity) mContext;
                /*if (Utils.isNewGlobalOOBE()) {
                    Utils.goToNextPage((Activity) mContext, activity.getIntent(), -1);
                    activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    if (Utils.shouldNotFinishDefaultActivity()) {
                        return;
                    }
                    activity.finish();
                    return;
                }*/
                activity.finishSetup();
                clearState();
                return;
            }
            mStateStack.add(mCurrentState);
            mCurrentState = getNextAvailableState(mCurrentState);
            mCurrentState.onEnter(mCurrentState.canBackTo(), true);
            ((Activity) mContext).overridePendingTransition(R.anim.provision_slide_in_right, R.anim.provision_slide_out_left);
            saveState();
        }

        private boolean needFinish() {
            return getNextAvailableState(mCurrentState) == null;
        }

        private void transitToPrevious() {
            if (!mStateStack.isEmpty()) {
                State previousState = getPreviousAvailableState(mStateStack);
                mCurrentState.onLeave();
                mCurrentState = previousState;
                if (previousState instanceof StartupState) {
                    ((StartupState) previousState).setBooted(true);
                }
                int size = mStateStack.size() - 1;
                mCurrentState.onEnter(size >= 0 && mStateStack.get(size).canBackTo(), false);
                ((Activity) mContext).overridePendingTransition(R.anim.provision_slide_in_left, R.anim.provision_slide_out_right);
                saveState();
            }
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
            mContext.getSharedPreferences("pref_state", 0).edit().clear().apply();
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
            SharedPreferences.Editor edit = mContext.getSharedPreferences("pref_state", 0).edit();
            edit.clear();
            for (int i = 0; i < mStateStack.size(); i++) {
                Log.w(TAG, " saveState is " + mStateStack.get(i).getClass().getSimpleName());
                edit.putString("com.android.provision.STATE_" + i, mStateStack.get(i).getClass().getSimpleName());
            }
            edit.putString("com.android.provision.STATE_" + mStateStack.size(), mCurrentState.getClass().getSimpleName());
            edit.apply();
        }

        private void restoreState() {
            int i = 0;
            SharedPreferences prefState = mContext.getSharedPreferences("pref_state", 0);
            String state = StartupState.class.getSimpleName();
            mCurrentState = getStateInfo(state).mCurrent;
            while (state != null) {
                state = null;
                try {
                    state = prefState.getString("com.android.provision.STATE_" + i, null);
                } catch (Exception e) {}
                if (state != null) {
                    if (i != 0) {
                        mStateStack.add(mCurrentState);
                    }
                    Log.w(TAG, " state is " + state + " and getStateInfo(state) is " + getStateInfo(state));
                    if (getStateInfo(state) != null) {
                        mCurrentState = getStateInfo(state).mCurrent;
                    }
                }
                i++;
            }
            if (!mCurrentState.isAvailable(true)) {
                mCurrentState = getPreviousAvailableState(mStateStack);
            }
        }

        public class StateInfo {
            private State mCurrent;
            private State mNext;

            public StateInfo(State current) {
                mCurrent = current;
            }

            public State getNext() {
                return mNext;
            }
        }
    }

    public static class State {

        public static final String PREFIX = "com.sevtinge.provision.activity.ProvisionActivity$";

        protected Context mContext;

        protected StateMachine mStateMachine;
        protected String mPackageName;
        public String mClassName;
        public Class<?> mTargetClass;

        protected Handler mHandler = new Handler(Looper.getMainLooper());

        public boolean canBackTo() {
            return true;
        }

        public State getNextState() {
            return null;
        }

        public void onLeave() {}

        public static State create(String name) {
            try {
                return (State) Class.forName(PREFIX + name).getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                Log.e(TAG, String.valueOf(e));
                return null;
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public State setPackageName(String packageName) {
            mPackageName = packageName;
            return this;
        }

        public State setClassName(String className) {
            mClassName = className;
            return this;
        }

        public State setTargetClass(Class<?> targetClass) {
            mTargetClass = targetClass;
            return this;
        }

        public State setStateMachine(StateMachine stateMachine) {
            mStateMachine = stateMachine;
            mContext = stateMachine.mContext;
            return this;
        }

        public void setStateContext(Context context) {
            mContext = context;
        }

        public void onEnter(boolean z, boolean z2) {
            Log.d(TAG, "targetClass is " + mTargetClass);
            Intent intent = getIntent();
            intent.putExtra("extra_disable_back", !z);
            intent.putExtra("extra_to_next", z2);
            ((Activity) mContext).startActivityForResult(intent, 0);
        }

        public boolean isAvailable(boolean available) {
            return mContext.getPackageManager().resolveActivity(getIntent(), 0) != null;
        }

        protected Intent getIntent() {
            Intent intent = new Intent();
            if (TextUtils.isEmpty(mPackageName)) {
                intent.setClass(mContext, mTargetClass);
            } else {
                intent.setClassName(mPackageName, mClassName);
            }
            return intent;
        }

        public String getPageTag() {
            return "";
        }
    }

}
