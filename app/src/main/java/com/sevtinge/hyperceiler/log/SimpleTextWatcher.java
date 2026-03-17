package com.sevtinge.hyperceiler.log;

import android.text.TextWatcher;

public interface SimpleTextWatcher extends TextWatcher {

    @Override
    default void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    default void onTextChanged(CharSequence s, int start, int before, int count) {

    }
}
