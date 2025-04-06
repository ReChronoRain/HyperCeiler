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
package com.sevtinge.hyperceiler.common.utils.search;

import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import fan.appcompat.app.Fragment;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.view.SearchActionMode;

public class SearchModeHelper {

    public static SearchActionMode startSearchMode(Fragment fragment, RecyclerView resultView, View contentView,
                                                   View anchorView, NestedHeaderLayout animateView, TextWatcher watcher) {
        contentView.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);
        animateView.setInSearchMode(true);
        SearchActionMode searchActionMode = (SearchActionMode) fragment.startActionMode(new SearchActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                return onCreateSearchMode(anchorView, animateView, watcher, actionMode);
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                onDestroySearchMode(watcher, actionMode);
                updateView(contentView, resultView);
                animateView.setInSearchMode(false);
            }
        });
        if (searchActionMode == null) throw new NullPointerException("null cannot be cast to non-null type moralnorm.appcompat.internal.view.SearchActionMode");
        return searchActionMode;
    }

    private static boolean onCreateSearchMode(View anchorView, View animateView, TextWatcher watcher,
                                              ActionMode actionMode) {
        SearchActionMode searchActionMode = (SearchActionMode) actionMode;
        searchActionMode.setAnchorView(anchorView);
        searchActionMode.setAnimateView(animateView);
        searchActionMode.getSearchInput().addTextChangedListener(watcher);
        return true;
    }

    private static void onDestroySearchMode(TextWatcher watcher, ActionMode actionMode) {
        SearchActionMode searchActionMode = (SearchActionMode) actionMode;
        searchActionMode.getSearchInput().removeTextChangedListener(watcher);
        exitSearchMode(searchActionMode);
    }

    public static void exitSearchMode(SearchActionMode actionMode) {
        if (actionMode != null) {
            actionMode = null;
        }
    }

    public static void updateView(View contentView, RecyclerView resultView) {
        contentView.setVisibility(View.VISIBLE);
        resultView.setVisibility(View.GONE);
    }
}
