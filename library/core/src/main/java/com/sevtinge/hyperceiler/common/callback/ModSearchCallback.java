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

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SearchView;

import fan.view.SearchActionMode;

public class ModSearchCallback implements SearchActionMode.Callback {

    private static final int MAX_SEARCH_LENGTH = 32;

    private View mAnchorView;
    private View mAnimView;
    private EditText mSearchInput;

    private final OnSearchListener mOnSearchListener;
    private final SearchView.OnQueryTextListener mOnQueryTextListener;

    private String mSearchText = "";
    private final TextWatcher mSearchTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mSearchText = s.toString();
            if (mOnQueryTextListener != null) {
                mOnQueryTextListener.onQueryTextChange(mSearchText);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    public interface OnSearchListener {

        void onCreateSearchMode(ActionMode mode, Menu menu);

        void onDestroySearchMode(ActionMode mode);
    }

    public ModSearchCallback(OnSearchListener onSearchListener, SearchView.OnQueryTextListener onQueryTextListener) {
        mOnSearchListener = onSearchListener;
        mOnQueryTextListener = onQueryTextListener;
    }

    public void setup(View anchor, View animView) {
        mAnchorView = anchor;
        mAnimView = animView;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        SearchActionMode searchActionMode = (SearchActionMode) mode;
        searchActionMode.setAnchorView(mAnchorView);
        searchActionMode.setAnimateView(mAnimView);
        searchActionMode.setResultView(mAnimView);
        mSearchInput = searchActionMode.getSearchInput();
        mSearchInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_SEARCH_LENGTH)});
        mSearchInput.addTextChangedListener(mSearchTextWatcher);
        if (mOnSearchListener != null) {
            mOnSearchListener.onCreateSearchMode(mode, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        ((SearchActionMode) mode).getSearchInput().removeTextChangedListener(mSearchTextWatcher);
        mSearchTextWatcher.afterTextChanged(Editable.Factory.getInstance().newEditable(""));
        if (mOnSearchListener != null) {
            mOnSearchListener.onDestroySearchMode(mode);
        }
    }
}
