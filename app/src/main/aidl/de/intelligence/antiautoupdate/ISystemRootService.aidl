package de.intelligence.antiautoupdate;

interface ISystemRootService {

    IBinder getFileSystemService();

    void forceStopPackage(String packageName);

}
