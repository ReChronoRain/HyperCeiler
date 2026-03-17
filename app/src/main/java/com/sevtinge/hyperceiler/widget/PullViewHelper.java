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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.widget;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.log.AndroidLog;

import fan.springback.trigger.BaseTrigger;
import fan.springback.trigger.DefaultTrigger;
import fan.springback.view.SpringBackLayout;

public class PullViewHelper {

    private static final String TAG = "MiuiPullRecyclerViewHelper";
    private static final int ACTION_REMOVE_PRIVATE = 0;
    private static final int ACTION_REMOVE_REFRESH = 1;
    private static final int ACTION_REMOVE_LOAD = 2;

    private final Context mContext;
    private boolean mEnableLoadMore = false;
    private boolean mEnablePrivate = false;
    private boolean mEnablePullRefresh = false;
    private final LoadAction mLoadAction;
    private final LoadUpAction mLoadUpAction;
    private final LockAction mLockAction;
    private final SparseBooleanArray mPendingActions = new SparseBooleanArray();
    private PullToRefreshListener mPullListener;
    private final DefaultTrigger mTrigger;

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
                    AndroidLog.i(TAG, "setEnableLoadMore: action running, defer remove load more");
                } else {
                    mTrigger.removeAction(mLoadUpAction);
                }
            } else {
                AndroidLog.i(TAG, "setEnableLoadMore: action exists, clear pending remove");
                mPendingActions.delete(ACTION_REMOVE_LOAD);
            }
        } else if (mEnableLoadMore) {
            AndroidLog.i(TAG, "setEnableLoadMore: add load more action");
            addAction(mLoadUpAction);
        }
    }

    public void setEnablePrivate(boolean enabled) {
        mEnablePrivate = enabled;
        if (mTrigger.containAction(mLockAction)) {
            if (!mEnablePrivate) {
                AndroidLog.i(TAG, "setEnablePrivate: remove lock action");
                if (mTrigger.isActionRunning()) {
                    mPendingActions.put(ACTION_REMOVE_PRIVATE, true);
                } else {
                    mTrigger.removeAction(mLockAction);
                }
            } else {
                AndroidLog.i(TAG, "setEnablePrivate: action exists, clear pending remove");
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
                AndroidLog.i(TAG, "setEnablePullRefresh: action exists, clear pending remove");
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
                AndroidLog.i(TAG, "checkPendingTask: remove lock action");
                mTrigger.removeAction(mLockAction);
                mPendingActions.delete(ACTION_REMOVE_PRIVATE);
            }
            if (mPendingActions.get(ACTION_REMOVE_REFRESH)) {
                AndroidLog.i(TAG, "checkPendingTask: remove refresh action");
                mTrigger.removeAction(mLoadAction);
                mPendingActions.delete(ACTION_REMOVE_REFRESH);
            }
            if (mPendingActions.get(ACTION_REMOVE_LOAD)) {
                AndroidLog.i(TAG, "checkPendingTask: remove load more action");
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
            AndroidLog.i(TAG, "LoadAction: activated");
        }

        public void onEntered() {
            AndroidLog.i(TAG, "LoadAction: entered");
        }

        public void onExit() {
            AndroidLog.i(TAG, "LoadAction: exit");
        }

        public void onFinished() {
            AndroidLog.i(TAG, "LoadAction: finished");
            checkPendingTask();
        }

        public void onTriggered() {
            AndroidLog.i(TAG, "LoadAction: triggered");
            notifyListenerRefresh();
        }
    }

    public class LoadUpAction extends BaseTrigger.IndeterminateUpAction {

        public LoadUpAction(int i) {
            super(i);
        }

        public void onActivated() {
            AndroidLog.i(TAG, "LoadUpAction: activated");
        }

        public void onEntered() {
            AndroidLog.i(TAG, "LoadUpAction: entered");
        }

        public void onExit() {
            AndroidLog.i(TAG, "LoadUpAction: exit");
        }

        public void onFinished() {
            AndroidLog.i(TAG, "LoadUpAction: finished");
            checkPendingTask();
        }

        public void onTriggered() {
            AndroidLog.i(TAG, "LoadUpAction: triggered");
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
            AndroidLog.i(TAG, "LockAction: activated");
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
            AndroidLog.i(TAG, "LockAction: entered");
            mIconBody.setImageDrawable(mContext.getDrawable(fan.springback.R.drawable.miuix_sbl_simple_indicator_locked_body_gray));
            mIconHeader.setImageDrawable(mContext.getDrawable(fan.springback.R.drawable.miuix_sbl_simple_indicator_locked_header_gray));
            mLabel.setText(R.string.pull_to_enter_private);
            mLabel.setTextColor(mContext.getColor(fan.springback.R.color.miuix_sbl_locked_text_gray));
        }

        public void onExit() {
            AndroidLog.i(TAG, "LockAction: exit");
        }

        public void onFinished() {
            AndroidLog.i(TAG, "LockAction: finished");
            checkPendingTask();
        }

        public void onTriggered() {
            AndroidLog.i(TAG, "LockAction: triggered");
            notifyListenerEnterPrivate();
        }
    }

    public static class PendingTask {

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
