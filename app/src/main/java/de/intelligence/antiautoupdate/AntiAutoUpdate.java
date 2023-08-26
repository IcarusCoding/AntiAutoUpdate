package de.intelligence.antiautoupdate;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import rikka.sui.Sui;

public final class AntiAutoUpdate extends Application {

    public static final String LOG_TAG = "AntiAutoUpdate";
    public static final String VENDING_PACKAGE = "com.android.vending";

    private static final boolean insideSuiEnvironment;

    static {
        insideSuiEnvironment = Sui.init("de.intelligence.antiautoupdate");
    }

    public static boolean isInsideSuiEnvironment() {
        return AntiAutoUpdate.insideSuiEnvironment;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(LOG_TAG, "Created!");
        HiddenApiBypass.addHiddenApiExemptions();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

}
