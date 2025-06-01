/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;

import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.widget.PullToRefreshListener;

import fan.springback.trigger.BaseTrigger;
import fan.springback.trigger.DefaultTrigger;
import fan.springback.view.SpringBackLayout;

public class PullViewHelper {

    private static final String TAG = "MiuiPullRecyclerViewHelper";
    private static final int ACTION_REMOVE_PRIVATE = 0;
    private static final int ACTION_REMOVE_REFRESH = 1;
    private static final int ACTION_REMOVE_LOAD = 2;

    private Context mContext;
    private boolean mEnableLoadMore = false;
    private boolean mEnablePrivate = false;
    private boolean mEnablePullRefresh = false;
    private LoadAction mLoadAction;
    private LoadUpAction mLoadUpAction;
    private LockAction mLockAction;
    private SparseBooleanArray mPendingActions = new SparseBooleanArray();
    private PullToRefreshListener mPullListener;
    private DefaultTrigger mTrigger;

    public PullViewHelper(Context context, PullToRefreshListener pullToRefreshListener) {
        mContext = context;
        mPullListener = pullToRefreshListener;
        mTrigger = new DefaultTrigger(context);
        mLoadAction = new LoadAction(ACTION_REMOVE_PRIVATE);
        mLockAction = new LockAction();
        mLoadUpAction = new LoadUpAction(ACTION_REMOVE_PRIVATE);
    }

    public void attachSpringBackLayout(SpringBackLayout springBackLayout) {
        init(springBackLayout);
    }

    private void init(SpringBackLayout springBackLayout) {
        if (mEnablePrivate) addAction(mLockAction);
        if (mEnablePullRefresh) addAction(mLoadAction);
        if (mEnableLoadMore) addAction(mLoadUpAction);
        mTrigger.attach(springBackLayout);
    }


    public boolean canLoadMore() {
        return mEnableLoadMore;
    }

    public boolean enablePrivate() {
        return mEnablePrivate;
    }

    public LoadAction getLoadAction() {
        return mLoadAction;
    }

    public LockAction getLockAction() {
        return mLockAction;
    }

    public void setEnableLoadMore(boolean enabled) {
        mEnableLoadMore = enabled;
        if (mTrigger.containAction(mLoadUpAction)) {
            if (!mEnableLoadMore) {
                if (mTrigger.isActionRunning()) {
                    mPendingActions.put(ACTION_REMOVE_LOAD, true);
                    Log.i(TAG, "setEnableLoadMore false isActionRunning, addTo PendingTask");
                } else {
                    mTrigger.removeAction(mLoadUpAction);
                }
            } else {
                Log.i(TAG, "setEnableLoadMore contain, enable, clear loadMore");
                mPendingActions.delete(ACTION_REMOVE_LOAD);
            }
        } else if (mEnableLoadMore) {
            Log.i(TAG, "setEnableLoadMore addAction");
            addAction(mLoadUpAction);
        }
    }

    public void setEnablePrivate(boolean enabled) {
        mEnablePrivate = enabled;
        if (mTrigger.containAction(mLockAction)) {
            if (!mEnablePrivate) {
                Log.i(TAG, "setEnablePrivate remove LockAction");
                if (mTrigger.isActionRunning()) {
                    mPendingActions.put(ACTION_REMOVE_PRIVATE, true);
                } else {
                    mTrigger.removeAction(mLockAction);
                }
            } else {
                Log.i(TAG, "setEnablePrivate contain, enable, clear private");
                mPendingActions.delete(ACTION_REMOVE_PRIVATE);
            }
        } else if (mEnablePrivate) {
            addAction(mLockAction);
        }
    }

    public void setEnablePullRefresh(boolean enabled) {
        mEnablePullRefresh = enabled;
        if (mTrigger.containAction(mLoadAction)) {
            if (!mEnablePullRefresh) {
                if (mTrigger.isActionRunning()) {
                    mPendingActions.put(ACTION_REMOVE_REFRESH, true);
                } else {
                    mTrigger.removeAction(this.mLoadAction);
                }
            } else {
                Log.i(TAG, "setEnablePullRefresh contain, enable, clear refresh");
                mPendingActions.delete(ACTION_REMOVE_REFRESH);
            }
        } else if (mEnablePullRefresh) {
            addAction(mLoadAction);
        }
    }

    public void setPullListener(PullToRefreshListener pullToRefreshListener) {
        mPullListener = pullToRefreshListener;
    }

    public void triggerPullToRefreshIndeterminate() {
        if (mLoadUpAction != null) {
            mLoadAction.startIndeterminateAction();
        }
    }

    private void addAction(BaseTrigger.Action action) {
        if (action != null && !mTrigger.containAction(action)) {
            mTrigger.addAction(action);
        }
    }

    public boolean isActionRunning() {
        return mTrigger != null && mTrigger.isActionRunning();
    }

    public void checkPendingTask() {
        if (mTrigger != null && mPendingActions.size() != 0) {
            if (mPendingActions.get(ACTION_REMOVE_PRIVATE)) {
                Log.i(TAG, "checkPendingTask remove LockAction");
                mTrigger.removeAction(mLockAction);
                mPendingActions.delete(ACTION_REMOVE_PRIVATE);
            }
            if (mPendingActions.get(ACTION_REMOVE_REFRESH)) {
                Log.i(TAG, "checkPendingTask remove LoadAction");
                mTrigger.removeAction(mLoadAction);
                mPendingActions.delete(ACTION_REMOVE_REFRESH);
            }
            if (mPendingActions.get(ACTION_REMOVE_LOAD)) {
                Log.i(TAG, "checkPendingTask remove LoadUpAction ");
                mTrigger.removeAction(mLoadUpAction);
                mPendingActions.delete(ACTION_REMOVE_LOAD);
            }
        }
    }

    public void onLoadMoreComplete() {
        if (mLoadUpAction != null) {
            mLoadUpAction.notifyLoadComplete();
        }
    }

    public void onLoadMoreError() {
        if (mLoadUpAction != null) {
            mLoadUpAction.notifyLoadFail();
        }
    }

    public void onLoadMoreNoData() {
        if (mLoadUpAction != null) {
            mLoadUpAction.notifyActionNoData();
        }
    }

    public void onPullRefreshComplete() {
        if (mLoadAction != null) {
            mLoadAction.notifyLoadComplete();
        }
    }

    public void notifyListenerEnterPrivate() {
        if (mPullListener != null) {
            mPullListener.onEnterPrivate();
        }
    }

    public void notifyListenerRefresh() {
        if (mPullListener != null) {
            mPullListener.onRefresh();
        }
    }

    public void notifyListenerLoadMore() {
        if (mPullListener != null) {
            mPullListener.onLoadMore();
        }
    }

    public class LoadAction extends BaseTrigger.IndeterminateAction {

        public LoadAction(int i) {
            super(i);
        }

        public void onActivated() {
            Log.i(TAG, "LoadAction_onActivated");
        }

        public void onEntered() {
            Log.i(TAG, "LoadAction_onEntered");
        }

        public void onExit() {
            Log.i(TAG, "LoadAction_onExit");
        }

        public void onFinished() {
            Log.i(TAG, "LoadAction_onFinish");
            checkPendingTask();
        }

        public void onTriggered() {
            Log.i(TAG, "LoadAction_onTriggered");
            notifyListenerRefresh();
        }
    }

    public class LoadUpAction extends BaseTrigger.IndeterminateUpAction {

        public LoadUpAction(int i) {
            super(i);
        }

        public void onActivated() {
            Log.i("MiuiPullRecyclerViewHelper", "LoadUpAction_onActivated");
        }

        public void onEntered() {
            Log.i("MiuiPullRecyclerViewHelper", "LoadUpAction_onEntered");
        }

        public void onExit() {
            Log.i("MiuiPullRecyclerViewHelper", "LoadUpAction_onExit");
        }

        public void onFinished() {
            Log.i("MiuiPullRecyclerViewHelper", "LoadUpAction_onFinish");
            checkPendingTask();
        }

        public void onTriggered() {
            Log.i("MiuiPullRecyclerViewHelper", "LoadUpAction_onTriggered");
            notifyListenerLoadMore();
        }
    }

    public class LockAction extends BaseTrigger.SimpleAction {

        private ImageView mIconBody;
        private ImageView mIconHeader;
        private TextView mLabel;
        private View mView;

        public LockAction() {
            if (mContext != null) {
                mView = createLockView(LayoutInflater.from(mContext), null);
            }
        }

        private View createLockView(LayoutInflater inflater, ViewGroup root) {
            View view;
            if (root != null) {
                view = inflater.inflate(fan.springback.R.layout.miuix_sbl_simple_indicator, root, false);
            } else {
                view = inflater.inflate(fan.springback.R.layout.miuix_sbl_simple_indicator, null);
            }
            mIconHeader = view.findViewById(fan.springback.R.id.indicator_locked_header);
            mIconBody = view.findViewById(fan.springback.R.id.indicator_locked_body);
            mLabel = view.findViewById(fan.springback.R.id.label);
            return view;
        }

        public void onActivated() {
            Log.i("MiuiPullRecyclerViewHelper", "LockAction_onActivated");
            mIconBody.setImageDrawable(mContext.getDrawable(fan.springback.R.drawable.miuix_sbl_simple_indicator_locked_body_blue));
            mIconHeader.setImageDrawable(mContext.getDrawable(fan.springback.R.drawable.miuix_sbl_simple_indicator_locked_header_blue));
            mLabel.setText(R.string.release_enter_private);
            mLabel.setTextColor(mContext.getColor(fan.springback.R.color.miuix_sbl_locked_text_blue));
        }

        public View onCreateIndicator(LayoutInflater layoutInflater, ViewGroup viewGroup) {
            if (mView == null) mView = createLockView(layoutInflater, viewGroup);
            return mView;
        }

        public void onEntered() {
            Log.i("PullRecyclerViewHelper", "LockAction_onEntered");
            mIconBody.setImageDrawable(mContext.getDrawable(fan.springback.R.drawable.miuix_sbl_simple_indicator_locked_body_gray));
            mIconHeader.setImageDrawable(mContext.getDrawable(fan.springback.R.drawable.miuix_sbl_simple_indicator_locked_header_gray));
            mLabel.setText(R.string.pull_to_enter_private);
            mLabel.setTextColor(mContext.getColor(fan.springback.R.color.miuix_sbl_locked_text_gray));
        }

        public void onExit() {
            Log.i("PullRecyclerViewHelper", "LockAction_onExit");
        }

        public void onFinished() {
            Log.i("PullRecyclerViewHelper", "LockAction_onFinish");
            checkPendingTask();
        }

        public void onTriggered() {
            Log.i("PullRecyclerViewHelper", "LockAction_onTriggered");
            notifyListenerEnterPrivate();
        }
    }

    public class PendingTask {

        private boolean mRemovePullRefresh = false;
        private boolean mRemovePullLoad = false;
        private boolean mRemovePrivate = false;

        public boolean isRemovePrivate() {
            return this.mRemovePrivate;
        }

        public boolean isRemovePullLoad() {
            return mRemovePullLoad;
        }

        public boolean isRemovePullRefresh() {
            return mRemovePullRefresh;
        }

        public void setRemovePrivate(boolean removePrivate) {
            mRemovePrivate = removePrivate;
        }

        public void setRemovePullLoad(boolean pullLoad) {
            mRemovePullLoad = pullLoad;
        }

        public void setRemovePullRefresh(boolean pullRefresh) {
            mRemovePullRefresh = pullRefresh;
        }
    }
}
