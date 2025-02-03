/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.ui.app.main.utils;

import com.xhinliang.lunarcalendar.Lunar;
import com.xhinliang.lunarcalendar.LunarCalendar;

import java.time.LocalDate;

public class PersistConfig {
    private static final LocalDate localDate = LocalDate.now();
    private static final LunarCalendar lunarCalender = LunarCalendar.obtainCalendar(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
    private static final Lunar lunar = lunarCalender.getLunar();

    public final static boolean isNeedGrayView = false;
    public final static boolean isLunarNewYearThemeView =
                    (lunar.month == 12 && 29 <= lunar.day && lunar.day <= 30) ||
                    (lunar.month == 1 && 1 <= lunar.day && lunar.day <= 15) ||
                    (localDate.getMonthValue() == 12 && localDate.getDayOfMonth() == 31) ||
                    (localDate.getMonthValue() == 1 && 1 <= localDate.getDayOfMonth() && localDate.getDayOfMonth() <= 5);
}
