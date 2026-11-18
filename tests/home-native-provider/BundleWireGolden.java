/* SPDX-License-Identifier: AGPL-3.0-or-later */
import android.os.Bundle;
import android.os.Parcel;
import android.graphics.Rect;

/** Execute through a separate app_process to compare the device's real wire producer. */
public final class BundleWireGolden {
    public static void main(String[] args) {
        Bundle bundle = new Bundle();
        if (args.length > 0) bundle.putParcelable("parcelable", new Rect(11, 22, 33, 44));
        else bundle.putBoolean("frameCommitted", false);
        Parcel parcel = Parcel.obtain();
        bundle.writeToParcel(parcel, 0);
        StringBuilder hex = new StringBuilder();
        for (byte value : parcel.marshall()) hex.append(String.format("%02x", value & 255));
        System.out.println("JAVA_POSITION=" + parcel.dataPosition() + " SIZE=" + parcel.dataSize());
        System.out.println("JAVA_BUNDLE_HEX=" + hex);
        parcel.recycle();
    }
}
