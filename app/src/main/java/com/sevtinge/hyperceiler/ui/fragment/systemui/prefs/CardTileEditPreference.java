package com.sevtinge.hyperceiler.ui.fragment.systemui.prefs;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.adapter.CardTileAdapter;
import com.sevtinge.hyperceiler.data.adapter.CardTileAddAdapter;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceViewHolder;

public class CardTileEditPreference extends Preference {

    GridLayoutManager mCradLayoutManager;
    View mItemView;
    RecyclerView mCardTiles;
    RecyclerView mAddCardTiles;
    CardTileAdapter mCardTileAdapter;
    CardTileAddAdapter mAddCardTileAdapter;
    List<String> mCardList = new ArrayList<>();
    List<String> mCardData = new ArrayList<>();
    List<String> mAddCardData = new ArrayList<>();
    List<String> mDefaultCardData = new ArrayList<>(Arrays.asList("wifi", "cell"));

    public CardTileEditPreference(@NonNull Context context) {
        this(context, null);
    }

    public CardTileEditPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardTileEditPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_card_tile);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mItemView = holder.itemView;
        mCardTiles = mItemView.findViewById(android.R.id.list);
        mAddCardTiles = mItemView.findViewById(R.id.card_tile_add_list);
        initView();
    }

    private void initView() {
        initCardData();
        mCardTileAdapter = new CardTileAdapter(mCardData);
        mAddCardTileAdapter = new CardTileAddAdapter(mAddCardData);

        createCardView(mCardTiles, mCardTileAdapter);
        createCardView(mAddCardTiles, mAddCardTileAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback());
        itemTouchHelper.attachToRecyclerView(mCardTiles);

        mCardTileAdapter.setOnDataChangeListener((changed, tile) -> {
            onDataSetChanged(tile, true);
        });

        mAddCardTileAdapter.setOnDataChangeListener((changed, tile) -> {
            onDataSetChanged(tile, false);
        });
        setVisibility(PrefsUtils.mSharedPreferences.getBoolean("prefs_key_systemui_plugin_card_tiles_enabled", false));
    }

    @Override
    public void onDependencyChanged(@NonNull Preference dependency, boolean disableDependent) {
        setVisibility(!disableDependent);
    }

    private void createCardView(RecyclerView cardView, RecyclerView.Adapter adapter) {
        mCradLayoutManager = new GridLayoutManager(getContext(), 2);
        cardView.setLayoutManager(mCradLayoutManager);
        cardView.setAdapter(adapter);
        cardView.setNestedScrollingEnabled(false);
    }

    public void setVisibility(boolean visibility) {
        if (mItemView != null) {
            mItemView.setVisibility(visibility ? View.VISIBLE : View.GONE);
        }
    }

    private List<String> getTileList() {
        String str = PrefsUtils.mSharedPreferences.getString("prefs_key_systemui_plugin_card_tiles", "").replace("List_", "");
        return TextUtils.isEmpty(str) ? new ArrayList<>() : Arrays.asList(str.split("\\|"));
    }

    private void initCardData() {
        String[] cardTileList = getContext().getResources().getStringArray(R.array.card_tile_list);
        mCardList = Arrays.asList(cardTileList);
        List<String> tiles = getTileList();
        if (!PrefsUtils.mSharedPreferences.getString("prefs_key_systemui_plugin_card_tiles", "").isEmpty()) {
            mCardData.clear();
            mCardData.addAll(tiles);
        } else {
            mCardData.addAll(mDefaultCardData);
        }
        if (mAddCardData.isEmpty()) {
            mAddCardData.addAll(mCardList);
        }
        mAddCardData.removeAll(mCardData);
    }

    private void onDataSetChanged(String tile, boolean isAdd) {
        if (isAdd) {
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
        String mCardStyleTiles = "List_" + builder;
        PrefsUtils.putString("prefs_key_systemui_plugin_card_tiles", mCardStyleTiles);
    }

    public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP|ItemTouchHelper.DOWN|ItemTouchHelper.START|ItemTouchHelper.END;
            int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = source.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            if (fromPosition < toPosition) {
                //分别把中间所有的item的位置重新交换
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(mCardData, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(mCardData, i, i - 1);
                }
            }
            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
            saveList();
            //返回true表示执行拖动
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }
    }
}
