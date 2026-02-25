package com.sevtinge.hyperceiler.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.SpringItemTouchHelper;

import com.sevtinge.hyperceiler.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fan.appcompat.app.Fragment;
import fan.bottomsheet.BottomSheetModal;
import fan.recyclerview.widget.RecyclerView;

public class CardLicenseOrderFragment extends Fragment implements View.OnClickListener {

    public Button cancelButton;
    public Button confirmButton;

    public View mRootView;
    public View mTitle;
    public RecyclerView recyclerView;

    public List<Header> mCustomOrderList;
    public CardLicenseDraggableAdapter adapter;
    public SpringItemTouchHelper mItemTouchHelper;

    public BottomSheetModal mBottomSheetModal;
    public OnOrderCompletedCallBack mOrderCompletedCallBack;

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_home_order, container, false);
        recyclerView = mRootView.findViewById(R.id.recycler_view);
        mTitle = mRootView.findViewById(R.id.order_actionbar);
        cancelButton = mRootView.findViewById(R.id.cancel_button);
        confirmButton = mRootView.findViewById(R.id.confirm_button);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        Context context = getContext();
        if (mCustomOrderList == null) {
            mCustomOrderList = Collections.emptyList();
        }
        adapter = new CardLicenseDraggableAdapter(context, new ArrayList<>(mCustomOrderList));
        attachTouchHelper();
        recyclerView.setAdapter(this.adapter);
        cancelButton.setOnClickListener(this);
        confirmButton.setOnClickListener(this);
        return mRootView;
    }

    @Override
    public void onClick(View v) {

    }

    public void setCustomOrderList(List<Header> headers) {
        mCustomOrderList = headers;
    }

    public void setBottomSheetModal(BottomSheetModal bottomSheetModal) {
        mBottomSheetModal = bottomSheetModal;
        //bottomSheetModal.getBehavior().setOnModeChangeListener(new CardLicenseOrderFragment$.ExternalSyntheticLambda0(this));
    }


    public void setCompleteCallBack(OnOrderCompletedCallBack callBack) {
        mOrderCompletedCallBack = callBack;
    }

    public final void attachTouchHelper() {
        if (mItemTouchHelper != null) {
            mItemTouchHelper.attachToRecyclerView(null);
        }
        mItemTouchHelper = new SpringItemTouchHelper(new CardLicenseTouchHelperCallback());
        mItemTouchHelper.attachToRecyclerView(this.recyclerView);
    }
}
