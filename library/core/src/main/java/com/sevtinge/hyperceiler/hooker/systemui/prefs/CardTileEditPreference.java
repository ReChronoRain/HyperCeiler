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
package com.sevtinge.hyperceiler.hooker.systemui.prefs;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.model.adapter.CardTileAdapter;
import com.sevtinge.hyperceiler.model.adapter.CardTileAddAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CardTileEditPreference extends Preference {

    private static final String CARD_TILES_ENABLED_KEY =
        "prefs_key_systemui_plugin_card_tiles_enabled";
    private static final String CARD_TILES_KEY = "prefs_key_systemui_plugin_card_tiles";

    private final List<String> mCardList = new ArrayList<>();
    private final List<String> mCardData = new ArrayList<>();
    private final List<String> mAddCardData = new ArrayList<>();
    private final List<String> mDefaultCardData = Arrays.asList("wifi", "cell");
    private final ItemTouchHelper mItemTouchHelper =
        new ItemTouchHelper(new ItemTouchHelperCallback());

    private CardTileAdapter mCardTileAdapter;
    private CardTileAddAdapter mAddCardTileAdapter;

    public CardTileEditPreference(@NonNull Context context) {
        this(context, null);
    }

    public CardTileEditPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardTileEditPreference(
        @NonNull Context context,
        @Nullable AttributeSet attrs,
        int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_card_tile);
        setVisible(PrefsBridge.getBoolean(CARD_TILES_ENABLED_KEY, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        RecyclerView cardTiles = holder.itemView.findViewById(android.R.id.list);
        RecyclerView addCardTiles = holder.itemView.findViewById(R.id.card_tile_add_list);
        initView(cardTiles, addCardTiles);
    }

    private void initView(RecyclerView cardTiles, RecyclerView addCardTiles) {
        initCardData();
        mCardTileAdapter = new CardTileAdapter(mCardData);
        mAddCardTileAdapter = new CardTileAddAdapter(mAddCardData);

        createCardView(cardTiles, mCardTileAdapter);
        createCardView(addCardTiles, mAddCardTileAdapter);

        mItemTouchHelper.attachToRecyclerView(cardTiles);

        mCardTileAdapter.setOnDataChangeListener((changed, tile) -> onDataSetChanged(tile, true));
        mAddCardTileAdapter.setOnDataChangeListener((changed, tile) -> onDataSetChanged(tile, false));
    }

    @Override
    public void onDependencyChanged(@NonNull Preference dependency, boolean disableDependent) {
        setVisible(!disableDependent);
    }

    private void createCardView(RecyclerView cardView, RecyclerView.Adapter<?> adapter) {
        cardView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        cardView.setAdapter(adapter);
        cardView.setNestedScrollingEnabled(false);
    }

    private List<String> getTileList(String storedTiles) {
        String tiles = storedTiles.replace("List_", "");
        return TextUtils.isEmpty(tiles) ? new ArrayList<>() : Arrays.asList(tiles.split("\\|"));
    }

    private void initCardData() {
        mCardList.clear();
        mCardList.addAll(Arrays.asList(
            getContext().getResources().getStringArray(R.array.card_tile_list)
        ));

        String storedTiles = PrefsBridge.getString(CARD_TILES_KEY, "");
        mCardData.clear();
        if (storedTiles.isEmpty()) {
            mCardData.addAll(mDefaultCardData);
        } else {
            mCardData.addAll(getTileList(storedTiles));
        }

        mAddCardData.clear();
        mAddCardData.addAll(mCardList);
        mAddCardData.removeAll(mCardData);
    }

    private void onDataSetChanged(String tile, boolean removeFromCardList) {
        if (removeFromCardList) {
            mCardData.remove(tile);
            mAddCardData.add(tile);
        } else {
            mCardData.add(tile);
            mAddCardData.remove(tile);
        }
        saveList();
        mAddCardTileAdapter.notifyDataSetChanged();
        mCardTileAdapter.notifyDataSetChanged();
    }

    private void saveList() {
        StringBuilder builder = new StringBuilder();
        for (String tile : mCardData) {
            builder.append(tile).append("|");
        }
        PrefsBridge.putByApp(CARD_TILES_KEY, "List_" + builder);
    }

    private final class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder
        ) {
            int dragFlags = ItemTouchHelper.UP
                | ItemTouchHelper.DOWN
                | ItemTouchHelper.START
                | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder source,
            @NonNull RecyclerView.ViewHolder target
        ) {
            int fromPosition = source.getBindingAdapterPosition();
            int toPosition = target.getBindingAdapterPosition();
            if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                return false;
            }

            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(mCardData, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(mCardData, i, i - 1);
                }
            }

            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
            if (adapter != null) {
                adapter.notifyItemMoved(fromPosition, toPosition);
            }
            saveList();
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
    }
}
