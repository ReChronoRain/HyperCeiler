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
import com.hchen.hooktool.tool.ExpandTool;
import com.hchen.hooktool.tool.FieldTool;
import com.hchen.hooktool.tool.MethodTool;

public abstract class BaseTool extends BaseHook {
    public HCHook hcHook;
    public ClassTool classTool;
    public MethodTool methodTool;
    public FieldTool fieldTool;
    public ExpandTool expandTool;

    public abstract void doHook();

    @Override
    public void init() {
        hcHook = new HCHook();
        classTool = hcHook.classTool();
        methodTool = hcHook.methodTool();
        fieldTool = hcHook.fieldTool();
        expandTool = hcHook.expandTool();
        hcHook.setThisTag(TAG);
        doHook();
    }
}
