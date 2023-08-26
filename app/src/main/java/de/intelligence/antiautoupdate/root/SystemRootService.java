package de.intelligence.antiautoupdate.root;

import java.lang.reflect.Method;

import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.FileSystemManager;

import org.jetbrains.annotations.NotNull;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.NonNull;

import de.intelligence.antiautoupdate.ISystemRootService;

public final class SystemRootService extends RootService {

    private static final class SystemRootIPC extends ISystemRootService.Stub {

        @Override
        public IBinder getFileSystemService() {
            return FileSystemManager.getService();
        }

        @Override
        public void forceStopPackage(String packageName) throws RemoteException {
            try {
                final Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
                final Method getServiceImpl = activityManagerClass.getDeclaredMethod("getService");
                final Object activityManager = getServiceImpl.invoke(null);
                final Class<?> activityManagerStubProxyClass = activityManager.getClass();
                final Method forceStopPackageImpl = activityManagerStubProxyClass.getDeclaredMethod("forceStopPackage",
                        String.class, int.class);
                forceStopPackageImpl.invoke(activityManager, packageName, 0);
            } catch (Exception ex) {
                throw new RemoteException(ex.getMessage());
            }
        }

    }

    @Override
    public IBinder onBind(@NonNull @NotNull Intent intent) {
        return new SystemRootIPC();
    }

}
