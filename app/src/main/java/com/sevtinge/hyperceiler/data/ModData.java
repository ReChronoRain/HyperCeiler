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
package com.sevtinge.hyperceiler.data;

public class ModData {
    public String title;
    public String breadcrumbs;
    public int xml;
    public String key;
    public ModCat cat;
    public String sub;
    public int order;
    public String fragment;
    public int catTitleResId;

    public enum ModCat {
        pref_key_system,
        pref_key_launcher,
        pref_key_controls,
        pref_key_various
    }
}
