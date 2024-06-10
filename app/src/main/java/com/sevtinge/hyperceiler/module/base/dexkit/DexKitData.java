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
package com.sevtinge.hyperceiler.module.base.dexkit;

import java.util.ArrayList;

/**
 * dexkit 数据转为 JSON 储存
 */
public class DexKitData {
    public static final String EMPTY = "";
    public static final ArrayList<String> EMPTYLIST = new ArrayList<>();
    // public String label;
    public final String tag;
    public final String type;
    public final String clazz;
    public final String method;
    public final ArrayList<String> param;
    public final String field;

    public DexKitData(String tag, String type, String clazz,
                      String method, ArrayList<String> param,
                      String field) {
        // label = l;
        this.tag = tag;
        this.type = type;
        this.clazz = clazz;
        this.method = method;
        this.param = param;
        this.field = field;
    }
}
