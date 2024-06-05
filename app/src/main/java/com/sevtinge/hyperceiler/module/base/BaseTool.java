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
package com.sevtinge.hyperceiler.module.base;

import com.hchen.hooktool.HCHook;
import com.hchen.hooktool.tool.ClassTool;
import com.hchen.hooktool.tool.DexkitTool;
import com.hchen.hooktool.tool.ExpandTool;
import com.hchen.hooktool.tool.FieldTool;
import com.hchen.hooktool.tool.MethodTool;

public abstract class BaseTool extends BaseHook {
    public static HCHook hcHook;
    public static ClassTool classTool;
    public static MethodTool methodTool;
    public static FieldTool fieldTool;
    public static DexkitTool dexkitTool;
    public static ExpandTool expandTool;

    public abstract void doHook();

    @Override
    public void init() {
        BaseTool.hcHook = new HCHook();
        BaseTool.classTool = hcHook.classTool();
        BaseTool.methodTool = hcHook.methodTool();
        BaseTool.fieldTool = hcHook.fieldTool();
        BaseTool.dexkitTool = hcHook.dexkitTool();
        BaseTool.expandTool = hcHook.expandTool();
        BaseTool.hcHook.setThisTag(TAG);
        doHook();
    }
}
