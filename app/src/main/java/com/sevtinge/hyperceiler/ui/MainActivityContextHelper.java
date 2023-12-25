package com.sevtinge.hyperceiler.ui;

import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getLanguage;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.provider.Settings;

import com.sevtinge.hyperceiler.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
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

    public String getRandomTip() {
        AssetManager assetManager = context.getAssets();
        String fileName = "tips/tips-" + getLanguage();
        List<String> tipsList = new ArrayList<>();

        try {
            InputStream inputStream;
            try {
                inputStream = assetManager.open(fileName);
            } catch (IOException ex) {
                inputStream = assetManager.open("tips/tips");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                tipsList.add(line);
            }

            reader.close();
            inputStream.close();

            Random random = new Random();
            String randomTip = "";
            while (randomTip.isEmpty() && !tipsList.isEmpty()) {
                int randomIndex = random.nextInt(tipsList.size());
                randomTip = tipsList.get(randomIndex);
                tipsList.remove(randomIndex);
            }

            if (!randomTip.isEmpty()) {
                return randomTip;
            } else {
                return "Get random tip is empty.";
            }
        } catch (IOException e) {
            logE("MainActivityContextHelper", "getRandomTip() error: " + e.getMessage());
            return "error";
        }
    }
}
