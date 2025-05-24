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

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IAnimCallback extends IInterface {

    void onNextAminStart() throws RemoteException;

    void onBackAnimStart() throws RemoteException;

    class Default implements IAnimCallback {

        @Override
        public void onNextAminStart() throws RemoteException {

        }

        @Override
        public void onBackAnimStart() throws RemoteException {

        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    }

    abstract class Stub extends Binder implements IAnimCallback {
        private static final String DESCRIPTOR = "com.sevtinge.provision.utils.IAnimCallback";
        static final int TRANSACTION_onNextAminStart = 1;
        static final int TRANSACTION_onBackAnimStart = 2;

        @Override
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAnimCallback asInterface(IBinder iBinder) {
            if (iBinder != null) {
                IInterface iInterface = iBinder.queryLocalInterface(DESCRIPTOR);
                if (iInterface != null && (iInterface instanceof IAnimCallback)) {
                    return (IAnimCallback) iInterface;
                }
                return new Proxy(iBinder);
            } else {
                return null;
            }
        }

        @Override
        protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
            if (code == TRANSACTION_onNextAminStart) {
                data.enforceInterface(DESCRIPTOR);
                onNextAminStart();
                reply.writeNoException();
                return true;
            }
            if (code == TRANSACTION_onBackAnimStart) {
                data.enforceInterface(DESCRIPTOR);
                onBackAnimStart();
                reply.writeNoException();
                return true;
            } else if (code == 1598968902) {
                reply.writeString(DESCRIPTOR);
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IAnimCallback {
            public static IAnimCallback sDefaultImpl;
            private IBinder mRemote;

            Proxy(IBinder binder) {
                mRemote = binder;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public void onNextAminStart() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    if (!mRemote.transact(TRANSACTION_onNextAminStart, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onNextAminStart();
                    } else {
                        reply.readException();
                    }
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void onBackAnimStart() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    if (!mRemote.transact(TRANSACTION_onBackAnimStart, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onBackAnimStart();
                    } else {
                        reply.readException();
                    }
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IAnimCallback callback) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (callback == null) {
                return false;
            }
            Proxy.sDefaultImpl = callback;
            return true;
        }

        public static IAnimCallback getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
