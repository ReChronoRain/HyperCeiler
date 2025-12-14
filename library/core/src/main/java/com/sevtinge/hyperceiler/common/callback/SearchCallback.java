package com.sevtinge.hyperceiler.common.callback;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;

import fan.view.ActionModeAnimationListener;
import fan.view.SearchActionMode;

public class SearchCallback implements SearchActionMode.Callback {

    private static final int MAX_SEARCH_LENGTH = 32;
    private static final String MULT_PHONE_NUMBER_SUFFIX = "...";

    private String mSearchText = "";

    private View mAnimView;
    private View mAnchorView;
    private View mStickyView;

    private EditText mSearchInput;

    private ActionMode mActionMode;
    private SearchView.OnQueryTextListener mOnQueryTextListener;
    private final OnSearchListener mOnSearchListener;
    private final TextWatcher mSearchTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            mSearchText = s.toString().trim();
            if (mOnQueryTextListener != null) {
                mOnQueryTextListener.onQueryTextChange(mSearchText);
            }
            if (mAnimView != null) {
                mAnimView.setImportantForAccessibility(1);
            }
        }
    };
    private final TextView.OnEditorActionListener mEditorActionListener = (v, actionId, event) -> {
        if (v != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
        return false;
    };


    public interface OnSearchListener {
        void onCreateSearchMode(ActionMode actionMode, Menu menu);

        void onSearchModeAnimStart(boolean enabled);

        void onSearchModeAnimUpdate(boolean enabled, float f);

        void onSearchModeAnimStop(boolean enabled);

        void onDestroySearchMode(ActionMode actionMode);
    }

    public SearchCallback(SearchView.OnQueryTextListener onQueryTextListener, OnSearchListener onSearchListener) {
        mOnQueryTextListener = onQueryTextListener;
        mOnSearchListener = onSearchListener;
    }

    public void setup(View view, View view3) {
        mAnchorView = view;
        mAnimView = view3;
    }

    public boolean isSearchOn() {
        return mActionMode != null;
    }

    public String getSearchText() {
        return isSearchOn() ? mSearchText : null;
    }

    public void setSearchText(String str, boolean z, boolean z2) {
        if (mSearchInput != null) {
            if (!z) {
                mSearchInput.setCompoundDrawables(null, null, null, null);
                mSearchInput.clearFocus();
                if (!TextUtils.isEmpty(str) && str.length() > MAX_SEARCH_LENGTH) {
                    str = str.substring(0, 29).trim() + MULT_PHONE_NUMBER_SUFFIX;
                }
                mSearchInput.setText(str);
                mSearchInput.setSelection(mSearchInput.length());
            } else {
                mSearchInput.setText(str);
            }
        }
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

    public void finish() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mActionMode = mode;
        SearchActionMode searchActionMode = (SearchActionMode) mode;
        searchActionMode.setAnchorView(mAnchorView);
        searchActionMode.setAnimateView(mAnimView);
        mSearchInput = searchActionMode.getSearchInput();
        mSearchInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_SEARCH_LENGTH)});
        mSearchInput.setHint("");
        mSearchInput.addTextChangedListener(mSearchTextWatcher);
        mSearchInput.setOnEditorActionListener(mEditorActionListener);
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
        mSearchTextWatcher.afterTextChanged(Editable.Factory.getInstance().newEditable(""));
        mSearchInput = null;
        if (mOnSearchListener != null) {
            mOnSearchListener.onDestroySearchMode(mode);
        }
    }

    public void restoreFocus() {
        if (mSearchInput != null) {
            mSearchInput.requestFocus();
        }
    }
}
