package com.sevtinge.cemiuiler.ui.systemui; 

 import android.widget.SeekBar;

 import androidx.fragment.app.Fragment;
  
 import com.sevtinge.cemiuiler.R;
 import com.sevtinge.cemiuiler.prefs.SeekBarPreferenceEx;
 import com.sevtinge.cemiuiler.ui.base.SubFragment;
 import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;

 import moralnorm.preference.SeekBarPreference;

public class HardwareActivity extends BaseSystemUIActivity {
     @Override 
     public Fragment initFragment() { 
         return new SystemUIHardwareFragment(); 
     } 
  
     public static class SystemUIHardwareFragment extends SubFragment {

         SeekBarPreferenceEx minBrightness;
         SeekBarPreferenceEx maxBrightness;

         @Override 
         public int getContentResId() { 
             return R.xml.system_ui_hardware; 
         }

         @Override
         public void initPrefs() {
             maxBrightness = findPreference("pref_key_system_ui_auto_brightness_max");
             minBrightness = findPreference("pref_key_system_ui_auto_brightness_min");
             assert minBrightness != null;
             minBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                 @Override
                 public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                     if (!fromUser) return;
                     if (maxBrightness.getValue() <= progress) maxBrightness.setValue(progress + 1);
                     maxBrightness.setMinValue(progress + 1);
                 }

                 @Override
                 public void onStartTrackingTouch(SeekBar seekBar) {}

                 @Override
                 public void onStopTrackingTouch(SeekBar seekBar) {}
             });
         }
     } 
 }