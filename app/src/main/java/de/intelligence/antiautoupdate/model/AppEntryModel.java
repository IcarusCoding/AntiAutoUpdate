package de.intelligence.antiautoupdate.model;

import android.graphics.drawable.Drawable;

public final class AppEntryModel {

    private final String appName;
    private final Drawable appIcon;
    private final String packageName;
    private boolean excluded;

    public AppEntryModel(String appName, Drawable appIcon, String packageName, boolean excluded) {
        this.appName = appName;
        this.appIcon = appIcon;
        this.packageName = packageName;
        this.excluded = excluded;
    }

    public String getAppName() {
        return this.appName;
    }

    public Drawable getAppIcon() {
        return this.appIcon;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public boolean isExcluded() {
        return this.excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }

}
