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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.main.page;

import com.sevtinge.hyperceiler.main.fragment.ContentFragment.IFragmentChange;
import com.sevtinge.hyperceiler.main.fragment.PageFragment;
import com.sevtinge.hyperceiler.main.page.settings.SettingsFragment;

import fan.appcompat.app.ActionBar;
import fan.preference.PreferenceFragment;

public class SettingsPage extends PageFragment implements IFragmentChange {

    @Override
    public PreferenceFragment getPreferenceFragment() {
        return new SettingsFragment();
    }

    @Override
    public void onEnter(ActionBar actionBar) {}

    @Override
    public void onLeave(ActionBar actionBar) {}
}
