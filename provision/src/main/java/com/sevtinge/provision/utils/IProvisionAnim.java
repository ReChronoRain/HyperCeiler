package com.sevtinge.provision.utils;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IProvisionAnim extends IInterface {

    boolean isAnimEnd() throws RemoteException;

    void playNextAnim(int i) throws RemoteException;

    void playBackAnim(int i) throws RemoteException;

    void registerRemoteCallback(IAnimCallback callback) throws RemoteException;

    void unregisterRemoteCallback(IAnimCallback callback) throws RemoteException;

    class Default implements IProvisionAnim {

        @Override
        public boolean isAnimEnd() throws RemoteException {
            return false;
        }

        @Override
        public void playNextAnim(int i) throws RemoteException {

        }

        @Override
        public void playBackAnim(int i) throws RemoteException {

        }

        @Override
        public void registerRemoteCallback(IAnimCallback callback) throws RemoteException {

        }

        @Override
        public void unregisterRemoteCallback(IAnimCallback callback) throws RemoteException {

        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    }

    abstract class Stub extends Binder implements IProvisionAnim {

        private static final String DESCRIPTOR = "com.sevtinge.provision.utils.IProvisionAnim";
        static final int TRANSACTION_playNextAnim = 1;
        static final int TRANSACTION_playBackAnim = 2;
        static final int TRANSACTION_isAnimEnd = 3;
        static final int TRANSACTION_registerRemoteCallback = 4;
        static final int TRANSACTION_unregisterRemoteCallback = 5;

        @Override
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IProvisionAnim asInterface(IBinder service) {
            if (service != null) {
                IInterface iInterface = service.queryLocalInterface(DESCRIPTOR);
                if (iInterface != null && (iInterface instanceof IProvisionAnim)) {
                    return (IProvisionAnim) iInterface;
                }
                return new Proxy(service);
            } else {
                return null;
            }
        }

        @Override
        protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case TRANSACTION_playNextAnim -> {
                    data.enforceInterface(DESCRIPTOR);
                    playNextAnim(data.readInt());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_playBackAnim -> {
                    data.enforceInterface(DESCRIPTOR);
                    playBackAnim(data.readInt());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_isAnimEnd -> {
                    data.enforceInterface(DESCRIPTOR);
                    boolean isAnimEnd = isAnimEnd();
                    reply.writeNoException();
                    reply.writeInt(isAnimEnd ? 1 : 0);
                    return true;
                }
                case TRANSACTION_registerRemoteCallback -> {
                    data.enforceInterface(DESCRIPTOR);
                    registerRemoteCallback(IAnimCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
                case 5 -> {
                    data.enforceInterface(DESCRIPTOR);
                    unregisterRemoteCallback(IAnimCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
                case 1598968902 -> {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                default -> {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

        private static class Proxy implements IProvisionAnim {
            public static IProvisionAnim sDefaultImpl;
            private IBinder mRemote;

            Proxy(IBinder service) {
                mRemote = service;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public void playNextAnim(int i) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(i);
                    if (!mRemote.transact(1, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().playNextAnim(i);
                    } else {
                        reply.readException();
                    }
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void playBackAnim(int i) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(i);
                    if (!mRemote.transact(2, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().playBackAnim(i);
                    } else {
                        reply.readException();
                    }
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean isAnimEnd() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    if (!mRemote.transact(3, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isAnimEnd();
                    }
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void registerRemoteCallback(IAnimCallback iAnimCallback) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeStrongBinder(iAnimCallback != null ? iAnimCallback.asBinder() : null);
                    if (!mRemote.transact(4, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().registerRemoteCallback(iAnimCallback);
                        reply.recycle();
                        data.recycle();
                    } else {
                        reply.readException();
                        reply.recycle();
                        data.recycle();
                    }
                } catch (Throwable th) {
                    reply.recycle();
                    data.recycle();
                    throw th;
                }
            }

            @Override
            public void unregisterRemoteCallback(IAnimCallback iAnimCallback) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeStrongBinder(iAnimCallback != null ? iAnimCallback.asBinder() : null);
                    if (!mRemote.transact(5, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().unregisterRemoteCallback(iAnimCallback);
                        reply.recycle();
                        data.recycle();
                    } else {
                        reply.readException();
                        reply.recycle();
                        data.recycle();
                    }
                } catch (Throwable th) {
                    reply.recycle();
                    data.recycle();
                    throw th;
                }
            }
        }

        public static boolean setDefaultImpl(IProvisionAnim iProvisionAnim) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (iProvisionAnim == null) {
                return false;
            }
            Proxy.sDefaultImpl = iProvisionAnim;
            return true;
        }

        public static IProvisionAnim getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
