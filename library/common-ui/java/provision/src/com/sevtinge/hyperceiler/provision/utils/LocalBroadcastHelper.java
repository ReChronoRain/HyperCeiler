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
package com.sevtinge.hyperceiler.provision.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;

public class LocalBroadcastHelper {

    private static final String TAG = "LocalBroadcastManager";
    private static LocalBroadcastHelper mInstance;
    private static final Object mLock = new Object();

    private final Context mAppContext;
    private final Handler mHandler;
    private final ArrayList<BroadcastRecord> mPendingBroadcasts = new ArrayList<>();
    private final HashMap<String, ArrayList<ReceiverRecord>> mActions = new HashMap<>();
    private final HashMap<BroadcastReceiver, ArrayList<IntentFilter>> mReceivers = new HashMap<>();


    public static LocalBroadcastHelper getInstance(Context context) {
        LocalBroadcastHelper localBroadcastHelper;
        synchronized (mLock) {
            try {
                if (mInstance == null) {
                    mInstance = new LocalBroadcastHelper(context.getApplicationContext());
                }
                localBroadcastHelper = mInstance;
            } catch (Throwable th) {
                throw th;
            }
        }
        return localBroadcastHelper;
    }

    private LocalBroadcastHelper(Context context) {
        mAppContext = context;
        mHandler = new Handler(context.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 1) {
                    executePendingBroadcasts();
                } else {
                    super.handleMessage(msg);
                }
            }
        };
    }

    public void registerReceiver(BroadcastReceiver receiver, IntentFilter intentFilter) {
        synchronized (mReceivers) {
            try {
                ReceiverRecord receiverRecord = new ReceiverRecord(intentFilter, receiver);
                ArrayList<IntentFilter> arrayList = mReceivers.get(receiver);
                if (arrayList == null) {
                    arrayList = new ArrayList<>(1);
                    mReceivers.put(receiver, arrayList);
                }
                arrayList.add(intentFilter);
                for (int i = 0; i < intentFilter.countActions(); i++) {
                    String action = intentFilter.getAction(i);
                    ArrayList<ReceiverRecord> arrayList2 = mActions.get(action);
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList<>(1);
                        mActions.put(action, arrayList2);
                    }
                    arrayList2.add(receiverRecord);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void unregisterReceiver(BroadcastReceiver broadcastReceiver) {
        synchronized (mReceivers) {
            try {
                ArrayList<IntentFilter> remove = mReceivers.remove(broadcastReceiver);
                if (remove != null) {
                    for (int i = 0; i < remove.size(); i++) {
                        IntentFilter intentFilter = remove.get(i);
                        for (int i2 = 0; i2 < intentFilter.countActions(); i2++) {
                            String action = intentFilter.getAction(i2);
                            ArrayList<ReceiverRecord> arrayList = this.mActions.get(action);
                            if (arrayList != null) {
                                int i3 = 0;
                                while (i3 < arrayList.size()) {
                                    if (arrayList.get(i3).mReceiver == broadcastReceiver) {
                                        arrayList.remove(i3);
                                        i3--;
                                    }
                                    i3++;
                                }
                                if (arrayList.size() <= 0) {
                                    mActions.remove(action);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void executePendingBroadcasts() {
        int size;
        BroadcastRecord[] broadcastRecords;
        while (true) {
            synchronized (mReceivers) {
                try {
                    size = mPendingBroadcasts.size();
                    if (size > 0) {
                        broadcastRecords = new BroadcastRecord[size];
                        mPendingBroadcasts.toArray(broadcastRecords);
                        mPendingBroadcasts.clear();
                    } else {
                        return;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            for (int i = 0; i < size; i++) {
                BroadcastRecord broadcastRecord = broadcastRecords[i];
                for (int i2 = 0; i2 < broadcastRecord.mReceivers.size(); i2++) {
                    broadcastRecord.mReceivers.get(i2).mReceiver.onReceive(mAppContext, broadcastRecord.mIntent);
                }
            }
        }
    }

    private static class BroadcastRecord {
        final Intent mIntent;
        final ArrayList<ReceiverRecord> mReceivers;

        BroadcastRecord(Intent intent, ArrayList<ReceiverRecord> receivers) {
            mIntent = intent;
            mReceivers = receivers;
        }
    }

    private static class ReceiverRecord {
        boolean broadcasting;
        final IntentFilter mFilter;
        final BroadcastReceiver mReceiver;

        ReceiverRecord(IntentFilter filter, BroadcastReceiver receiver) {
            mFilter = filter;
            mReceiver = receiver;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder(128);
            builder.append("Receiver{");
            builder.append(mReceiver);
            builder.append(" filter=");
            builder.append(mFilter);
            builder.append("}");
            return builder.toString();
        }
    }

}
