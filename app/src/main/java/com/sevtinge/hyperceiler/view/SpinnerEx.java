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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

import moralnorm.appcompat.widget.Spinner;


public class SpinnerEx extends Spinner {

    public CharSequence[] entries;
    public int[] entryValues;
    private final ArrayList<Integer> disabledItems = new ArrayList<>();

    public SpinnerEx(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray xmlAttrs = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.entries, 0});
        entries = xmlAttrs.getTextArray(0);
        if (xmlAttrs.getResourceId(1, 0) != 0) entryValues = getResources().getIntArray(xmlAttrs.getResourceId(1, 0));
        xmlAttrs.recycle();
    }

    private int findIndex(int val, int[] vals) {
        for (int i = 0; i < vals.length; i++)
            if (vals[i] == val) return i;
        return -1;
    }

    public void init(int val) {
        if (entries == null || entryValues == null) return;
        ArrayAdapterEx newAdapter = new ArrayAdapterEx(getContext(), android.R.layout.simple_spinner_item, entries);
        setAdapter(newAdapter);
        setSelection(findIndex(val, entryValues));
    }

    public void addDisabledItems(int item) {
        disabledItems.add(item);
    }

    public int getSelectedArrayValue() {
        return entryValues[getSelectedItemPosition()];
    }

    class ArrayAdapterEx extends ArrayAdapter<CharSequence> {

        ArrayAdapterEx(Context context, int resource, CharSequence[] objects) {
            super(context, resource, objects);
        }

        @Override
        public boolean isEnabled(int position) {
            return !disabledItems.contains(position);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            view.setEnabled(isEnabled(position));
            return view;
        }
    }
}
