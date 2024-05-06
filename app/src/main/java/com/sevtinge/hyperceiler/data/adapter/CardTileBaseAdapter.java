package com.sevtinge.hyperceiler.data.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.viewholder.CardTileViewHolder;

import java.util.List;

public abstract class CardTileBaseAdapter extends RecyclerView.Adapter<CardTileViewHolder> {

    protected List<String> mData;
    protected OnDataChangeListener onDataChangeListener;

    public CardTileBaseAdapter(List<String> data) {
        mData = data;
    }

    @NonNull
    @Override
    public CardTileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CardTileViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_tile, parent, false));
    }

    public int getCardIcon(Context context, String tile) {
        String[] cardTileList = context.getResources().getStringArray(R.array.card_tile_list);
        for (int i = 0; i < cardTileList.length; i++) {
            String title = cardTileList[i];
            if (title.equals(tile)) {
                return obtainTypedArray(context, R.array.card_tile_icon_list, i);
            }
        }
        return 0;
    }

    public String getCardTitle(Context context, String tile) {
        String[] cardTileList = context.getResources().getStringArray(R.array.card_tile_list);
        String[] cardTileTitleList = context.getResources().getStringArray(R.array.card_tile_title_list);
        for (int i = 0; i < cardTileList.length; i++) {
            String title = cardTileList[i];
            if (title.equals(tile)) {
                return cardTileTitleList[i];
            }
        }
        return tile;
    }

    public int obtainTypedArray(Context context, @ArrayRes int id, int position) {
        return context.getResources().obtainTypedArray(id).getResourceId(position, 0);
    }

    @Override
    public int getItemCount() {
        return !mData.isEmpty() ? mData.size() : 0;
    }

    public void setOnDataChangeListener(OnDataChangeListener listener) {
        onDataChangeListener = listener;
    }

    public interface OnDataChangeListener {
        void onDataSetChanged(boolean changed, String tile);
    }
}
