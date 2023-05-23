package com.sevtinge.cemiuiler.ui.systemui; 

 import androidx.fragment.app.Fragment; 
  
 import com.sevtinge.cemiuiler.R; 
 import com.sevtinge.cemiuiler.ui.base.SubFragment; 
 import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity; 
  
 public class HardwareActivity extends BaseSystemUIActivity { 
     @Override 
     public Fragment initFragment() { 
         return new SystemUIHardwareFragment(); 
     } 
  
     public static class SystemUIHardwareFragment extends SubFragment { 
  
         @Override 
         public int getContentResId() { 
             return R.xml.system_ui_hardware; 
         } 

     } 
 }