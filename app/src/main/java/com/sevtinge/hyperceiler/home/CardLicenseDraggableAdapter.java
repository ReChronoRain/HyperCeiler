package com.sevtinge.hyperceiler.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;

import java.util.List;

public class CardLicenseDraggableAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public Context mContext;
    public List<Header> mCustomOrderItems;

    public CardLicenseDraggableAdapter(Context context, List<Header> headers) {
        mContext = context;
        mCustomOrderItems = headers;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1) {
            return new RestoreButtonViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_custom_order_restore_item, parent, false));
        }
        return new DraggableItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_custom_order_draggable_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == 1) {
            ((RestoreButtonViewHolder) holder).restoreButton.setOnClickListener(v -> {

            });
        } else {
            ((DraggableItemViewHolder) holder).bind(mCustomOrderItems.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mCustomOrderItems.size();
    }


    public class DraggableItemViewHolder extends RecyclerView.ViewHolder {

        public View mRootview;
        public ImageButton moveButton;
        public TextView titleView;

        public DraggableItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mRootview = itemView;
            titleView = itemView.findViewById(R.id.title_text_view);
            moveButton = itemView.findViewById(R.id.move_button);
        }

        public void bind(Header header) {
            titleView.setText(header.title);
            moveButton.setContentDescription(header.title);
            mRootview.setContentDescription(header.title);
        }
    }

    public class RestoreButtonViewHolder extends RecyclerView.ViewHolder {

        public TextView restoreButton;

        public RestoreButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            restoreButton = itemView.findViewById(R.id.restore_button);
        }
    }

    public class CardLicenseOrderDiffCallback extends DiffUtil.Callback {
        public final List<Header> newList;
        public final List<Header> oldList;

        public CardLicenseOrderDiffCallback(List<Header> list, List<Header> list2) {
            this.oldList = list;
            this.newList = list2;
        }

        public int getOldListSize() {
            return this.oldList.size();
        }

        public int getNewListSize() {
            return this.newList.size();
        }

        public boolean areItemsTheSame(int i, int i2) {
            return this.oldList.get(i).equals(this.newList.get(i2));
        }

        public boolean areContentsTheSame(int i, int i2) {
            return this.oldList.get(i).equals(this.newList.get(i2));
        }
    }

}
