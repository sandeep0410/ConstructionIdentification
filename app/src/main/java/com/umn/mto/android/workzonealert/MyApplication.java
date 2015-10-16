package com.umn.mto.android.workzonealert;

import android.app.Application;

public class MyApplication extends Application {
    private static boolean activityVisible = false;

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void onResumeCalled() {
        activityVisible = true;
    }

    public static void onPauseCalled() {
        activityVisible = false;
    }
}
