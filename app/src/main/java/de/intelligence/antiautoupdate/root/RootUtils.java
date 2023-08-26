package de.intelligence.antiautoupdate.root;

import java.util.function.Consumer;

import com.topjohnwu.superuser.Shell;

public final class RootUtils {

    private RootUtils() {}

    static {
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER));
    }

    public static void executeIfRooted(Consumer<Shell> ifRooted) {
        if (checkRootState() == RootState.ALLOWED) {
            Shell.getShell(ifRooted::accept);
        }
    }

    public static void executeIfRootedOrElse(Consumer<Shell> ifRooted, Runnable notRooted) {
        if (checkRootState() == RootState.ALLOWED) {
            Shell.getShell(ifRooted::accept);
        } else {
            notRooted.run();
        }
    }

    public static RootState checkRootState() {
        final Boolean rootState = Shell.isAppGrantedRoot();
        if (rootState == null) {
            return RootState.UNKNOWN;
        }
        if (!rootState) {
            return RootState.DENIED;
        }
        return RootState.ALLOWED;
    }

}
