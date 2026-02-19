package com.sevtinge.hyperceiler.home;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.home.utils.HeaderManager;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fan.bottomsheet.BottomSheetModal;

public class CustomOrderFragment extends Fragment implements View.OnClickListener {

    public DraggableCardAdapter adapter;

    public View mRootView;
    public View mTitleBar;
    public Button cancelButton;
    public Button confirmButton;
    public RecyclerView recyclerView;

    public BottomSheetModal mBottomSheetModal;

    public OnCompleteCallBack mCompleteCallBack;
    public List<Header> mCustomOrderList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_custom_order, container, false);
        recyclerView = mRootView.findViewById(R.id.recycler_view);
        mTitleBar = mRootView.findViewById(R.id.set_order_actionbar);
        cancelButton = mRootView.findViewById(R.id.cancel_button);
        confirmButton = mRootView.findViewById(R.id.confirm_button);

        // 获取原始全量数据
        List<Header> rawHeaders = CustomOrderManager.getCustomOrderList();

        // 使用工具类获取用于弹窗的列表（已处理拷贝和安装过滤）
        mCustomOrderList = HeaderManager.getCustomOrderHeaders(getContext(), rawHeaders);

        /*if (mCustomOrderList == null || mCustomOrderList.size() != CustomOrderConstants.getCustomOrderCount()) {
            mCustomOrderList = ItemTypeSortManager.getCustomOrderList(true);
        }*/
        /*ItemTypeSortManager.removeUnsupportedCustomItems(this.mCustomOrderList);
        ArrayList arrayList = new ArrayList();
        Iterator<CustomOrderItem> it = this.mCustomOrderList.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().deepCopy());
        }*/
        recyclerView.setLayoutManager(createLayoutManager(false));
        recyclerView.setHasFixedSize(true);
        adapter = new DraggableCardAdapter(getContext(), mCustomOrderList);
        recyclerView.setAdapter(adapter);
        //updateTouchHelper(isNCMode);
        confirmButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        return this.mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /*if (mBottomSheetModal != null && mBottomSheetModal.getRootView() != null) {
            mMaskView = mBottomSheetModal.getRootView().findViewById(0x7f0a08fc);
        }
        if (mMaskView != null) {
            mMaskView.setOnClickListener(new CustomOrderFragment$2(this));
        }
        refreshView();*/
    }

    public final RecyclerView.LayoutManager createLayoutManager(boolean z) {
        if (!z) {
            return new LinearLayoutManager(getActivity());
        }
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (position == adapter.getItemCount() - 1 || adapter.getItemViewType(position) == 2) ? 2 : 1;
            }
        });
        return gridLayoutManager;
    }

    @Override
    public void onClick(View v) {
        if (v == cancelButton) {
            onCancelButtonClick();
        } else if (v == confirmButton) {
            onCompleteButtonClick();
        }
    }

    public void setCompleteCallBack(OnCompleteCallBack callBack) {
        mCompleteCallBack = callBack;
    }

    public void onCompleteButtonClick() {
        if (adapter != null) {
            HeaderManager.saveHeaderPreferences(getContext(), adapter.getData());
            bottomSheetModalDismiss();
            if (mCompleteCallBack != null) {
                mCompleteCallBack.refresh();
            }
            mCompleteCallBack = null;
        }
    }

    public void onCancelButtonClick() {
        bottomSheetModalDismiss();
    }


    public void bottomSheetModalDismiss() {
        if (mBottomSheetModal != null) {
            mBottomSheetModal.dismiss();
        }
    }

    public void setBottomSheetModal(BottomSheetModal bottomSheetModal) {
        mBottomSheetModal = bottomSheetModal;
        //bottomSheetModal.getBehavior().setOnModeChangeListener(new CustomOrderFragment$.ExternalSyntheticLambda0(this));
    }
}
