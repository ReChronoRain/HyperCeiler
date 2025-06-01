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
package com.sevtinge.hyperceiler.common.callback;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SearchView;

import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.common.model.adapter.ModSearchAdapter;

import fan.view.ActionModeAnimationListener;
import fan.view.SearchActionMode;

public class ModSearchCallback implements SearchActionMode.Callback {

    private static final int MAX_SEARCH_LENGTH = 32;
    private static final String MULT_PHONE_NUMBER_SUFFIX = "...";
    private ActionMode mActionMode;
    private View mAnchorView;
    private View mAnimView;
    private Context mContext;
    private RecyclerView mSearchResultView;
    private SearchView.OnQueryTextListener mOnQueryTextListener;
    private OnSearchListener mOnSearchListener;
    private EditText mSearchInput;
    private TextWatcher mSearchTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            findMod(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
    private String mSearchText = "";

    public interface OnSearchListener {

        void onCreateSearchMode(ActionMode mode, Menu menu);

        void onDestroySearchMode(ActionMode mode);

        void onSearchModeAnimStart(boolean z);

        void onSearchModeAnimStop(boolean z);

        void onSearchModeAnimUpdate(boolean z, float f);
    }

    public ModSearchCallback(Context context, RecyclerView searchResultView, OnSearchListener onSearchListener) {
        mContext = context;
        mSearchResultView = searchResultView;
        mOnSearchListener = onSearchListener;
    }

    public void setup(View anchor, View anim) {
        mAnchorView = anchor;
        mAnimView = anim;
    }

    public boolean isSearchOn() {
        return mActionMode != null;
    }

    public String getSearchText() {
        return isSearchOn() ? mSearchText : null;
    }

    public void removeTextChangedListener() {
        if (mSearchInput != null) {
            mSearchInput.removeTextChangedListener(mSearchTextWatcher);
        }
        mOnQueryTextListener = null;
    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

    void findMod(String filter) {
        mSearchResultView.setVisibility(filter.isEmpty() ? View.GONE : View.VISIBLE);
        ModSearchAdapter adapter = (ModSearchAdapter) mSearchResultView.getAdapter();
        if (adapter == null) return;
        adapter.getFilter(mContext).filter(filter);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mActionMode = mode;
        SearchActionMode searchActionMode = (SearchActionMode) mode;
        searchActionMode.setAnchorView(mAnchorView);
        searchActionMode.setAnimateView(mAnimView);
        mSearchInput = searchActionMode.getSearchInput();
        mSearchInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_SEARCH_LENGTH)});
        //mSearchInput.setHint("");
        //mSearchInput.setHintTextColor(mContext.getResources().getColor(0x7f0607dc));
        mSearchInput.addTextChangedListener(mSearchTextWatcher);
        //mSearchInput.setOnEditorActionListener(mEditorActionListener);
        if (mOnSearchListener != null) {
            mOnSearchListener.onCreateSearchMode(mode, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        ((SearchActionMode) mode).addAnimationListener(new ActionModeAnimationListener() {
            @Override
            public void onStart(boolean z) {
                if (mOnSearchListener != null) {
                    mOnSearchListener.onSearchModeAnimStart(z);
                }
            }

            @Override
            public void onStop(boolean z) {
                if (mOnSearchListener != null) {
                    mOnSearchListener.onSearchModeAnimStop(z);
                }
            }

            @Override
            public void onUpdate(boolean z, float f) {
                if (mOnSearchListener != null) {
                    mOnSearchListener.onSearchModeAnimUpdate(z, f);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        ((SearchActionMode) mode).getSearchInput().removeTextChangedListener(mSearchTextWatcher);
        //mSearchTextWatcher.afterTextChanged(Editable.Factory.getInstance().newEditable(""));
        if (mOnSearchListener != null) {
            mOnSearchListener.onDestroySearchMode(mode);
        }
    }

    public void restoreFocus() {
        if (mSearchInput != null) {
            mSearchInput.requestFocus();
        }
    }

    public void finish() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }
}
