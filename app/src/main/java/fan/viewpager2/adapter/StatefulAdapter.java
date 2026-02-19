package fan.viewpager2.adapter;

import android.os.Parcelable;
import android.view.View;

import androidx.annotation.NonNull;

import fan.viewpager2.widget.ViewPager2;

/**
 * {@link ViewPager2} adapters should implement this interface to be called during
 * {@link View#onSaveInstanceState()} and {@link View#onRestoreInstanceState(Parcelable)}
 */
public interface StatefulAdapter {
    /** Saves adapter state */
    @NonNull Parcelable saveState();

    /** Restores adapter state */
    void restoreState(@NonNull Parcelable savedState);
}