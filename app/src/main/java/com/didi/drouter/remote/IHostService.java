/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.didi.drouter.remote;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IHostService extends IInterface {
    RemoteResult execute(RemoteCommand var1) throws RemoteException;

    public abstract static class Stub extends Binder implements IHostService {
        private static final String DESCRIPTOR = "com.didi.drouter.remote.IHostService";
        static final int TRANSACTION_execute = 1;

        public Stub() {
            this.attachInterface(this, "com.didi.drouter.remote.IHostService");
        }

        public static IHostService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            } else {
                IInterface iin = obj.queryLocalInterface("com.didi.drouter.remote.IHostService");
                return (IHostService)(iin != null && iin instanceof IHostService ? (IHostService)iin : new IHostService.Stub.Proxy(obj));
            }
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = "com.didi.drouter.remote.IHostService";
            switch(code) {
            case 1:
                data.enforceInterface(descriptor);
                RemoteCommand _arg0;
                if (0 != data.readInt()) {
                    _arg0 = (RemoteCommand)RemoteCommand.CREATOR.createFromParcel(data);
                } else {
                    _arg0 = null;
                }

                RemoteResult _result = this.execute(_arg0);
                reply.writeNoException();
                if (_result != null) {
                    reply.writeInt(1);
                    _result.writeToParcel(reply, 1);
                } else {
                    reply.writeInt(0);
                }

                return true;
            case 1598968902:
                reply.writeString(descriptor);
                return true;
            default:
                return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IHostService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return "com.didi.drouter.remote.IHostService";
            }

            public RemoteResult execute(RemoteCommand command) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();

                RemoteResult _result;
                try {
                    _data.writeInterfaceToken("com.didi.drouter.remote.IHostService");
                    if (command != null) {
                        _data.writeInt(1);
                        command.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }

                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (0 != _reply.readInt()) {
                        _result = (RemoteResult)RemoteResult.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }

                return _result;
            }
        }
    }
}
