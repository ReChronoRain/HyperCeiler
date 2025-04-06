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
package com.sevtinge.hyperceiler.ui.common.model.data;

import android.graphics.Bitmap;

public class AppData {
    public int user = 0;
    public Bitmap icon;
    public String label;
    public String packageName;
    public String activityName;
    public String versionName;
    public String versionCode;
    public boolean isSystemApp;
    public boolean enabled;
}
