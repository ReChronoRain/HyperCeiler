/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.os.Parcel;

/** The unchanged HCL4 payload shared by the WMS compatibility route and local JNI publisher. */
public final class HomeLayoutWire {
    private static String lastMediumDiagnostic;
    private HomeLayoutWire() { }

    public static void write(Parcel reply, HomeLayoutNativeEndpointOS4.Snapshot value,
                             int[] gestures, int publisherPid) {
        reply.writeInt(value == null ? HomeLayoutNativeEndpointOS4.ACK : value.acknowledgment());
        reply.writeInt(value == null ? 0 : value.gridEnabled());
        reply.writeInt(value == null ? 0 : value.cellX());
        reply.writeInt(value == null ? 0 : value.cellY());
        int[] enabled = value == null || value.knobEnabled() == null ? new int[0] : value.knobEnabled();
        int[] deltas = value == null || value.knobDeltaDp() == null ? new int[0] : value.knobDeltaDp();
        reply.writeInt(enabled.length);
        for (int i = 0; i < enabled.length; ++i) {
            reply.writeInt(enabled[i]);
            reply.writeInt(i < deltas.length ? deltas[i] : 0);
        }
        int[] tweaks = value == null || value.tweaks() == null ? new int[0] : value.tweaks();
        for (int i = 0; i < HomeLayoutNativeEndpointOS4.TWEAK_COUNT; ++i)
            reply.writeInt(i < tweaks.length ? tweaks[i] : 0);
        int[] title = value == null || value.title() == null ? new int[]{12, 12, -1, 0} : value.title();
        for (int part : title) reply.writeInt(part);
        reply.writeInt(0x48435431);
        String[][] labels = value == null || value.customTitles() == null ? new String[0][] : value.customTitles();
        reply.writeInt(labels.length);
        for (String[] pair : labels) for (String text : pair) {
            reply.writeInt(text.length());
            for (int i = 0; i < text.length(); ++i) reply.writeInt(text.charAt(i));
        }
        reply.writeInt(0x48435731);
        reply.writeInt(value != null && value.widgetAllowMove() ? 1 : 0);
        reply.writeInt(HomeLayoutNativeEndpointOS4.FOLDER_MAGIC);
        int[] folder = value == null || value.folder() == null
            ? new int[HomeLayoutNativeEndpointOS4.FOLDER_COUNT] : value.folder();
        for (int part : folder) reply.writeInt(part);
        reply.writeInt(HomeLayoutNativeEndpointOS4.BACK_GESTURE_MAGIC);
        int[] back = value == null || value.backGesture() == null ? new int[]{60, 100} : value.backGesture();
        for (int part : back) reply.writeInt(part);
        reply.writeInt(HomeLayoutNativeEndpointOS4.FOLDER_AUTO_CLOSE_MAGIC);
        reply.writeInt(value != null && value.folderAutoClose() ? 1 : 0);
        reply.writeInt(0x48434731);
        for (int part : gestures) reply.writeInt(part);
        reply.writeInt(HomeLayoutNativeEndpointOS4.OBJECT_SCALE_MAGIC);
        int[] scales = value == null || value.objectScales() == null
            ? new int[]{0, 100, 0, 100, 0, 100} : value.objectScales();
        for (int i=0;i<6;++i) reply.writeInt(scales[i]);
        reply.writeInt(HomeLayoutNativeEndpointOS4.ENDPOINT_DIAGNOSTICS_MAGIC);
        reply.writeInt(HomeLayoutNativeEndpointOS4.ENDPOINT_DIAGNOSTICS_SCHEMA);
        reply.writeInt(HomeLayoutNativeEndpointOS4.ENDPOINT_REVISION);
        reply.writeInt(HomeLayoutNativeEndpointOS4.ENDPOINT_CAPABILITIES);
        reply.writeInt(publisherPid);
        reply.writeInt(HomeLayoutNativeEndpointOS4.DOCK_TITLE_MAGIC);
        reply.writeInt(value != null && value.dockShowTitle() ? 1 : 0);
        int[] recent = value == null || value.recent() == null
            ? HomeLayoutNativeEndpointOS4.recentDefaults() : value.recent();
        HomeLayoutNativeEndpointOS4.writeRecent(reply, recent);
        HomeLayoutNativeEndpointOS4.writeDrawer(reply, value != null && value.drawerHideSearch());
        HomeLayoutNativeEndpointOS4.writePolicies(reply, value == null ? 0L : value.policies());
        HomeLayoutNativeEndpointOS4.writeDockHorizontal(reply, value == null ? 0 : value.dockHorizontalDelta());
        HomeLayoutNativeEndpointOS4.writeInfiniteScroll(reply, value != null && value.infiniteScroll());
        HomeLayoutNativeEndpointOS4.writeRecentHeader(reply, recent);
        reply.writeInt(0x48434d53);
        for(int i=6;i<10;++i)reply.writeInt(i<scales.length?scales[i]:(i%2==0?0:100));
        String medium=java.util.Arrays.toString(scales);
        if(!medium.equals(lastMediumDiagnostic)) {
            lastMediumDiagnostic=medium;android.util.Log.i("HyperCeiler.HomeConfig","wire scales="+medium);
        }
    }
}
