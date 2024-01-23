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
package com.sevtinge.hyperceiler.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.customhook.CustomHookConfigActivity;

import moralnorm.appcompat.app.AppCompatActivity;

public class CustomHookActivity extends AppCompatActivity {

    Button mAddConfig;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_custom_hook);
        initView();
    }

    private void initView() {
        mAddConfig = findViewById(R.id.add_config);

        mAddConfig.setOnClickListener(v -> startActivity(new Intent(this, CustomHookConfigActivity.class)));
    }
}
