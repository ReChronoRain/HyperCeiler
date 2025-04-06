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
package com.sevtinge.hyperceiler.hook.utils.prefs;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class PrefsMap<K, V> extends HashMap<K, V> {

    public Object getObject(String key, Object defValue) {
        return get(key) == null ? defValue : get(key);
    }

    public int getInt(String key, int defValue) {
        key = "prefs_key_" + key;
        return get(key) == null ? defValue : (Integer) get(key);
    }

    public String getString(String key, String defValue) {
        key = "prefs_key_" + key;
        return get(key) == null ? defValue : (String) get(key);
    }

    public int getStringAsInt(String key, int defValue) {
        key = "prefs_key_" + key;
        return get(key) == null ? defValue : Integer.parseInt((String) get(key));
    }

    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key) {
        key = "prefs_key_" + key;
        return get(key) == null ? new LinkedHashSet<>() : (Set<String>) get(key);
    }

    public boolean getBoolean(String key) {
        key = "prefs_key_" + key;
        return get(key) != null && (Boolean) get(key);
    }

}
