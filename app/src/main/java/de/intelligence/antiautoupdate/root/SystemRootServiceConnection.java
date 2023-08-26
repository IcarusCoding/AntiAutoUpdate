package de.intelligence.antiautoupdate.root;

import com.topjohnwu.superuser.nio.FileSystemManager;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import de.intelligence.antiautoupdate.ISystemRootService;

public final class SystemRootServiceConnection implements ServiceConnection {

    public interface SystemRootServiceConnectionListener {

        void onRootServiceConnected(ComponentName componentName, ISystemRootService systemRootService, FileSystemManager fileSystemManager);

        void onRootServiceDisconnected(ComponentName componentName);

        void onRootServiceException(Throwable throwable);

    }

    private final SystemRootServiceConnection.SystemRootServiceConnectionListener listener;

    public SystemRootServiceConnection(SystemRootServiceConnection.SystemRootServiceConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        try {
            final ISystemRootService rootService = ISystemRootService.Stub.asInterface(service);
            final IBinder rootFileServiceBinder = rootService.getFileSystemService();
            this.listener.onRootServiceConnected(name, rootService, FileSystemManager.getRemote(rootFileServiceBinder));
        } catch (RemoteException ex) {
            this.listener.onRootServiceException(ex);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        this.listener.onRootServiceDisconnected(name);
    }

}
