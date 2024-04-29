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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.sevtinge.hyperceiler.R;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivityContextHelper {

    private Context context;

    public MainActivityContextHelper(Context context) {
        this.context = context;
    }

    @SuppressLint("HardwareIds")
    public String getAndroidId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public String getSHA256Signature() {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);

            byte[] cert = info.signingInfo.getApkContentsSigners()[0].toByteArray();

            MessageDigest md = MessageDigest.getInstance("SHA256");
            byte[] publicKey = md.digest(cert);
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                    .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                if (i < publicKey.length - 1) hexString.append(":");
            }
            return hexString.toString();
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isSignCheckPass(){
        ArrayList<String> signList = new ArrayList<>();
        signList.add("46:4C:5C:9D:A2:8C:AE:E6:B3:28:8D:AE:13:2C:A3:6D:52:A1:64:89:E0:95:CF:7B:52:AC:A7:11:F0:93:82:3C");
        signList.add("79:04:4B:BC:29:6B:E1:1A:9E:33:84:C4:91:F1:AD:D1:C0:CA:EE:CE:22:B9:24:FD:5B:7E:5A:14:C0:C3:99:60");
        signList.add("E8:52:B0:9F:FC:89:45:A3:26:39:70:54:FE:E0:1B:DC:10:9F:E5:1F:89:8E:20:E7:53:4D:BF:10:B3:06:2A:16");
        String sign = getSHA256Signature();
        if (signList.contains(sign)) {
            for (String element : signList) {
                if (!sign.equals(element)) {
                    break;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean verifyDexCRC() {
        String dexCrcStr = context.getResources().getString(R.string.crc_value);
        if(dexCrcStr.startsWith("Error")) return false;
        //long dexCrc = Long.parseLong("613BD799");

        //String orginalCrc = getString(R.string.str_code);
        ZipFile zf;
        try {
            zf = new ZipFile(context.getApplicationContext().getPackageCodePath());
            ZipEntry ze = zf.getEntry("classes.dex");
            long decCrc = ze.getCrc();
            String strCrc = Long.toHexString(decCrc);
            strCrc = strCrc.toUpperCase();
            //String MD5Crc = MD5Util.GetMD5Code(strCrc);
            //Log.e("checkcrc", MD5Crc);
            if (dexCrcStr.equals(strCrc)) {
                //ActivityManagerUtil.getScreenManager().removeAllActivity();
                return true;
            } else return false;
        } catch (IOException e) {
            return false;
        }
    }

    public String getDexCRC() {
        String dexCrcStr = context.getResources().getString(R.string.crc_value);
        if(dexCrcStr.startsWith("Error")) return dexCrcStr;
        //long dexCrc = Long.parseLong("613BD799");

        //String orginalCrc = getString(R.string.str_code);
        ZipFile zf;
        try {
            zf = new ZipFile(context.getApplicationContext().getPackageCodePath());
            ZipEntry ze = zf.getEntry("classes.dex");
            long decCrc = ze.getCrc();
            String strCrc = Long.toHexString(decCrc);
            strCrc = strCrc.toUpperCase();
            //String MD5Crc = MD5Util.GetMD5Code(strCrc);
            //Log.e("checkcrc", MD5Crc);
            if (dexCrcStr.equals(strCrc)) {
                //ActivityManagerUtil.getScreenManager().removeAllActivity();
                return dexCrcStr + " = " + strCrc;
            } else return dexCrcStr + " != " + strCrc;
        } catch (IOException e) {
            return e.toString();
        }
    }
}
