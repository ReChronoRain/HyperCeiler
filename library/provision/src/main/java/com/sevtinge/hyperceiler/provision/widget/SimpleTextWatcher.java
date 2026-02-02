package com.sevtinge.hyperceiler.provision.widget;

import android.text.Editable;
import android.text.TextWatcher;

public interface SimpleTextWatcher extends TextWatcher {

    @Override
    default void afterTextChanged(Editable s) {

    }

    @Override
    default void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }
}
